package com.nsr.qilin.data.service.downloader

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
 * Robust HLS VOD Downloader ported from super-video-downloader.
 *
 * Downloads all HLS media segments in parallel using coroutines and semaphore-bounded concurrency.
 * Supports resumption of already completed segments, real-time speed/ETA tracking,
 * and seamless container concatenation.
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
        downloadDir: File,
        outputFileName: String,
        outputFormat: String = "mp4"
    ): File = withContext(Dispatchers.IO) {
        if (!downloadDir.exists()) downloadDir.mkdirs()

        controller.start()

        // 1. Fetch playlist manifest and resolve media playlist (handles nested masters)
        Log.d(TAG, "Fetching HLS manifest: $m3u8Url")
        var currentUrl = m3u8Url
        var mediaPlaylist: HlsPlaylistParser.MediaPlaylist? = null
        var iterations = 0

        while (iterations < MAX_PLAYLIST_REDIRECTIONS) {
            val manifestContent = fetchText(currentUrl)
            val parsed = HlsPlaylistParser.parse(manifestContent, currentUrl)
            when (parsed) {
                is HlsPlaylistParser.ParsedHlsResult.Media -> {
                    mediaPlaylist = parsed.media
                    break
                }
                is HlsPlaylistParser.ParsedHlsResult.Master -> {
                    val bestVariant = parsed.master.variants.firstOrNull()
                        ?: throw IOException("No media variants found in master playlist: $currentUrl")
                    Log.d(TAG, "Selected master variant bandwidth=${bestVariant.bandwidth}, res=${bestVariant.resolution}, url=${bestVariant.url}")
                    currentUrl = bestVariant.url
                    iterations++
                }
            }
        }

        val resolvedMediaPlaylist = mediaPlaylist
            ?: throw IOException("Could not resolve media playlist from $m3u8Url after $iterations redirects")

        // Dynamic sliding window accumulation if #EXT-X-ENDLIST is absent
        val allSegments = mutableListOf<HlsPlaylistParser.MediaSegment>()
        allSegments.addAll(resolvedMediaPlaylist.segments)
        val knownSegmentUrls = resolvedMediaPlaylist.segments.map { it.url }.toMutableSet()
        var hasEnd = resolvedMediaPlaylist.hasEndList

        if (!hasEnd && allSegments.size < 25) {
            Log.d(TAG, "Sliding window HLS detected (#EXT-X-ENDLIST absent). Polling for segments...")
            var consecutiveEmpty = 0
            val pollDelayMs = (resolvedMediaPlaylist.targetDuration * 1000L).coerceIn(1500L, 5000L)
            while (!hasEnd && consecutiveEmpty < 6 && !controller.isInterrupted()) {
                kotlinx.coroutines.delay(pollDelayMs)
                try {
                    val freshText = fetchText(currentUrl)
                    val freshParsed = HlsPlaylistParser.parse(freshText, currentUrl)
                    if (freshParsed is HlsPlaylistParser.ParsedHlsResult.Media) {
                        var added = 0
                        freshParsed.media.segments.forEach { seg ->
                            if (knownSegmentUrls.add(seg.url)) {
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
                            Log.d(TAG, "Found #EXT-X-ENDLIST; stream complete.")
                        }
                    }
                } catch (pollEx: Exception) {
                    consecutiveEmpty++
                }
            }
        }

        val segments = allSegments
        if (segments.isEmpty()) {
            throw IOException("No segments found in HLS playlist")
        }

        val isFmp4 = resolvedMediaPlaylist.isFmp4
        val segmentExt = if (isFmp4) "m4s" else "ts"
        val totalSegments = segments.size
        Log.d(TAG, "HLS parsed successfully: totalSegments=$totalSegments, isFmp4=$isFmp4, duration=${resolvedMediaPlaylist.totalDuration}s")

        val segmentDownloader = SegmentDownloader(httpClient, headers, controller)
        val segmentsDir = File(downloadDir, ".hls_segments").apply { if (!exists()) mkdirs() }

        // 2. Download fMP4 Initialization Segment if present
        var initFile: File? = null
        if (isFmp4 && !resolvedMediaPlaylist.initSegmentUrl.isNullOrEmpty()) {
            val init = segmentsDir.resolve("init.mp4")
            if (!init.exists() || init.length() == 0L) {
                Log.d(TAG, "Downloading fMP4 initialization segment: ${resolvedMediaPlaylist.initSegmentUrl}")
                segmentDownloader.download(resolvedMediaPlaylist.initSegmentUrl, init, "HLS-Init", 0)
            }
            initFile = init
        }

        // 3. Download AES-128 key if present
        var keyBytes: ByteArray? = null
        if (!resolvedMediaPlaylist.keyUrl.isNullOrEmpty()) {
            val keyFile = segmentsDir.resolve("encryption.key")
            if (!keyFile.exists() || keyFile.length() == 0L) {
                Log.d(TAG, "Downloading HLS encryption key: ${resolvedMediaPlaylist.keyUrl}")
                val req = Request.Builder().url(resolvedMediaPlaylist.keyUrl).applySafeHeaders(headers).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("Failed to download HLS encryption key: HTTP ${resp.code}")
                    val bytes = resp.body.bytes()
                    keyFile.writeBytes(bytes)
                }
            }
            if (keyFile.exists() && keyFile.length() >= 16L) {
                keyBytes = keyFile.readBytes().copyOf(16)
            }
        }

        // 4. Calculate initial resume state
        val downloadedBytesCount = AtomicLong(0L)
        val completedSegmentsCount = AtomicInteger(0)

        segments.forEach { segment ->
            val segFile = segmentsDir.resolve("segment_${"%05d".format(segment.index)}.$segmentExt")
            if (segFile.exists() && segFile.length() > 0) {
                completedSegmentsCount.incrementAndGet()
                downloadedBytesCount.addAndGet(segFile.length())
            }
        }

        Log.d(TAG, "Resuming HLS: ${completedSegmentsCount.get()}/$totalSegments segments already downloaded (${downloadedBytesCount.get()} bytes)")

        val startTime = SystemClock.elapsedRealtime()
        val lastProgressAt = AtomicLong(0L)
        val lastSampleBytes = AtomicLong(downloadedBytesCount.get())
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

                    val progressPercent = if (totalSegments > 0) {
                        (completed.toFloat() / totalSegments.toFloat()) * 100f
                    } else 0f

                    val estimatedTotalBytes = if (completed > 0) {
                        (downloaded / completed) * totalSegments
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

        // 5. Parallel download with Semaphore
        val effectiveThreads = threadCount.coerceIn(1, 8)
        val semaphore = Semaphore(effectiveThreads)

        coroutineScope {
            val jobs = segments.map { segment ->
                async {
                    val segFile = segmentsDir.resolve("segment_${"%05d".format(segment.index)}.$segmentExt")
                    if (segFile.exists() && segFile.length() > 0) {
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
                            logPrefix = "HLS",
                            segmentIdentifier = segment.index,
                            byteRange = byteRange
                        )
                        if (downloaded > 0) {
                            downloadedBytesCount.addAndGet(downloaded)
                        }
                        completedSegmentsCount.incrementAndGet()
                        updateProgress()
                    }
                }
            }
            jobs.awaitAll()
        }

        if (controller.isInterrupted()) {
            throw CancellationException("Download interrupted by user")
        }

        updateProgress(force = true)

        // 6. Concatenate segments into final output file
        val finalExt = when {
            outputFormat.equals("mkv", ignoreCase = true) -> "mkv"
            outputFormat.equals("ts", ignoreCase = true) -> "ts"
            outputFormat.equals("mp3", ignoreCase = true) -> "mp3"
            outputFormat.equals("m4a", ignoreCase = true) -> "m4a"
            else -> "mp4"
        }
        val cleanName = if (outputFileName.contains(".")) outputFileName.substringBeforeLast('.') else outputFileName
        val finalFile = File(downloadDir, "$cleanName.$finalExt")

        Log.d(TAG, "Merging $totalSegments segments into ${finalFile.absolutePath}")
        FileOutputStream(finalFile).use { out ->
            if (isFmp4 && initFile != null && initFile.exists()) {
                initFile.inputStream().use { it.copyTo(out, 64 * 1024) }
            }
            val key = keyBytes
            segments.forEach { segment ->
                val segFile = segmentsDir.resolve("segment_${"%05d".format(segment.index)}.$segmentExt")
                if (segFile.exists() && segFile.length() > 0) {
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
                            val rawBytes = segFile.readBytes()
                            val decrypted = cipher.doFinal(rawBytes)
                            out.write(decrypted)
                        } catch (e: Exception) {
                            Log.w(TAG, "AES-128 decrypt failed for segment ${segment.index}, copying raw: ${e.message}")
                            segFile.inputStream().use { it.copyTo(out, 64 * 1024) }
                        }
                    } else {
                        segFile.inputStream().use { it.copyTo(out, 64 * 1024) }
                    }
                }
            }
            out.flush()
        }

        if (!finalFile.exists() || finalFile.length() == 0L) {
            throw IOException("HLS merge resulted in an empty or missing file")
        }

        // 7. Clean up intermediate segment files
        try {
            segmentsDir.deleteRecursively()
        } catch (ignored: Exception) {}

        Log.d(TAG, "HLS download and merge complete: ${finalFile.absolutePath} (${finalFile.length()} bytes)")
        finalFile
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
