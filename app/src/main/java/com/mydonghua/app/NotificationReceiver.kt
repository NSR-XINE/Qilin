package com.mydonghua.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mydonghua.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        val showId = intent.getStringExtra("show_id")
        if (showId.isNullOrEmpty()) return

        val initialTitle = intent.getStringExtra("show_title")
        val releaseDay = intent.getStringExtra("release_day")
        val releaseTime = intent.getStringExtra("release_time")
        val alarmCode = intent.getIntExtra("alarm_code", -1)
        val initialCurrentEp = intent.getIntExtra("current_ep", 0)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verify show exists in Room DB and is still ongoing with alarms enabled
                val db = AppDatabase.getDatabase(context)
                val show = db.showDao().getShowById(showId)

                // If the show was deleted, completed/stopped, or alarm was cleared in UI:
                // Cancel the alarm in AlarmManager and abort without notifying or rescheduling.
                if (show == null || show.status != "ongoing" || show.alarmRequestCode == null || (alarmCode > 0 && show.alarmRequestCode != alarmCode)) {
                    if (alarmCode > 0) {
                        AlarmScheduler.cancel(context, showId, alarmCode)
                    }
                    return@launch
                }

                val title = show.title.ifEmpty { initialTitle ?: "A Tracked Donghua" }
                val currentEp = show.currentEp
                val day = show.releaseDay ?: releaseDay
                val time = show.releaseTime ?: releaseTime
                val code = show.alarmRequestCode ?: alarmCode

                val dedupKey = "$showId:$currentEp"
                val now = System.currentTimeMillis()
                val lastNotify = _recentNotifications[dedupKey]
                if (lastNotify != null && (now - lastNotify) < DEDUP_WINDOW_MS) return@launch
                _recentNotifications[dedupKey] = now

                if (_recentNotifications.size > 200) {
                    val cutoff = now - 300000
                    _recentNotifications.entries.removeIf { it.value < cutoff }
                }

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return@launch

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Show Countdown Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Alerts when a saved show is about to broadcast."
                        setShowBadge(true)
                    }
                    nm.createNotificationChannel(channel)
                }

                val activityIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("target_show_id", showId)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

                val requestCode = if (code > 0) code else (showId.hashCode() and 0x7FFFFFFF)
                var flags = PendingIntent.FLAG_UPDATE_CURRENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags = flags or PendingIntent.FLAG_IMMUTABLE
                }
                val pi = PendingIntent.getActivity(context, requestCode, activityIntent, flags)

                val nextEp = currentEp + 1
                val episodeText = "Episode $nextEp is airing now!"

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(episodeText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("$title - $episodeText"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (PackageManager.PERMISSION_GRANTED !=
                        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    ) {
                        return@launch
                    }
                }

                val notifyId = dedupKey.hashCode() and 0x7FFFFFFF
                nm.notify(notifyId, builder.build())

                if (day != null && time != null && code > 0) {
                    AlarmScheduler.schedule(context, showId, title, day, time, code, currentEp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "donghua_countdown_reminders"
        private const val DEDUP_WINDOW_MS = 5000L

        private val _recentNotifications = ConcurrentHashMap<String, Long>()
    }
}
