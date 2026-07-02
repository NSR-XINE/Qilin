package com.mydonghua.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    private DatabaseHelper dbHelper;

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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        webView = findViewById(R.id.webview);

        InsetsHelper insetsHelper = new InsetsHelper(this, webView);
        insetsHelper.setupSystemUi();
        insetsHelper.enableMaxRefreshRate();
        insetsHelper.setOnApplyWindowInsetsListener();

        WebViewSetup.setupWebView(this, webView, dbHelper, insetsHelper);
    }

    static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private android.webkit.ValueCallback<android.net.Uri[]> fileChooserCallback;

    void setFileChooserCallback(android.webkit.ValueCallback<android.net.Uri[]> callback) {
        fileChooserCallback = callback;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (fileChooserCallback != null) {
                android.net.Uri[] results = null;
                if (resultCode == RESULT_OK && data != null) {
                    if (data.getData() != null) {
                        results = new android.net.Uri[]{data.getData()};
                    } else if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new android.net.Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    }
                }
                fileChooserCallback.onReceiveValue(results);
                fileChooserCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    static final int NOTIFICATION_PERMISSION_CODE = 1002;

    void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (webView != null) {
                webView.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('notificationPermissionResult', {detail: " +
                    granted + "}));", null);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
}
