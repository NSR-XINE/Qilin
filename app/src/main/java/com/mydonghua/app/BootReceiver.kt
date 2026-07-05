package com.mydonghua.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.mydonghua.app.data.db.AppDatabase

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
            Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
            rescheduleAlarms(context)
        }
    }

    companion object {
        @JvmStatic
        fun rescheduleAlarms(context: Context) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyDonghua:AlarmReschedule")
            wakeLock?.acquire(30000L)

            Thread {
                kotlinx.coroutines.runBlocking {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val showsList = db.showDao().getAllShowsList()
                        val ongoing = showsList.filter { it.status == "ongoing" }

                        ongoing.forEach { show ->
                            AlarmScheduler.schedule(
                                context = context,
                                showId = show.id,
                                title = show.title,
                                releaseDay = show.releaseDay ?: "Sunday",
                                releaseTime = show.releaseTime ?: "10:00",
                                alarmCode = show.alarmRequestCode ?: Math.abs(show.id.hashCode()),
                                currentEp = show.currentEp
                            )
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
                    }
                }
            }.start()
        }
    }
}
