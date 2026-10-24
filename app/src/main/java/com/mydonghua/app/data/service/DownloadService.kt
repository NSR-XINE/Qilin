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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mydonghua.app.R
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

                startDownload(downloadId, url, formatId, title, path, expectedSize)
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
        expectedSize: String
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
            .setContentText("Connecting 16 Aria2c streams...")
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
                val destDir = File(path)
                if (!destDir.exists()) destDir.mkdirs()

                val request = YoutubeDLRequest(url)
                if (!formatId.isNullOrEmpty()) {
                    request.addOption("-f", formatId)
                }
                val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                request.addOption("-o", "$path/$sanitizedTitle.%(ext)s")
                request.addOption("--external-downloader", "aria2c")
                request.addOption("--external-downloader-args", "aria2c:-c -j 16 -x 16 -s 16 -k 1M --file-allocation=none")

                val speedRegex = Regex("(\\d+\\.?\\d*\\s*[KMGT]?i?B/s)")
                val sizeRegex = Regex("(\\d+\\.?\\d*\\s*[KMGT]?i?B)/(\\d+\\.?\\d*\\s*[KMGT]?i?B)")
                var finalFilePath: String? = null

                val response = withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().execute(request, id) { progress, etaInSeconds, line ->
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
                }

                if (response.exitCode == 0) {
                    if (finalFilePath == null || !File(finalFilePath).exists()) {
                        val files = destDir.listFiles()
                        finalFilePath = files?.maxByOrNull { it.lastModified() }?.absolutePath
                    }

                    updatePrefs(id, "Completed", finalFilePath ?: "")

                    val successIntent = Intent("DOWNLOAD_SUCCESS").apply {
                        putExtra("id", id)
                        putExtra("title", title)
                        putExtra("path", finalFilePath ?: "")
                        setPackage(packageName)
                    }
                    sendBroadcast(successIntent)
                } else {
                    Log.e(TAG, "YoutubeDL exit code: ${response.exitCode}")
                    updatePrefs(id, "Failed", "")
                    val failedIntent = Intent("DOWNLOAD_FAILED").apply {
                        putExtra("id", id)
                        setPackage(packageName)
                    }
                    sendBroadcast(failedIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for ID: $id", e)
                updatePrefs(id, "Failed", "")
                val failedIntent = Intent("DOWNLOAD_FAILED").apply {
                    putExtra("id", id)
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
