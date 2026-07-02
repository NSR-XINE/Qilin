package com.mydonghua.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "donghua_countdown_reminders";
    private static final long DEDUP_WINDOW_MS = 5000;
    private static final String PREFS_NOTIFY = "notification_prefs";
    private static final String PREF_QUIET_HOURS_START = "quiet_hours_start";
    private static final String PREF_QUIET_HOURS_END = "quiet_hours_end";
    private static final String PREF_NOTIFY_ENABLED_PREFIX = "notify_enabled_";
    private static final String PREF_SNOOZE_UNTIL_PREFIX = "snooze_until_";

    private static final java.util.concurrent.ConcurrentHashMap<String, Long> _recentNotifications =
        new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || context == null) return;

        String showId = intent.getStringExtra("show_id");
        String title = intent.getStringExtra("show_title");
        String releaseDay = intent.getStringExtra("release_day");
        String releaseTime = intent.getStringExtra("release_time");
        int alarmCode = intent.getIntExtra("alarm_code", -1);
        int currentEp = intent.getIntExtra("current_ep", 0);

        if (showId == null || showId.isEmpty()) return;
        if (title == null || title.isEmpty()) {
            title = "A Tracked Donghua";
        }

        String dedupKey = showId + ":" + currentEp;
        long now = System.currentTimeMillis();
        Long lastNotify = _recentNotifications.get(dedupKey);
        if (lastNotify != null && (now - lastNotify) < DEDUP_WINDOW_MS) return;
        _recentNotifications.put(dedupKey, now);

        if (_recentNotifications.size() > 200) {
            long cutoff = now - 300000;
            _recentNotifications.entrySet().removeIf(e -> e.getValue() < cutoff);
        }

        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NOTIFY, Context.MODE_PRIVATE);

        if (!prefs.getBoolean(PREF_NOTIFY_ENABLED_PREFIX + showId, true)) return;

        long snoozeUntil = prefs.getLong(PREF_SNOOZE_UNTIL_PREFIX + showId, 0);
        if (now < snoozeUntil) return;

        if (isQuietHours(prefs, now)) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Show Countdown Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts when a saved show is about to broadcast.");
            channel.setShowBadge(true);
            nm.createNotificationChannel(channel);
        }

        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.putExtra("target_show_id", showId);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = alarmCode > 0 ? alarmCode : Math.abs(showId.hashCode());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(context, requestCode, activityIntent, flags);

        int nextEp = currentEp + 1;
        String episodeText = "Episode " + nextEp + " is airing now!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(episodeText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(title + " - " + episodeText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (PackageManager.PERMISSION_GRANTED !=
                    context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)) {
                return;
            }
        }

        int notifyId = Math.abs(dedupKey.hashCode()) & Integer.MAX_VALUE;
        nm.notify(notifyId, builder.build());

        if (releaseDay != null && releaseTime != null && alarmCode > 0) {
            AlarmScheduler.schedule(context, showId, title, releaseDay, releaseTime, alarmCode, currentEp);
        }
    }

    private boolean isQuietHours(android.content.SharedPreferences prefs, long now) {
        String startStr = prefs.getString(PREF_QUIET_HOURS_START, null);
        String endStr = prefs.getString(PREF_QUIET_HOURS_END, null);
        if (startStr == null || endStr == null) return false;
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            String[] startParts = startStr.split(":");
            String[] endParts = endStr.split(":");
            int startMin = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
            int endMin = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);
            int nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);
            if (startMin <= endMin) {
                return nowMin >= startMin && nowMin < endMin;
            } else {
                return nowMin >= startMin || nowMin < endMin;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
