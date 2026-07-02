package com.mydonghua.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class AlarmScheduler {

    private static final int MIN_SCHEDULE_INTERVAL_MS = 60000;
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> _lastScheduled =
        new java.util.concurrent.ConcurrentHashMap<>();

    public static void schedule(Context context, String showId, String title, String releaseDay, String releaseTime, int alarmCode) {
        schedule(context, showId, title, releaseDay, releaseTime, alarmCode, 0);
    }

    public static void schedule(Context context, String showId, String title, String releaseDay, String releaseTime, int alarmCode, int currentEp) {
        if (context == null || showId == null || showId.isEmpty()) return;
        if (releaseDay == null || releaseTime == null) return;

        String dedupKey = showId + "|" + releaseDay + "|" + releaseTime;
        long now = System.currentTimeMillis();
        Long lastTime = _lastScheduled.get(dedupKey);
        if (lastTime != null && (now - lastTime) < MIN_SCHEDULE_INTERVAL_MS) return;
        _lastScheduled.put(dedupKey, now);

        int effectiveCode = alarmCode > 0 ? alarmCode : Math.abs(showId.hashCode());

        cancel(context, showId, effectiveCode);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("show_id", showId);
        intent.putExtra("show_title", title != null ? title : "");
        intent.putExtra("release_day", releaseDay);
        intent.putExtra("release_time", releaseTime);
        intent.putExtra("current_ep", currentEp);
        intent.putExtra("alarm_code", effectiveCode);

        int flags = PendingIntent.FLAG_CANCEL_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, effectiveCode, intent, flags);

        long triggerTime = calculateNextReleaseTime(releaseDay, releaseTime);
        if (triggerTime == 0) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            }
        } catch (SecurityException se) {
            am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi);
        }
    }

    public static void cancel(Context context, String showId, int alarmCode) {
        if (context == null) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        int effectiveCode = alarmCode > 0 ? alarmCode : (showId != null ? Math.abs(showId.hashCode()) : 0);
        Intent intent = new Intent(context, NotificationReceiver.class);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getBroadcast(context, effectiveCode, intent,
                PendingIntent.FLAG_NO_CREATE | flags);

        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    public static void rescheduleAllAfterBoot(Context context) {
        BootReceiver.rescheduleAlarms(context);
    }

    private static long calculateNextReleaseTime(String releaseDay, String releaseTime) {
        try {
            String[] timeParts = releaseTime.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            int dayOfWeek = getDayOfWeekValue(releaseDay);
            if (dayOfWeek == -1) return 0;

            Calendar now = Calendar.getInstance();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
            int daysUntil = (dayOfWeek - currentDayOfWeek + 7) % 7;

            cal.add(Calendar.DAY_OF_YEAR, daysUntil);

            long triggerTime = cal.getTimeInMillis();
            long nowMillis = now.getTimeInMillis();

            if (triggerTime <= nowMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 7);
                triggerTime = cal.getTimeInMillis();
            }

            return triggerTime;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int getDayOfWeekValue(String dayName) {
        switch (dayName.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return -1;
        }
    }
}
