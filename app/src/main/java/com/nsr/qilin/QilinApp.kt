package com.nsr.qilin

import android.app.Application
import android.util.Log
import com.nsr.qilin.data.pref.LegacySettingsMigrator
import com.nsr.qilin.domain.repository.SettingsKeys
import com.nsr.qilin.domain.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class QilinApp : Application() {
    @Inject lateinit var settingsMigrator: LegacySettingsMigrator
    @Inject lateinit var settingsRepository: SettingsRepository

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

        com.nsr.qilin.ui.theme.Haptics.init(this)
        com.nsr.qilin.data.service.DownloadEngine.init(this)
        com.nsr.qilin.data.service.AniListService.init(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsMigrator.migrate()
            settingsRepository.getBoolFlow(SettingsKeys.HAPTICS_ENABLED, true).collect { enabled ->
                com.nsr.qilin.ui.theme.Haptics.enabled = enabled
            }
        }
    }
}
