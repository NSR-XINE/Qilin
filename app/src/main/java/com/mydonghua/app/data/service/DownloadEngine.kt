package com.mydonghua.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.webkit.CookieManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.mydonghua.app.MainActivity
import com.mydonghua.app.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.math.BigInteger
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.coroutineContext

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class DownloadThreadInfo(
    val id: Int,
    val startByte: Long,
    val endByte: Long,
    @Volatile var downloadedBytes: Long = 0L
)

data class DownloadTask(
    val id: String,
    val title: String,
    val url: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f, // 0.0 to 100.0
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBps: Long = 0L, // Bytes per second
    val etaSeconds: Long = 0L,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val numThreads: Int = 1,
    val isHls: Boolean = false
)

/**
 * High-performance, NDM/1DM-grade download engine.
 *
 * Features:
 * - Dynamic multi-threaded chunked downloading with HTTP Range requests.
 * - Full HLS (.m3u8) playlist parsing, segment extraction, and AES-128 decryption.
 * - Browser cookie & session clearance forwarding (CookieManager).
 * - WakeLock & WifiLock to prevent Android OS sleep/throttling during downloads.
 * - Scoped Storage compliance with zero-permission requirements on Android 10-15.
 * - Resumable downloads with partial file support.
 * - JSON task state persistence across app restarts.
 */
object DownloadEngine {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private val activeTasks = ConcurrentHashMap<String, DownloadTask>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _tasksState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasksState: StateFlow<List<DownloadTask>> = _tasksState.asStateFlow()

    private const val CHANNEL_ID = "donghua_downloads"
    private const val CHANNEL_NAME = "Video Downloads"
    private const val TASKS_PERSISTENCE_FILE = "download_tasks_v2.json"

    // Power & Network Management
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    /**
     * Resolves the primary storage directory for offline donghua videos.
     * Uses app-specific external files (100% compliant with Android 10-15 Scoped Storage).
     */
    fun getPrimaryStorageDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val dir = File(base, "MyDonghua")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Returns all directories where offline donghua videos might be saved,
     * including legacy public Movies and Download folders.
     */
    fun getAllStorageDirs(context: Context): List<File> {
        val dirs = LinkedHashSet<File>()
        dirs.add(getPrimaryStorageDir(context))
        try {
            val pubMovies = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MyDonghua")
            if (pubMovies.exists()) dirs.add(pubMovies)
        } catch (ignored: Exception) {}
        try {
            val pubDownloads = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MyDonghua")
            if (pubDownloads.exists()) dirs.add(pubDownloads)
        } catch (ignored: Exception) {}
        return dirs.toList()
    }

    fun init(context: Context) {
        loadPersistedTasks(context.applicationContext)
    }

    fun startDownload(context: Context, url: String, title: String): String {
        val taskId = "dl_${System.currentTimeMillis()}_${(title.hashCode() and 0x7FFFFFFF)}"
        val sanitizedTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val safeName = if (sanitizedTitle.isBlank()) "Donghua_Episode_${System.currentTimeMillis()}" else sanitizedTitle
        val fileName = "$safeName.mp4"

        val storageDir = getPrimaryStorageDir(context)
        val outputFile = File(storageDir, fileName)

        val task = DownloadTask(
            id = taskId,
            title = title,
            url = url,
            status = DownloadStatus.QUEUED,
            filePath = outputFile.absolutePath
        )

        activeTasks[taskId] = task
        notifyStateChanged(context.applicationContext)

        val job = scope.launch {
            acquireLocks(context.applicationContext)
            runDownloadJob(context.applicationContext, taskId, url, outputFile)
            releaseLocksIfIdle()
        }

        activeJobs[taskId] = job
        return taskId
    }

    fun pauseDownload(context: Context, taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        val existing = activeTasks[taskId]
        if (existing != null) {
            activeTasks[taskId] = existing.copy(status = DownloadStatus.PAUSED, speedBps = 0L)
            notifyStateChanged(context.applicationContext)
        }
        releaseLocksIfIdle()
    }

