package com.mydonghua.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;

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
