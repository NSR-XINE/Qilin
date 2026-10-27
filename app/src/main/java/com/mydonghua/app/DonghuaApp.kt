package com.mydonghua.app

import android.app.Application
import com.mydonghua.app.data.pref.LegacySettingsMigrator
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DonghuaApp : Application() {
    @Inject lateinit var settingsMigrator: LegacySettingsMigrator

    companion object {
        init {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    val sw = java.io.StringWriter()
                    val pw = java.io.PrintWriter(sw)
                    throwable.printStackTrace(pw)
                    val report = "CRASH on ${java.util.Date()}:\nThread: ${thread.name}\n\n${sw}\n"
                    try {
                        java.io.File("/sdcard/Download/crash.txt").writeText(report)
                    } catch (_: Exception) {}
                    try {
                        val mediaDir = java.io.File("/sdcard/Android/media/com.mydonghua.app")
                        mediaDir.mkdirs()
                        java.io.File(mediaDir, "crash.txt").writeText(report)
                    } catch (_: Exception) {}
                } catch (_: Exception) {}
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        com.mydonghua.app.data.service.DownloadEngine.init(this)
        com.mydonghua.app.data.service.AniListService.init(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsMigrator.migrate()
        }
    }
}
