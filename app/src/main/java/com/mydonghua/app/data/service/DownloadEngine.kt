package com.mydonghua.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

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
    val errorMessage: String? = null
)

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
        notifyStateChanged()

        val job = scope.launch {
            runDownloadJob(context.applicationContext, taskId, url, outputFile)
        }

        activeJobs[taskId] = job
        return taskId
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
            activeTasks[taskId] = existing.copy(status = DownloadStatus.FAILED, errorMessage = "Cancelled by user")
            notifyStateChanged()
        }
    }

    private suspend fun runDownloadJob(context: Context, taskId: String, url: String, outputFile: File) {
        updateTask(taskId) { it.copy(status = DownloadStatus.DOWNLOADING) }
        createNotificationChannel(context)

        try {
            // Attempt to resolve real video stream if URL is an HTML webpage
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

            if (isHls) {
                runHlsDownload(context, taskId, targetUrl, outputFile)
            } else {
                runDirectStreamDownload(context, taskId, targetUrl, outputFile)
            }

            showToast(context, "Downloaded: ${outputFile.nameWithoutExtension}")

        } catch (e: Exception) {
            if (e is CancellationException) {
                updateTask(taskId) { it.copy(status = DownloadStatus.FAILED, errorMessage = "Cancelled by user") }
            } else {
                e.printStackTrace()
                val errorMsg = e.localizedMessage ?: "Download failed"
                updateTask(taskId) { it.copy(status = DownloadStatus.FAILED, errorMessage = errorMsg) }
                showFailedNotification(context, activeTasks[taskId] ?: return, errorMsg)
                showToast(context, "Download failed: $errorMsg")
            }
        } finally {
            activeJobs.remove(taskId)
        }
    }

    /**
     * Downloads an HLS (.m3u8) stream by resolving master playlists and concatenating TS segments.
     */
    private suspend fun runHlsDownload(
        context: Context,
        taskId: String,
        playlistUrl: String,
        outputFile: File
    ) {
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        if (tempFile.exists()) tempFile.delete()

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
            val variantReq = buildRequest(bestVariant)
            mediaContent = client.newCall(variantReq).execute().use { res ->
                if (!res.isSuccessful) throw IOException("Failed to load stream variant: HTTP ${res.code}")
                res.body?.string() ?: throw IOException("Empty variant content")
            }
        }

        // 3. Extract segment URLs
        val segments = extractSegments(mediaPlaylistUrl, mediaContent)
        if (segments.isEmpty()) {
            throw IOException("No media segments found in stream playlist")
        }

        val totalSegments = segments.size
        val estimatedTotalBytes = totalSegments * 650_000L // ~650 KB average per segment for progress display

        updateTask(taskId) {
            it.copy(
                totalBytes = estimatedTotalBytes,
                progress = 0f
            )
        }

        var downloadedBytes = 0L
        var bytesInLastSec = 0L
        var lastSampleTime = System.currentTimeMillis()

        FileOutputStream(tempFile, false).use { outStream ->
            for (index in segments.indices) {
                if (!coroutineContext.isActive) {
                    tempFile.delete()
                    throw CancellationException("Download cancelled")
                }

                val segUrl = segments[index]
                val segReq = buildRequest(segUrl)
                var attempts = 0
                var success = false

                while (attempts < 3 && !success) {
                    attempts++
                    try {
                        client.newCall(segReq).execute().use { res ->
                            if (res.isSuccessful) {
                                val body = res.body
                                if (body != null) {
                                    val bytes = body.bytes()
                                    outStream.write(bytes)
                                    downloadedBytes += bytes.size
                                    bytesInLastSec += bytes.size
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
                totalBytes = downloadedBytes
            )
        }

        activeTasks[taskId]?.let { showCompletedNotification(context, it) }
    }

    /**
     * Downloads a direct video stream (.mp4, .mkv) with Range request resumption support and high-speed buffered I/O.
     */
    private suspend fun runDirectStreamDownload(
        context: Context,
        taskId: String,
        url: String,
        outputFile: File
    ) {
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        val req = buildRequest(url, if (existingBytes > 0) "bytes=$existingBytes-" else null)
        val response = client.newCall(req).execute()

        if (!response.isSuccessful && response.code != 206) {
            throw IOException("Server returned HTTP ${response.code}: ${response.message}")
        }

        val body = response.body ?: throw IOException("Empty response body from server")
        val isPartial = response.code == 206
        val appendMode = isPartial && existingBytes > 0

        val contentLength = body.contentLength()
        val totalBytes = if (appendMode) {
            if (contentLength > 0) existingBytes + contentLength else -1L
        } else {
            if (contentLength > 0) contentLength else -1L
        }

        var downloadedBytes = if (appendMode) existingBytes else 0L
        var bytesInLastSec = 0L
        var lastSampleTime = System.currentTimeMillis()

        updateTask(taskId) {
            it.copy(
                totalBytes = totalBytes,
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
                    val progressPercent = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes) * 100f else 0f
                    val remainingBytes = if (totalBytes > 0) (totalBytes - downloadedBytes).coerceAtLeast(0L) else 0L
                    val etaSec = if (speedBps > 0) remainingBytes / speedBps else 0L

                    updateTask(taskId) {
                        it.copy(
                            progress = progressPercent,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
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

        if (totalBytes > 0 && downloadedBytes < totalBytes) {
            throw IOException("Download incomplete: received $downloadedBytes of $totalBytes bytes")
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
                downloadedBytes = if (totalBytes > 0) totalBytes else downloadedBytes,
                totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes
            )
        }

        activeTasks[taskId]?.let { showCompletedNotification(context, it) }
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

    private fun buildRequest(url: String, rangeHeader: String? = null): Request {
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
            builder.header("Referer", "$origin/")
        } catch (ignored: Exception) {}

        if (rangeHeader != null) {
            builder.header("Range", rangeHeader)
        }
        return builder.build()
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
        notifyStateChanged()
    }

    private fun notifyStateChanged() {
        _tasksState.value = activeTasks.values.toList()
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
}
