package com.mydonghua.app;

import android.os.Build;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.ref.WeakReference;

class AndroidBridge {
    private static final String[] ALLOWED_PREFIXES = {
        "https://animecountdown.com/",
        "https://api.jikan.moe/",
        "https://graphql.anilist.co/",
        "https://anilist.co/",
        "https://donghuastream.org/",
        "https://www.donghuastream.org/",
        "https://luciferdonghua.org/",
        "https://www.luciferdonghua.org/",
        "https://cdn.",
        "https://img."
    };

    private final WeakReference<MainActivity> activityRef;
    private final DatabaseHelper dbHelper;
    private final WebView webView;

    AndroidBridge(MainActivity activity, DatabaseHelper dbHelper, WebView webView) {
        this.activityRef = new WeakReference<>(activity);
        this.dbHelper = dbHelper;
        this.webView = webView;
    }

    private MainActivity getActivity() {
        return activityRef.get();
    }

    private void runOnUi(Runnable r) {
        MainActivity a = getActivity();
        if (a != null) a.runOnUiThread(r);
    }

    private void callJs(String js) {
        runOnUi(() -> {
            MainActivity a = getActivity();
            if (a != null && a.webView != null) {
                a.webView.evaluateJavascript(js, null);
            }
        });
    }

    @JavascriptInterface
    public void exitApp() {
        runOnUi(() -> {
            MainActivity a = getActivity();
            if (a != null) a.finish();
        });
    }

