package com.mydonghua.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.webkit.CookieManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mydonghua.app.R
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val PREFS_NAME = "donghua_prefs"
        const val SAVED_DOWNLOADS_KEY = "saved_downloads"
        private const val TAG = "DownloadService"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyDonghua::DownloadWakeLock")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create WakeLock", e)
        }
        createNotificationChannel()
        ensureInitialized(this)
    }

    private fun ensureInitialized(context: Context) {
        val app = context.applicationContext
        try {
            YoutubeDL.getInstance().init(app)
        } catch (e: Exception) {
            Log.e(TAG, "YoutubeDL init error", e)
        }
        try {
            FFmpeg.getInstance().init(app)
        } catch (e: Exception) {
            Log.e(TAG, "FFmpeg init error", e)
        }
        try {
            Aria2c.getInstance().init(app)
        } catch (e: Exception) {
            Log.e(TAG, "Aria2c init error", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time status of video downloads"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            "START_DOWNLOAD" -> {
                val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                val formatId = intent.getStringExtra("formatId")
                val downloadId = intent.getStringExtra("downloadId")
                    ?: intent.getStringExtra("id")
                    ?: UUID.randomUUID().toString()
                val title = intent.getStringExtra("title") ?: "Donghua_Video"
                val path = intent.getStringExtra("path")
                    ?: (getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath ?: cacheDir.absolutePath)
                val expectedSize = intent.getStringExtra("expectedSize") ?: ""
                val referer = intent.getStringExtra("referer")
                val isAudioOnly = intent.getBooleanExtra("isAudioOnly", false) || (formatId?.contains("audio", ignoreCase = true) == true)

                startDownload(downloadId, url, formatId, title, path, expectedSize, referer, isAudioOnly)
            }
            "CANCEL_DOWNLOAD" -> {
                val downloadId = intent.getStringExtra("downloadId")
                    ?: intent.getStringExtra("download_id")
                    ?: intent.getStringExtra("id")
                if (downloadId != null) {
                    cancelDownload(downloadId)
                }
            }
        }

        return START_NOT_STICKY
    }

    fun startDownload(
        id: String,
        url: String,
        formatId: String?,
        title: String,
        path: String,
        expectedSize: String,
        referer: String? = null,
        isAudioOnly: Boolean = false
    ) {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(7200000L) // 2 hours
            }
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock acquire error", e)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = id.hashCode() and 0x7FFFFFFF

        val cancelIntent = Intent(this, DownloadReceiver::class.java).apply {
            action = "CANCEL_DOWNLOAD"
            putExtra("download_id", id)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Connecting download stream...")
            .setSmallIcon(R.drawable.ic_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)

        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notification)
        }

        val job = serviceScope.launch {
            try {
                ensureInitialized(this@DownloadService)

                val destDir = File(path)
                if (!destDir.exists()) destDir.mkdirs()

                val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val isHls = url.contains(".m3u8", ignoreCase = true) || url.contains("/hls/", ignoreCase = true)

                val finalReferer = if (!referer.isNullOrEmpty()) {
                    referer
                } else {
                    try {
                        val uri = android.net.Uri.parse(url)
                        val host = uri.host
                        if (!host.isNullOrEmpty()) "${uri.scheme}://$host/" else null
                    } catch (ignored: Exception) { null }
                }

                val targetAudioExt = if (isAudioOnly) {
                    when {
                        formatId?.contains("mp3", ignoreCase = true) == true || title.contains("mp3", ignoreCase = true) -> "mp3"
                        formatId?.contains("aac", ignoreCase = true) == true || title.contains("aac", ignoreCase = true) -> "aac"
                        else -> "m4a"
                    }
                } else null

                val outputTemplate = if (targetAudioExt != null) {
                    "$path/$sanitizedTitle.$targetAudioExt"
                } else {
                    "$path/$sanitizedTitle.%(ext)s"
                }

                val request = YoutubeDLRequest(url).apply {
                    if (!formatId.isNullOrEmpty() && !formatId.startsWith("http://") && !formatId.startsWith("https://") && formatId != "best" && formatId != "auto" && !isAudioOnly) {
                        addOption("-f", formatId)
                    }
                    if (isAudioOnly) {
                        addOption("-x")
                        addOption("--audio-format", targetAudioExt ?: "m4a")
                        addOption("--audio-quality", "0")
                    }
                    addOption("-o", outputTemplate)
                    addOption("--no-check-certificates")
                    addOption("--user-agent", "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")

                    if (!finalReferer.isNullOrEmpty()) {
                        addOption("--referer", finalReferer)
                    }

                    try {
                        val cookie = CookieManager.getInstance().getCookie(url)
                        if (!cookie.isNullOrEmpty()) {
                            addOption("--add-header", "Cookie:$cookie")
                        }
                    } catch (ignored: Exception) {}

                    if (isHls) {
                        addOption("--downloader", "m3u8:native")
                        addOption("--concurrent-fragments", "16")
                        addOption("--hls-use-mpegts")
                    } else {
                        addOption("--concurrent-fragments", "16")
                        addOption("--buffer-size", "16M")
                    }
                    addOption("--retries", "10")
                    addOption("--fragment-retries", "10")
                }

                val speedRegex = Regex("(\\d+\\.?\\d*\\s*[KMGT]?i?B/s)")
                val sizeRegex = Regex("(\\d+\\.?\\d*\\s*[KMGT]?i?B)/(\\d+\\.?\\d*\\s*[KMGT]?i?B)")
                var finalFilePath: String? = null
                var lastLogLine = ""

                val progressCallback: (Float, Long, String) -> Unit = { progress, etaInSeconds, line ->
                    lastLogLine = line
                    if (line.contains("Destination:")) {
                        finalFilePath = line.substringAfter("Destination:").trim().replace("\"", "")
                    } else if (line.contains("Merging formats into")) {
                        finalFilePath = line.substringAfter("into").trim().replace("\"", "")
                    }

                    var speed = speedRegex.find(line)?.value ?: "0 MB/s"
                    speed = speed.replace("MiB", "MB").replace("KiB", "KB")

                    val sizeMatch = sizeRegex.find(line)
                    val downloadedSize = sizeMatch?.groupValues?.getOrNull(1)?.replace("iB", "B") ?: ""
                    val totalSize = sizeMatch?.groupValues?.getOrNull(2)?.replace("iB", "B") ?: expectedSize

                    val progressInt = progress.toInt().coerceIn(0, 100)
                    val etaStr = if (etaInSeconds > 0) "${etaInSeconds}s" else "--:--"

                    builder.setProgress(100, progressInt, false)
                    builder.setContentText("$speed - $progressInt%")
                    notificationManager.notify(notificationId, builder.build())

                    val progressIntent = Intent("DOWNLOAD_PROGRESS").apply {
                        putExtra("id", id)
                        putExtra("progress", progressInt)
                        putExtra("speed", speed)
                        putExtra("downloadedSize", downloadedSize)
                        putExtra("totalSize", totalSize)
                        putExtra("eta", etaStr)
                        setPackage(packageName)
                    }
                    sendBroadcast(progressIntent)

                    val updateIntent = Intent("UPDATE_DOWNLOAD_PROGRESS").apply {
                        putExtra("id", id)
                        putExtra("progress", progressInt)
                        putExtra("speed", speed)
                        putExtra("downloadedSize", downloadedSize)
                        putExtra("totalSize", totalSize)
                        putExtra("eta", etaStr)
                        setPackage(packageName)
                    }
                    sendBroadcast(updateIntent)
                }

                val response = withContext(Dispatchers.IO) {
                    try {
                        YoutubeDL.getInstance().execute(request, id, progressCallback)
                    } catch (primaryEx: Exception) {
                        Log.w(TAG, "Primary download attempt threw exception (${primaryEx.message}). Executing safe native fallback...", primaryEx)
                        val fallbackRequest = YoutubeDLRequest(url).apply {
                            if (isAudioOnly) {
                                addOption("-x")
                                addOption("--audio-format", targetAudioExt ?: "m4a")
                                addOption("--audio-quality", "0")
                            }
                            addOption("-o", outputTemplate)
                            addOption("--no-check-certificates")
                            addOption("--user-agent", "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                            if (!finalReferer.isNullOrEmpty()) {
                                addOption("--referer", finalReferer)
                            }
                            try {
                                val cookie = CookieManager.getInstance().getCookie(url)
                                if (!cookie.isNullOrEmpty()) {
                                    addOption("--add-header", "Cookie:$cookie")
                                }
                            } catch (ignored: Exception) {}
                            if (isHls) {
                                addOption("--downloader", "m3u8:native")
                                addOption("--hls-use-mpegts")
                            }
                            addOption("--retries", "10")
                        }
                        YoutubeDL.getInstance().execute(fallbackRequest, id, progressCallback)
                    }
                }

                if (response.exitCode == 0) {
                    var currentPath = finalFilePath
                    if (currentPath == null || !File(currentPath).exists()) {
                        val files = destDir.listFiles()
                        currentPath = files?.filter { it.name.startsWith(sanitizedTitle) }?.maxByOrNull { it.lastModified() }?.absolutePath
                            ?: files?.maxByOrNull { it.lastModified() }?.absolutePath
                    }

                    updatePrefs(id, "Completed", currentPath ?: "")

                    builder.setContentText("Download complete")
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                    notificationManager.notify(notificationId, builder.build())

                    val successIntent = Intent("DOWNLOAD_SUCCESS").apply {
                        putExtra("id", id)
                        putExtra("title", title)
                        putExtra("path", currentPath ?: "")
                        setPackage(packageName)
                    }
                    sendBroadcast(successIntent)
                } else {
                    Log.e(TAG, "YoutubeDL exit code: ${response.exitCode}, last line: $lastLogLine")
                    updatePrefs(id, "Failed", "")
                    builder.setContentText("Download failed (code ${response.exitCode})")
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                    notificationManager.notify(notificationId, builder.build())

                    val failedIntent = Intent("DOWNLOAD_FAILED").apply {
                        putExtra("id", id)
                        putExtra("error", "Exit code ${response.exitCode}: $lastLogLine")
                        setPackage(packageName)
                    }
                    sendBroadcast(failedIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for ID: $id", e)
                updatePrefs(id, "Failed", "")
                val cleanError = when {
                    e.message?.contains("403", ignoreCase = true) == true -> "Server HTTP 403 Forbidden (stream access expired or protected)"
                    e.message?.contains("Requested format", ignoreCase = true) == true -> "Requested video/audio format is not available"
                    e.message?.contains("Unable to extract", ignoreCase = true) == true -> "Unable to extract stream from website"
                    else -> e.message?.lines()?.lastOrNull { it.isNotBlank() } ?: "Download failed or interrupted"
                }

                builder.setContentText("Download failed: $cleanError")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                notificationManager.notify(notificationId, builder.build())

                val failedIntent = Intent("DOWNLOAD_FAILED").apply {
                    putExtra("id", id)
                    putExtra("error", cleanError)
                    setPackage(packageName)
                }
                sendBroadcast(failedIntent)
            } finally {
                downloadJobs.remove(id)
                notificationManager.cancel(notificationId)
                if (downloadJobs.isEmpty()) {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock?.release()
                        }
                    } catch (ignored: Exception) {}
                    ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }

        downloadJobs[id] = job
    }

    fun cancelDownload(id: String) {
        serviceScope.launch {
            try {
                YoutubeDL.getInstance().destroyProcessById(id)
            } catch (e: Exception) {
                Log.e(TAG, "Cancel process failed", e)
            }

            downloadJobs[id]?.cancel()
            downloadJobs.remove(id)
            updatePrefs(id, "Cancelled", "")

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(id.hashCode() and 0x7FFFFFFF)

            val cancelIntent = Intent("DOWNLOAD_CANCELLED").apply {
                putExtra("id", id)
                setPackage(packageName)
            }
            sendBroadcast(cancelIntent)

            if (downloadJobs.isEmpty()) {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock?.release()
                    }
                } catch (ignored: Exception) {}
                ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun updatePrefs(id: String, status: String, filePath: String) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(SAVED_DOWNLOADS_KEY, "[]") ?: "[]"
            val type = object : TypeToken<MutableList<DownloadModel>>() {}.type
            val list: MutableList<DownloadModel> = Gson().fromJson(json, type) ?: mutableListOf()
            val item = list.firstOrNull { it.id == id }
            if (item != null) {
                item.status = status
                if (status == "Completed") {
                    item.progress = 100
                }
                if (filePath.isNotEmpty()) {
                    item.filePath = filePath
                }
                prefs.edit().putString(SAVED_DOWNLOADS_KEY, Gson().toJson(list)).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update prefs", e)
        }
    }

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (ignored: Exception) {}
        serviceJob.cancel()
        super.onDestroy()
    }
}
