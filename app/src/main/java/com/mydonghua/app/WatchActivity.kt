package com.mydonghua.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class WatchActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var fullscreenContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val watchUrl = intent.getStringExtra("watch_url") ?: ""
        val showTitle = intent.getStringExtra("show_title") ?: "Donghua Episode"

        if (watchUrl.isBlank()) {
            Toast.makeText(this, "Empty stream URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val isDirectVideo = watchUrl.endsWith(".mp4", ignoreCase = true) ||
                watchUrl.endsWith(".m3u8", ignoreCase = true) ||
                watchUrl.endsWith(".mkv", ignoreCase = true) ||
                watchUrl.contains(".mp4?", ignoreCase = true) ||
                watchUrl.contains(".m3u8?", ignoreCase = true)

        setContent {
            val context = LocalContext.current

            if (isDirectVideo) {
                DirectVideoPlayerScreen(
                    watchUrl = watchUrl,
                    showTitle = showTitle,
                    onBack = { finish() },
                    onDownload = { downloadVideo(context, watchUrl, showTitle) }
                )
            } else {
                InAppWebPlayerScreen(
                    watchUrl = watchUrl,
                    showTitle = showTitle,
                    onBack = { finish() },
                    onDownload = { downloadVideo(context, watchUrl, showTitle) },
                    onOpenExternal = {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl)))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun DirectVideoPlayerScreen(
        watchUrl: String,
        showTitle: String,
        onBack: () -> Unit,
        onDownload: () -> Unit
    ) {
        val context = LocalContext.current
        var exoPlayerState by remember { mutableStateOf<ExoPlayer?>(null) }

        DisposableEffect(context) {
            val playerInstance = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(Uri.parse(watchUrl))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
            exoPlayerState = playerInstance
            player = playerInstance

            onDispose {
                playerInstance.release()
                exoPlayerState = null
                player = null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.player = exoPlayerState
                }
            )

            // Header Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = showTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download Episode", tint = Color.White)
                }
            }
        }
    }

    @Composable
    private fun InAppWebPlayerScreen(
        watchUrl: String,
        showTitle: String,
        onBack: () -> Unit,
        onDownload: () -> Unit,
        onOpenExternal: () -> Unit
    ) {
        val context = LocalContext.current
        var webViewRef by remember { mutableStateOf<WebView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                webViewRef?.destroy()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color(0xFF141416))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = showTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Download Episode", tint = Color(0xFF00F2FE))
                    }
                    IconButton(onClick = onOpenExternal) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser", tint = Color.White)
                    }
                }
            }

            // In-App Fullscreen Web Player
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            setSupportMultipleWindows(false)
                            javaScriptCanOpenWindowsAutomatically = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                // Intercept popup ads and redirect loops
                                if (url.contains("popads") || url.contains("adsterra") || url.contains("doubleclick")) {
                                    return true
                                }
                                return false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                if (customView != null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                customView = view
                                customViewCallback = callback
                                fullscreenContainer = FrameLayout(ctx).apply {
                                    addView(view, FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    ))
                                }
                                (window.decorView as ViewGroup).addView(fullscreenContainer, ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                ))
                            }

                            override fun onHideCustomView() {
                                if (customView == null) return
                                (window.decorView as ViewGroup).removeView(fullscreenContainer)
                                fullscreenContainer = null
                                customView = null
                                customViewCallback?.onCustomViewHidden()
                            }
                        }

                        loadUrl(watchUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun downloadVideo(context: Context, videoUrl: String, title: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(videoUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle(title)
                setDescription("Downloading episode for offline viewing...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MOVIES,
                    "MyDonghua/${title.replace(Regex("[^a-zA-Z0-9_]"), "_")}.mp4"
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            downloadManager.enqueue(request)
            Toast.makeText(context, "Started downloading: $title", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download started via browser...", Toast.LENGTH_SHORT).show()
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
            } catch (ignored: Exception) {}
        }
    }
}