    @JavascriptInterface
    public void setSystemThemeMode(final String themeMode) {
        runOnUi(() -> {
            try {
                MainActivity a = getActivity();
                if (a == null) return;
                boolean isLight = "light".equalsIgnoreCase(themeMode);
                boolean isAmoled = "amoled".equalsIgnoreCase(themeMode);

                String barColor;
                if (isAmoled) barColor = "#000000";
                else if (isLight) barColor = "#ffffff";
                else barColor = "#05060a";

                a.getWindow().setStatusBarColor(android.graphics.Color.parseColor(barColor));
                a.getWindow().setNavigationBarColor(android.graphics.Color.parseColor(barColor));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.view.Window w = a.getWindow();
                    android.view.WindowInsetsController controller = w.getInsetsController();
                    if (controller != null) {
                        int appearance = 0;
                        if (isLight) {
                            appearance = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                                         android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                        }
                        controller.setSystemBarsAppearance(appearance,
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                    }
                } else {
                    int flags = a.getWindow().getDecorView().getSystemUiVisibility();
                    if (isLight) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    }
                    a.getWindow().getDecorView().setSystemUiVisibility(flags);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private boolean isUrlAllowed(String url) {
        if (url == null) return false;
        for (String prefix : ALLOWED_PREFIXES) {
            if (url.startsWith(prefix)) return true;
        }
        return false;
    }

    private void fetchUrlInternal(final String url, final String jsonBody, final String callbackName, final boolean isPost) {
        if (!isUrlAllowed(url)) {
            callJs(callbackName + "(null);");
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL urlObj = new URL(url);
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod(isPost ? "POST" : "GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (isPost) {
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    if (jsonBody != null) {
                        OutputStream os = conn.getOutputStream();
                        os.write(jsonBody.getBytes("UTF-8"));
                        os.close();
                    }
                }

                int responseCode = conn.getResponseCode();
                InputStream inStream = (responseCode >= 200 && responseCode < 300)
                        ? conn.getInputStream() : conn.getErrorStream();

                if (inStream == null) {
                    callJs(callbackName + "(null);");
                    return;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine).append("\n");
                }
                in.close();

                final String body = response.toString();
                callJs(callbackName + "(" + org.json.JSONObject.quote(body) + ");");
            } catch (Throwable t) {
                callJs(callbackName + "(null);");
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    @JavascriptInterface
    public void fetchUrl(final String url, final String callbackName) {
        fetchUrlInternal(url, null, callbackName, false);
    }

    @JavascriptInterface
    public void fetchUrlPost(final String url, final String jsonBody, final String callbackName) {
        fetchUrlInternal(url, jsonBody, callbackName, true);
    }

    @JavascriptInterface
    public void setAutoRotateWebView(final boolean enabled) {
        try {
            MainActivity a = getActivity();
            if (a == null) return;
            a.getSharedPreferences("donghua_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putBoolean("auto_rotate_webview", enabled).apply();
        } catch (Throwable t) {
            android.util.Log.w("DonghuaTracker", "Failed to save auto-rotate pref", t);
        }
    }

    @JavascriptInterface
    public boolean getAutoRotateWebView() {
        try {
            MainActivity a = getActivity();
            if (a == null) return true;
            return a.getSharedPreferences("donghua_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("auto_rotate_webview", true);
        } catch (Throwable t) {
            return true;
        }
    }

    @JavascriptInterface
    public void openWatchScreen(final String url) {
        runOnUi(() -> {
            try {
                MainActivity a = getActivity();
                if (a == null) return;
                android.content.Intent intent = new android.content.Intent(a, WatchActivity.class);
                intent.putExtra("watch_url", url);
                intent.putExtra("auto_rotate", getAutoRotateWebView());
                a.startActivity(intent);
            } catch (Throwable t) {
                android.util.Log.e("DonghuaTracker", "Failed to start WatchActivity", t);
            }
        });
    }

    @JavascriptInterface
    public void shareJsonFile(final String jsonContent) {
        runOnUi(() -> {
            try {
                MainActivity a = getActivity();
                if (a == null) return;
                java.io.File backupDir = new java.io.File(a.getCacheDir(), "backups");
                if (!backupDir.exists()) backupDir.mkdirs();

                String fileName = "mydonghua_backup_" +
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .format(new java.util.Date()) + ".json";
                java.io.File backupFile = new java.io.File(backupDir, fileName);

                FileWriter writer = new FileWriter(backupFile, false);
                writer.write(jsonContent);
                writer.close();

                android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    a, "com.mydonghua.app.fileprovider", backupFile);

                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                intent.setType("application/json");
                intent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "My Donghua Backup");
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                a.startActivity(android.content.Intent.createChooser(intent, "Export Backup"));
            } catch (Throwable t) {
                android.util.Log.e("DonghuaTracker", "Share file failed", t);
            }
        });
    }

    @JavascriptInterface
    public String dbGetAllShows() {
        try { return dbHelper.getAllShows(); }
        catch (Throwable t) { t.printStackTrace(); return "[]"; }
    }

    @JavascriptInterface
    public String dbGetShowsByStatus(final String status) {
        try { return dbHelper.getShowsByStatus(status); }
        catch (Throwable t) { t.printStackTrace(); return "[]"; }
    }

    @JavascriptInterface
    public String dbGetFavoriteShows() {
        try { return dbHelper.getFavoriteShows(); }
        catch (Throwable t) { t.printStackTrace(); return "[]"; }
    }

    @JavascriptInterface
    public String dbSearchShows(final String query) {
        try { return dbHelper.searchShows(query); }
        catch (Throwable t) { t.printStackTrace(); return "[]"; }
    }

    @JavascriptInterface
    public boolean dbInsertShow(final String showJsonStr) {
        try { return dbHelper.insertShow(new org.json.JSONObject(showJsonStr)); }
        catch (Throwable t) { t.printStackTrace(); return false; }
    }

    @JavascriptInterface
    public boolean dbUpdateShow(final String showJsonStr) {
        try { return dbHelper.updateShow(new org.json.JSONObject(showJsonStr)); }
        catch (Throwable t) { t.printStackTrace(); return false; }
    }

    @JavascriptInterface
    public boolean dbDeleteShow(final String showId) {
        try { return dbHelper.deleteShow(showId); }
        catch (Throwable t) { t.printStackTrace(); return false; }
    }

    @JavascriptInterface
    public void dbAddWatchHistory(final String showId, final int episodeNum) {
        try { dbHelper.addWatchHistory(showId, episodeNum); }
        catch (Throwable t) { t.printStackTrace(); }
    }

    @JavascriptInterface
    public String dbGetWatchHistory() {
        try { return dbHelper.getWatchHistory(); }
        catch (Throwable t) { t.printStackTrace(); return "[]"; }
    }

    @JavascriptInterface
    public String dbGetWatchHistoryLimited(final int limit) {
        try { return dbHelper.getWatchHistory(limit); }
        catch (Throwable t) { t.printStackTrace(); return "[]"; }
    }

    @JavascriptInterface
    public String dbGetRecentlyUpdated(final int limit) {
        try { return dbHelper.getRecentlyUpdatedShows(limit); }
        catch (Throwable t) { t.printStackTrace(); return "[]"; }
    }

    @JavascriptInterface
    public String dbGetStats() {
        try { return dbHelper.getStats(); }
        catch (Throwable t) { t.printStackTrace(); return "{}"; }
    }

    @JavascriptInterface
    public boolean dbBulkInsert(final String showsJsonStr) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray(showsJsonStr);
            dbHelper.bulkInsert(arr);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @JavascriptInterface
    public void dbSaveSetting(final String key, final String value) {
        try { dbHelper.saveSetting(key, value); }
        catch (Throwable t) { t.printStackTrace(); }
    }

    @JavascriptInterface
    public String dbGetSetting(final String key, final String defaultVal) {
        try { return dbHelper.getSetting(key, defaultVal); }
        catch (Throwable t) { t.printStackTrace(); return defaultVal; }
    }

    @JavascriptInterface
    public void scheduleReminder(final String id, final String title, final String releaseDay, final String releaseTime, final int alarmCode) {
        runOnUi(() -> {
            try {
                MainActivity a = getActivity();
                if (a == null) return;
                a.requestNotificationPermission();
                AlarmScheduler.schedule(a, id, title, releaseDay, releaseTime, alarmCode);
            } catch (Throwable t) { t.printStackTrace(); }
        });
    }

    @JavascriptInterface
    public void cancelReminder(final String id, final int alarmCode) {
        runOnUi(() -> {
            try {
                MainActivity a = getActivity();
                if (a == null) return;
                AlarmScheduler.cancel(a, id, alarmCode);
            } catch (Throwable t) { t.printStackTrace(); }
        });
    }

    @JavascriptInterface
    public void syncAllAlarms() {
        new Thread(() -> {
            try {
                String showsJson = dbHelper.getAllShows();
                org.json.JSONArray arr = new org.json.JSONArray(showsJson);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject show = arr.getJSONObject(i);
                    final String id = show.getString("id");
                    final String title = show.getString("title");
                    final String status = show.optString("status", "ongoing");
                    final int alarmCode = show.optInt("alarmRequestCode", Math.abs(id.hashCode()));
                    final String day = show.optString("releaseDay", "Sunday");
                    final String time = show.optString("releaseTime", "10:00");

                    runOnUi(() -> {
                        MainActivity a = getActivity();
                        if (a == null) return;
                        if ("ongoing".equals(status)) {
                            a.requestNotificationPermission();
                            AlarmScheduler.schedule(a, id, title, day, time, alarmCode);
                        } else {
                            AlarmScheduler.cancel(a, id, alarmCode);
                        }
                    });
                }
            } catch (Throwable t) { t.printStackTrace(); }
        }).start();
    }

    @JavascriptInterface
    public void requestNotificationPermission() {
        runOnUi(() -> {
            MainActivity a = getActivity();
            if (a != null) a.requestNotificationPermission();
        });
    }

    @JavascriptInterface
    public boolean hasNotificationPermission() {
        MainActivity a = getActivity();
        if (a != null) return a.hasNotificationPermission();
        return false;
    }

    @JavascriptInterface
    public void requestExactAlarmPermission() {
        runOnUi(() -> {
            MainActivity a = getActivity();
            if (a == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.app.AlarmManager am = (android.app.AlarmManager) a.getSystemService(android.content.Context.ALARM_SERVICE);
                if (am != null && !am.canScheduleExactAlarms()) {
                    android.content.Intent intent = new android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(android.net.Uri.parse("package:" + a.getPackageName()));
                    a.startActivity(intent);
                }
            }
        });
    }

    @JavascriptInterface
    public boolean hasExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MainActivity a = getActivity();
            if (a != null) {
                android.app.AlarmManager am = (android.app.AlarmManager) a.getSystemService(android.content.Context.ALARM_SERVICE);
                return am != null && am.canScheduleExactAlarms();
            }
            return false;
        }
        return true;
    }

    @JavascriptInterface
    public void requestStoragePermission() {
        runOnUi(() -> {
            MainActivity a = getActivity();
            if (a != null) a.requestStoragePermission();
        });
    }

    @JavascriptInterface
    public boolean hasStoragePermission() {
        MainActivity a = getActivity();
        if (a != null) return a.hasStoragePermission();
        return false;
    }

    @JavascriptInterface
    public void updateWidget(final String title, final String countdown, final String episode) {
        runOnUi(() -> {
            try {
                MainActivity a = getActivity();
                if (a == null) return;
                android.content.Intent intent = new android.content.Intent(a, CountdownWidget.class);
                intent.setAction("com.mydonghua.app.UPDATE_WIDGET");
                intent.putExtra("widget_title", title);
                intent.putExtra("widget_countdown", countdown);
                intent.putExtra("widget_episode", episode);
                a.sendBroadcast(intent);
            } catch (Throwable t) {
                android.util.Log.e("DonghuaTracker", "Widget update failed", t);
            }
        });
    }

    @JavascriptInterface
    public String getDynamicColors() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return "null";
            }
            android.app.WallpaperManager wm = android.app.WallpaperManager.getInstance(
                (android.content.Context) activityRef.get());
            if (wm == null) return "null";
            android.app.WallpaperColors colors = wm.getWallpaperColors(
                android.app.WallpaperManager.FLAG_SYSTEM);
            if (colors == null) {
                colors = wm.getWallpaperColors(
                    android.app.WallpaperManager.FLAG_LOCK);
            }
            if (colors == null) return "null";
            int primaryColor = colors.getPrimaryColor().toArgb();
            int secondaryColor = colors.getSecondaryColor() != null ? colors.getSecondaryColor().toArgb() : primaryColor;
            int tertiaryColor = colors.getTertiaryColor() != null ? colors.getTertiaryColor().toArgb() : secondaryColor;
            org.json.JSONObject result = new org.json.JSONObject();
            result.put("primary", String.format("#%06X", (0xFFFFFF & primaryColor)));
            result.put("secondary", String.format("#%06X", (0xFFFFFF & secondaryColor)));
            result.put("tertiary", String.format("#%06X", (0xFFFFFF & tertiaryColor)));
            result.put("isDark", (android.graphics.Color.luminance(primaryColor) < 0.5));
            return result.toString();
        } catch (Throwable t) {
            t.printStackTrace();
            return "null";
        }
    }
}
