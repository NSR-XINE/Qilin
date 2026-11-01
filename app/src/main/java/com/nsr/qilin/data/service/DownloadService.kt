package com.nsr.qilin.data.service

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
import android.os.SystemClock
import android.util.Log
import android.webkit.CookieManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nsr.qilin.R
import com.nsr.qilin.data.service.downloader.CustomFileDownloader
import com.nsr.qilin.data.service.downloader.DownloadListener
import com.nsr.qilin.data.service.downloader.FileBasedDownloadController
import com.nsr.qilin.data.service.downloader.HlsDownloader
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val PREFS_NAME = "donghua_prefs"
        const val SAVED_DOWNLOADS_KEY = "saved_downloads"
        private const val TAG = "DownloadService"
        private const val NOTIFY_INTERVAL_MS = 1000L
        private const val PERSIST_INTERVAL_MS = 2000L
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        fun logDebug(msg: String) {
            if (com.nsr.qilin.BuildConfig.DEBUG) {
                Log.d(TAG, msg)
            }
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val pausedIds = ConcurrentHashMap.newKeySet<String>()
    private val cancelledIds = ConcurrentHashMap.newKeySet<String>()
    private var wakeLock: PowerManager.WakeLock? = null

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Qilin::DownloadWakeLock")
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
        if (intent == null) {
            if (downloadJobs.isEmpty()) {
                ensureForegroundFallback()
                stopSelf()
            }
            return START_NOT_STICKY
        }

        when (intent.action) {
            "START_DOWNLOAD" -> {
                val url = intent.getStringExtra("url")
                if (url.isNullOrBlank()) {
                    if (downloadJobs.isEmpty()) {
                        ensureForegroundFallback()
                        stopSelf()
                    }
                    return START_NOT_STICKY
                }
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
                val isHls = intent.getBooleanExtra("isHls", false) ||
                        url.contains(".m3u8", ignoreCase = true) ||
                        url.contains("/hls/", ignoreCase = true)
                val isResume = intent.getBooleanExtra("resume", false)
                val format = intent.getStringExtra("format") ?: if (isAudioOnly) "m4a" else "mp4"
                val resolution = intent.getStringExtra("resolution") ?: "HD"
                val audioUrl = intent.getStringExtra("audioUrl")
                val masterUrl = intent.getStringExtra("masterUrl")

                pausedIds.remove(downloadId)
                cancelledIds.remove(downloadId)
                startDownload(downloadId, url, formatId, title, path, expectedSize, referer, isAudioOnly, isHls, isResume, format, resolution, audioUrl, masterUrl)
            }
            "PAUSE_DOWNLOAD" -> {
                val downloadId = intent.getStringExtra("downloadId")
                    ?: intent.getStringExtra("download_id")
                    ?: intent.getStringExtra("id")
                if (downloadId != null) {
                    pauseDownload(downloadId)
                }
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

    private fun ensureForegroundFallback() {
        try {
            val fallback = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Video Download")
                .setContentText("Initializing...")
                .setSmallIcon(R.drawable.ic_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1001, fallback, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1001, fallback)
            }
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (ignored: Exception) {}
    }

    fun startDownload(
        id: String,
        url: String,
        formatId: String?,
        title: String,
        path: String,
        expectedSize: String,
        referer: String? = null,
        isAudioOnly: Boolean = false,
        isHls: Boolean = false,
        isResume: Boolean = false,
        format: String = "mp4",
        resolution: String = "HD",
        audioUrl: String? = null,
        masterUrl: String? = null
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
            .setContentText(if (isResume) "Resuming download..." else "Connecting download stream...")
            .setSmallIcon(R.drawable.ic_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)

        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notification)
        }

        val job = serviceScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            var lastNotifyAt = 0L
            var lastPersistAt = 0L
            var finalFilePath: String? = null
            var lastLogLine = ""
            val destDir = File(path)
            var tempDir = File(cacheDir, "downloads/$id")
            val controller = FileBasedDownloadController(tempDir)

            try {
                logDebug("START_DOWNLOAD: id=$id, url=$url, formatId=$formatId, isAudioOnly=$isAudioOnly, isHls=$isHls, resume=$isResume, referer=$referer")
                ensureInitialized(this@DownloadService)

                if (url.isBlank() || (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true))) {
                    throw Exception("Invalid video URL. Please ensure a valid streaming link is captured.")
                }

                if (!destDir.exists()) destDir.mkdirs()
                if (destDir.usableSpace in 1 until 50L * 1024L * 1024L) {
                    throw Exception("Not enough free storage space to start this download")
                }

                val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val cleanUrlStr = url.lowercase()
                val hls = isHls || cleanUrlStr.contains(".m3u8") || cleanUrlStr.contains("/hls/") || cleanUrlStr.contains("/hls-")
                val isExtractorSite = cleanUrlStr.contains("youtube.com") || cleanUrlStr.contains("youtu.be") ||
                        cleanUrlStr.contains("bilibili") || cleanUrlStr.contains("dood") ||
                        cleanUrlStr.contains("streamtape") || cleanUrlStr.contains("swish") ||
                        cleanUrlStr.contains("mixdrop") || cleanUrlStr.contains("mp4upload") ||
                        cleanUrlStr.contains("vidhide")

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

                // Download into a per-task temp folder and move the finished file afterwards.
                // This keeps partial fragments (.part/.ts) out of the user's offline library.
                if (!isResume && tempDir.exists()) tempDir.deleteRecursively()
                if (!tempDir.exists()) tempDir.mkdirs()
                controller.start()
                val outputTemplate = "${tempDir.absolutePath}/$sanitizedTitle.%(ext)s"

                val speedRegex = Regex("([\\d.]+)\\s*([KMGT]?i?B)/s")
                val sizeRegex = Regex("([\\d.]+)\\s*([KMGT]?i?B)\\s+of\\s+~?\\s*([\\d.]+)\\s*([KMGT]?i?B)")
                val totalOnlyRegex = Regex("of\\s+~?\\s*([\\d.]+)\\s*([KMGT]?i?B)")
                val etaRegex = Regex("ETA\\s+(\\d+:\\d+(?::\\d+)?)")

                fun publishProgress(progress: Float, line: String, force: Boolean = false) {
                    lastLogLine = line
                    if (line.contains("Destination:")) {
                        finalFilePath = line.substringAfter("Destination:").trim().replace("\"", "")
                    } else if (line.contains("Merging formats into")) {
                        finalFilePath = line.substringAfter("into").trim().replace("\"", "")
                    }

                    val now = SystemClock.elapsedRealtime()
                    if (!force && now - lastNotifyAt < NOTIFY_INTERVAL_MS) return
                    lastNotifyAt = now

                    val speedMatch = speedRegex.find(line)
                    val speedText = speedMatch?.let {
                        "${it.groupValues[1]} ${it.groupValues[2]}/s".replace("MiB", "MB").replace("KiB", "KB")
                    } ?: "0 MB/s"

                    val sizeMatch = sizeRegex.find(line)
                    val totalMatch = totalOnlyRegex.find(line)
                    var downloadedBytes = 0L
                    var totalBytes = 0L
                    if (sizeMatch != null) {
                        downloadedBytes = parseSize(sizeMatch.groupValues[1], sizeMatch.groupValues[2])
                        totalBytes = parseSize(sizeMatch.groupValues[3], sizeMatch.groupValues[4])
                    } else if (totalMatch != null) {
                        totalBytes = parseSize(totalMatch.groupValues[1], totalMatch.groupValues[2])
                    }
                    if (totalBytes <= 0L && expectedSize.isNotBlank()) {
                        totalBytes = parseReadableSize(expectedSize)
                    }
                    if (downloadedBytes <= 0L && totalBytes > 0L) {
                        downloadedBytes = (totalBytes * (progress / 100f)).toLong()
                    }

                    val etaSeconds = etaRegex.find(line)?.groupValues?.getOrNull(1)?.let { parseEta(it) } ?: 0L
                    val progressInt = progress.toInt().coerceIn(0, 100)
                    val etaStr = if (etaSeconds > 0) "${etaSeconds}s" else "--:--"

                    builder.setProgress(100, progressInt, totalBytes <= 0L)
                    builder.setContentText("$speedText - $progressInt%")
                    try {
                        notificationManager.notify(notificationId, builder.build())
                    } catch (ignored: Exception) {}

                    val progressIntent = Intent("DOWNLOAD_PROGRESS").apply {
                        putExtra("id", id)
                        putExtra("progress", progressInt)
                        putExtra("speed", speedText)
                        putExtra("downloadedSize", formatBytes(downloadedBytes))
                        putExtra("totalSize", formatBytes(totalBytes))
                        putExtra("downloadedBytes", downloadedBytes)
                        putExtra("totalBytes", totalBytes)
                        putExtra("eta", etaStr)
                        setPackage(packageName)
                    }
                    sendBroadcast(progressIntent)

                    if (now - lastPersistAt >= PERSIST_INTERVAL_MS) {
                        lastPersistAt = now
                        updateProgressPrefs(id, progressInt, speedText, downloadedBytes, totalBytes, etaStr)
                    }
                }

                val progressCallback: (Float, Long, String) -> Unit = { progress, _, line ->
                    if (!cancelledIds.contains(id) && !pausedIds.contains(id)) {
                        publishProgress(progress, line)
                    }
                }

                val request = buildRequest(
                    url = url,
                    formatId = formatId,
                    outputTemplate = outputTemplate,
                    referer = finalReferer,
                    isAudioOnly = isAudioOnly,
                    targetAudioExt = targetAudioExt,
                    isHls = hls,
                    isResume = isResume,
                    includeFormatSelector = true
                )

                fun publishProgressExplicit(
                    progress: Float,
                    speedBps: Long,
                    downloadedBytes: Long,
                    totalBytes: Long,
                    etaSec: Long,
                    force: Boolean = false
                ) {
                    val now = SystemClock.elapsedRealtime()
                    if (!force && now - lastNotifyAt < NOTIFY_INTERVAL_MS) return
                    lastNotifyAt = now

                    val speedText = formatSpeed(speedBps)
                    val progressInt = progress.toInt().coerceIn(0, 100)
                    val etaStr = if (etaSec > 0) "${etaSec}s" else "--:--"

                    builder.setProgress(100, progressInt, totalBytes <= 0L)
                    builder.setContentText("$speedText - $progressInt%")
                    try {
                        notificationManager.notify(notificationId, builder.build())
                    } catch (ignored: Exception) {}

                    val progressIntent = Intent("DOWNLOAD_PROGRESS").apply {
                        putExtra("id", id)
                        putExtra("progress", progressInt)
                        putExtra("speed", speedText)
                        putExtra("downloadedSize", formatBytes(downloadedBytes))
                        putExtra("totalSize", formatBytes(totalBytes))
                        putExtra("downloadedBytes", downloadedBytes)
                        putExtra("totalBytes", totalBytes)
                        putExtra("eta", etaStr)
                        setPackage(packageName)
                    }
                    sendBroadcast(progressIntent)

                    if (now - lastPersistAt >= PERSIST_INTERVAL_MS) {
                        lastPersistAt = now
                        updateProgressPrefs(id, progressInt, speedText, downloadedBytes, totalBytes, etaStr)
                    }
                }

                var downloadedFile: File? = null
                var downloadSuccess = false

                if (isExtractorSite) {
                    Log.i(TAG, "Extractor site detected ($url). Executing YoutubeDL first...")
                    var exitCode = -1
                    try {
                        exitCode = executeYoutubeDL(
                            request = request,
                            id = id,
                            progressCallback = progressCallback,
                            url = url,
                            outputTemplate = outputTemplate,
                            finalReferer = finalReferer,
                            isAudioOnly = isAudioOnly,
                            targetAudioExt = targetAudioExt,
                            hls = hls,
                            controller = controller
                        )
                    } catch (e: YoutubeDL.CanceledException) {
                        throw e
                    } catch (e: Exception) {
                        if (cancelledIds.contains(id) || pausedIds.contains(id) || controller.isInterrupted()) {
                            throw e
                        }
                        Log.w(TAG, "YoutubeDL engine execution failed: ${e.message}")
                        exitCode = -1
                    }

                    if (exitCode == 0) {
                        downloadSuccess = true
                    } else if (!cancelledIds.contains(id) && !pausedIds.contains(id) && !controller.isInterrupted()) {
                        Log.i(TAG, "YoutubeDL failed, attempting native downloader fallback...")
                        try {
                            if (hls) {
                                downloadedFile = withContext(Dispatchers.IO) {
                                    downloadNativeHls(url, audioUrl, masterUrl, tempDir, sanitizedTitle, finalReferer, format, controller) { percent, speed, down, total, eta ->
                                        publishProgressExplicit(percent, speed, down, total, eta)
                                    }
                                }
                            } else {
                                downloadedFile = withContext(Dispatchers.IO) {
                                    downloadDirectHttp(url, tempDir, sanitizedTitle, finalReferer, expectedSize, id, controller, isAudioOnly, targetAudioExt) { percent, speed, down, total, eta ->
                                        publishProgressExplicit(percent, speed, down, total, eta)
                                    }
                                }
                            }
                            downloadSuccess = downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "Native downloader fallback also failed: ${e.message}")
                        }
                    }
                } else if (hls) {
                    Log.i(TAG, "Engaging HlsDownloader native parallel engine for $id")
                    try {
                        downloadedFile = withContext(Dispatchers.IO) {
                            downloadNativeHls(url, audioUrl, masterUrl, tempDir, sanitizedTitle, finalReferer, format, controller) { percent, speed, down, total, eta ->
                                publishProgressExplicit(percent, speed, down, total, eta)
                            }
                        }
                        downloadSuccess = downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (cancelledIds.contains(id) || pausedIds.contains(id) || controller.isInterrupted()) {
                            throw e
                        }
                        Log.w(TAG, "Native HlsDownloader failed: ${e.message}. Falling back to YoutubeDL engine...")
                    }

                    if (!downloadSuccess && !cancelledIds.contains(id) && !pausedIds.contains(id) && !controller.isInterrupted()) {
                        try {
                            val ytdlTargetUrl = if (!masterUrl.isNullOrEmpty()) masterUrl else url
                            val exitCode = executeYoutubeDL(
                                request = request,
                                id = id,
                                progressCallback = progressCallback,
                                url = ytdlTargetUrl,
                                outputTemplate = outputTemplate,
                                finalReferer = finalReferer,
                                isAudioOnly = isAudioOnly,
                                targetAudioExt = targetAudioExt,
                                hls = hls,
                                controller = controller
                            )
                            downloadSuccess = (exitCode == 0)
                        } catch (e: YoutubeDL.CanceledException) {
                            throw e
                        } catch (e: Exception) {
                            if (cancelledIds.contains(id) || pausedIds.contains(id) || controller.isInterrupted()) throw e
                            Log.e(TAG, "YoutubeDL fallback also failed: ${e.message}")
                        }
                    }
                } else {
                    Log.i(TAG, "Engaging CustomFileDownloader native chunked engine for $id")
                    try {
                        downloadedFile = withContext(Dispatchers.IO) {
                            downloadDirectHttp(url, tempDir, sanitizedTitle, finalReferer, expectedSize, id, controller, isAudioOnly, targetAudioExt) { percent, speed, down, total, eta ->
                                publishProgressExplicit(percent, speed, down, total, eta)
                            }
                        }
                        downloadSuccess = downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (cancelledIds.contains(id) || pausedIds.contains(id) || controller.isInterrupted()) {
                            throw e
                        }
                        Log.w(TAG, "Native CustomFileDownloader failed: ${e.message}. Falling back to YoutubeDL engine...")
                    }

                    if (!downloadSuccess && !cancelledIds.contains(id) && !pausedIds.contains(id) && !controller.isInterrupted()) {
                        try {
                            val exitCode = executeYoutubeDL(
                                request = request,
                                id = id,
                                progressCallback = progressCallback,
                                url = url,
                                outputTemplate = outputTemplate,
                                finalReferer = finalReferer,
                                isAudioOnly = isAudioOnly,
                                targetAudioExt = targetAudioExt,
                                hls = hls,
                                controller = controller
                            )
                            downloadSuccess = (exitCode == 0)
                        } catch (e: YoutubeDL.CanceledException) {
                            throw e
                        } catch (e: Exception) {
                            if (cancelledIds.contains(id) || pausedIds.contains(id) || controller.isInterrupted()) throw e
                            Log.e(TAG, "YoutubeDL fallback also failed: ${e.message}")
                        }
                    }
                }

                if (downloadSuccess) {
                    val produced = downloadedFile?.takeIf { it.exists() && it.length() > 0 }
                        ?: findProducedFile(tempDir, sanitizedTitle, destDir)
                    if (produced == null) {
                        throw Exception("Download finished but no video file was produced")
                    }

                    val finalFile = moveToLibrary(produced, destDir)
                    finalFilePath = finalFile.absolutePath
                    try {
                        tempDir.deleteRecursively()
                    } catch (ignored: Exception) {}

                    logDebug("DOWNLOAD_SUCCESS: id=$id, path=$finalFilePath")
                    updatePrefs(id, "Completed", finalFilePath ?: "")
                    publishProgress(100f, "Download complete", force = true)

                    builder.setContentText("Download complete")
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                    try {
                        notificationManager.notify(notificationId, builder.build())
                    } catch (ignored: Exception) {}

                    val successIntent = Intent("DOWNLOAD_SUCCESS").apply {
                        putExtra("id", id)
                        putExtra("title", title)
                        putExtra("path", finalFilePath ?: "")
                        setPackage(packageName)
                    }
                    sendBroadcast(successIntent)
                } else {
                    logDebug("DOWNLOAD_FAILED: id=$id, lastLine=$lastLogLine")
                    Log.e(TAG, "Download failed for id: $id, last line: $lastLogLine")
                    updatePrefs(id, "Failed", "")
                    builder.setContentText("Download failed")
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                    try {
                        notificationManager.notify(notificationId, builder.build())
                    } catch (ignored: Exception) {}

                    val failedIntent = Intent("DOWNLOAD_FAILED").apply {
                        putExtra("id", id)
                        putExtra("error", if (lastLogLine.isNotBlank()) lastLogLine else "Download could not be completed")
                        setPackage(packageName)
                    }
                    sendBroadcast(failedIntent)
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Download cancelled by coroutine for ID: $id")
            } catch (e: YoutubeDL.CanceledException) {
                Log.d(TAG, "Download process cancelled for ID: $id")
            } catch (e: Exception) {
                if (cancelledIds.contains(id)) {
                    logDebug("DOWNLOAD_CANCELLED_DURING_WORK: id=$id")
                } else if (pausedIds.contains(id)) {
                    logDebug("DOWNLOAD_PAUSED_DURING_WORK: id=$id")
                } else {
                    logDebug("DOWNLOAD_EXCEPTION: id=$id, error=${e.message}")
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
                    try {
                        notificationManager.notify(notificationId, builder.build())
                    } catch (ignored: Exception) {}

                    val failedIntent = Intent("DOWNLOAD_FAILED").apply {
                        putExtra("id", id)
                        putExtra("error", cleanError)
                        setPackage(packageName)
                    }
                    sendBroadcast(failedIntent)
                }
            } finally {
                synchronized(downloadJobs) {
                    downloadJobs.remove(id)
                }
                cancelledIds.remove(id)
                pausedIds.remove(id)
                try {
                    notificationManager.cancel(notificationId)
                } catch (ignored: Exception) {}
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

        val shouldStart = synchronized(downloadJobs) {
            if (cancelledIds.contains(id) || pausedIds.contains(id)) {
                false
            } else {
                downloadJobs[id] = job
                true
            }
        }
        if (shouldStart) {
            job.start()
        } else {
            job.cancel()
        }
    }

    private fun buildRequest(
        url: String,
        formatId: String?,
        outputTemplate: String,
        referer: String?,
        isAudioOnly: Boolean,
        targetAudioExt: String?,
        isHls: Boolean,
        isResume: Boolean,
        includeFormatSelector: Boolean
    ): YoutubeDLRequest {
        val isDirectFormat = formatId?.startsWith("http://") == true || formatId?.startsWith("https://") == true
        return YoutubeDLRequest(url).apply {
            if (includeFormatSelector && !isAudioOnly && !formatId.isNullOrEmpty() &&
                !isDirectFormat && formatId != "best" && formatId != "auto"
            ) {
                // Video-only formats need an audio track; the trailing fallback keeps
                // already-muxed formats working when bestaudio is unavailable.
                val selector = if (formatId.contains("+")) formatId else "$formatId+bestaudio/$formatId"
                addOption("-f", selector)
                addOption("--merge-output-format", "mp4")
            }
            if (isAudioOnly) {
                addOption("-x")
                addOption("--audio-format", targetAudioExt ?: "m4a")
                addOption("--audio-quality", "0")
            }
            addOption("-o", outputTemplate)
            addOption("--no-warnings")
            addOption("--no-playlist")
            addOption("--newline")
            addOption("--progress")
            addOption("--progress-delta", "2")
            addOption("--user-agent", USER_AGENT)

            if (!referer.isNullOrEmpty()) {
                addOption("--referer", referer)
            }

            try {
                val cookie = CookieManager.getInstance().getCookie(url)
                if (!cookie.isNullOrEmpty()) {
                    addOption("--add-header", "Cookie:$cookie")
                }
            } catch (ignored: Exception) {}

            if (isHls) {
                addOption("--hls-prefer-native")
                addOption("--hls-use-mpegts")
            } else {
                addOption("--buffer-size", "16M")
            }
            addOption("-N", "16")
            addOption("--retries", "10")
            addOption("--fragment-retries", "10")
            if (isResume) {
                addOption("--continue")
            }
        }
    }

    private fun findProducedFile(tempDir: File, sanitizedTitle: String, destDir: File): File? {
        val mediaExtensions = setOf("mp4", "mkv", "webm", "ts", "mp3", "m4a", "aac", "flv")
        val candidates = tempDir.walkTopDown()
            .filter {
                it.isFile &&
                it.extension.lowercase() in mediaExtensions &&
                !it.name.contains(".part") &&
                !it.name.startsWith("segment_") &&
                !it.name.startsWith("audio_segment_") &&
                !it.name.startsWith("init") &&
                !it.path.contains(".hls_segments") &&
                it.length() > 64 * 1024
            }
            .toList()

        return candidates.filter { it.name.startsWith(sanitizedTitle) }.maxByOrNull { it.lastModified() }
            ?: candidates.maxByOrNull { it.lastModified() }
            ?: destDir.listFiles()
                ?.filter {
                    it.isFile &&
                    it.name.startsWith(sanitizedTitle) &&
                    !it.name.contains(".part") &&
                    !it.name.startsWith("segment_") &&
                    !it.name.startsWith("audio_segment_") &&
                    !it.name.startsWith("init") &&
                    !it.path.contains(".hls_segments") &&
                    it.length() > 64 * 1024
                }
                ?.maxByOrNull { it.lastModified() }
    }

    private fun moveToLibrary(source: File, destDir: File): File {
        if (source.parentFile?.canonicalPath == destDir.canonicalPath) {
            return source
        }
        val target = File(destDir, source.name)
        if (target.exists()) target.delete()
        try {
            source.renameTo(target)
        } catch (ignored: Exception) {
        }
        if (!target.exists()) {
            source.inputStream().use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            }
            source.delete()
        }
        return target
    }

    private suspend fun executeYoutubeDL(
        request: YoutubeDLRequest,
        id: String,
        progressCallback: (Float, Long, String) -> Unit,
        url: String,
        outputTemplate: String,
        finalReferer: String?,
        isAudioOnly: Boolean,
        targetAudioExt: String?,
        hls: Boolean,
        controller: FileBasedDownloadController
    ): Int = withContext(Dispatchers.IO) {
        try {
            val response = YoutubeDL.getInstance().execute(request, id, progressCallback)
            response.exitCode
        } catch (e: YoutubeDL.CanceledException) {
            throw e
        } catch (primaryEx: Exception) {
            if (cancelledIds.contains(id) || pausedIds.contains(id) || controller.isInterrupted()) {
                throw primaryEx
            }
            Log.w(TAG, "Primary YoutubeDL attempt failed (${primaryEx.message}). Trying safe fallback...", primaryEx)
            val fallbackRequest = buildRequest(
                url = url,
                formatId = null,
                outputTemplate = outputTemplate,
                referer = finalReferer,
                isAudioOnly = isAudioOnly,
                targetAudioExt = targetAudioExt,
                isHls = hls,
                isResume = true,
                includeFormatSelector = false
            )
            val response = YoutubeDL.getInstance().execute(fallbackRequest, id, progressCallback)
            response.exitCode
        }
    }

    private fun downloadDirectHttp(
        url: String,
        tempDir: File,
        fileName: String,
        referer: String?,
        expectedSize: String,
        id: String,
        controller: FileBasedDownloadController,
        isAudioOnly: Boolean = false,
        targetAudioExt: String? = null,
        onProgress: (Float, Long, Long, Long, Long) -> Unit
    ): File? {
        val extension = if (isAudioOnly) {
            targetAudioExt ?: "m4a"
        } else {
            try {
                val path = android.net.Uri.parse(url).lastPathSegment.orEmpty()
                val ext = path.substringAfterLast('.', "").lowercase()
                if (ext in setOf("mp4", "mkv", "webm", "flv", "m4a", "mp3", "aac", "ts")) ext else "mp4"
            } catch (ignored: Exception) {
                "mp4"
            }
        }

        if (!tempDir.exists()) tempDir.mkdirs()
        val targetFile = File(tempDir, "$fileName.$extension")

        val headersMap = mutableMapOf<String, String>()
        headersMap["User-Agent"] = USER_AGENT
        headersMap["Accept"] = "*/*"
        if (!referer.isNullOrEmpty()) headersMap["Referer"] = referer
        try {
            val cookie = CookieManager.getInstance().getCookie(url)
            if (!cookie.isNullOrEmpty()) headersMap["Cookie"] = cookie
        } catch (ignored: Exception) {}

        var caughtFailure: Throwable? = null
        val customDownloader = CustomFileDownloader(
            url = java.net.URL(url),
            file = targetFile,
            threadCount = 6,
            headers = headersMap,
            client = httpClient,
            controller = controller,
            listener = object : DownloadListener {
                override fun onSuccess() {}
                override fun onFailure(e: Throwable) {
                    caughtFailure = e
                }
                override fun onProgressUpdate(downloadedBytes: Long, totalBytes: Long, speedBps: Long) {
                    val percent = if (totalBytes > 0) (downloadedBytes * 100f / totalBytes) else 0f
                    val etaSec = if (speedBps > 0 && totalBytes > downloadedBytes) (totalBytes - downloadedBytes) / speedBps else 0L
                    onProgress(percent, speedBps, downloadedBytes, totalBytes, etaSec)
                }
            }
        )

        customDownloader.download()
        val failure = caughtFailure
        if (failure != null) {
            if (controller.isInterrupted() || failure.message in setOf(
                    CustomFileDownloader.PAUSE_ACTION,
                    CustomFileDownloader.CANCELED_ACTION,
                    CustomFileDownloader.STOPPED_AND_SAVE_ACTION
                )
            ) {
                throw CancellationException(failure.message)
            }
            throw if (failure is Exception) failure else RuntimeException(failure)
        }
        return if (targetFile.exists() && targetFile.length() > 0) targetFile else null
    }

    private suspend fun downloadNativeHls(
        url: String,
        audioUrl: String?,
        masterUrl: String?,
        tempDir: File,
        fileName: String,
        referer: String?,
        format: String = "mp4",
        controller: FileBasedDownloadController,
        onProgress: (Float, Long, Long, Long, Long) -> Unit
    ): File? {
        val headersMap = mutableMapOf<String, String>()
        headersMap["User-Agent"] = USER_AGENT
        headersMap["Accept"] = "*/*"
        if (!referer.isNullOrEmpty()) headersMap["Referer"] = referer
        try {
            val cookie = CookieManager.getInstance().getCookie(url)
            if (!cookie.isNullOrEmpty()) headersMap["Cookie"] = cookie
        } catch (ignored: Exception) {}

        val hlsDownloader = HlsDownloader(
            httpClient = httpClient,
            headers = headersMap,
            threadCount = 6,
            controller = controller,
            onProgress = { percent, speedBps, downloadedBytes, totalBytes, etaSec ->
                onProgress(percent, speedBps, downloadedBytes, totalBytes, etaSec)
            }
        )

        return hlsDownloader.download(
            m3u8Url = url,
            audioM3u8Url = audioUrl,
            masterM3u8Url = masterUrl,
            downloadDir = tempDir,
            outputFileName = fileName,
            outputFormat = format,
            context = this@DownloadService
        )
    }

    fun pauseDownload(id: String) {
        pausedIds.add(id)
        try {
            val tempDir = File(cacheDir, "downloads/$id")
            FileBasedDownloadController(tempDir).requestPause()
        } catch (ignored: Exception) {}
        val job = synchronized(downloadJobs) {
            downloadJobs[id]
        }
        job?.cancel()
        try {
            YoutubeDL.getInstance().destroyProcessById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Pause process failed", e)
        }
        updatePrefs(id, "PAUSED", "")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.cancel(id.hashCode() and 0x7FFFFFFF)
        } catch (ignored: Exception) {}
    }

    fun cancelDownload(id: String) {
        cancelledIds.add(id)
        try {
            val tempDir = File(cacheDir, "downloads/$id")
            FileBasedDownloadController(tempDir).requestCancel()
        } catch (ignored: Exception) {}
        val job = synchronized(downloadJobs) {
            downloadJobs.remove(id)
        }
        job?.cancel()
        try {
            YoutubeDL.getInstance().destroyProcessById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Cancel process failed", e)
        }

        try {
            File(cacheDir, "downloads/$id").deleteRecursively()
        } catch (ignored: Exception) {}
        removeModelFromPrefs(id)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.cancel(id.hashCode() and 0x7FFFFFFF)
        } catch (ignored: Exception) {}

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

    private fun formatSpeed(bps: Long): String = when {
        bps >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f GB/s", bps / (1024.0 * 1024.0 * 1024.0))
        bps >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB/s", bps / (1024.0 * 1024.0))
        bps >= 1024L -> String.format(Locale.US, "%.1f KB/s", bps / 1024.0)
        else -> "$bps B/s"
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

    private fun updateProgressPrefs(
        id: String,
        progress: Int,
        speed: String,
        downloadedBytes: Long,
        totalBytes: Long,
        eta: String
    ) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(SAVED_DOWNLOADS_KEY, "[]") ?: "[]"
            val type = object : TypeToken<MutableList<DownloadModel>>() {}.type
            val list: MutableList<DownloadModel> = Gson().fromJson(json, type) ?: mutableListOf()
            val item = list.firstOrNull { it.id == id } ?: return
            item.progress = progress
            item.speed = speed
            item.eta = eta
            item.downloadedBytes = downloadedBytes
            item.totalBytes = totalBytes
            item.downloadedSize = formatBytes(downloadedBytes)
            item.totalSize = formatBytes(totalBytes)
            item.status = "DOWNLOADING"
            prefs.edit().putString(SAVED_DOWNLOADS_KEY, Gson().toJson(list)).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update progress prefs", e)
        }
    }

    private fun removeModelFromPrefs(id: String) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(SAVED_DOWNLOADS_KEY, "[]") ?: "[]"
            val type = object : TypeToken<MutableList<DownloadModel>>() {}.type
            val list: MutableList<DownloadModel> = Gson().fromJson(json, type) ?: mutableListOf()
            if (list.removeAll { it.id == id }) {
                prefs.edit().putString(SAVED_DOWNLOADS_KEY, Gson().toJson(list)).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove model from prefs", e)
        }
    }

    private fun parseSize(value: String, unit: String): Long {
        val number = value.toDoubleOrNull() ?: return 0L
        return when (unit) {
            "GiB", "GB" -> (number * 1024 * 1024 * 1024).toLong()
            "MiB", "MB" -> (number * 1024 * 1024).toLong()
            "KiB", "KB" -> (number * 1024).toLong()
            else -> number.toLong()
        }
    }

    private fun parseReadableSize(text: String): Long {
        if (text.isBlank()) return 0L
        val match = Regex("([\\d.]+)\\s*([KMGT]?i?B)").find(text) ?: return 0L
        return parseSize(match.groupValues[1], match.groupValues[2])
    }

    private fun parseEta(eta: String): Long {
        return try {
            val parts = eta.split(":")
            when (parts.size) {
                2 -> (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
                3 -> (parts[0].toLongOrNull() ?: 0L) * 3600 + (parts[1].toLongOrNull() ?: 0L) * 60 + (parts[2].toLongOrNull() ?: 0L)
                else -> 0L
            }
        } catch (ignored: Exception) {
            0L
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
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
