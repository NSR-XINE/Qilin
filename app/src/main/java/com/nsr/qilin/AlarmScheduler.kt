package com.nsr.qilin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

object AlarmScheduler {

    private const val MIN_SCHEDULE_INTERVAL_MS = 60000L
    private val _lastScheduled = ConcurrentHashMap<String, Long>()

    @JvmStatic
    fun schedule(context: Context, showId: String, title: String?, releaseDay: String, releaseTime: String, alarmCode: Int) {
        schedule(context, showId, title, releaseDay, releaseTime, alarmCode, 0)
    }

    @JvmStatic
    fun schedule(context: Context, showId: String?, title: String?, releaseDay: String?, releaseTime: String?, alarmCode: Int, currentEp: Int) {
        if (showId.isNullOrEmpty()) return
        if (releaseDay == null || releaseTime == null) return

        val dedupKey = "$showId|$releaseDay|$releaseTime"
        val now = System.currentTimeMillis()
        val lastTime = _lastScheduled[dedupKey]
        if (lastTime != null && (now - lastTime) < MIN_SCHEDULE_INTERVAL_MS) return
        _lastScheduled[dedupKey] = now

        val effectiveCode = if (alarmCode > 0) alarmCode else (showId.hashCode() and 0x7FFFFFFF)

        cancel(context, showId, effectiveCode)

        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("show_id", showId)
            putExtra("show_title", title ?: "")
            putExtra("release_day", releaseDay)
            putExtra("release_time", releaseTime)
            putExtra("current_ep", currentEp)
            putExtra("alarm_code", effectiveCode)
        }

        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val pi = PendingIntent.getBroadcast(context, effectiveCode, intent, flags)

        val triggerTime = calculateNextReleaseTime(releaseDay, releaseTime)
        if (triggerTime == 0L) return

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true

        try {
            if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            }
        } catch (se: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            }
        }
    }

    @JvmStatic
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            return am?.canScheduleExactAlarms() ?: false
        }
        return true
    }

    @JvmStatic
    fun cancel(context: Context?, showId: String?, alarmCode: Int) {
        if (context == null) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val effectiveCode = if (alarmCode > 0) alarmCode else ((showId?.hashCode() ?: 0) and 0x7FFFFFFF)
        val intent = Intent(context, NotificationReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getBroadcast(
            context, effectiveCode, intent,
            PendingIntent.FLAG_NO_CREATE or flags
        )

        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    @JvmStatic
    fun rescheduleAllAfterBoot(context: Context) {
        BootReceiver.rescheduleAlarms(context)
    }

    private fun calculateNextReleaseTime(releaseDay: String, releaseTime: String): Long {
        return try {
            val timeParts = releaseTime.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()

            val dayOfWeek = getDayOfWeekValue(releaseDay)
            if (dayOfWeek == -1) return 0L

            val now = Calendar.getInstance()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            val daysUntil = (dayOfWeek - currentDayOfWeek + 7) % 7

            cal.add(Calendar.DAY_OF_YEAR, daysUntil)

            var triggerTime = cal.timeInMillis
            val nowMillis = now.timeInMillis

            if (triggerTime <= nowMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 7)
                triggerTime = cal.timeInMillis
            }

            triggerTime
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    private fun getDayOfWeekValue(dayName: String): Int {
        return when (dayName.lowercase()) {
            "sunday" -> Calendar.SUNDAY
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            else -> -1
        }
    }
}
