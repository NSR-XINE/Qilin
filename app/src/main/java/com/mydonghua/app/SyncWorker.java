package com.mydonghua.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {

    private static final String PREFS_SYNC = "sync_prefs";
    private static final String KEY_LAST_SYNC = "last_sync_time";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm != null ?
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyDonghua:SyncWorker") : null;
        if (wakeLock != null) wakeLock.acquire(30000L);

        DatabaseHelper db = null;
        try {
            db = new DatabaseHelper(context);
            String showsJson = db.getAllShows();

            SharedPreferences prefs = context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE);
            prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply();

            return Result.success();
        } catch (Exception e) {
            android.util.Log.e("DonghuaTracker", "SyncWorker failed", e);
            return Result.retry();
        } finally {
            if (db != null) {
                try { db.close(); } catch (Exception ignored) {}
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                try { wakeLock.release(); } catch (Exception ignored) {}
            }
        }
    }

    public static long getLastSyncTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_SYNC, 0);
    }
}