package com.mydonghua.app.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "CANCEL_DOWNLOAD") {
            val downloadId = intent.getStringExtra("download_id") ?: intent.getStringExtra("downloadId")
            val serviceIntent = Intent(context, DownloadService::class.java).apply {
                action = "CANCEL_DOWNLOAD"
                putExtra("downloadId", downloadId)
                putExtra("download_id", downloadId)
            }
            try {
                context.startService(serviceIntent)
            } catch (e: Exception) {
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e2: Exception) {
                    Log.e("DownloadReceiver", "Failed to deliver cancellation to DownloadService", e2)
                }
            }
        }
    }
}
