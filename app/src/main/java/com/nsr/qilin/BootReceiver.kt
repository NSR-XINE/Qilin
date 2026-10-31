package com.nsr.qilin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.nsr.qilin.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
            Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
            val pendingResult = goAsync()
            rescheduleAlarms(context, pendingResult)
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun rescheduleAlarms(context: Context, pendingResult: BroadcastReceiver.PendingResult? = null) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Qilin:AlarmReschedule")
            wakeLock?.acquire(30000L)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val showsList = db.showDao().getAllShowsList()
                    val ongoing = showsList.filter { it.status == "ongoing" }

                    ongoing.forEach { show ->
                        val code = show.alarmRequestCode
                        if (code != null) {
                            AlarmScheduler.schedule(
                                context = context,
                                showId = show.id,
                                title = show.title,
                                releaseDay = show.releaseDay ?: "Sunday",
                                releaseTime = show.releaseTime ?: "10:00",
                                alarmCode = code,
                                currentEp = show.currentEp
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (wakeLock != null && wakeLock.isHeld) {
                        try {
                            wakeLock.release()
                        } catch (ignored: Exception) {
                        }
                    }
                    try {
                        pendingResult?.finish()
                    } catch (ignored: Exception) {
                    }
                }
            }
        }
    }
}
