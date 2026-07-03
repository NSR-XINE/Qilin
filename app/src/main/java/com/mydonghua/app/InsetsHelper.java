package com.mydonghua.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.webkit.WebView;

public class InsetsHelper {

    private final Activity activity;
    private final WebView webView;
    private float statusBarHeightDp = 0f;
    private float navBarHeightDp = 0f;
    private float safeAreaLeftDp = 0f;
    private float safeAreaRightDp = 0f;
    private boolean isImmersive = false;

    public InsetsHelper(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    public void setupSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
            activity.getWindow().setStatusBarColor(android.graphics.Color.parseColor("#05060a"));
            activity.getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#05060a"));
        }
    }

    public void onOrientationChanged(int orientation) {
        boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (landscape && !isImmersive) {
            setImmersiveMode(true);
        } else if (!landscape && isImmersive) {
            setImmersiveMode(false);
        }
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript(
                "document.documentElement.dataset.orientation='" +
                (landscape ? "landscape" : "portrait") + "'", null));
        }
    }

    private void setImmersiveMode(boolean enable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
        isImmersive = enable;
        activity.getWindow().getDecorView().post(() -> {
            int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
            if (enable) {
                flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                       | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                       | View.SYSTEM_UI_FLAG_FULLSCREEN;
            } else {
                flags &= ~(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                          | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                          | View.SYSTEM_UI_FLAG_FULLSCREEN);
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                       | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                       | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            }
            activity.getWindow().getDecorView().setSystemUiVisibility(flags);
        });
    }

    @SuppressWarnings("deprecation")
    public void enableMaxRefreshRate() {
        android.view.Window window = activity.getWindow();
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

    public void setOnApplyWindowInsetsListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            activity.getWindow().getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
                int topInset = insets.getSystemWindowInsetTop();
                int bottomInset = insets.getSystemWindowInsetBottom();
                int leftInset = insets.getSystemWindowInsetLeft();
                int rightInset = insets.getSystemWindowInsetRight();

                float density = activity.getResources().getDisplayMetrics().density;
                statusBarHeightDp = topInset / density;
                navBarHeightDp = bottomInset / density;
                safeAreaLeftDp = leftInset / density;
                safeAreaRightDp = rightInset / density;

                updateWebViewInsets();
                return insets;
            });
        }
    }

    public void updateWebViewInsets() {
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
}
