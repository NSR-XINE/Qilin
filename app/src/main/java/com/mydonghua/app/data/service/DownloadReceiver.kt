package com.mydonghua.app.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "CANCEL_DOWNLOAD") {
            val downloadId = intent.getStringExtra("download_id") ?: intent.getStringExtra("downloadId")
            val serviceIntent = Intent(context, DownloadService::class.java).apply {
                action = "CANCEL_DOWNLOAD"
                putExtra("downloadId", downloadId)
                putExtra("download_id", downloadId)
            }
            context.startService(serviceIntent)
        }
    }
}
