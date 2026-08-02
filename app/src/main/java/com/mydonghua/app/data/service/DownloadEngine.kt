package com.mydonghua.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import com.mydonghua.app.MainActivity
import com.mydonghua.app.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeTasks = ConcurrentHashMap<String, DownloadTask>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _tasksState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasksState: StateFlow<List<DownloadTask>> = _tasksState.asStateFlow()

    private const val CHANNEL_ID = "donghua_downloads"
    private const val CHANNEL_NAME = "Video Downloads"

    fun startDownload(context: Context, url: String, title: String): String {
        val taskId = "dl_${System.currentTimeMillis()}_${title.hashCode()}"
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9_ -]"), "_")
        val fileName = "$sanitizedTitle.mp4"

        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "MyDonghua"
        )
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

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
            activeTasks[taskId] = existing.copy(status = DownloadStatus.FAILED, errorMessage = "Cancelled by user")
            notifyStateChanged()
        }
    }

    private suspend fun runDownloadJob(context: Context, taskId: String, url: String, outputFile: File) {
        updateTask(taskId) { it.copy(status = DownloadStatus.DOWNLOADING) }
        createNotificationChannel(context)

        try {
            // Check headers & total size via HEAD request
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            var totalBytes = -1L
            var acceptsRanges = false

            try {
                client.newCall(headRequest).execute().use { res ->
                    if (res.isSuccessful) {
                        totalBytes = res.header("Content-Length")?.toLongOrNull() ?: -1L
                        acceptsRanges = res.header("Accept-Ranges")?.lowercase() == "bytes" || totalBytes > 2_000_000L
                    }
                }
            } catch (ignored: Exception) {}

            if (acceptsRanges && totalBytes > 2_000_000L) {
                runParallelChunkDownload(context, taskId, url, outputFile, totalBytes)
            } else {
                runSingleStreamDownload(context, taskId, url, outputFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            updateTask(taskId) { it.copy(status = DownloadStatus.FAILED, errorMessage = e.localizedMessage) }
        } finally {
            activeJobs.remove(taskId)
        }
    }

    private suspend fun runParallelChunkDownload(
        context: Context,
        taskId: String,
        url: String,
        outputFile: File,
        totalBytes: Long
    ) = coroutineScope {
        val numChunks = 4
        val chunkSize = totalBytes / numChunks
        val totalDownloadedCounter = java.util.concurrent.atomic.AtomicLong(0L)
        val bytesInLastSecCounter = java.util.concurrent.atomic.AtomicLong(0L)

        // Pre-allocate file size
        java.io.RandomAccessFile(outputFile, "rw").use { raf ->
            raf.setLength(totalBytes)
        }

        var lastSampleTime = System.currentTimeMillis()

        val jobs = (0 until numChunks).map { index ->
            val startByte = index * chunkSize
            val endByte = if (index == numChunks - 1) totalBytes - 1 else (startByte + chunkSize - 1)

            launch(Dispatchers.IO) {
                val req = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=$startByte-$endByte")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) return@launch
                    val body = response.body ?: return@launch
                    val stream = body.byteStream()
                    val buffer = ByteArray(8 * 1024)

                    java.io.RandomAccessFile(outputFile, "rw").use { raf ->
                        raf.seek(startByte)
                        var read: Int
                        while (stream.read(buffer).also { read = it } != -1) {
                            if (!isActive) return@launch
                            raf.write(buffer, 0, read)
                            totalDownloadedCounter.addAndGet(read.toLong())
                            bytesInLastSecCounter.addAndGet(read.toLong())
                        }
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
                showNotification(context, activeTasks[taskId]!!)
                lastSampleTime = now
            }
        }

        jobs.joinAll()
        monitorJob.cancel()

        updateTask(taskId) {
            it.copy(
                status = DownloadStatus.COMPLETED,
                progress = 100f,
                downloadedBytes = totalBytes,
                totalBytes = totalBytes
            )
        }
        showCompletedNotification(context, activeTasks[taskId]!!)
    }

    private suspend fun runSingleStreamDownload(
        context: Context,
        taskId: String,
        url: String,
        outputFile: File
    ) {
        var input: InputStream? = null
        var output: FileOutputStream? = null

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                updateTask(taskId) { it.copy(status = DownloadStatus.FAILED, errorMessage = "HTTP ${response.code}") }
                return
            }

            val body = response.body ?: run {
                updateTask(taskId) { it.copy(status = DownloadStatus.FAILED, errorMessage = "Empty response body") }
                return
            }

            val totalBytes = body.contentLength()
            input = body.byteStream()
            output = FileOutputStream(outputFile)

            val buffer = ByteArray(8 * 1024)
            var downloaded = 0L
            var bytesInLastSecond = 0L
            var lastSampleTime = System.currentTimeMillis()

            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                if (!coroutineContext.isActive) {
                    outputFile.delete()
                    updateTask(taskId) { it.copy(status = DownloadStatus.FAILED, errorMessage = "Cancelled") }
                    return
                }

                output.write(buffer, 0, read)
                downloaded += read
                bytesInLastSecond += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastSampleTime

                if (elapsed >= 1000L) {
                    val speedBps = (bytesInLastSecond * 1000L) / elapsed
                    val progressPercent = if (totalBytes > 0) (downloaded.toFloat() / totalBytes) * 100f else 0f
                    val remainingBytes = if (totalBytes > 0) totalBytes - downloaded else 0L
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

                    showNotification(context, activeTasks[taskId]!!)

                    bytesInLastSecond = 0L
                    lastSampleTime = now
                }
            }

            output.flush()

            updateTask(taskId) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100f,
                    downloadedBytes = if (totalBytes > 0) totalBytes else downloaded,
                    totalBytes = if (totalBytes > 0) totalBytes else downloaded
                )
            }

            showCompletedNotification(context, activeTasks[taskId]!!)

        } catch (e: Exception) {
            e.printStackTrace()
            updateTask(taskId) { it.copy(status = DownloadStatus.FAILED, errorMessage = e.localizedMessage) }
        } finally {
            try { input?.close() } catch (ignored: Exception) {}
            try { output?.close() } catch (ignored: Exception) {}
        }
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
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, task: DownloadTask) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val speedMb = String.format("%.1f MB/s", task.speedBps.toFloat() / (1024 * 1024))
        val progressInt = task.progress.toInt()

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Downloading: ${task.title}")
            .setContentText("$progressInt% • $speedMb • ${task.etaSeconds}s left")
            .setProgress(100, progressInt, task.totalBytes <= 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(task.id.hashCode(), notification)
    }

    private fun showCompletedNotification(context: Context, task: DownloadTask) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Download Complete")
            .setContentText(task.title)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(task.id.hashCode(), notification)
    }
}
