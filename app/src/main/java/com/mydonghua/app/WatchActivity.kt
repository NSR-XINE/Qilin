package com.mydonghua.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class WatchActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val watchUrl = intent.getStringExtra("watch_url") ?: ""
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

        if (!isDirectVideo) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl))
                startActivity(browserIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }

        setContent {
            val context = LocalContext.current

            DisposableEffect(Unit) {
                val exoPlayer = ExoPlayer.Builder(context).build().apply {
                    val mediaItem = MediaItem.fromUri(Uri.parse(watchUrl))
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
                player = exoPlayer

                onDispose {
                    exoPlayer.release()
                    player = null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = this@WatchActivity.player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        view.player = player
                    }
                )
            }
        }
    }
}
