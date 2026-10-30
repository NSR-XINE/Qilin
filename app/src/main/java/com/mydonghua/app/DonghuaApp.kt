package com.mydonghua.app

import android.app.Application
import android.util.Log
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

    override fun onCreate() {
        super.onCreate()

        // Capture unhandled crashes. In debug builds the report is written to internal cache
        // storage (not public /sdcard/) so it is readable via adb pull without WRITE_EXTERNAL_STORAGE.
        // In release builds we only log to logcat; crash files are never written to public storage.
        if (BuildConfig.DEBUG) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    val sw = java.io.StringWriter()
                    val pw = java.io.PrintWriter(sw)
                    throwable.printStackTrace(pw)
                    val report = "CRASH on ${java.util.Date()}:\nThread: ${thread.name}\n\n${sw}\n"
                    // Write to app-private cache dir — no storage permission required, no Scoped Storage issue.
                    try {
                        java.io.File(cacheDir, "crash.txt").writeText(report)
                    } catch (_: Exception) {}
                } catch (_: Exception) {}
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        com.mydonghua.app.ui.theme.Haptics.init(this)
        com.mydonghua.app.data.service.DownloadEngine.init(this)
        com.mydonghua.app.data.service.AniListService.init(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsMigrator.migrate()
        }
    }
}
