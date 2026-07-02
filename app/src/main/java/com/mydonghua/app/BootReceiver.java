package com.mydonghua.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            rescheduleAlarms(context);
        }
    }

    public static void rescheduleAlarms(final Context context) {
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = pm != null ?
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyDonghua:AlarmReschedule") : null;
        if (wakeLock != null) wakeLock.acquire(60000L);

        new Thread(() -> {
            DatabaseHelper db = null;
            try {
                db = new DatabaseHelper(context);
                String showsJsonStr = db.getAllShows();
                JSONArray showsArray = new JSONArray(showsJsonStr);

                List<ShowAlarm> pending = new ArrayList<>();
                for (int i = 0; i < showsArray.length(); i++) {
                    JSONObject show = showsArray.getJSONObject(i);
                    String status = show.optString("status", "ongoing");
                    if ("ongoing".equals(status)) {
                        pending.add(new ShowAlarm(
                            show.getString("id"),
                            show.getString("title"),
                            show.optString("releaseDay", "Sunday"),
                            show.optString("releaseTime", "10:00"),
                            show.optInt("alarmRequestCode", Math.abs(show.getString("id").hashCode())),
                            show.optInt("currentEp", 0)
                        ));
                    }
                }

                for (ShowAlarm sa : pending) {
                    AlarmScheduler.schedule(context, sa.id, sa.title, sa.releaseDay, sa.releaseTime, sa.alarmCode, sa.currentEp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (db != null) db.close();
                if (wakeLock != null && wakeLock.isHeld()) {
                    try { wakeLock.release(); } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    private static class ShowAlarm {
        final String id;
        final String title;
        final String releaseDay;
        final String releaseTime;
        final int alarmCode;
        final int currentEp;

        ShowAlarm(String id, String title, String releaseDay, String releaseTime, int alarmCode, int currentEp) {
            this.id = id;
            this.title = title;
            this.releaseDay = releaseDay;
            this.releaseTime = releaseTime;
            this.alarmCode = alarmCode;
            this.currentEp = currentEp;
        }
    }
}
