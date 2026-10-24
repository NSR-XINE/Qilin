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

    override fun onCreate() {
        super.onCreate()
        com.mydonghua.app.data.service.DownloadEngine.init(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsMigrator.migrate()
        }
    }
}
