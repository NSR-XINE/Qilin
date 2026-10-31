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
        outputFileName: String
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

        val segments = resolvedMediaPlaylist.segments
        if (segments.isEmpty()) {
            throw IOException("No segments found in HLS playlist")
        }

        val isFmp4 = resolvedMediaPlaylist.isFmp4
        val segmentExt = if (isFmp4) "m4s" else "ts"
        val totalSegments = segments.size
        Log.d(TAG, "HLS parsed successfully: totalSegments=$totalSegments, isFmp4=$isFmp4, duration=${resolvedMediaPlaylist.totalDuration}s")

        val segmentDownloader = SegmentDownloader(httpClient, headers, controller)

        // 2. Download fMP4 Initialization Segment if present
        var initFile: File? = null
        if (isFmp4 && !resolvedMediaPlaylist.initSegmentUrl.isNullOrEmpty()) {
            val init = downloadDir.resolve("init.mp4")
            if (!init.exists() || init.length() == 0L) {
                Log.d(TAG, "Downloading fMP4 initialization segment: ${resolvedMediaPlaylist.initSegmentUrl}")
                segmentDownloader.download(resolvedMediaPlaylist.initSegmentUrl, init, "HLS-Init", 0)
            }
            initFile = init
        }

        // 3. Download AES-128 key if present
        if (!resolvedMediaPlaylist.keyUrl.isNullOrEmpty()) {
            val keyFile = downloadDir.resolve("encryption.key")
            if (!keyFile.exists() || keyFile.length() == 0L) {
                Log.d(TAG, "Downloading HLS encryption key: ${resolvedMediaPlaylist.keyUrl}")
                val req = Request.Builder().url(resolvedMediaPlaylist.keyUrl).applySafeHeaders(headers).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("Failed to download HLS encryption key: HTTP ${resp.code}")
                    val bytes = resp.body.bytes()
                    keyFile.writeBytes(bytes)
                }
            }
        }

        // 4. Calculate initial resume state
        val downloadedBytesCount = AtomicLong(0L)
        val completedSegmentsCount = AtomicInteger(0)

        segments.forEach { segment ->
            val segFile = downloadDir.resolve("segment_${"%05d".format(segment.index)}.$segmentExt")
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
                    val segFile = downloadDir.resolve("segment_${"%05d".format(segment.index)}.$segmentExt")
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
                        val downloaded = segmentDownloader.download(
                            segmentUrl = segment.url,
                            outputFile = segFile,
                            logPrefix = "HLS",
                            segmentIdentifier = segment.index
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
        val finalExt = if (isFmp4) "mp4" else "ts"
        val cleanName = if (outputFileName.contains(".")) outputFileName.substringBeforeLast('.') else outputFileName
        val finalFile = File(downloadDir, "$cleanName.$finalExt")

        Log.d(TAG, "Merging $totalSegments segments into ${finalFile.absolutePath}")
        FileOutputStream(finalFile).use { out ->
            if (isFmp4 && initFile != null && initFile.exists()) {
                initFile.inputStream().use { it.copyTo(out, 64 * 1024) }
            }
            segments.forEach { segment ->
                val segFile = downloadDir.resolve("segment_${"%05d".format(segment.index)}.$segmentExt")
                if (segFile.exists() && segFile.length() > 0) {
                    segFile.inputStream().use { it.copyTo(out, 64 * 1024) }
                }
            }
            out.flush()
        }

        if (!finalFile.exists() || finalFile.length() == 0L) {
            throw IOException("HLS merge resulted in an empty or missing file")
        }

        // 7. Clean up intermediate segment files
        try {
            segments.forEach { segment ->
                downloadDir.resolve("segment_${"%05d".format(segment.index)}.$segmentExt").delete()
            }
            initFile?.delete()
            downloadDir.resolve("encryption.key").delete()
        } catch (ignored: Exception) {}

        Log.d(TAG, "HLS download and merge complete: ${finalFile.absolutePath} (${finalFile.length()} bytes)")
        finalFile
    }

    private fun fetchText(url: String): String {
        val req = Request.Builder().url(url).applySafeHeaders(headers).build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to fetch $url: HTTP ${resp.code}")
            return resp.body.string()
        }
    }
}
