package com.nsr.qilin.data.service.detector

import com.nsr.qilin.data.service.downloader.applySafeHeaders
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Utility for identifying media stream candidates and probing Content-Type from URLs.
 * Ported from super-video-downloader.
 */
object VideoUtils {

    private val NON_MEDIA_REGEX = Regex(
        "\\.(js|css|png|jpg|jpeg|gif|webp|svg|woff|woff2|ttf|otf|ico|json|map|html)(\\?.*)?$|^blob:",
        RegexOption.IGNORE_CASE
    )

    fun isStaticOrNonMediaUrl(url: String): Boolean {
        if (url.isBlank()) return true
        val clean = url.trim().lowercase()
        if (clean.startsWith("data:") || clean.startsWith("javascript:") || clean.startsWith("about:")) {
            return true
        }
        if (clean.contains("google-analytics") || clean.contains("/telemetry") ||
            clean.contains("doubleclick.net") || clean.contains("googlesyndication") ||
            clean.contains("facebook.com/tr")
        ) {
            return true
        }
        return NON_MEDIA_REGEX.containsMatchIn(clean)
    }

    fun quickDetectContentType(url: String): ContentType {
        val clean = url.lowercase()
        return when {
            clean.contains(".m3u8") || clean.contains("/hls/") || clean.contains("/hls-") -> ContentType.M3U8
            clean.contains(".mpd") -> ContentType.MPD
            clean.endsWith(".mp4") || clean.contains(".mp4?") ||
                    clean.endsWith(".mkv") || clean.contains(".mkv?") ||
                    clean.endsWith(".webm") || clean.contains(".webm?") ||
                    clean.endsWith(".flv") || clean.contains(".flv?") -> ContentType.VIDEO
            clean.endsWith(".m4a") || clean.contains(".m4a?") ||
                    clean.endsWith(".mp3") || clean.contains(".mp3?") ||
                    clean.endsWith(".aac") || clean.contains(".aac?") -> ContentType.AUDIO
            else -> ContentType.OTHER
        }
    }

    fun probeContentType(
        url: String,
        headers: Map<String, String>? = null,
        client: OkHttpClient
    ): ContentType {
        if (isStaticOrNonMediaUrl(url)) {
            return ContentType.OTHER
        }

        val quick = quickDetectContentType(url)
        if (quick != ContentType.OTHER) {
            return quick
        }

        return try {
            val reqBuilder = Request.Builder().url(url)
            headers?.let { reqBuilder.applySafeHeaders(it) }
            reqBuilder.header("Range", "bytes=0-15")

            client.newCall(reqBuilder.build()).execute().use { response ->
                val ct = response.header("Content-Type")?.lowercase() ?: ""
                when {
                    ct.contains("mpegurl") || ct.contains("vnd.apple.mpegurl") || ct.contains("x-mpegurl") -> ContentType.M3U8
                    ct.contains("dash+xml") || ct.contains("dash") -> ContentType.MPD
                    ct.contains("video/") -> ContentType.VIDEO
                    ct.contains("audio/") -> ContentType.AUDIO
                    ct.contains("application/octet-stream") || ct.contains("binary/octet-stream") -> {
                        val bodyStream = response.body.byteStream()
                        val buffer = ByteArray(7)
                        val read = bodyStream?.read(buffer) ?: 0
                        val magic = if (read > 0) String(buffer, 0, read, Charsets.UTF_8) else ""
                        when {
                            magic.startsWith("#EXTM3U") -> ContentType.M3U8
                            magic.contains("<MPD") -> ContentType.MPD
                            else -> ContentType.OTHER
                        }
                    }
                    else -> ContentType.OTHER
                }
            }
        } catch (ignored: Exception) {
            ContentType.OTHER
        }
    }
}
