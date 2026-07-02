package com.mydonghua.app;

import android.annotation.SuppressLint;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewSetup {

    @SuppressLint("SetJavaScriptEnabled")
    public static void setupWebView(MainActivity activity, WebView webView, DatabaseHelper dbHelper, InsetsHelper insetsHelper) {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
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
                insetsHelper.updateWebViewInsets();
                String targetShowId = activity.getIntent().getStringExtra("target_show_id");
                if (targetShowId != null) {
                    if (targetShowId.matches("[a-zA-Z0-9\\-]+")) {
                        webView.evaluateJavascript(
                            "setTimeout(function() { if (typeof openDetailsById === 'function') { openDetailsById(" +
                            org.json.JSONObject.quote(targetShowId) + "); } }, 800);", null);
                    }
                    activity.getIntent().removeExtra("target_show_id");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message,
                                        final JsResult result) {
                new android.app.AlertDialog.Builder(activity)
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
                new android.app.AlertDialog.Builder(activity)
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

        webView.addJavascriptInterface(new AndroidBridge(activity, dbHelper, webView), "AndroidApp");

        webView.loadUrl("file:///android_asset/index.html?v=" + System.currentTimeMillis());
    }
}
