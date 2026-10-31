package com.nsr.qilin.data.service.detector

import android.util.Log
import com.nsr.qilin.DownloadQualityOption
import com.nsr.qilin.data.service.downloader.HlsPlaylistParser
import com.nsr.qilin.data.service.downloader.applySafeHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Intelligent Website Video Detector ported from super-video-downloader.
 *
 * Automatically inspects intercepted WebView requests, ServiceWorker streams, and DOM media tags.
 * Discovers progressive media, HLS (.m3u8), and DASH (.mpd) playlists, resolves CDN cookies,
 * and parses multi-resolution variants (1080p, 720p, 480p, 360p) in real time.
 */
class WebsiteVideoDetector(
    private val client: OkHttpClient = defaultClient(),
    private val detectorScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {
        private const val TAG = "WebsiteVideoDetector"

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }

        private fun log(msg: String) {
            try {
                Log.d(TAG, msg)
            } catch (ignored: Throwable) {}
        }
    }

    private val inspectedUrls = ConcurrentHashMap.newKeySet<String>()

    private val _detectedVideos = MutableStateFlow<List<DetectedVideo>>(emptyList())
    val detectedVideos: StateFlow<List<DetectedVideo>> = _detectedVideos.asStateFlow()

    private var onVideoDetectedCallback: ((DetectedVideo) -> Unit)? = null

    fun setOnVideoDetectedListener(listener: ((DetectedVideo) -> Unit)?) {
        this.onVideoDetectedCallback = listener
    }

    fun reset() {
        inspectedUrls.clear()
        _detectedVideos.value = emptyList()
    }

    fun inspectUrlAsync(
        url: String,
        requestHeaders: Map<String, String> = emptyMap(),
        pageUrl: String? = null,
        pageTitle: String? = null
    ) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || VideoUtils.isStaticOrNonMediaUrl(cleanUrl)) {
            return
        }

        if (!inspectedUrls.add(cleanUrl)) {
            return
        }

        detectorScope.launch {
            try {
                inspectUrl(cleanUrl, requestHeaders, pageUrl, pageTitle)
            } catch (e: Throwable) {
                log("Inspection error for $cleanUrl: ${e.message}")
            }
        }
    }

    fun inspectUrl(
        url: String,
        requestHeaders: Map<String, String> = emptyMap(),
        pageUrl: String? = null,
        pageTitle: String? = null
    ): DetectedVideo? {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || VideoUtils.isStaticOrNonMediaUrl(cleanUrl)) {
            return null
        }

        val referer = pageUrl ?: requestHeaders["Referer"] ?: requestHeaders["referer"]
        val cookies = CookieUtils.getAggregatedCookies(cleanUrl, referer)

        val headers = requestHeaders.toMutableMap()
        if (cookies.isNotEmpty() && !headers.containsKey("Cookie")) {
            headers["Cookie"] = cookies
        }
        if (!referer.isNullOrEmpty() && !headers.containsKey("Referer") && !headers.containsKey("referer")) {
            headers["Referer"] = referer
        }

        val contentType = VideoUtils.probeContentType(cleanUrl, headers, client)
        if (contentType == ContentType.OTHER) {
            return null
        }

        val detected = when (contentType) {
            ContentType.M3U8 -> handleM3u8Detected(cleanUrl, headers, pageTitle)
            ContentType.MPD -> handleMpdDetected(cleanUrl, headers, pageTitle)
            ContentType.VIDEO -> handleProgressiveMediaDetected(cleanUrl, headers, pageTitle, isAudio = false)
            ContentType.AUDIO -> handleProgressiveMediaDetected(cleanUrl, headers, pageTitle, isAudio = true)
            ContentType.OTHER -> null
        }

        if (detected != null) {
            pushDetectedVideo(detected)
        }
        return detected
    }

    private fun handleM3u8Detected(
        url: String,
        headers: Map<String, String>,
        pageTitle: String?
    ): DetectedVideo {
        log("M3U8 Stream Detected: $url")
        var manifestContent: String? = null
        try {
            val req = Request.Builder().url(url).applySafeHeaders(headers).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    manifestContent = response.body.string()
                }
            }
        } catch (e: Throwable) {
            log("Failed to fetch manifest content: ${e.message}")
        }

        val parsed = manifestContent?.let { content ->
            try {
                HlsPlaylistParser.parse(content, url)
            } catch (ignored: Throwable) {
                null
            }
        }

        val variants = mutableListOf<VideoQualityVariant>()
        var durationSec = 0.0

        when (parsed) {
            is HlsPlaylistParser.ParsedHlsResult.Master -> {
                parsed.master.variants.forEach { v ->
                    val h = v.resolution?.substringAfter('x')?.toIntOrNull()
                    val badge = if (h != null && h > 0) "${h}p" else {
                        when {
                            v.bandwidth >= 4_000_000 -> "1080p"
                            v.bandwidth >= 2_000_000 -> "720p"
                            v.bandwidth >= 1_000_000 -> "480p"
                            else -> "360p"
                        }
                    }
                    variants.add(
                        VideoQualityVariant(
                            url = v.url,
                            quality = badge,
                            bandwidth = v.bandwidth,
                            resolution = v.resolution,
                            codecs = v.codecs
                        )
                    )
                }
            }
            is HlsPlaylistParser.ParsedHlsResult.Media -> {
                durationSec = parsed.media.totalDuration
                val guessedQuality = guessQualityFromUrl(url)
                variants.add(
                    VideoQualityVariant(
                        url = url,
                        quality = guessedQuality ?: "AUTO"
                    )
                )
            }
            null -> {
                val guessedQuality = guessQualityFromUrl(url)
                variants.add(
                    VideoQualityVariant(
                        url = url,
                        quality = guessedQuality ?: "AUTO"
                    )
                )
            }
        }

        val primaryQuality = variants.firstOrNull()?.quality ?: "HLS"
        return DetectedVideo(
            url = url,
            title = pageTitle ?: "HLS Video",
            contentType = ContentType.M3U8,
            quality = primaryQuality,
            durationSec = durationSec,
            headers = headers,
            variants = variants.sortedByDescending { it.bandwidth }
        )
    }

    private fun handleMpdDetected(
        url: String,
        headers: Map<String, String>,
        pageTitle: String?
    ): DetectedVideo {
        log("MPD Stream Detected: $url")
        return DetectedVideo(
            url = url,
            title = pageTitle ?: "DASH Video",
            contentType = ContentType.MPD,
            quality = "DASH",
            headers = headers
        )
    }

    private fun handleProgressiveMediaDetected(
        url: String,
        headers: Map<String, String>,
        pageTitle: String?,
        isAudio: Boolean
    ): DetectedVideo {
        log("Progressive ${if (isAudio) "Audio" else "Video"} Stream Detected: $url")
        var contentLength = 0L
        try {
            val req = Request.Builder().url(url).applySafeHeaders(headers).head().build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
                }
            }
        } catch (ignored: Throwable) {}

        val guessedQuality = guessQualityFromUrl(url)
        val qualityBadge = if (isAudio) "AUDIO" else (guessedQuality ?: "DIRECT")

        return DetectedVideo(
            url = url,
            title = pageTitle ?: if (isAudio) "Audio Stream" else "Video Stream",
            contentType = if (isAudio) ContentType.AUDIO else ContentType.VIDEO,
            quality = qualityBadge,
            contentLength = contentLength,
            headers = headers,
            variants = if (!isAudio) {
                listOf(VideoQualityVariant(url = url, quality = qualityBadge))
            } else emptyList()
        )
    }

    private fun pushDetectedVideo(video: DetectedVideo) {
        val currentList = _detectedVideos.value
        if (currentList.any { it.url == video.url }) {
            return
        }

        val updated = currentList + video
        _detectedVideos.value = updated
        try {
            onVideoDetectedCallback?.invoke(video)
        } catch (ignored: Throwable) {}
    }

    private fun guessQualityFromUrl(url: String): String? {
        val clean = url.lowercase()
        return when {
            clean.contains("1080") -> "1080p"
            clean.contains("720") -> "720p"
            clean.contains("480") -> "480p"
            clean.contains("360") -> "360p"
            clean.contains("240") -> "240p"
            else -> null
        }
    }

    fun toQualityOptions(
        detected: DetectedVideo,
        currentPlaybackHeight: Int = 0
    ): List<DownloadQualityOption> {
        if (detected.variants.isNotEmpty()) {
            return detected.variants.map { variant ->
                val h = variant.quality
                val sizeStr = if (variant.bandwidth > 0) "${variant.bandwidth / 1000} kbps" else null
                val isCurrentHeight = h.removeSuffix("p").toIntOrNull() == currentPlaybackHeight
                DownloadQualityOption(
                    id = "detected_${variant.quality}_${variant.url.hashCode()}",
                    title = when (h) {
                        "2160p" -> "4K Ultra HD"
                        "1440p" -> "2K Quad HD"
                        "1080p" -> "1080p Full HD"
                        "720p" -> "720p HD"
                        "480p" -> "480p SD"
                        "360p" -> "360p Data Saver"
                        else -> "$h Stream"
                    },
                    subtitle = "Website Stream • ${variant.resolution ?: h} • ${sizeStr ?: "Direct CDN"}",
                    badge = h,
                    formatId = variant.url,
                    isAudio = false,
                    fileSizeFormatted = sizeStr,
                    isSyncedFromWebsite = true,
                    isPlayingNow = isCurrentHeight
                )
            }.distinctBy { it.badge }.sortedByDescending { it.badge.removeSuffix("p").toIntOrNull() ?: 0 }
        } else {
            val badge = detected.quality ?: "DIRECT"
            return listOf(
                DownloadQualityOption(
                    id = "detected_direct_${detected.url.hashCode()}",
                    title = if (badge != "DIRECT") "$badge Website Stream" else "Website Video Stream",
                    subtitle = "Direct stream detected from website",
                    badge = badge,
                    formatId = detected.url,
                    isAudio = detected.contentType == ContentType.AUDIO,
                    fileSizeFormatted = if (detected.contentLength > 0) "${detected.contentLength / (1024 * 1024)} MB" else null,
                    isSyncedFromWebsite = true,
                    isPlayingNow = true
                )
            )
        }
    }
}
