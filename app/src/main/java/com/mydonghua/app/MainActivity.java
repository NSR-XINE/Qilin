package com.mydonghua.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.*;
import androidx.appcompat.app.AppCompatActivity;
import java.lang.ref.WeakReference;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    private DatabaseHelper dbHelper;
    private float statusBarHeightDp = 0f;
    private float navBarHeightDp = 0f;
    private float safeAreaLeftDp = 0f;
    private float safeAreaRightDp = 0f;

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String targetShowId = intent.getStringExtra("target_show_id");
        if (targetShowId != null && webView != null) {
            if (targetShowId.matches("[a-zA-Z0-9\\-]+")) {
                webView.evaluateJavascript(
                    "setTimeout(function() { if (typeof openDetailsById === 'function') { openDetailsById(" +
                    org.json.JSONObject.quote(targetShowId) + "); } }, 500);", null);
            }
            getIntent().removeExtra("target_show_id");
        }
    }

    @SuppressWarnings("deprecation")
    private void enableMaxRefreshRate() {
        android.view.Window window = getWindow();
        if (window == null) return;
        try {
            android.view.WindowManager.LayoutParams lp = window.getAttributes();
            android.view.Display display = window.getWindowManager().getDefaultDisplay();
            float maxRefresh = 60f;
            if (display != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        maxRefresh = display.getMode().getRefreshRate();
                    } catch (Exception ignored) {}
                } else {
                    float[] rates = display.getSupportedRefreshRates();
                    if (rates != null && rates.length > 0) {
                        maxRefresh = rates[rates.length - 1];
                    }
                }
            }
            lp.preferredRefreshRate = maxRefresh;
            window.setAttributes(lp);
        } catch (Exception ignored) {}
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#05060a"));
            getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#05060a"));
        }

        enableMaxRefreshRate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            getWindow().getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
                int topInset = insets.getSystemWindowInsetTop();
                int bottomInset = insets.getSystemWindowInsetBottom();
                int leftInset = insets.getSystemWindowInsetLeft();
                int rightInset = insets.getSystemWindowInsetRight();

                float density = getResources().getDisplayMetrics().density;
                statusBarHeightDp = topInset / density;
                navBarHeightDp = bottomInset / density;
                safeAreaLeftDp = leftInset / density;
                safeAreaRightDp = rightInset / density;

                updateWebViewInsets();
                return insets;
            });
        }

        dbHelper = new DatabaseHelper(this);

        webView = findViewById(R.id.webview);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (request != null && request.getUrl() != null) {
                        String url = request.getUrl().toString();
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            return false;
                        }
                    }
                }
                return true;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    return false;
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                updateWebViewInsets();
                String targetShowId = getIntent().getStringExtra("target_show_id");
                if (targetShowId != null) {
                    if (targetShowId.matches("[a-zA-Z0-9\\-]+")) {
                        webView.evaluateJavascript(
                            "setTimeout(function() { if (typeof openDetailsById === 'function') { openDetailsById(" +
                            org.json.JSONObject.quote(targetShowId) + "); } }, 800);", null);
                    }
                    getIntent().removeExtra("target_show_id");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message,
                                        final JsResult result) {
                new android.app.AlertDialog.Builder(MainActivity.this)
                    .setMessage(message)
                    .setPositiveButton("OK", (d, w) -> result.confirm())
                    .setNegativeButton("Cancel", (d, w) -> result.cancel())
                    .setCancelable(false)
                    .show();
                return true;
            }
            @Override
            public boolean onJsAlert(WebView view, String url, String message,
                                      final JsResult result) {
                new android.app.AlertDialog.Builder(MainActivity.this)
                    .setMessage(message)
                    .setPositiveButton("OK", (d, w) -> result.confirm())
                    .setCancelable(false)
                    .show();
                return true;
            }
        });

        webView.setBackgroundColor(android.graphics.Color.parseColor("#05060a"));

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.clearCache(true);

        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);

        webView.addJavascriptInterface(new AndroidBridge(this, dbHelper, webView), "AndroidApp");

        webView.loadUrl("file:///android_asset/index.html?v=" + System.currentTimeMillis());
    }

    private void updateWebViewInsets() {
        if (webView != null) {
            webView.post(() -> {
                webView.evaluateJavascript(
                    "document.documentElement.style.setProperty('--status-bar-height', '" + statusBarHeightDp + "px');" +
                    "document.documentElement.style.setProperty('--navigation-bar-height', '" + navBarHeightDp + "px');" +
                    "document.documentElement.style.setProperty('--safe-area-left', '" + safeAreaLeftDp + "px');" +
                    "document.documentElement.style.setProperty('--safe-area-right', '" + safeAreaRightDp + "px');",
                    null
                );
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView == null) {
            finish();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            webView.evaluateJavascript("handleBackPress();", null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.evaluateJavascript("pauseTimers();", null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.evaluateJavascript("resumeTimers();", null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    static class AndroidBridge {
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
        public void openWatchScreen(final String url) {
            runOnUi(() -> {
                try {
                    MainActivity a = getActivity();
                    if (a == null) return;
                    Intent intent = new Intent(a, WatchActivity.class);
                    intent.putExtra("watch_url", url);
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
                    File backupDir = new File(a.getCacheDir(), "backups");
                    if (!backupDir.exists()) backupDir.mkdirs();

                    String fileName = "mydonghua_backup_" +
                        new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            .format(new java.util.Date()) + ".json";
                    File backupFile = new File(backupDir, fileName);

                    FileWriter writer = new FileWriter(backupFile, false);
                    writer.write(jsonContent);
                    writer.close();

                    android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                        a, "com.mydonghua.app.fileprovider", backupFile);

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "My Donghua Backup");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    a.startActivity(Intent.createChooser(intent, "Export Backup"));
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
            try { return dbHelper.insertShow(new JSONObject(showJsonStr)); }
            catch (Throwable t) { t.printStackTrace(); return false; }
        }

        @JavascriptInterface
        public boolean dbUpdateShow(final String showJsonStr) {
            try { return dbHelper.updateShow(new JSONObject(showJsonStr)); }
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
                                AlarmScheduler.schedule(a, id, title, day, time, alarmCode);
                            } else {
                                AlarmScheduler.cancel(a, id, alarmCode);
                            }
                        });
                    }
                } catch (Throwable t) { t.printStackTrace(); }
            }).start();
        }
    }
}