    fun resumeDownload(context: Context, taskId: String) {
        val task = activeTasks[taskId] ?: return
        if (task.status == DownloadStatus.DOWNLOADING) return

        val outputFile = task.filePath?.let { File(it) } ?: run {
            val storageDir = getPrimaryStorageDir(context)
            val safe = task.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            File(storageDir, "$safe.mp4")
        }

        activeTasks[taskId] = task.copy(status = DownloadStatus.QUEUED, errorMessage = null)
        notifyStateChanged(context.applicationContext)

        val job = scope.launch {
            acquireLocks(context.applicationContext)
            runDownloadJob(context.applicationContext, taskId, task.url, outputFile)
            releaseLocksIfIdle()
        }

        activeJobs[taskId] = job
    }

    fun cancelDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        val existing = activeTasks[taskId]
        if (existing != null) {
            existing.filePath?.let { path ->
                try {
                    val file = File(path)
                    val tmp = File(file.parentFile, "${file.name}.tmp")
                    if (tmp.exists()) tmp.delete()
                } catch (ignored: Exception) {}
            }
            activeTasks[taskId] = existing.copy(status = DownloadStatus.FAILED, errorMessage = "Cancelled by user", speedBps = 0L)
            _tasksState.value = activeTasks.values.toList()
        }
        releaseLocksIfIdle()
    }

    private suspend fun runDownloadJob(context: Context, taskId: String, url: String, outputFile: File) {
        updateTask(taskId) { it.copy(status = DownloadStatus.DOWNLOADING) }
        createNotificationChannel(context)

        try {
            // 1. Attempt to sniff actual video stream if URL is an HTML webpage
            var targetUrl = url.trim()
            if (!targetUrl.contains(".mp4", ignoreCase = true) &&
                !targetUrl.contains(".m3u8", ignoreCase = true) &&
                !targetUrl.contains(".mkv", ignoreCase = true)
            ) {
                val extracted = tryExtractVideoFromHtml(targetUrl)
                if (extracted != null) {
                    targetUrl = extracted
                }
            }

            val isHls = targetUrl.contains(".m3u8", ignoreCase = true)
            updateTask(taskId) { it.copy(isHls = isHls) }

            if (isHls) {
                runHlsDownload(context, taskId, targetUrl, outputFile)
            } else {
                runDirectStreamDownload(context, taskId, targetUrl, outputFile)
            }

            showToast(context, "Downloaded: ${outputFile.nameWithoutExtension}")

        } catch (e: Exception) {
            if (e is CancellationException) {
                updateTask(taskId) { it.copy(status = DownloadStatus.PAUSED, errorMessage = "Paused") }
            } else {
                e.printStackTrace()
                val errorMsg = e.localizedMessage ?: "Download failed"
                updateTask(taskId) { it.copy(status = DownloadStatus.FAILED, errorMessage = errorMsg) }
                showFailedNotification(context, activeTasks[taskId] ?: return, errorMsg)
                showToast(context, "Download failed: $errorMsg")
            }
        } finally {
            activeJobs.remove(taskId)
            persistTasks(context)
        }
    }

    /**
     * NDM-grade HLS (.m3u8) Downloader with Master Playlist resolution & AES-128 segment decryption.
     */
    private suspend fun runHlsDownload(
        context: Context,
        taskId: String,
        playlistUrl: String,
        outputFile: File
    ) {
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")

        // 1. Fetch playlist content
        val playlistReq = buildRequest(playlistUrl)
        val playlistContent = client.newCall(playlistReq).execute().use { res ->
            if (!res.isSuccessful) throw IOException("Failed to load playlist: HTTP ${res.code}")
            res.body?.string() ?: throw IOException("Empty playlist content")
        }

        // 2. Resolve master playlist if necessary
        var mediaPlaylistUrl = playlistUrl
        var mediaContent = playlistContent

        if (playlistContent.contains("#EXT-X-STREAM-INF")) {
            val bestVariant = extractBestVariantUrl(playlistUrl, playlistContent)
                ?: throw IOException("No playable stream variant found in playlist")
            mediaPlaylistUrl = bestVariant
            val variantReq = buildRequest(bestVariant, referer = playlistUrl)
            mediaContent = client.newCall(variantReq).execute().use { res ->
                if (!res.isSuccessful) throw IOException("Failed to load stream variant: HTTP ${res.code}")
                res.body?.string() ?: throw IOException("Empty variant content")
            }
        }

        // 3. Extract AES-128 decryption key if encrypted
        var cipher: Cipher? = null
        val keyTag = mediaContent.lines().firstOrNull { it.startsWith("#EXT-X-KEY") }
        if (keyTag != null && keyTag.contains("METHOD=AES-128")) {
            val keyUriMatch = Regex("""URI=["']([^"']+)["']""").find(keyTag)
            val keyUri = keyUriMatch?.groupValues?.getOrNull(1)
            if (keyUri != null) {
                val resolvedKeyUrl = resolveUrl(mediaPlaylistUrl, keyUri)
                val keyReq = buildRequest(resolvedKeyUrl, referer = mediaPlaylistUrl)
                val keyBytes = client.newCall(keyReq).execute().use { res ->
                    if (res.isSuccessful) res.body?.bytes() else null
                }
                if (keyBytes != null && keyBytes.size == 16) {
                    val ivMatch = Regex("""IV=0x([0-9a-fA-F]+)""").find(keyTag)
                    val ivBytes = if (ivMatch != null) {
                        hexStringToByteArray(ivMatch.groupValues[1])
                    } else {
                        ByteArray(16) // default zero IV
                    }
                    val keySpec = SecretKeySpec(keyBytes, "AES")
                    val ivSpec = IvParameterSpec(ivBytes)
                    cipher = Cipher.getInstance("AES/CBC/PKCS7Padding").apply {
                        init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
                    }
                }
            }
        }

        // 4. Extract segment URLs
        val segments = extractSegments(mediaPlaylistUrl, mediaContent)
        if (segments.isEmpty()) {
            throw IOException("No media segments found in stream playlist")
        }

        val totalSegments = segments.size
        val estimatedTotalBytes = totalSegments * 650_000L

        updateTask(taskId) {
            it.copy(
                totalBytes = estimatedTotalBytes,
                progress = 0f
            )
        }

        var downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L
        var bytesInLastSec = 0L
        var lastSampleTime = System.currentTimeMillis()

        FileOutputStream(tempFile, true).use { outStream ->
            for (index in segments.indices) {
                if (!coroutineContext.isActive) {
                    throw CancellationException("Download cancelled")
                }

                val segUrl = segments[index]
                val segReq = buildRequest(segUrl, referer = mediaPlaylistUrl)
                var attempts = 0
                var success = false

                while (attempts < 3 && !success) {
                    attempts++
                    try {
                        client.newCall(segReq).execute().use { res ->
                            if (res.isSuccessful) {
                                val body = res.body
                                if (body != null) {
                                    val rawBytes = body.bytes()
                                    val finalBytes = if (cipher != null) {
                                        try {
                                            cipher.doFinal(rawBytes)
                                        } catch (e: Exception) {
                                            rawBytes
                                        }
                                    } else {
                                        rawBytes
                                    }
                                    outStream.write(finalBytes)
                                    downloadedBytes += finalBytes.size
                                    bytesInLastSec += finalBytes.size
                                    success = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (attempts >= 3) throw e
                        delay(400L)
                    }
                }

                val now = System.currentTimeMillis()
                val elapsed = now - lastSampleTime
                if (elapsed >= 1000L || index == totalSegments - 1) {
                    val speedBps = if (elapsed > 0) (bytesInLastSec * 1000L) / elapsed else 0L
                    val progressPercent = ((index + 1).toFloat() / totalSegments) * 100f
                    val remainingSegments = totalSegments - (index + 1)
                    val etaSec = if (speedBps > 0) {
                        val avgBytesPerSeg = (downloadedBytes / (index + 1)).coerceAtLeast(100_000L)
                        (remainingSegments * avgBytesPerSeg) / speedBps
                    } else 0L

                    updateTask(taskId) {
                        it.copy(
                            progress = progressPercent,
                            downloadedBytes = downloadedBytes,
                            totalBytes = if (downloadedBytes > estimatedTotalBytes) downloadedBytes else estimatedTotalBytes,
                            speedBps = speedBps,
                            etaSeconds = etaSec
                        )
                    }

                    activeTasks[taskId]?.let { showNotification(context, it) }

                    bytesInLastSec = 0L
                    lastSampleTime = now
                }
            }
            outStream.flush()
        }

        // Atomic swap
        if (outputFile.exists()) outputFile.delete()
        if (!tempFile.renameTo(outputFile)) {
            tempFile.copyTo(outputFile, overwrite = true)
            tempFile.delete()
        }

        scanFile(context, outputFile)

        updateTask(taskId) {
            it.copy(
                status = DownloadStatus.COMPLETED,
                progress = 100f,
                downloadedBytes = downloadedBytes,
                totalBytes = downloadedBytes,
                speedBps = 0L
            )
        }

        activeTasks[taskId]?.let { showCompletedNotification(context, it) }
    }

    /**
     * NDM-grade multi-threaded & single-stream direct video downloader with dynamic chunk partitioning.
     */
    private suspend fun runDirectStreamDownload(
        context: Context,
        taskId: String,
        url: String,
        outputFile: File
    ) {
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        var existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        // Probe total size and range support via HEAD or 0-0 range request
        var totalBytes = -1L
        var acceptsRanges = false

        try {
            val headReq = buildRequest(url)
            client.newCall(headReq).execute().use { res ->
                if (res.isSuccessful) {
                    totalBytes = res.header("Content-Length")?.toLongOrNull() ?: -1L
                    acceptsRanges = res.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true
                }
            }
        } catch (ignored: Exception) {}

        if (totalBytes <= 0) {
            try {
                val rangeProbe = buildRequest(url, rangeHeader = "bytes=0-0")
                client.newCall(rangeProbe).execute().use { res ->
                    if (res.code == 206) {
                        acceptsRanges = true
                        val contentRange = res.header("Content-Range")
                        if (contentRange != null) {
                            totalBytes = contentRange.substringAfterLast("/").toLongOrNull() ?: -1L
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }

        val numThreads = if (acceptsRanges && totalBytes > 10_000_000L) {
            if (totalBytes > 100_000_000L) 6 else 4
        } else {
            1
        }

        updateTask(taskId) { it.copy(numThreads = numThreads, totalBytes = totalBytes) }

        if (numThreads > 1) {
            runMultiThreadedDownload(context, taskId, url, tempFile, totalBytes, numThreads)
        } else {
            runSingleStreamDownload(context, taskId, url, tempFile, totalBytes, existingBytes)
        }

        // Atomic swap
        if (outputFile.exists()) outputFile.delete()
        if (!tempFile.renameTo(outputFile)) {
            tempFile.copyTo(outputFile, overwrite = true)
            tempFile.delete()
        }

        scanFile(context, outputFile)

        updateTask(taskId) {
            it.copy(
                status = DownloadStatus.COMPLETED,
                progress = 100f,
                downloadedBytes = if (totalBytes > 0) totalBytes else it.downloadedBytes,
                totalBytes = if (totalBytes > 0) totalBytes else it.downloadedBytes,
                speedBps = 0L
            )
        }

        activeTasks[taskId]?.let { showCompletedNotification(context, it) }
    }

    /**
     * Executes multi-threaded chunked downloading partitioned into N byte slices (Aspsine MultiThreadDownload pattern).
     */
    private suspend fun runMultiThreadedDownload(
        context: Context,
        taskId: String,
        url: String,
        tempFile: File,
        totalBytes: Long,
        numThreads: Int
    ) = coroutineScope {
        // Pre-allocate full file length
        RandomAccessFile(tempFile, "rw").use { raf ->
            raf.setLength(totalBytes)
        }

        val chunkSize = totalBytes / numThreads
        val threadInfos = (0 until numThreads).map { i ->
            val start = i * chunkSize
            val end = if (i == numThreads - 1) totalBytes - 1 else (start + chunkSize - 1)
            DownloadThreadInfo(id = i, startByte = start, endByte = end, downloadedBytes = 0L)
        }

        val totalDownloadedCounter = AtomicLong(0L)
        val bytesInLastSecCounter = AtomicLong(0L)
        var lastSampleTime = System.currentTimeMillis()

        val jobs = threadInfos.map { threadInfo ->
            launch(Dispatchers.IO) {
                var attempts = 0
                var chunkFinished = false

                while (attempts < 3 && !chunkFinished) {
                    attempts++
                    val currentStart = threadInfo.startByte + threadInfo.downloadedBytes
                    if (currentStart > threadInfo.endByte) {
                        chunkFinished = true
                        break
                    }

                    val req = buildRequest(url, rangeHeader = "bytes=$currentStart-${threadInfo.endByte}")
                    try {
                        client.newCall(req).execute().use { res ->
                            if (!res.isSuccessful && res.code != 206) {
                                throw IOException("Chunk HTTP ${res.code}")
                            }

                            val body = res.body ?: throw IOException("Empty chunk body")
                            val inStream = body.byteStream()
                            val buffer = ByteArray(32 * 1024)

                            RandomAccessFile(tempFile, "rw").use { raf ->
                                raf.seek(currentStart)
                                var read: Int
                                while (inStream.read(buffer).also { read = it } != -1) {
                                    if (!isActive) throw CancellationException("Thread cancelled")
                                    raf.write(buffer, 0, read)
                                    threadInfo.downloadedBytes += read
                                    totalDownloadedCounter.addAndGet(read.toLong())
                                    bytesInLastSecCounter.addAndGet(read.toLong())
                                }
                            }
                            chunkFinished = true
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        if (attempts >= 3) throw e
                        delay(500L)
                    }
                }
            }
        }

        val monitorJob = launch {
            while (isActive && jobs.any { it.isActive }) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val elapsed = now - lastSampleTime
                val bytesThisSec = bytesInLastSecCounter.getAndSet(0L)
                val downloaded = totalDownloadedCounter.get()

                val speedBps = if (elapsed > 0) (bytesThisSec * 1000L) / elapsed else 0L
                val progressPercent = (downloaded.toFloat() / totalBytes) * 100f
                val remainingBytes = (totalBytes - downloaded).coerceAtLeast(0L)
                val etaSec = if (speedBps > 0) remainingBytes / speedBps else 0L

                updateTask(taskId) {
                    it.copy(
                        progress = progressPercent,
                        downloadedBytes = downloaded,
                        totalBytes = totalBytes,
                        speedBps = speedBps,
                        etaSeconds = etaSec
                    )
                }

                activeTasks[taskId]?.let { showNotification(context, it) }
                lastSampleTime = now
            }
        }

        jobs.joinAll()
        monitorJob.cancel()
    }

    private suspend fun runSingleStreamDownload(
        context: Context,
        taskId: String,
        url: String,
        tempFile: File,
        totalBytes: Long,
        existingBytes: Long
    ) {
        val isResuming = existingBytes > 0
        val req = buildRequest(url, rangeHeader = if (isResuming) "bytes=$existingBytes-" else null)
        val response = client.newCall(req).execute()

        if (!response.isSuccessful && response.code != 206) {
            throw IOException("Server returned HTTP ${response.code}: ${response.message}")
        }

        val body = response.body ?: throw IOException("Empty response body from server")
        val isPartial = response.code == 206
        val appendMode = isPartial && isResuming

        val contentLength = body.contentLength()
        val effectiveTotalBytes = if (appendMode) {
            if (contentLength > 0) existingBytes + contentLength else totalBytes
        } else {
            if (contentLength > 0) contentLength else totalBytes
        }

        var downloadedBytes = if (appendMode) existingBytes else 0L
        var bytesInLastSec = 0L
        var lastSampleTime = System.currentTimeMillis()

        updateTask(taskId) {
            it.copy(
                totalBytes = effectiveTotalBytes,
                downloadedBytes = downloadedBytes
            )
        }

        val inStream = body.byteStream()
        FileOutputStream(tempFile, appendMode).use { outStream ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (inStream.read(buffer).also { read = it } != -1) {
                if (!coroutineContext.isActive) {
                    throw CancellationException("Download cancelled")
                }

                outStream.write(buffer, 0, read)
                downloadedBytes += read
                bytesInLastSec += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastSampleTime

                if (elapsed >= 1000L) {
                    val speedBps = (bytesInLastSec * 1000L) / elapsed
                    val progressPercent = if (effectiveTotalBytes > 0) (downloadedBytes.toFloat() / effectiveTotalBytes) * 100f else 0f
                    val remainingBytes = if (effectiveTotalBytes > 0) (effectiveTotalBytes - downloadedBytes).coerceAtLeast(0L) else 0L
                    val etaSec = if (speedBps > 0) remainingBytes / speedBps else 0L

                    updateTask(taskId) {
                        it.copy(
                            progress = progressPercent,
                            downloadedBytes = downloadedBytes,
                            totalBytes = effectiveTotalBytes,
                            speedBps = speedBps,
                            etaSeconds = etaSec
                        )
                    }

                    activeTasks[taskId]?.let { showNotification(context, it) }

                    bytesInLastSec = 0L
                    lastSampleTime = now
                }
            }
            outStream.flush()
        }

        if (effectiveTotalBytes > 0 && downloadedBytes < effectiveTotalBytes) {
            throw IOException("Download incomplete: received $downloadedBytes of $effectiveTotalBytes bytes")
        }
    }

    private fun extractBestVariantUrl(masterUrl: String, playlistContent: String): String? {
        val lines = playlistContent.lines()
        var bestBandwidth = -1L
        var bestUrl: String? = null

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                val bwMatch = Regex("""BANDWIDTH=(\d+)""").find(line)
                val bw = bwMatch?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
                val uriLine = lines.getOrNull(i + 1)?.trim()
                if (uriLine != null && !uriLine.startsWith("#") && uriLine.isNotEmpty()) {
                    if (bw > bestBandwidth || bestUrl == null) {
                        bestBandwidth = bw
                        bestUrl = resolveUrl(masterUrl, uriLine)
                    }
                }
            }
        }
        return bestUrl ?: lines.firstOrNull { !it.startsWith("#") && it.contains(".m3u8") }?.let { resolveUrl(masterUrl, it) }
    }

    private fun extractSegments(mediaPlaylistUrl: String, mediaContent: String): List<String> {
        val segments = mutableListOf<String>()
        val lines = mediaContent.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            segments.add(resolveUrl(mediaPlaylistUrl, trimmed))
        }
        return segments
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                relativeUrl
            } else {
                val baseUri = URI(baseUrl)
                baseUri.resolve(relativeUrl).toString()
            }
        } catch (e: Exception) {
            if (relativeUrl.startsWith("/")) {
                val baseUri = URI(baseUrl)
                "${baseUri.scheme}://${baseUri.host}$relativeUrl"
            } else {
                val prefix = baseUrl.substringBeforeLast("/")
                "$prefix/$relativeUrl"
            }
        }
    }

    private fun tryExtractVideoFromHtml(pageUrl: String): String? {
        try {
            val req = buildRequest(pageUrl)
            client.newCall(req).execute().use { res ->
                val cType = res.header("Content-Type")?.lowercase() ?: ""
                if (!cType.contains("text/html") && !cType.contains("application/xhtml")) {
                    return null
                }
                val html = res.body?.string() ?: return null

                // Search for embedded m3u8
                val m3u8Regex = Regex("""https?://[^"'\s<>]+\.m3u8(?:\?[^"'\s<>]*)?""", RegexOption.IGNORE_CASE)
                m3u8Regex.find(html)?.value?.let { return it }

                // Search for embedded direct video mp4/mkv
                val mp4Regex = Regex("""https?://[^"'\s<>]+\.(?:mp4|mkv)(?:\?[^"'\s<>]*)?""", RegexOption.IGNORE_CASE)
                mp4Regex.find(html)?.value?.let { return it }
            }
        } catch (ignored: Exception) {}
        return null
    }

    fun buildRequest(url: String, rangeHeader: String? = null, referer: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Connection", "keep-alive")

        try {
            val uri = URI(url)
            val origin = "${uri.scheme}://${uri.host}"
            builder.header("Origin", origin)
            builder.header("Referer", referer ?: "$origin/")
        } catch (ignored: Exception) {}

        // Forward session clearance & cookies
        try {
            val cookies = CookieManager.getInstance().getCookie(url)
            if (!cookies.isNullOrBlank()) {
                builder.header("Cookie", cookies)
            }
        } catch (ignored: Exception) {}

        if (rangeHeader != null) {
            builder.header("Range", rangeHeader)
        }
        return builder.build()
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun scanFile(context: Context, file: File) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("video/mp4"),
                null
            )
        } catch (ignored: Exception) {}
    }

    private fun updateTask(taskId: String, block: (DownloadTask) -> DownloadTask) {
        val current = activeTasks[taskId] ?: return
        val updated = block(current)
        activeTasks[taskId] = updated
        _tasksState.value = activeTasks.values.toList()
    }

    private fun notifyStateChanged(context: Context) {
        _tasksState.value = activeTasks.values.toList()
        persistTasks(context)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time status and progress of offline video downloads."
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showNotification(context: Context, task: DownloadTask) {
        if (!canPostNotifications(context)) return

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val speedMb = String.format("%.1f MB/s", task.speedBps.toFloat() / (1024 * 1024))
            val progressInt = task.progress.toInt()

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(task.title)
                .setContentText("$progressInt% • $speedMb" + if (task.etaSeconds > 0) " • ${task.etaSeconds}s left" else "")
                .setProgress(100, progressInt, task.totalBytes <= 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(task.id.hashCode() and 0x7FFFFFFF, notification)
        } catch (ignored: Exception) {}
    }

    private fun showCompletedNotification(context: Context, task: DownloadTask) {
        if (!canPostNotifications(context)) return

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Download Complete")
                .setContentText(task.title)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(task.id.hashCode() and 0x7FFFFFFF, notification)
        } catch (ignored: Exception) {}
    }

    private fun showFailedNotification(context: Context, task: DownloadTask, error: String) {
        if (!canPostNotifications(context)) return

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Download Failed: ${task.title}")
                .setContentText(error)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(task.id.hashCode() and 0x7FFFFFFF, notification)
        } catch (ignored: Exception) {}
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (ignored: Exception) {}
        }
    }

    private fun acquireLocks(context: Context) {
        synchronized(this) {
            try {
                if (wakeLock == null) {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyDonghua:DownloadWakeLock")?.apply {
                        setReferenceCounted(false)
                        acquire(2 * 60 * 60 * 1000L)
                    }
                }
                if (wifiLock == null) {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                    } else {
                        @Suppress("DEPRECATION")
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF
                    }
                    wifiLock = wm?.createWifiLock(mode, "MyDonghua:DownloadWifiLock")?.apply {
                        setReferenceCounted(false)
                        acquire()
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun releaseLocksIfIdle() {
        synchronized(this) {
            val hasActive = activeTasks.values.any { it.status == DownloadStatus.DOWNLOADING }
            if (!hasActive) {
                try {
                    if (wakeLock?.isHeld == true) wakeLock?.release()
                    wakeLock = null
                } catch (ignored: Exception) {}
                try {
                    if (wifiLock?.isHeld == true) wifiLock?.release()
                    wifiLock = null
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun persistTasks(context: Context) {
        scope.launch {
            try {
                val jsonArr = JSONArray()
                for (task in activeTasks.values) {
                    val obj = JSONObject().apply {
                        put("id", task.id)
                        put("title", task.title)
                        put("url", task.url)
                        put("status", task.status.name)
                        put("progress", task.progress.toDouble())
                        put("downloadedBytes", task.downloadedBytes)
                        put("totalBytes", task.totalBytes)
                        put("filePath", task.filePath ?: "")
                        put("isHls", task.isHls)
                    }
                    jsonArr.put(obj)
                }
                val file = File(context.filesDir, TASKS_PERSISTENCE_FILE)
                file.writeText(jsonArr.toString())
            } catch (ignored: Exception) {}
        }
    }

    private fun loadPersistedTasks(context: Context) {
        try {
            val file = File(context.filesDir, TASKS_PERSISTENCE_FILE)
            if (!file.exists()) return
            val jsonArr = JSONArray(file.readText())
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val id = obj.getString("id")
                val statusStr = obj.optString("status", DownloadStatus.QUEUED.name)
                val status = try {
                    DownloadStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    DownloadStatus.FAILED
                }
                val task = DownloadTask(
                    id = id,
                    title = obj.getString("title"),
                    url = obj.getString("url"),
                    status = if (status == DownloadStatus.DOWNLOADING) DownloadStatus.PAUSED else status,
                    progress = obj.optDouble("progress", 0.0).toFloat(),
                    downloadedBytes = obj.optLong("downloadedBytes", 0L),
                    totalBytes = obj.optLong("totalBytes", 0L),
                    filePath = obj.optString("filePath").takeIf { it.isNotEmpty() },
                    isHls = obj.optBoolean("isHls", false)
                )
                activeTasks[id] = task
            }
            _tasksState.value = activeTasks.values.toList()
        } catch (ignored: Exception) {}
    }
}
