package com.nsr.qilin.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBps: Long = 0L,
    val etaSeconds: Long = 0L,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val numThreads: Int = 16,
    val isHls: Boolean = false,
    val formatId: String? = null,
    val referer: String? = null,
    val isAudioOnly: Boolean = false
)

/**
 * High-Performance Download Engine powered by YoutubeDL + Aria2c 16-connection acceleration.
 *
 * Task metadata (URL, format, referer, progress) is persisted so paused downloads can be
 * resumed with `--continue` both during the session and after the process is restarted.
 */
object DownloadEngine {

    private const val TAG = "DownloadEngine"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeTasks = ConcurrentHashMap<String, DownloadTask>()
    private val _tasksState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasksState: StateFlow<List<DownloadTask>> = _tasksState.asStateFlow()

    private var isInitialized = false
    private var appContext: Context? = null

    fun init(context: Context) {
        if (isInitialized) return
        val app = context.applicationContext
        appContext = app

        // Initialize YoutubeDL, FFmpeg, Aria2c asynchronously
        scope.launch {
            try {
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
                Aria2c.getInstance().init(app)
                Log.d(TAG, "Fresh YoutubeDL, FFmpeg & Aria2c engines initialized successfully")

                try {
                    YoutubeDL.getInstance().updateYoutubeDL(app, YoutubeDL.UpdateChannel.STABLE)
                    Log.d(TAG, "yt-dlp core updated successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "yt-dlp core update skipped, using bundled release", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing fresh download engine", e)
            }
        }

        // Register broadcast receiver to bridge DownloadService events to tasksState
        val filter = IntentFilter().apply {
            addAction("DOWNLOAD_PROGRESS")
            addAction("UPDATE_DOWNLOAD_PROGRESS")
            addAction("DOWNLOAD_SUCCESS")
            addAction("DOWNLOAD_FAILED")
            addAction("DOWNLOAD_CANCELLED")
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                val id = intent.getStringExtra("id") ?: return

                when (intent.action) {
                    "DOWNLOAD_PROGRESS", "UPDATE_DOWNLOAD_PROGRESS" -> {
                        val progress = intent.getIntExtra("progress", 0).toFloat()
                        val speedStr = intent.getStringExtra("speed") ?: "0 MB/s"
                        val etaStr = intent.getStringExtra("eta") ?: "0s"
                        val speedBps = parseSpeedToBps(speedStr)
                        val etaSec = parseEtaToSeconds(etaStr)
                        val downloadedBytes = intent.getLongExtra("downloadedBytes", 0L)
                        val totalBytes = intent.getLongExtra("totalBytes", 0L)

                        val current = activeTasks[id]
                        if (current != null) {
                            activeTasks[id] = current.copy(
                                status = DownloadStatus.DOWNLOADING,
                                progress = progress,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                speedBps = speedBps,
                                etaSeconds = etaSec
                            )
                            _tasksState.value = activeTasks.values.toList()
                        }
                    }
                    "DOWNLOAD_SUCCESS" -> {
                        val path = intent.getStringExtra("path")
                        val current = activeTasks[id]
                        if (current != null) {
                            val completed = current.copy(
                                status = DownloadStatus.COMPLETED,
                                progress = 100f,
                                speedBps = 0L,
                                etaSeconds = 0L,
                                filePath = path
                            )
                            activeTasks[id] = completed
                            _tasksState.value = activeTasks.values.toList()
                            persistTask(app, completed, "Completed")
                        }
                        showToast(app, "Download complete: ${current?.title ?: "Video"}")
                    }
                    "DOWNLOAD_FAILED" -> {
                        val error = intent.getStringExtra("error") ?: "Download failed or interrupted"
                        val current = activeTasks[id]
                        if (current != null) {
                            val failed = current.copy(
                                status = DownloadStatus.FAILED,
                                speedBps = 0L,
                                etaSeconds = 0L,
                                errorMessage = error
                            )
                            activeTasks[id] = failed
                            _tasksState.value = activeTasks.values.toList()
                            persistTask(app, failed, "Failed")
                        }
                        showToast(app, "Download failed: $error")
                    }
                    "DOWNLOAD_CANCELLED" -> {
                        activeTasks.remove(id)
                        _tasksState.value = activeTasks.values.toList()
                        removeModelFromPrefs(app, id)
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(receiver, filter)
        }

        loadSavedDownloads(app)
        isInitialized = true
    }

    fun startDownload(
        context: Context,
        url: String,
        title: String,
        formatId: String? = null,
        referer: String? = null,
        isAudioOnly: Boolean = false,
        resumeId: String? = null,
        resumeProgress: Float = 0f,
        isResume: Boolean = false
    ) {
        val app = context.applicationContext
        val cleanTitle = title.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim()
        val taskId = resumeId ?: UUID.randomUUID().toString()

        val isDirectUrlFormat = formatId?.startsWith("http://") == true || formatId?.startsWith("https://") == true
        val effectiveUrl = if (isDirectUrlFormat) formatId!! else url
        val effectiveFormatId = if (isDirectUrlFormat) null else formatId
        val isHls = effectiveUrl.contains(".m3u8", ignoreCase = true) || effectiveUrl.contains("/hls/", ignoreCase = true)

        val task = DownloadTask(
            id = taskId,
            title = cleanTitle,
            url = effectiveUrl,
            status = DownloadStatus.DOWNLOADING,
            progress = resumeProgress,
            numThreads = 16,
            isHls = isHls,
            formatId = effectiveFormatId,
            referer = referer,
            isAudioOnly = isAudioOnly
        )
        activeTasks[taskId] = task
        _tasksState.value = activeTasks.values.toList()

        val saveDir = getPrimaryStorageDir(app)

        persistTask(app, task, "DOWNLOADING")

        val serviceIntent = Intent(app, DownloadService::class.java).apply {
            action = "START_DOWNLOAD"
            putExtra("url", effectiveUrl)
            putExtra("downloadId", taskId)
            putExtra("title", cleanTitle)
            putExtra("path", saveDir.absolutePath)
            putExtra("isHls", isHls)
            putExtra("resume", isResume)
            if (!effectiveFormatId.isNullOrEmpty()) {
                putExtra("formatId", effectiveFormatId)
            }
            if (!referer.isNullOrEmpty()) {
                putExtra("referer", referer)
            }
            putExtra("isAudioOnly", isAudioOnly)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(serviceIntent)
            } else {
                app.startService(serviceIntent)
            }
            showToast(
                app,
                when {
                    isResume -> "Resuming download..."
                    isAudioOnly -> "Extracting audio with high speed..."
                    else -> "Accelerating download with 16 streams..."
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start DownloadService", e)
            showToast(app, "Error starting download: ${e.message}")
        }
    }

    fun cancelDownload(id: String) {
        val app = appContext ?: return
        val serviceIntent = Intent(app, DownloadService::class.java).apply {
            action = "CANCEL_DOWNLOAD"
            putExtra("downloadId", id)
        }
        app.startService(serviceIntent)

        activeTasks.remove(id)
        _tasksState.value = activeTasks.values.toList()
        removeModelFromPrefs(app, id)
    }

    fun pauseDownload(context: Context, id: String) {
        val current = activeTasks[id] ?: return
        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            action = "PAUSE_DOWNLOAD"
            putExtra("downloadId", id)
        }
        context.startService(serviceIntent)

        val paused = current.copy(status = DownloadStatus.PAUSED, speedBps = 0L, etaSeconds = 0L)
        activeTasks[id] = paused
        _tasksState.value = activeTasks.values.toList()
        persistTask(context.applicationContext, paused, "PAUSED")
    }

    fun resumeDownload(context: Context, id: String) {
        val current = activeTasks[id] ?: return
        val url = current.url.ifBlank { loadPersistedModel(context, id)?.url.orEmpty() }
        if (url.isBlank()) {
            showToast(context, "Cannot resume: stream URL is no longer available")
            return
        }
        startDownload(
            context = context,
            url = url,
            title = current.title,
            formatId = current.formatId,
            referer = current.referer,
            isAudioOnly = current.isAudioOnly,
            resumeId = current.id,
            resumeProgress = current.progress,
            isResume = true
        )
    }

    fun getPrimaryStorageDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val dir = File(base, "Qilin")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAllStorageDirs(context: Context): List<File> {
        val dirs = LinkedHashSet<File>()
        dirs.add(getPrimaryStorageDir(context))
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val legacyDir = File(base, "MyDonghua")
        if (legacyDir.exists()) dirs.add(legacyDir)
        try {
            val pubMovies = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Qilin")
            if (pubMovies.exists()) dirs.add(pubMovies)
            val legacyPubMovies = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MyDonghua")
            if (legacyPubMovies.exists()) dirs.add(legacyPubMovies)
        } catch (ignored: Exception) {}
        try {
            val pubDownloads = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Qilin")
            if (pubDownloads.exists()) dirs.add(pubDownloads)
            val legacyPubDownloads = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MyDonghua")
            if (legacyPubDownloads.exists()) dirs.add(legacyPubDownloads)
        } catch (ignored: Exception) {}
        return dirs.toList()
    }

    fun isDownloaded(context: Context, title: String): Boolean {
        return getLocalFileForTitle(context, title) != null
    }

    fun getLocalFileForTitle(context: Context, title: String): File? {
        val sanitized = title.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
        for (dir in getAllStorageDirs(context)) {
            val files = dir.listFiles() ?: continue
            for (file in files) {
                if (!file.isFile) continue
                val name = file.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
                if (name == sanitized || name.contains(sanitized) || sanitized.contains(name)) {
                    if (file.length() > 1024 * 1024) {
                        return file
                    }
                }
            }
        }
        return null
    }

    private fun parseSpeedToBps(speedStr: String): Long {
        return try {
            val trimmed = speedStr.trim()
            val num = trimmed.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            when {
                trimmed.contains("GiB", ignoreCase = true) || trimmed.contains("GB", ignoreCase = true) -> (num * 1024 * 1024 * 1024).toLong()
                trimmed.contains("MiB", ignoreCase = true) || trimmed.contains("MB", ignoreCase = true) -> (num * 1024 * 1024).toLong()
                trimmed.contains("KiB", ignoreCase = true) || trimmed.contains("KB", ignoreCase = true) -> (num * 1024).toLong()
                else -> num.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun parseEtaToSeconds(etaStr: String): Long {
        return try {
            val trimmed = etaStr.replace("s", "").trim()
            if (trimmed.contains(":")) {
                val parts = trimmed.split(":")
                if (parts.size == 2) {
                    (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
                } else if (parts.size == 3) {
                    (parts[0].toLongOrNull() ?: 0L) * 3600 + (parts[1].toLongOrNull() ?: 0L) * 60 + (parts[2].toLongOrNull() ?: 0L)
                } else 0L
            } else {
                trimmed.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun readModels(context: Context): MutableList<DownloadModel> {
        return try {
            val prefs = context.getSharedPreferences(DownloadService.PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(DownloadService.SAVED_DOWNLOADS_KEY, "[]") ?: "[]"
            val type = object : TypeToken<MutableList<DownloadModel>>() {}.type
            Gson().fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read saved downloads", e)
            mutableListOf()
        }
    }

    private fun writeModels(context: Context, list: List<DownloadModel>) {
        try {
            val prefs = context.getSharedPreferences(DownloadService.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(DownloadService.SAVED_DOWNLOADS_KEY, Gson().toJson(list)).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write saved downloads", e)
        }
    }

    private fun persistTask(context: Context, task: DownloadTask, status: String) {
        try {
            val list = readModels(context)
            list.removeAll { it.id == task.id }
            list.add(
                0,
                DownloadModel(
                    id = task.id,
                    title = task.title,
                    progress = task.progress.toInt().coerceIn(0, 100),
                    speed = formatSpeed(task.speedBps),
                    status = status,
                    filePath = task.filePath.orEmpty(),
                    downloadedSize = formatBytes(task.downloadedBytes),
                    totalSize = formatBytes(task.totalBytes),
                    eta = if (task.etaSeconds > 0) "${task.etaSeconds}s" else "--:--",
                    url = task.url,
                    formatId = task.formatId.orEmpty(),
                    referer = task.referer.orEmpty(),
                    isAudioOnly = task.isAudioOnly,
                    isHls = task.isHls,
                    downloadedBytes = task.downloadedBytes,
                    totalBytes = task.totalBytes,
                    errorMessage = task.errorMessage.orEmpty()
                )
            )
            writeModels(context, list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist task", e)
        }
    }

    private fun removeModelFromPrefs(context: Context, id: String) {
        try {
            val list = readModels(context)
            if (list.removeAll { it.id == id }) {
                writeModels(context, list)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove saved download", e)
        }
    }

    private fun loadPersistedModel(context: Context, id: String): DownloadModel? {
        return readModels(context).firstOrNull { it.id == id }
    }

    private fun loadSavedDownloads(context: Context) {
        try {
            val list = readModels(context)
            var changed = false
            for (model in list) {
                val status = model.status.orEmpty()
                if (status == "DOWNLOADING" || status == "PAUSED") {
                    if (status == "DOWNLOADING") {
                        model.status = "PAUSED"
                        changed = true
                    }
                    val task = DownloadTask(
                        id = model.id,
                        title = model.title,
                        url = model.url.orEmpty(),
                        status = DownloadStatus.PAUSED,
                        progress = model.progress.toFloat().coerceIn(0f, 100f),
                        downloadedBytes = model.downloadedBytes,
                        totalBytes = model.totalBytes,
                        filePath = model.filePath.orEmpty().takeIf { it.isNotEmpty() },
                        isHls = model.isHls,
                        formatId = model.formatId.orEmpty().takeIf { it.isNotEmpty() },
                        referer = model.referer.orEmpty().takeIf { it.isNotEmpty() },
                        isAudioOnly = model.isAudioOnly
                    )
                    activeTasks[model.id] = task
                }
            }
            if (changed) writeModels(context, list)
            _tasksState.value = activeTasks.values.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load saved downloads", e)
        }
    }

    private fun formatSpeed(bps: Long): String = when {
        bps >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f GB/s", bps / (1024.0 * 1024.0 * 1024.0))
        bps >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB/s", bps / (1024.0 * 1024.0))
        bps >= 1024L -> String.format(java.util.Locale.US, "%.1f KB/s", bps / 1024.0)
        else -> "$bps B/s"
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (ignored: Exception) {}
        }
    }
}
