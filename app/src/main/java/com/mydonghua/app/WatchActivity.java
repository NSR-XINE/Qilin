package com.mydonghua.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.*;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class WatchActivity extends AppCompatActivity {

    private WebView playerView;
    private FrameLayout fullscreenContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            try {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            } catch (Throwable t) {
                android.util.Log.w("DonghuaTracker", "Could not configure sensor rotation: " + t.getMessage());
            }

            FrameLayout rootLayout = new FrameLayout(this);
            rootLayout.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            playerView = new WebView(this);
            playerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            playerView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            playerView.setBackgroundColor(android.graphics.Color.BLACK);
            rootLayout.addView(playerView);

            fullscreenContainer = new FrameLayout(this);
            fullscreenContainer.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            fullscreenContainer.setVisibility(View.GONE);
            fullscreenContainer.setBackgroundColor(android.graphics.Color.BLACK);
            rootLayout.addView(fullscreenContainer);

            setContentView(rootLayout);

            applyImmersiveMode();

            WebSettings s = playerView.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setJavaScriptCanOpenWindowsAutomatically(false);
            s.setMediaPlaybackRequiresUserGesture(false);
            s.setAllowFileAccess(false);
            s.setSupportZoom(true);
            s.setBuiltInZoomControls(true);
            s.setDisplayZoomControls(false);
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
            s.setUserAgentString("Mozilla/5.0 (Linux; Android 11; Pixel 5) "
                + "AppleWebkit/537.36 (KHTML, like Gecko) "
                + "Chrome/120.0.0.0 Mobile Safari/537.36");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                try {
                    CookieManager cookieManager = CookieManager.getInstance();
                    if (cookieManager != null) {
                        cookieManager.setAcceptThirdPartyCookies(playerView, true);
                    }
                } catch (Throwable t) {
                    android.util.Log.w("DonghuaTracker", "CookieManager init failed: " + t.getMessage());
                }
            }

            playerView.setWebViewClient(new WebViewClient() {
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
            });

            playerView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onShowCustomView(View view, CustomViewCallback callback) {
                    if (customView != null) {
                        onHideCustomView();
                    }
                    customView = view;
                    customViewCallback = callback;
                    if (fullscreenContainer != null) {
                        fullscreenContainer.addView(view);
                        fullscreenContainer.setVisibility(View.VISIBLE);
                        if (playerView != null) {
                            playerView.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onHideCustomView() {
                    if (fullscreenContainer != null) {
                        fullscreenContainer.setVisibility(View.GONE);
                        fullscreenContainer.removeAllViews();
                        if (playerView != null) {
                            playerView.setVisibility(View.VISIBLE);
                        }
                    }
                    if (customViewCallback != null) {
                        customViewCallback.onCustomViewHidden();
                        customViewCallback = null;
                    }
                    customView = null;
                }
            });

            Intent intent = getIntent();
            String url = null;
            if (intent != null) {
                url = intent.getStringExtra("watch_url");
            }
            if (url != null && !url.isEmpty() && (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://"))) {
                playerView.loadUrl(url);
            } else {
                finish();
                return;
            }

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (customView != null) {
                        if (fullscreenContainer != null) {
                            fullscreenContainer.setVisibility(View.GONE);
                            fullscreenContainer.removeAllViews();
                            if (playerView != null) {
                                playerView.setVisibility(View.VISIBLE);
                            }
                        }
                        if (customViewCallback != null) {
                            customViewCallback.onCustomViewHidden();
                            customViewCallback = null;
                        }
                        customView = null;
                    } else if (playerView != null && playerView.canGoBack()) {
                        playerView.goBack();
                    } else {
                        finish();
                    }
                }
            });
        } catch (Throwable t) {
            android.util.Log.e("DonghuaTracker", "Crash in WatchActivity onCreate", t);
            android.widget.Toast.makeText(this, "Crash starting player: " + t.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void applyImmersiveMode() {
        try {
            android.view.Window window = getWindow();
            if (window == null) return;

            window.setStatusBarColor(android.graphics.Color.BLACK);
            window.setNavigationBarColor(android.graphics.Color.BLACK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
                WindowInsetsController ctrl = window.getInsetsController();
                if (ctrl != null) {
                    ctrl.show(WindowInsets.Type.systemBars());
                }
            } else {
                View decorView = window.getDecorView();
                if (decorView != null) {
                    decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    );
                }
            }
        } catch (Throwable t) {
            android.util.Log.w("DonghuaTracker", "Failed to apply immersive mode: " + t.getMessage());
        }
    }
}
