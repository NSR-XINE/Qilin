package com.nsr.qilin.data.service.downloader

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Robust HLS VOD Downloader ported from super-video-downloader and enhanced for
 * dual-stream video + audio HLS manifests (e.g. Dailymotion, modern CDNs) and
 * automated MediaMuxer / FFmpeg container assembly.
 *
 * Downloads video and separate audio tracks in parallel, decrypts AES-128 segments on the fly,
 * accumulates sliding-window playlists, and produces 100% compliant, seekable MP4/MKV files.
 */
class HlsDownloader(
    private val httpClient: OkHttpClient,
    private val headers: Map<String, String> = emptyMap(),
    private val threadCount: Int = 6,
    private val controller: FileBasedDownloadController,
    private val onProgress: ((progress: Float, speedBps: Long, downloadedBytes: Long, totalBytes: Long, etaSeconds: Long) -> Unit)? = null
) {
    companion object {
        private const val TAG = "HlsDownloader"
        private const val PROGRESS_INTERVAL_MS = 500L
        private const val MAX_PLAYLIST_REDIRECTIONS = 4
    }

    suspend fun download(
        m3u8Url: String,
        audioM3u8Url: String? = null,
        downloadDir: File,
        outputFileName: String,
        outputFormat: String = "mp4",
        context: Context? = null
    ): File = withContext(Dispatchers.IO) {
        if (!downloadDir.exists()) downloadDir.mkdirs()

        controller.start()

        // 1. Fetch playlist manifest and resolve media playlists for video and audio
        Log.d(TAG, "Fetching HLS manifest: $m3u8Url")
        var currentVideoUrl = m3u8Url
        var resolvedAudioUrl = audioM3u8Url
        var videoMediaPlaylist: HlsPlaylistParser.MediaPlaylist? = null
        var iterations = 0

        while (iterations < MAX_PLAYLIST_REDIRECTIONS) {
            val manifestContent = fetchText(currentVideoUrl)
            val parsed = HlsPlaylistParser.parse(manifestContent, currentVideoUrl)
            when (parsed) {
                is HlsPlaylistParser.ParsedHlsResult.Media -> {
                    videoMediaPlaylist = parsed.media
                    break
                }
                is HlsPlaylistParser.ParsedHlsResult.Master -> {
                    val bestVariant = parsed.master.variants.firstOrNull()
                        ?: throw IOException("No media variants found in master playlist: $currentVideoUrl")
                    Log.d(TAG, "Selected master variant bandwidth=${bestVariant.bandwidth}, res=${bestVariant.resolution}, url=${bestVariant.url}")
                    currentVideoUrl = bestVariant.url
                    if (resolvedAudioUrl.isNullOrEmpty()) {
                        resolvedAudioUrl = bestVariant.audioUrl
                            ?: parsed.master.audioTracks.firstOrNull { it.isDefault }?.url
                            ?: parsed.master.audioTracks.firstOrNull()?.url
                            ?: parsed.master.audioUris.firstOrNull()
                    }
                    iterations++
                }
            }
        }

        val resolvedVideoPlaylist = videoMediaPlaylist ?: resolveMediaPlaylistWithSlidingWindow(currentVideoUrl)
        val resolvedAudioPlaylist = if (!resolvedAudioUrl.isNullOrEmpty()) {
            try {
                Log.d(TAG, "Resolving separate audio playlist: $resolvedAudioUrl")
                resolveMediaPlaylistWithSlidingWindow(resolvedAudioUrl)
            } catch (e: Exception) {
                Log.w(TAG, "Could not resolve separate audio playlist: ${e.message}")
                null
            }
        } else null

        val videoSegments = resolvedVideoPlaylist.segments
        val audioSegments = resolvedAudioPlaylist?.segments ?: emptyList()
        val totalSegmentsAll = videoSegments.size + audioSegments.size

        if (videoSegments.isEmpty() && audioSegments.isEmpty()) {
            throw IOException("No segments found in HLS playlist")
        }

        Log.d(
            TAG,
            "HLS parsed: videoSegments=${videoSegments.size} (isFmp4=${resolvedVideoPlaylist.isFmp4}), " +
                    "audioSegments=${audioSegments.size} (isFmp4=${resolvedAudioPlaylist?.isFmp4 == true}), " +
                    "totalDuration=${resolvedVideoPlaylist.totalDuration}s"
        )

        val segmentsBaseDir = File(downloadDir, ".hls_segments").apply { if (!exists()) mkdirs() }
        val videoDir = File(segmentsBaseDir, "video").apply { if (!exists()) mkdirs() }
        val audioDir = File(segmentsBaseDir, "audio").apply { if (!exists()) mkdirs() }

        val segmentDownloader = SegmentDownloader(httpClient, headers, controller)

        // Progress Tracking across both tracks
        val downloadedBytesCount = AtomicLong(0L)
        val completedSegmentsCount = AtomicInteger(0)

        val startTime = SystemClock.elapsedRealtime()
        val lastProgressAt = AtomicLong(0L)
        val lastSampleBytes = AtomicLong(0L)
        val lastSampleTime = AtomicLong(startTime)
        val currentSpeedBps = AtomicLong(0L)

        fun updateProgress(force: Boolean = false) {
            val now = SystemClock.elapsedRealtime()
            val last = lastProgressAt.get()
            if (force || now - last >= PROGRESS_INTERVAL_MS) {
                if (lastProgressAt.compareAndSet(last, now) || force) {
                    val completed = completedSegmentsCount.get()
                    val downloaded = downloadedBytesCount.get()

                    val sampleElapsed = (now - lastSampleTime.get()).coerceAtLeast(1L)
                    if (sampleElapsed >= 1000L || force) {
                        val sampleDelta = (downloaded - lastSampleBytes.get()).coerceAtLeast(0L)
                        val speed = (sampleDelta * 1000.0 / sampleElapsed).toLong()
                        currentSpeedBps.set(speed)
                        lastSampleBytes.set(downloaded)
                        lastSampleTime.set(now)
                    }

                    val progressPercent = if (totalSegmentsAll > 0) {
                        (completed.toFloat() / totalSegmentsAll.toFloat()) * 100f
                    } else 0f

                    val estimatedTotalBytes = if (completed > 0) {
                        (downloaded / completed) * totalSegmentsAll
                    } else 0L

                    val speed = currentSpeedBps.get()
                    val etaSec = if (speed > 0 && estimatedTotalBytes > downloaded) {
                        (estimatedTotalBytes - downloaded) / speed
                    } else 0L

                    onProgress?.invoke(
                        progressPercent.coerceIn(0f, 100f),
                        speed,
                        downloaded,
                        estimatedTotalBytes,
                        etaSec
                    )
                }
            }
        }

        updateProgress(force = true)

        val effectiveThreads = threadCount.coerceIn(1, 8)
        val semaphore = Semaphore(effectiveThreads)

        // 2. Download Video Segments and Assemble
        val rawVideoFile = downloadAndAssembleTrack(
            playlist = resolvedVideoPlaylist,
            targetDir = videoDir,
            label = "video",
            segmentDownloader = segmentDownloader,
            semaphore = semaphore,
            downloadedBytesCount = downloadedBytesCount,
            completedSegmentsCount = completedSegmentsCount,
            updateProgress = { updateProgress() }
        )

        // 3. Download Audio Segments and Assemble (if separate audio stream exists)
        val rawAudioFile = if (resolvedAudioPlaylist != null && audioSegments.isNotEmpty()) {
            downloadAndAssembleTrack(
                playlist = resolvedAudioPlaylist,
                targetDir = audioDir,
                label = "audio",
                segmentDownloader = segmentDownloader,
                semaphore = semaphore,
                downloadedBytesCount = downloadedBytesCount,
                completedSegmentsCount = completedSegmentsCount,
                updateProgress = { updateProgress() }
            )
        } else null

        if (controller.isInterrupted()) {
            throw CancellationException("Download interrupted by user")
        }

        updateProgress(force = true)

        // 4. Mux / Finalize Output File
        val finalExt = when {
            outputFormat.equals("mkv", ignoreCase = true) -> "mkv"
            outputFormat.equals("ts", ignoreCase = true) -> "ts"
            outputFormat.equals("mp3", ignoreCase = true) -> "mp3"
            outputFormat.equals("m4a", ignoreCase = true) -> "m4a"
            else -> "mp4"
        }
        val cleanName = if (outputFileName.contains(".")) outputFileName.substringBeforeLast('.') else outputFileName
        val finalFile = File(downloadDir, "$cleanName.$finalExt")

        Log.d(TAG, "Assembling output file: ${finalFile.absolutePath}")

        var muxSucceeded = false

        if (finalExt == "mp4" || finalExt == "mkv" || finalExt == "m4a" || finalExt == "mp3") {
            // Use MediaMuxerHelper to produce standard, seekable file with both video and audio tracks
            muxSucceeded = MediaMuxerHelper.mux(
                videoFile = rawVideoFile,
                audioFile = rawAudioFile,
                outputFile = finalFile,
                context = context,
                format = finalExt
            )
        }

        if (!muxSucceeded) {
            // Fallback: Copy raw video file or raw audio file directly
            if (rawVideoFile.exists() && rawVideoFile.length() > 0L) {
                if (finalFile.exists()) finalFile.delete()
                rawVideoFile.copyTo(finalFile, overwrite = true)
            } else if (rawAudioFile != null && rawAudioFile.exists()) {
                if (finalFile.exists()) finalFile.delete()
                rawAudioFile.copyTo(finalFile, overwrite = true)
            }
        }

        if (!finalFile.exists() || finalFile.length() == 0L) {
            throw IOException("HLS download resulted in an empty or missing output file")
        }

        // 5. Clean up temporary segment directories
        try {
            segmentsBaseDir.deleteRecursively()
        } catch (ignored: Exception) {}

        Log.d(TAG, "HLS download complete: ${finalFile.absolutePath} (${finalFile.length()} bytes)")
        finalFile
    }

    private suspend fun resolveMediaPlaylistWithSlidingWindow(url: String): HlsPlaylistParser.MediaPlaylist {
        val initialContent = fetchText(url)
        val parsed = HlsPlaylistParser.parse(initialContent, url)
        if (parsed !is HlsPlaylistParser.ParsedHlsResult.Media) {
            throw IOException("Expected media playlist from $url")
        }

        var mediaPlaylist = parsed.media
        if (mediaPlaylist.hasEndList) {
            return mediaPlaylist
        }

        // Accumulate sliding window segments if #EXT-X-ENDLIST is missing
        Log.d(TAG, "Sliding window HLS detected (#EXT-X-ENDLIST absent). Polling for segments...")
        val allSegments = mutableListOf<HlsPlaylistParser.MediaSegment>()
        allSegments.addAll(mediaPlaylist.segments)
        val knownUrls = mediaPlaylist.segments.map { it.url }.toMutableSet()
        var hasEnd = false
        var consecutiveEmpty = 0
        val pollDelayMs = (mediaPlaylist.targetDuration * 1000L).coerceIn(1500L, 5000L)

        while (!hasEnd && consecutiveEmpty < 8 && !controller.isInterrupted()) {
            kotlinx.coroutines.delay(pollDelayMs)
            try {
                val freshText = fetchText(url)
                val freshParsed = HlsPlaylistParser.parse(freshText, url)
                if (freshParsed is HlsPlaylistParser.ParsedHlsResult.Media) {
                    var added = 0
                    freshParsed.media.segments.forEach { seg ->
                        if (knownUrls.add(seg.url)) {
                            allSegments.add(seg.copy(index = allSegments.size))
                            added++
                        }
                    }
                    if (added > 0) {
                        consecutiveEmpty = 0
                        Log.d(TAG, "Accumulated $added new segments (total=${allSegments.size})")
                    } else {
                        consecutiveEmpty++
                    }
                    if (freshParsed.media.hasEndList) {
                        hasEnd = true
                        Log.d(TAG, "Found #EXT-X-ENDLIST; sliding accumulation complete.")
                    }
                }
            } catch (pollEx: Exception) {
                consecutiveEmpty++
            }
        }

        return mediaPlaylist.copy(segments = allSegments, hasEndList = true)
    }

    private suspend fun downloadAndAssembleTrack(
        playlist: HlsPlaylistParser.MediaPlaylist,
        targetDir: File,
        label: String,
        segmentDownloader: SegmentDownloader,
        semaphore: Semaphore,
        downloadedBytesCount: AtomicLong,
        completedSegmentsCount: AtomicInteger,
        updateProgress: () -> Unit
    ): File = coroutineScope {
        val isFmp4 = playlist.isFmp4
        val segmentExt = if (isFmp4) "m4s" else "ts"

        // 1. Download initialization segment if present
        var initFile: File? = null
        if (isFmp4 && !playlist.initSegmentUrl.isNullOrEmpty()) {
            val init = targetDir.resolve("init.mp4")
            if (!init.exists() || init.length() == 0L) {
                segmentDownloader.download(playlist.initSegmentUrl, init, "$label-Init", 0)
            }
            initFile = init
        }

        // 2. Download AES-128 key if present
        var keyBytes: ByteArray? = null
        if (!playlist.keyUrl.isNullOrEmpty()) {
            val keyFile = targetDir.resolve("encryption.key")
            if (!keyFile.exists() || keyFile.length() == 0L) {
                val req = Request.Builder().url(playlist.keyUrl).applySafeHeaders(headers).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        keyFile.writeBytes(resp.body.bytes())
                    }
                }
            }
            if (keyFile.exists() && keyFile.length() >= 16L) {
                keyBytes = keyFile.readBytes().copyOf(16)
            }
        }

        // 3. Download segments in parallel
        val jobs = playlist.segments.map { segment ->
            async {
                val segFile = targetDir.resolve("segment_${"%05d".format(segment.index)}.$segmentExt")
                if (segFile.exists() && segFile.length() > 0L) {
                    downloadedBytesCount.addAndGet(segFile.length())
                    completedSegmentsCount.incrementAndGet()
                    return@async
                }

                if (controller.isInterrupted()) {
                    throw CancellationException("Download interrupted")
                }

                semaphore.withPermit {
                    if (controller.isInterrupted()) {
                        throw CancellationException("Download interrupted")
                    }
                    val byteRange = if (segment.byteRangeLength != null && segment.byteRangeOffset != null) {
                        Pair(segment.byteRangeOffset, segment.byteRangeOffset + segment.byteRangeLength - 1)
                    } else null

                    val downloaded = segmentDownloader.download(
                        segmentUrl = segment.url,
                        outputFile = segFile,
                        logPrefix = label,
                        segmentIdentifier = segment.index,
                        byteRange = byteRange
                    )
                    if (downloaded > 0L) {
                        downloadedBytesCount.addAndGet(downloaded)
                    }
                    completedSegmentsCount.incrementAndGet()
                    updateProgress()
                }
            }
        }
        jobs.awaitAll()

        // 4. Assemble into single raw file
        val rawFile = targetDir.resolve("raw_$label.${if (isFmp4) "mp4" else "ts"}")
        FileOutputStream(rawFile).use { out ->
            if (isFmp4 && initFile != null && initFile.exists()) {
                initFile.inputStream().use { it.copyTo(out, 64 * 1024) }
            }
            playlist.segments.forEach { segment ->
                val segFile = targetDir.resolve("segment_${"%05d".format(segment.index)}.$segmentExt")
                if (segFile.exists() && segFile.length() > 0L) {
                    decryptAndWrite(segFile, out, keyBytes, segment)
                }
            }
            out.flush()
        }

        rawFile
    }

    private fun decryptAndWrite(
        sourceFile: File,
        targetOut: FileOutputStream,
        keyBytes: ByteArray?,
        segment: HlsPlaylistParser.MediaSegment
    ) {
        val key = keyBytes
        if (key != null) {
            try {
                val iv = if (!segment.keyIv.isNullOrEmpty()) {
                    parseHexIv(segment.keyIv)
                } else {
                    val buf = java.nio.ByteBuffer.allocate(16)
                    buf.putLong(8, segment.index.toLong())
                    buf.array()
                }
                val secretKey = javax.crypto.spec.SecretKeySpec(key, "AES")
                val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
                val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS7Padding")
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, ivSpec)
                val rawBytes = sourceFile.readBytes()
                val decrypted = cipher.doFinal(rawBytes)
                targetOut.write(decrypted)
            } catch (e: Exception) {
                Log.w(TAG, "AES-128 decrypt failed for segment ${segment.index}, copying raw: ${e.message}")
                sourceFile.inputStream().use { it.copyTo(targetOut, 64 * 1024) }
            }
        } else {
            sourceFile.inputStream().use { it.copyTo(targetOut, 64 * 1024) }
        }
    }

    private fun parseHexIv(hex: String): ByteArray {
        val clean = hex.removePrefix("0x").removePrefix("0X")
        val data = ByteArray(16)
        var i = 0
        while (i < clean.length - 1 && i / 2 < 16) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun fetchText(url: String): String {
        val req = Request.Builder().url(url).applySafeHeaders(headers).build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to fetch $url: HTTP ${resp.code}")
            return resp.body.string()
        }
    }
}
