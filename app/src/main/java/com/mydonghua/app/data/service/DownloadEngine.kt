package com.mydonghua.app.data.service

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
 * Fresh High-Performance Download Engine powered by YoutubeDL + Aria2c 16-connection acceleration.
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

                        val current = activeTasks[id]
                        if (current != null) {
                            activeTasks[id] = current.copy(
                                status = DownloadStatus.DOWNLOADING,
                                progress = progress,
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
                            activeTasks[id] = current.copy(
                                status = DownloadStatus.COMPLETED,
                                progress = 100f,
                                speedBps = 0L,
                                etaSeconds = 0L,
                                filePath = path
                            )
                            _tasksState.value = activeTasks.values.toList()
                        }
                        showToast(app, "Download complete: ${current?.title ?: "Video"}")
                    }
                    "DOWNLOAD_FAILED" -> {
                        val error = intent.getStringExtra("error") ?: "Download failed or interrupted"
                        val current = activeTasks[id]
                        if (current != null) {
                            activeTasks[id] = current.copy(
                                status = DownloadStatus.FAILED,
                                speedBps = 0L,
                                etaSeconds = 0L,
                                errorMessage = error
                            )
                            _tasksState.value = activeTasks.values.toList()
                        }
                        showToast(app, "Download failed: $error")
                    }
                    "DOWNLOAD_CANCELLED" -> {
                        activeTasks.remove(id)
                        _tasksState.value = activeTasks.values.toList()
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
        isAudioOnly: Boolean = false
    ) {
        val app = context.applicationContext
        val cleanTitle = title.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim()
        val taskId = UUID.randomUUID().toString()

        val task = DownloadTask(
            id = taskId,
            title = cleanTitle,
            url = url,
            status = DownloadStatus.DOWNLOADING,
            progress = 0f,
            numThreads = 16,
            isHls = url.contains(".m3u8", ignoreCase = true) || url.contains("/hls/", ignoreCase = true),
            formatId = formatId,
            referer = referer,
            isAudioOnly = isAudioOnly
        )
        activeTasks[taskId] = task
        _tasksState.value = activeTasks.values.toList()

        val saveDir = getPrimaryStorageDir(app)

        saveModelToPrefs(app, DownloadModel(
            id = taskId,
            title = cleanTitle,
            progress = 0,
            speed = "0 MB/s",
            status = "DOWNLOADING"
        ))

        val serviceIntent = Intent(app, DownloadService::class.java).apply {
            action = "START_DOWNLOAD"
            putExtra("url", url)
            putExtra("downloadId", taskId)
            putExtra("title", cleanTitle)
            putExtra("path", saveDir.absolutePath)
            if (!formatId.isNullOrEmpty()) {
                putExtra("formatId", formatId)
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
            showToast(app, if (isAudioOnly) "Extracting audio with high speed..." else "Accelerating download with 16 streams...")
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
    }

    fun pauseDownload(context: Context, id: String) {
        val current = activeTasks[id] ?: return
        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            action = "CANCEL_DOWNLOAD"
            putExtra("downloadId", id)
        }
        context.startService(serviceIntent)

        activeTasks[id] = current.copy(status = DownloadStatus.PAUSED, speedBps = 0L)
        _tasksState.value = activeTasks.values.toList()
    }

    fun resumeDownload(context: Context, id: String) {
        val current = activeTasks[id] ?: return
        startDownload(
            context = context,
            url = current.url,
            title = current.title,
            formatId = current.formatId,
            referer = current.referer,
            isAudioOnly = current.isAudioOnly
        )
    }

    fun getPrimaryStorageDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val dir = File(base, "MyDonghua")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

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

    private fun saveModelToPrefs(context: Context, model: DownloadModel) {
        try {
            val prefs = context.getSharedPreferences(DownloadService.PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(DownloadService.SAVED_DOWNLOADS_KEY, "[]") ?: "[]"
            val type = object : TypeToken<MutableList<DownloadModel>>() {}.type
            val list: MutableList<DownloadModel> = Gson().fromJson(json, type) ?: mutableListOf()
            list.removeAll { it.id == model.id }
            list.add(0, model)
            prefs.edit().putString(DownloadService.SAVED_DOWNLOADS_KEY, Gson().toJson(list)).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save model to prefs", e)
        }
    }

    private fun loadSavedDownloads(context: Context) {
        try {
            val prefs = context.getSharedPreferences(DownloadService.PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(DownloadService.SAVED_DOWNLOADS_KEY, "[]") ?: "[]"
            val type = object : TypeToken<List<DownloadModel>>() {}.type
            val list: List<DownloadModel> = Gson().fromJson(json, type) ?: emptyList()
            for (model in list) {
                if (model.status == "DOWNLOADING") {
                    val task = DownloadTask(
                        id = model.id,
                        title = model.title,
                        url = "",
                        status = DownloadStatus.PAUSED,
                        progress = model.progress.toFloat(),
                        filePath = model.filePath.takeIf { it.isNotEmpty() }
                    )
                    activeTasks[model.id] = task
                }
            }
            _tasksState.value = activeTasks.values.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load saved downloads", e)
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (ignored: Exception) {}
        }
    }
}
