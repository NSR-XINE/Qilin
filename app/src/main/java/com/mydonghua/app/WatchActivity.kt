package com.mydonghua.app

import android.app.Activity
import android.app.DownloadManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlin.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mydonghua.app.data.service.AdBlocker
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.ColorOSMotion
import com.mydonghua.app.ui.theme.colorOSPressBounce
import kotlinx.coroutines.delay

class WatchActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var fullscreenContainer: FrameLayout? = null
    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUI()
        }

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
            val isInPip by remember { isInPipModeState }
            var useWebPlayer by remember { mutableStateOf(!isDirectVideo) }

            var showQualitySheet by remember { mutableStateOf(false) }
            var capturedStreamUrl by remember { mutableStateOf<String?>(if (isDirectVideo) watchUrl else null) }
            var capturedReferer by remember { mutableStateOf<String?>(null) }
            var currentPlaybackHeight by remember { mutableIntStateOf(0) }
            var websiteSyncedQualities by remember { mutableStateOf<List<DownloadQualityOption>>(emptyList()) }
            var isSyncingWebsite by remember { mutableStateOf(false) }

            // Dynamic HLS Master Playlist Fetching & Parsing directly from website playback
            LaunchedEffect(capturedStreamUrl) {
                val stream = capturedStreamUrl ?: return@LaunchedEffect
                if (stream.contains(".m3u8", ignoreCase = true) || stream.contains("/hls", ignoreCase = true)) {
                    isSyncingWebsite = true
                    withContext(Dispatchers.IO) {
                        try {
                            val client = OkHttpClient.Builder()
                                .followRedirects(true)
                                .connectTimeout(6, TimeUnit.SECONDS)
                                .readTimeout(6, TimeUnit.SECONDS)
                                .build()
                            val req = Request.Builder()
                                .url(stream)
                                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                                .apply {
                                    if (!capturedReferer.isNullOrEmpty()) header("Referer", capturedReferer!!)
                                }
                                .build()
                            val res = client.newCall(req).execute()
                            val body = res.body?.string() ?: ""
                            if (body.contains("#EXT-X-STREAM-INF")) {
                                val parsed = parseHlsMasterPlaylist(stream, body)
                                if (parsed.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        websiteSyncedQualities = parsed
                                    }
                                }
                            }
                        } catch (ignored: Exception) {
                        } finally {
                            isSyncingWebsite = false
                        }
                    }
                }
            }

            if (!useWebPlayer) {
                DirectVideoPlayerScreen(
                    watchUrl = watchUrl,
                    showTitle = showTitle,
                    isInPip = isInPip,
                    onBack = { finish() },
                    onDownload = {
                        capturedStreamUrl = watchUrl
                        capturedReferer = null
                        showQualitySheet = true
                    },
                    onEnterPip = { enterPipMode() },
                    onSwitchToWeb = { useWebPlayer = true }
                )
            } else {
                InAppWebPlayerScreen(
                    watchUrl = watchUrl,
                    showTitle = showTitle,
                    activeCapturedStream = capturedStreamUrl,
                    activePlaybackHeight = currentPlaybackHeight,
                    onStreamCaptured = { streamUrl, referer ->
                        capturedStreamUrl = streamUrl
                        capturedReferer = referer
                    },
                    onPlaybackHeightDetected = { height ->
                        currentPlaybackHeight = height
                    },
                    onBack = { finish() },
                    onDownload = { streamUrl, referer ->
                        capturedStreamUrl = streamUrl
                        capturedReferer = referer
                        showQualitySheet = true
                    },
                    onOpenExternal = {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl)))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            if (showQualitySheet) {
                QualitySelectorBottomSheet(
                    streamUrl = capturedStreamUrl ?: watchUrl,
                    title = showTitle,
                    referer = capturedReferer,
                    syncedWebsiteOptions = websiteSyncedQualities,
                    currentPlaybackHeight = currentPlaybackHeight,
                    isWebsiteStreamCaptured = (capturedStreamUrl != null),
                    isSyncingWebsite = isSyncingWebsite,
                    onDismiss = { showQualitySheet = false },
                    onOptionSelected = { option ->
                        showQualitySheet = false
                        val effectiveUrl = if (option.formatId.startsWith("http://") || option.formatId.startsWith("https://")) {
                            option.formatId
                        } else {
                            capturedStreamUrl ?: watchUrl
                        }
                        val effectiveFormatId = if (option.formatId.startsWith("http://") || option.formatId.startsWith("https://")) {
                            null
                        } else {
                            option.formatId
                        }
                        downloadVideo(
                            context = context,
                            videoUrl = effectiveUrl,
                            title = showTitle,
                            formatId = effectiveFormatId,
                            referer = capturedReferer,
                            isAudioOnly = option.isAudio
                        )
                    }
                )
            }
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                Toast.makeText(this, "PiP not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            hideSystemUI()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (customView != null) {
            (window.decorView as ViewGroup).removeView(fullscreenContainer)
            fullscreenContainer = null
            customView = null
            customViewCallback?.onCustomViewHidden()
            if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                showSystemUI()
            }
            return
        }
        super.onBackPressed()
    }

    @Composable
    private fun DirectVideoPlayerScreen(
        watchUrl: String,
        showTitle: String,
        isInPip: Boolean,
        onBack: () -> Unit,
        onDownload: () -> Unit,
        onEnterPip: () -> Unit,
        onSwitchToWeb: () -> Unit
    ) {
        val context = LocalContext.current
        val activity = context as? Activity
        val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

        var exoPlayerState by remember { mutableStateOf<ExoPlayer?>(null) }
        var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

        var showControls by remember { mutableStateOf(true) }
        var isPlaying by remember { mutableStateOf(true) }
        var currentPosMs by remember { mutableLongStateOf(0L) }
        var durationMs by remember { mutableLongStateOf(0L) }
        var bufferedPosMs by remember { mutableLongStateOf(0L) }
        var isBuffering by remember { mutableStateOf(false) }
        var playerError by remember { mutableStateOf<String?>(null) }

        var isScrubbing by remember { mutableStateOf(false) }
        var scrubPositionMs by remember { mutableLongStateOf(0L) }

        var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
        var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

        var brightnessHud by remember { mutableStateOf<Float?>(null) }
        var volumeHud by remember { mutableStateOf<Float?>(null) }
        var doubleTapSeekBadge by remember { mutableStateOf<String?>(null) }

        var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

        // Setup ExoPlayer
        DisposableEffect(context, watchUrl) {
            val playerInstance = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(Uri.parse(watchUrl))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        playerError = error.message ?: "Failed to play direct video stream."
                    }
                })
            }
            exoPlayerState = playerInstance
            player = playerInstance

            onDispose {
                playerInstance.release()
                exoPlayerState = null
                player = null
            }
        }

        // Tracking loop
        LaunchedEffect(exoPlayerState) {
            val p = exoPlayerState ?: return@LaunchedEffect
            while (true) {
                currentPosMs = p.currentPosition
                durationMs = if (p.duration > 0) p.duration else 0L
                bufferedPosMs = p.bufferedPosition
                isPlaying = p.isPlaying
                isBuffering = p.playbackState == Player.STATE_BUFFERING
                delay(200)
            }
        }

        // Auto-hide controls
        LaunchedEffect(showControls, lastInteractionTime, isPlaying) {
            if (showControls && isPlaying) {
                delay(4000)
                showControls = false
            }
        }

        // Auto-dismiss HUDs
        LaunchedEffect(brightnessHud) {
            if (brightnessHud != null) {
                delay(1200)
                brightnessHud = null
            }
        }
        LaunchedEffect(volumeHud) {
            if (volumeHud != null) {
                delay(1200)
                volumeHud = null
            }
        }
        LaunchedEffect(doubleTapSeekBadge) {
            if (doubleTapSeekBadge != null) {
                delay(700)
                doubleTapSeekBadge = null
            }
        }

        val screenConfig = LocalConfiguration.current
        val isLandscape = screenConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Video View
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        playerViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.player = exoPlayerState
                    view.resizeMode = resizeMode
                }
            )

            // Gesture Detector Layer (Tap & Double Tap across screen)
            if (!isInPip) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(exoPlayerState, durationMs) {
                            detectTapGestures(
                                onTap = {
                                    showControls = !showControls
                                    if (showControls) lastInteractionTime = System.currentTimeMillis()
                                },
                                onDoubleTap = { offset ->
                                    val p = exoPlayerState ?: return@detectTapGestures
                                    if (offset.x < size.width / 2f) {
                                        val target = (p.currentPosition - 10000L).coerceAtLeast(0L)
                                        p.seekTo(target)
                                        currentPosMs = target
                                        doubleTapSeekBadge = "-10s"
                                    } else {
                                        val maxD = if (p.duration > 0) p.duration else Long.MAX_VALUE
                                        val target = (p.currentPosition + 10000L).coerceAtMost(maxD)
                                        p.seekTo(target)
                                        currentPosMs = target
                                        doubleTapSeekBadge = "+10s"
                                    }
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )
                        }
                )

                // Left 25% Edge Strip for Brightness Drag
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.25f)
                        .align(Alignment.CenterStart)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { lastInteractionTime = System.currentTimeMillis() },
                                onDragEnd = { lastInteractionTime = System.currentTimeMillis() },
                                onDragCancel = { lastInteractionTime = System.currentTimeMillis() },
                                onVerticalDrag = { _, dragAmount ->
                                    val deltaRatio = -dragAmount / 500f
                                    val lp = activity?.window?.attributes
                                    val currentB = if (lp != null && lp.screenBrightness >= 0f) lp.screenBrightness else 0.5f
                                    val newB = (currentB + deltaRatio).coerceIn(0.02f, 1.0f)
                                    lp?.screenBrightness = newB
                                    activity?.window?.attributes = lp
                                    brightnessHud = newB
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )
                        }
                )

                // Right 25% Edge Strip for Volume Drag
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.25f)
                        .align(Alignment.CenterEnd)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { lastInteractionTime = System.currentTimeMillis() },
                                onDragEnd = { lastInteractionTime = System.currentTimeMillis() },
                                onDragCancel = { lastInteractionTime = System.currentTimeMillis() },
                                onVerticalDrag = { _, dragAmount ->
                                    val deltaRatio = -dragAmount / 500f
                                    val currentV = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                                    val newV = (currentV + deltaRatio * maxVolume).coerceIn(0f, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newV.toInt(), 0)
                                    volumeHud = newV / maxVolume
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )
                        }
                )
            }

            // Buffering Spinner
            if (isBuffering && !isInPip) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(52.dp),
                        color = Color(0xFF00E5FF),
                        strokeWidth = 3.5.dp
                    )
                }
            }

            // Player Error Fallback Dialog
            if (playerError != null && !isInPip) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .widthIn(max = 340.dp)
                            .clip(RoundedCornerShape(CornerRadius.card))
                            .background(Color(0xFF141724))
                            .border(
                                width = 0.75.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(CornerRadius.card)
                            )
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF4D4D),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Stream Playback Error",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Direct stream encountered an issue. Would you like to switch to the Web Player?",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Go Back", color = Color.White)
                            }
                            Button(
                                onClick = onSwitchToWeb,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                            ) {
                                Text("Web Player", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Double Tap Seek Ripple Badge
            AnimatedVisibility(
                visible = doubleTapSeekBadge != null && !isInPip,
                enter = fadeIn(ColorOSMotion.FadeInFloat) + scaleIn(ColorOSMotion.ScaleInFloat, initialScale = 0.88f),
                exit = fadeOut(ColorOSMotion.FadeOutFloat),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F1424).copy(alpha = 0.85f))
                        .border(
                            width = 0.75.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF00E5FF).copy(alpha = 0.6f), Color(0xFF0072FF).copy(alpha = 0.3f))
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (doubleTapSeekBadge?.startsWith("-") == true) Icons.Rounded.Replay10 else Icons.Rounded.Forward10,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = doubleTapSeekBadge ?: "",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Brightness HUD Pill
            AnimatedVisibility(
                visible = brightnessHud != null && !isInPip,
                enter = fadeIn(ColorOSMotion.FadeInFloat),
                exit = fadeOut(ColorOSMotion.FadeOutFloat),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
            ) {
                val bVal = brightnessHud ?: 0.5f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0E111C).copy(alpha = 0.85f))
                        .border(
                            width = 0.6.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(vertical = 14.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BrightnessMedium,
                        contentDescription = "Brightness",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(bVal)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF0072FF))
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(bVal * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Volume HUD Pill
            AnimatedVisibility(
                visible = volumeHud != null && !isInPip,
                enter = fadeIn(ColorOSMotion.FadeInFloat),
                exit = fadeOut(ColorOSMotion.FadeOutFloat),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            ) {
                val vVal = volumeHud ?: 0.5f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0E111C).copy(alpha = 0.85f))
                        .border(
                            width = 0.6.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(vertical = 14.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = if (vVal > 0f) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                        contentDescription = "Volume",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(vVal)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF0072FF))
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(vVal * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Complete ColorOS 17 Liquid Glass Controls Overlay
            AnimatedVisibility(
                visible = showControls && !isInPip,
                enter = fadeIn(ColorOSMotion.FadeInFloat),
                exit = fadeOut(ColorOSMotion.FadeOutFloat),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showControls = false
                        }
                ) {
                    // Top Liquid Glass Capsule Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {} // Consume click
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Capsule: Back + Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(CornerRadius.surface))
                                .background(Color(0xFF0F121C).copy(alpha = 0.85f))
                                .border(
                                    width = 0.65.dp,
                                    brush = Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                                    ),
                                    shape = RoundedCornerShape(CornerRadius.surface)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .colorOSPressBounce(pressedScale = 0.92f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = showTitle,
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E5FF))
                                    )
                                    Text(
                                        text = "DIRECT HD STREAM",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right Action Buttons Capsule
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(CornerRadius.surface))
                                .background(Color(0xFF0F121C).copy(alpha = 0.85f))
                                .border(
                                    width = 0.65.dp,
                                    brush = Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                                    ),
                                    shape = RoundedCornerShape(CornerRadius.surface)
                                )
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            // Switch to Web Player Button
                            IconButton(
                                onClick = onSwitchToWeb,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .colorOSPressBounce(pressedScale = 0.92f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = "Web Player",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Aspect Ratio Toggle
                            IconButton(
                                onClick = {
                                    resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    } else {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
                                            Color(0xFF00E5FF).copy(alpha = 0.20f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .colorOSPressBounce(pressedScale = 0.92f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) Color(0xFF00E5FF) else Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // PiP Button
                            IconButton(
                                onClick = onEnterPip,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .colorOSPressBounce(pressedScale = 0.92f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PictureInPictureAlt,
                                    contentDescription = "Picture in Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Download Button
                            IconButton(
                                onClick = onDownload,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                    .colorOSPressBounce(pressedScale = 0.92f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FileDownload,
                                    contentDescription = "Download Episode",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Center Control Buttons (Rewind 10, Play/Pause Hero, Fast Forward 10)
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {} // Consume click
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Rewind 10s
                        IconButton(
                            onClick = {
                                val p = exoPlayerState ?: return@IconButton
                                val target = (p.currentPosition - 10000L).coerceAtLeast(0L)
                                p.seekTo(target)
                                currentPosMs = target
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF101320).copy(alpha = 0.70f))
                                .border(
                                    width = 0.65.dp,
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = CircleShape
                                )
                                .colorOSPressBounce(pressedScale = 0.90f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Hero Play/Pause Button
                        IconButton(
                            onClick = {
                                val p = exoPlayerState ?: return@IconButton
                                if (p.isPlaying) {
                                    p.pause()
                                } else {
                                    p.play()
                                }
                                isPlaying = p.isPlaying
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF00E5FF).copy(alpha = 0.28f),
                                            Color(0xFF0072FF).copy(alpha = 0.15f),
                                            Color(0xFF0C0E16).copy(alpha = 0.85f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF0072FF))
                                    ),
                                    shape = CircleShape
                                )
                                .colorOSPressBounce(pressedScale = 0.90f)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = {
                                val p = exoPlayerState ?: return@IconButton
                                val maxD = if (p.duration > 0) p.duration else Long.MAX_VALUE
                                val target = (p.currentPosition + 10000L).coerceAtMost(maxD)
                                p.seekTo(target)
                                currentPosMs = target
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF101320).copy(alpha = 0.70f))
                                .border(
                                    width = 0.65.dp,
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = CircleShape
                                )
                                .colorOSPressBounce(pressedScale = 0.90f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Bottom Liquid Glass Controls & Scrubber Dock
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(CornerRadius.surface))
                            .background(Color(0xFF0F121C).copy(alpha = 0.85f))
                            .border(
                                width = 0.65.dp,
                                brush = Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                                ),
                                shape = RoundedCornerShape(CornerRadius.surface)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {} // Consume click
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        // Top row: Timestamps, Speed Chip, Orientation Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Timestamps
                            val displayPos = if (isScrubbing) scrubPositionMs else currentPosMs
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = formatTime(displayPos),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "/",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = formatTime(durationMs),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Right controls: Speed Chip & Orientation Toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Speed Selector Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .clickable {
                                            val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
                                            val nextIndex = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                                            val nextSpeed = speeds[nextIndex]
                                            playbackSpeed = nextSpeed
                                            exoPlayerState?.playbackParameters = PlaybackParameters(nextSpeed)
                                            lastInteractionTime = System.currentTimeMillis()
                                        }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Orientation Lock Toggle
                                IconButton(
                                    onClick = {
                                        if (activity != null) {
                                            activity.requestedOrientation = if (isLandscape) {
                                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } else {
                                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            }
                                        }
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .colorOSPressBounce(pressedScale = 0.90f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ScreenRotation,
                                        contentDescription = "Rotate Screen",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // ColorOS 17 Fluid Scrubber Slider
                        val progressFraction = if (durationMs > 0) {
                            val activeVal = if (isScrubbing) scrubPositionMs else currentPosMs
                            (activeVal.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Slider(
                            value = progressFraction,
                            onValueChange = { frac ->
                                isScrubbing = true
                                scrubPositionMs = (frac * durationMs).toLong()
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onValueChangeFinished = {
                                exoPlayerState?.seekTo(scrubPositionMs)
                                currentPosMs = scrubPositionMs
                                isScrubbing = false
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    }
                }
            }
        }
    }

    class WebStreamBridge(
        private val onStreamFound: (String, String?) -> Unit,
        private val onPlaybackActive: (Int, Int, Float) -> Unit
    ) {
        @JavascriptInterface
        fun onVideoSourceFound(src: String?) {
            if (!src.isNullOrBlank() && !src.startsWith("blob:")) {
                Handler(Looper.getMainLooper()).post {
                    onStreamFound(src, null)
                }
            }
        }

        @JavascriptInterface
        fun onVideoSourceFoundWithRef(src: String?, referer: String?) {
            if (!src.isNullOrBlank() && !src.startsWith("blob:")) {
                Handler(Looper.getMainLooper()).post {
                    onStreamFound(src, referer)
                }
            }
        }

        @JavascriptInterface
        fun onPlaybackActive(width: Int, height: Int, currentTime: Float) {
            Handler(Looper.getMainLooper()).post {
                onPlaybackActive(width, height, currentTime)
            }
        }
    }

    @Composable
    private fun InAppWebPlayerScreen(
        watchUrl: String,
        showTitle: String,
        activeCapturedStream: String?,
        activePlaybackHeight: Int,
        onStreamCaptured: (String, String?) -> Unit,
        onPlaybackHeightDetected: (Int) -> Unit,
        onBack: () -> Unit,
        onDownload: (String, String?) -> Unit,
        onOpenExternal: () -> Unit
    ) {
        val context = LocalContext.current
        var webViewRef by remember { mutableStateOf<WebView?>(null) }
        var webProgress by remember { mutableIntStateOf(0) }
        var detectedVideoUrl by remember(activeCapturedStream) { mutableStateOf(activeCapturedStream) }
        var detectedReferer by remember { mutableStateOf<String?>(null) }
        var isScanningStream by remember { mutableStateOf(false) }
        val isAdBlockerActive by AdBlocker.enabledFlow(context).collectAsState(initial = AdBlocker.isEnabled(context))
        val hasStream = !detectedVideoUrl.isNullOrBlank()

        LaunchedEffect(detectedVideoUrl) {
            if (!detectedVideoUrl.isNullOrBlank()) {
                Toast.makeText(context, "Website stream captured! Download ready.", Toast.LENGTH_SHORT).show()
            }
        }

        fun handleStreamFound(stream: String, referer: String?) {
            val clean = stream.lowercase()
            val isM3u8 = clean.contains(".m3u8") || clean.contains("/hls") || clean.contains("manifest")
            val isMaster = clean.contains("master") || clean.contains("playlist") || clean.contains("index")
            val ref = referer ?: detectedReferer ?: watchUrl

            if (detectedVideoUrl == null || (isM3u8 && (detectedVideoUrl?.contains(".m3u8") != true || isMaster))) {
                detectedVideoUrl = stream
                detectedReferer = ref
                onStreamCaptured(stream, ref)
            }
        }

        fun isVideoUrl(url: String): Boolean {
            val clean = url.lowercase()
            val pathOnly = try { Uri.parse(clean).path?.lowercase() ?: clean } catch (e: Exception) { clean }
            if (pathOnly.endsWith(".png") || pathOnly.endsWith(".jpg") || pathOnly.endsWith(".jpeg") ||
                pathOnly.endsWith(".webp") || pathOnly.endsWith(".svg") || pathOnly.endsWith(".gif") ||
                pathOnly.endsWith(".css") || pathOnly.endsWith(".woff") || pathOnly.endsWith(".woff2") ||
                pathOnly.endsWith(".ttf") || pathOnly.endsWith(".ico") || pathOnly.endsWith(".otf")
            ) {
                return false
            }
            if (clean.contains("google-analytics") || clean.contains("/telemetry") ||
                clean.contains("doubleclick.net") || clean.contains("googlesyndication") ||
                clean.contains("facebook.com/tr")
            ) {
                return false
            }
            return clean.contains(".m3u8") ||
                    clean.contains(".mp4") ||
                    clean.contains(".mkv") ||
                    clean.contains(".webm") ||
                    clean.contains(".mpd") ||
                    clean.contains("/hls/") ||
                    clean.contains("/hls-") ||
                    clean.contains("/hls") ||
                    clean.contains("/manifest") ||
                    clean.contains("master.txt") ||
                    clean.contains("/stream/") ||
                    clean.contains("mime=video") ||
                    clean.contains("video/") ||
                    clean.contains("mime=application%2fvnd.apple.mpegurl") ||
                    clean.contains("application/x-mpegurl") ||
                    clean.contains("application/vnd.apple.mpegurl")
        }

        DisposableEffect(Unit) {
            onDispose {
                webViewRef?.destroy()
            }
        }

        val parsedHost = remember(watchUrl) {
            try {
                Uri.parse(watchUrl).host?.removePrefix("www.") ?: "Stream Host"
            } catch (e: Exception) {
                "Stream Host"
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07080D))
        ) {
            // Liquid Glass Top Capsule Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.surface))
                        .background(Color(0xFF0F121C).copy(alpha = 0.88f))
                        .border(
                            width = 0.65.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color(0xFF00E5FF).copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(CornerRadius.surface)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back & Title Capsule
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .colorOSPressBounce(pressedScale = 0.90f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = showTitle,
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                )
                                Text(
                                    text = parsedHost.uppercase(),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Action Buttons (Ad Blocker, Reload, Download, Open External)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val nextState = !isAdBlockerActive
                                AdBlocker.setEnabled(context, nextState)
                                webViewRef?.settings?.apply {
                                    setSupportMultipleWindows(!nextState)
                                    javaScriptCanOpenWindowsAutomatically = !nextState
                                }
                                Toast.makeText(
                                    context,
                                    if (nextState) "Ad Blocker: Enabled" else "Ad Blocker: Disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                                webViewRef?.reload()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isAdBlockerActive) Color(0xFF00E5FF).copy(alpha = 0.20f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    width = 0.75.dp,
                                    color = if (isAdBlockerActive) Color(0xFF00E5FF).copy(alpha = 0.65f) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .colorOSPressBounce(pressedScale = 0.90f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = if (isAdBlockerActive) "Ad Blocker Enabled" else "Ad Blocker Disabled",
                                tint = if (isAdBlockerActive) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.40f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .colorOSPressBounce(pressedScale = 0.90f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reload Page",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val currentStream = detectedVideoUrl
                                if (!currentStream.isNullOrBlank()) {
                                    onDownload(currentStream, detectedReferer ?: watchUrl)
                                } else {
                                    isScanningStream = true
                                    webViewRef?.evaluateJavascript(
                                        """
                                        (function() {
                                            try {
                                                var vids = document.querySelectorAll('video, audio');
                                                for (var i = 0; i < vids.length; i++) {
                                                    var v = vids[i];
                                                    var s = v.currentSrc || v.src;
                                                    if (s && s.indexOf('blob:') !== 0 && s.indexOf('http') === 0) return s;
                                                    var sources = v.querySelectorAll('source');
                                                    for (var j = 0; j < sources.length; j++) {
                                                        var src = sources[j].src;
                                                        if (src && src.indexOf('blob:') !== 0 && src.indexOf('http') === 0) return src;
                                                    }
                                                }
                                            } catch(e) {}
                                            return null;
                                        })();
                                        """.trimIndent()
                                    ) { result ->
                                        isScanningStream = false
                                        val cleanResult = result?.trim('"', ' ', '\n', '\r')?.takeIf { it != "null" && it.isNotBlank() }
                                        if (!cleanResult.isNullOrBlank()) {
                                            handleStreamFound(cleanResult, detectedReferer ?: watchUrl)
                                            onDownload(cleanResult, detectedReferer ?: watchUrl)
                                        } else {
                                            // Fallback to webpage URL so user ALWAYS has quality selection available
                                            onDownload(watchUrl, detectedReferer ?: watchUrl)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (hasStream) Color(0xFF00E5FF).copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    width = if (hasStream) 1.dp else 0.5.dp,
                                    color = if (hasStream) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .colorOSPressBounce(pressedScale = 0.90f)
                        ) {
                            if (isScanningStream) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF00E5FF),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (hasStream) Icons.Rounded.DownloadDone else Icons.Rounded.FileDownload,
                                    contentDescription = if (hasStream) "Stream Ready - Tap to Download" else "Download Episode",
                                    tint = if (hasStream) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.60f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenExternal,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .colorOSPressBounce(pressedScale = 0.90f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInBrowser,
                                contentDescription = "Open in Browser",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Web Loading Progress Bar at bottom of capsule
                if (webProgress in 1..99) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(webProgress / 100f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF0072FF))
                                    )
                                )
                        )
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

                        addJavascriptInterface(WebStreamBridge(
                            onStreamFound = { src, ref ->
                                handleStreamFound(src, ref)
                            },
                            onPlaybackActive = { width, height, _ ->
                                if (height > 0) {
                                    onPlaybackHeightDetected(height)
                                }
                            }
                        ), "WebBridge")

                        val adBlockActive = AdBlocker.isEnabled(ctx)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            setSupportMultipleWindows(!adBlockActive)
                            javaScriptCanOpenWindowsAutomatically = !adBlockActive
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                val defuseScript = """
                                    (function() {
                                        try {
                                            window.P21G = { on: 0 };
                                            if (window.P21 && window.P21.ab) {
                                                window.P21.ab.on = 0;
                                                window.P21.ab.mode = 'none';
                                            }
                                            Object.defineProperty(window, 'P21G', {
                                                get: function() { return { on: 0 }; },
                                                set: function() {},
                                                configurable: true
                                            });
                                            if (!window.__streamHooked) {
                                                window.__streamHooked = true;
                                                var origFetch = window.fetch;
                                                if (origFetch) {
                                                    window.fetch = function() {
                                                        try {
                                                            var a = arguments[0];
                                                            var u = typeof a === 'string' ? a : (a && a.url ? a.url : '');
                                                            if (u && (u.indexOf('.m3u8') !== -1 || u.indexOf('.mp4') !== -1 || u.indexOf('/hls') !== -1)) {
                                                                window.WebBridge && window.WebBridge.onVideoSourceFoundWithRef(u, window.location.href);
                                                            }
                                                        } catch(e) {}
                                                        return origFetch.apply(this, arguments);
                                                    };
                                                }
                                                var origOpen = XMLHttpRequest.prototype.open;
                                                if (origOpen) {
                                                    XMLHttpRequest.prototype.open = function(m, u) {
                                                        try {
                                                            if (typeof u === 'string' && (u.indexOf('.m3u8') !== -1 || u.indexOf('.mp4') !== -1 || u.indexOf('/hls') !== -1)) {
                                                                window.WebBridge && window.WebBridge.onVideoSourceFoundWithRef(u, window.location.href);
                                                            }
                                                        } catch(e) {}
                                                        return origOpen.apply(this, arguments);
                                                    };
                                                }
                                            }
                                        } catch(e) {}
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(defuseScript, null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val cleanupScript = """
                                    (function() {
                                        try {
                                            if (window.P21 && window.P21.ab) {
                                                window.P21.ab.on = 0;
                                                window.P21.ab.mode = 'none';
                                            }
                                            if (window.P21G) {
                                                window.P21G.on = 0;
                                            }
                                        } catch(e) {}
                                        function purgeOverlays() {
                                            try {
                                                var elms = document.querySelectorAll('div[style*="z-index:2147483647"], div[style*="z-index: 2147483647"]');
                                                elms.forEach(function(el) { el.remove(); });
                                                document.querySelectorAll('div').forEach(function(d) {
                                                    if (d.textContent && (d.textContent.indexOf('turn off your ad blocker') !== -1 || d.textContent.indexOf('Open your blocker') !== -1)) {
                                                        var root = d;
                                                        while (root.parentElement && root.parentElement !== document.body && root.parentElement !== document.documentElement) {
                                                            root = root.parentElement;
                                                        }
                                                        root.remove();
                                                    }
                                                });
                                                document.documentElement.style.setProperty('overflow', 'auto', 'important');
                                                document.body.style.setProperty('overflow', 'auto', 'important');
                                            } catch(e) {}
                                        }
                                        purgeOverlays();
                                        setInterval(purgeOverlays, 350);

                                        function scanMedia() {
                                            try {
                                                var vids = document.querySelectorAll('video, audio');
                                                for (var i = 0; i < vids.length; i++) {
                                                    var v = vids[i];
                                                    if (v.videoWidth > 0 && v.videoHeight > 0) {
                                                        window.WebBridge && window.WebBridge.onPlaybackActive(v.videoWidth, v.videoHeight, v.currentTime || 0);
                                                    }
                                                    var s = v.currentSrc || v.src;
                                                    if (s && s.indexOf('blob:') !== 0 && s.indexOf('http') === 0) {
                                                        window.WebBridge && window.WebBridge.onVideoSourceFoundWithRef(s, window.location.href);
                                                    }
                                                    var sources = v.querySelectorAll('source');
                                                    for (var j = 0; j < sources.length; j++) {
                                                        var src = sources[j].src;
                                                        if (src && src.indexOf('blob:') !== 0 && src.indexOf('http') === 0) {
                                                            window.WebBridge && window.WebBridge.onVideoSourceFoundWithRef(src, window.location.href);
                                                        }
                                                    }
                                                }
                                            } catch(e) {}
                                        }
                                        scanMedia();
                                        setInterval(scanMedia, 1200);

                                        try {
                                            var s = document.createElement('style');
                                            s.textContent = 'div[style*="z-index:2147483647"], div[style*="z-index: 2147483647"] { display: none !important; opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; } html, body { overflow: auto !important; }';
                                            document.head.appendChild(s);
                                        } catch(e) {}
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(cleanupScript, null)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (AdBlocker.isEnabled(context)) {
                                    if (AdBlocker.isDisallowedScheme(url)) {
                                        return true
                                    }
                                    if (AdBlocker.isAdUrl(url)) {
                                        return true
                                    }
                                    return false
                                } else {
                                    if (url.startsWith("intent:") || url.startsWith("market:")) {
                                        try {
                                            context.startActivity(Intent.parseUri(url, Intent.URI_INTENT_SCHEME))
                                            return true
                                        } catch (ignored: Exception) {
                                            return true
                                        }
                                    }
                                    return false
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val url = request?.url?.toString()
                                if (url != null) {
                                    if (AdBlocker.isAntiAdblockProbe(url)) {
                                        return AdBlocker.createProbeResponse(url)
                                    }
                                    if (AdBlocker.isEnabled(context)) {
                                        if (AdBlocker.isAdUrl(url)) {
                                            return AdBlocker.createEmptyResponse()
                                        }
                                    }
                                    val accept = request.requestHeaders?.get("Accept")
                                        ?: request.requestHeaders?.get("accept")
                                        ?: ""
                                    val isMediaAccept = accept.contains("video/") || accept.contains("mpegurl")

                                    val cleanUrl = url.lowercase()
                                    if (cleanUrl.endsWith(".ts") || cleanUrl.contains(".ts?") || cleanUrl.contains(".m4s")) {
                                        val detectedH = when {
                                            cleanUrl.contains("1080") -> 1080
                                            cleanUrl.contains("720") -> 720
                                            cleanUrl.contains("480") -> 480
                                            cleanUrl.contains("360") -> 360
                                            else -> 0
                                        }
                                        if (detectedH > 0) {
                                            view?.post { onPlaybackHeightDetected(detectedH) }
                                        }
                                    }

                                    if (isVideoUrl(url) || isMediaAccept) {
                                        val ref = request.requestHeaders?.get("Referer")
                                            ?: request.requestHeaders?.get("referer")
                                            ?: view?.url
                                            ?: watchUrl
                                        view?.post {
                                            handleStreamFound(url, ref)
                                        }
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onLoadResource(view: WebView?, url: String?) {
                                super.onLoadResource(view, url)
                                if (url != null && isVideoUrl(url)) {
                                    view?.post {
                                        handleStreamFound(url, view.url ?: watchUrl)
                                    }
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                webProgress = newProgress
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                if (AdBlocker.isEnabled(context)) {
                                    return false
                                }
                                val tempWebView = WebView(ctx)
                                tempWebView.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                        val popupUrl = req?.url?.toString() ?: return false
                                        try {
                                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(popupUrl)))
                                        } catch (ignored: Exception) {}
                                        return true
                                    }
                                }
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                transport?.webView = tempWebView
                                resultMsg?.sendToTarget()
                                return true
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                if (customView != null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                hideSystemUI()
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
                                if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                                    showSystemUI()
                                }
                                (window.decorView as ViewGroup).removeView(fullscreenContainer)
                                fullscreenContainer = null
                                customView = null
                                customViewCallback?.onCustomViewHidden()
                            }
                        }

                        loadUrl(watchUrl)
                    }
                },
                update = { webView ->
                    webView.settings.apply {
                        setSupportMultipleWindows(!isAdBlockerActive)
                        javaScriptCanOpenWindowsAutomatically = !isAdBlockerActive
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    private fun downloadVideo(
        context: Context,
        videoUrl: String,
        title: String,
        formatId: String? = null,
        referer: String? = null,
        isAudioOnly: Boolean = false
    ) {
        try {
            com.mydonghua.app.data.service.DownloadEngine.startDownload(
                context = context,
                url = videoUrl,
                title = title,
                formatId = formatId,
                referer = referer,
                isAudioOnly = isAudioOnly
            )
            val msg = if (isAudioOnly) "Starting audio extraction: $title" else "Starting download: $title"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            try {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val uri = Uri.parse(videoUrl)
                val ext = if (isAudioOnly) ".m4a" else ".mp4"
                val request = DownloadManager.Request(uri).apply {
                    setTitle(title)
                    setDescription("Downloading episode for offline viewing...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(
                        if (isAudioOnly) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES,
                        "MyDonghua/${title.replace(Regex("[^a-zA-Z0-9_]"), "_")}$ext"
                    )
                    if (!referer.isNullOrEmpty()) {
                        addRequestHeader("Referer", referer)
                    }
                }
                downloadManager.enqueue(request)
                Toast.makeText(context, "Started downloading: $title", Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                Toast.makeText(context, "Opening download link...", Toast.LENGTH_SHORT).show()
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                } catch (ignored: Exception) {}
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun QualitySelectorBottomSheet(
        streamUrl: String,
        title: String,
        referer: String?,
        syncedWebsiteOptions: List<DownloadQualityOption>,
        currentPlaybackHeight: Int,
        isWebsiteStreamCaptured: Boolean,
        isSyncingWebsite: Boolean,
        onDismiss: () -> Unit,
        onOptionSelected: (DownloadQualityOption) -> Unit
    ) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var selectedTab by remember { mutableIntStateOf(0) } // 0: Video, 1: Audio

        val defaultVideoOptions = remember {
            listOf(
                DownloadQualityOption(
                    id = "video_1080p",
                    title = "1080p Full HD",
                    subtitle = "Crystal clear high definition",
                    badge = "1080p",
                    formatId = "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best"
                ),
                DownloadQualityOption(
                    id = "video_720p",
                    title = "720p HD",
                    subtitle = "Smooth playback, recommended for mobile",
                    badge = "720p",
                    formatId = "bestvideo[height<=720]+bestaudio/best[height<=720]/best"
                ),
                DownloadQualityOption(
                    id = "video_480p",
                    title = "480p SD",
                    subtitle = "Standard quality, faster download",
                    badge = "480p",
                    formatId = "bestvideo[height<=480]+bestaudio/best[height<=480]/best"
                ),
                DownloadQualityOption(
                    id = "video_360p",
                    title = "360p Data Saver",
                    subtitle = "Smallest file size, saves mobile data",
                    badge = "360p",
                    formatId = "bestvideo[height<=360]+bestaudio/best[height<=360]/best"
                ),
                DownloadQualityOption(
                    id = "video_best",
                    title = "Best Available (Source)",
                    subtitle = "Highest quality original direct stream",
                    badge = "AUTO",
                    formatId = "best"
                )
            )
        }

        val hasWebsiteSyncedStreams = syncedWebsiteOptions.isNotEmpty()

        // Video options prioritized by actual website streams!
        val videoOptions = remember(syncedWebsiteOptions, currentPlaybackHeight, streamUrl, isWebsiteStreamCaptured) {
            if (hasWebsiteSyncedStreams) {
                val mapped = syncedWebsiteOptions.map { opt ->
                    val h = opt.badge.removeSuffix("p").toIntOrNull() ?: 0
                    val isPlaying = (currentPlaybackHeight > 0 && h == currentPlaybackHeight)
                    opt.copy(isPlayingNow = isPlaying)
                }

                val masterOption = DownloadQualityOption(
                    id = "synced_master_auto",
                    title = "Original Stream (Adaptive Auto)",
                    subtitle = "Master playlist direct from website player",
                    badge = "AUTO",
                    formatId = streamUrl,
                    isAudio = false,
                    isSyncedFromWebsite = true
                )
                mapped + masterOption
            } else if (isWebsiteStreamCaptured) {
                val badge = if (currentPlaybackHeight > 0) "${currentPlaybackHeight}p" else "STREAM"
                val directOption = DownloadQualityOption(
                    id = "synced_direct_source",
                    title = if (currentPlaybackHeight > 0) "${currentPlaybackHeight}p Stream (Website Stream)" else "Website Video Stream",
                    subtitle = "Direct stream synced from active website playback",
                    badge = badge,
                    formatId = streamUrl,
                    isAudio = false,
                    isSyncedFromWebsite = true,
                    isPlayingNow = true
                )
                listOf(directOption) + defaultVideoOptions
            } else {
                defaultVideoOptions
            }
        }

        // Audio options extracted from the website stream!
        val audioOptions = remember(streamUrl, hasWebsiteSyncedStreams) {
            val audioSourceUrl = if (hasWebsiteSyncedStreams) {
                syncedWebsiteOptions.firstOrNull()?.formatId ?: streamUrl
            } else {
                streamUrl
            }
            listOf(
                DownloadQualityOption(
                    id = "audio_m4a",
                    title = "Website Audio (M4A Pristine)",
                    subtitle = "Lossless AAC extracted directly from website stream",
                    badge = "M4A",
                    formatId = audioSourceUrl,
                    isAudio = true,
                    isSyncedFromWebsite = isWebsiteStreamCaptured
                ),
                DownloadQualityOption(
                    id = "audio_mp3",
                    title = "Website Audio (MP3 320 kbps)",
                    subtitle = "Universal high bitrate MP3 extracted from website",
                    badge = "MP3",
                    formatId = audioSourceUrl,
                    isAudio = true,
                    isSyncedFromWebsite = isWebsiteStreamCaptured
                ),
                DownloadQualityOption(
                    id = "audio_aac",
                    title = "Compact Audio (AAC)",
                    subtitle = "Lightweight track extracted from website audio",
                    badge = "AAC",
                    formatId = audioSourceUrl,
                    isAudio = true,
                    isSyncedFromWebsite = isWebsiteStreamCaptured
                )
            )
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color(0xFF0F121C),
            scrimColor = Color.Black.copy(alpha = 0.65f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 6.dp)
                        .width(44.dp)
                        .height(4.5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                )
            },
            shape = RoundedCornerShape(topStart = CornerRadius.sheet, topEnd = CornerRadius.sheet)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                // Header: Title, Donghua Name, Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00E5FF).copy(alpha = 0.25f), Color(0xFF0072FF).copy(alpha = 0.15f))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Download Episode",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = title,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .colorOSPressBounce(pressedScale = 0.90f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Stream Sync Status Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (hasWebsiteSyncedStreams) Color(0xFF00E5FF).copy(alpha = 0.10f)
                            else if (isWebsiteStreamCaptured) Color(0xFF00E5FF).copy(alpha = 0.06f)
                            else Color(0xFFFFB300).copy(alpha = 0.08f)
                        )
                        .border(
                            width = 0.65.dp,
                            color = if (hasWebsiteSyncedStreams) Color(0xFF00E5FF).copy(alpha = 0.45f)
                                    else if (isWebsiteStreamCaptured) Color(0xFF00E5FF).copy(alpha = 0.25f)
                                    else Color(0xFFFFB300).copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasWebsiteSyncedStreams || isWebsiteStreamCaptured) Color(0xFF00E5FF)
                                    else Color(0xFFFFB300)
                                )
                        )
                        Text(
                            text = if (hasWebsiteSyncedStreams) "SYNCED FROM WEBSITE PLAYBACK (${syncedWebsiteOptions.size} streams)"
                                   else if (isWebsiteStreamCaptured) "SYNCED FROM WEBSITE STREAM"
                                   else "WEBSITE STREAM NOT STARTED",
                            color = if (hasWebsiteSyncedStreams || isWebsiteStreamCaptured) Color(0xFF00E5FF)
                                    else Color(0xFFFFB300),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isSyncingWebsite) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(11.dp),
                                color = Color(0xFF00E5FF),
                                strokeWidth = 1.8.dp
                            )
                            Text(
                                text = "Syncing...",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.5.sp
                            )
                        }
                    } else if (currentPlaybackHeight > 0) {
                        Text(
                            text = "Playing: ${currentPlaybackHeight}p",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Segmented Tab Row: Video Qualities vs Audio Formats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.chip))
                        .background(Color(0xFF0A0C14))
                        .border(
                            width = 0.65.dp,
                            color = Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(CornerRadius.chip)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tab 0: Video Qualities
                    val isVideoTab = selectedTab == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isVideoTab) Color(0xFF00E5FF).copy(alpha = 0.18f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (isVideoTab) 1.dp else 0.dp,
                                color = if (isVideoTab) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VideoLibrary,
                                contentDescription = null,
                                tint = if (isVideoTab) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Video Qualities",
                                color = if (isVideoTab) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = if (isVideoTab) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Tab 1: Audio Formats
                    val isAudioTab = selectedTab == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isAudioTab) Color(0xFFB388FF).copy(alpha = 0.18f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (isAudioTab) 1.dp else 0.dp,
                                color = if (isAudioTab) Color(0xFFB388FF).copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = if (isAudioTab) Color(0xFFB388FF) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Audio Formats",
                                color = if (isAudioTab) Color(0xFFB388FF) else Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = if (isAudioTab) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Options List
                val currentOptions = if (selectedTab == 0) videoOptions else audioOptions
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentOptions.forEach { option ->
                        QualityOptionCard(
                            option = option,
                            onClick = { onOptionSelected(option) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer note
                Text(
                    text = "Downloads will be saved to Movies or Music folder with 16 Aria2c streams.",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }

    @Composable
    private fun QualityOptionCard(
        option: DownloadQualityOption,
        onClick: () -> Unit
    ) {
        val isAudio = option.isAudio
        val accentColor = if (isAudio) Color(0xFFB388FF) else Color(0xFF00E5FF)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CornerRadius.card))
                .background(Color(0xFF131726))
                .border(
                    width = if (option.isPlayingNow) 1.25.dp else 0.75.dp,
                    brush = Brush.linearGradient(
                        if (option.isPlayingNow) listOf(Color(0xFF00E5FF), Color(0xFF0072FF))
                        else listOf(
                            Color.White.copy(alpha = 0.12f),
                            accentColor.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    ),
                    shape = RoundedCornerShape(CornerRadius.card)
                )
                .clickable(onClick = onClick)
                .colorOSPressBounce(pressedScale = 0.96f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Quality Badge Pill
                Box(
                    modifier = Modifier
                        .widthIn(min = 54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                if (isAudio) listOf(Color(0xFFB388FF).copy(alpha = 0.25f), Color(0xFF7C4DFF).copy(alpha = 0.15f))
                                else listOf(Color(0xFF00E5FF).copy(alpha = 0.25f), Color(0xFF0072FF).copy(alpha = 0.15f))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.badge,
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = option.title,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (option.isPlayingNow) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.20f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PLAYING NOW",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (option.isSyncedFromWebsite) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "WEBSITE",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Text(
                        text = option.subtitle,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!option.fileSizeFormatted.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = option.fileSizeFormatted,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = "Download",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    companion object {
        fun parseHlsMasterPlaylist(masterUrl: String, content: String): List<DownloadQualityOption> {
            val lines = content.lines()
            val options = mutableListOf<DownloadQualityOption>()
            var currentBandwidth = 0L
            var currentResolution = ""
            var currentWidth = 0
            var currentHeight = 0
            val uriBase = try { URI(masterUrl) } catch (e: Exception) { null }

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXT-X-STREAM-INF:")) {
                    val attrs = trimmed.removePrefix("#EXT-X-STREAM-INF:")
                    val bwMatch = Regex("BANDWIDTH=(\\d+)").find(attrs)
                    currentBandwidth = bwMatch?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L

                    val resMatch = Regex("RESOLUTION=(\\d+)x(\\d+)").find(attrs)
                    if (resMatch != null) {
                        currentWidth = resMatch.groupValues[1].toIntOrNull() ?: 0
                        currentHeight = resMatch.groupValues[2].toIntOrNull() ?: 0
                        currentResolution = "${currentHeight}p"
                    } else {
                        currentWidth = 0
                        currentHeight = 0
                        currentResolution = ""
                    }
                } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val absoluteVariantUrl = if (uriBase != null) {
                        try {
                            uriBase.resolve(trimmed).toString()
                        } catch (e: Exception) {
                            trimmed
                        }
                    } else {
                        trimmed
                    }

                    val badge = if (currentResolution.isNotEmpty()) currentResolution else {
                        when {
                            currentBandwidth >= 3500000 -> "1080p"
                            currentBandwidth >= 1800000 -> "720p"
                            currentBandwidth >= 900000 -> "480p"
                            currentBandwidth > 0 -> "360p"
                            else -> "HD"
                        }
                    }

                    val title = when (badge) {
                        "2160p" -> "4K Ultra HD"
                        "1440p" -> "2K Quad HD"
                        "1080p" -> "1080p Full HD"
                        "720p" -> "720p HD"
                        "480p" -> "480p SD"
                        "360p" -> "360p Data Saver"
                        else -> "$badge Stream"
                    }

                    val bwFormatted = if (currentBandwidth > 0) {
                        val mbps = currentBandwidth / 1_000_000.0
                        String.format(java.util.Locale.US, "%.1f Mbps", mbps)
                    } else null

                    val subtitle = if (bwFormatted != null) "Bitrate: $bwFormatted • Synced from website"
                                   else "Synced from website playback"

                    options.add(
                        DownloadQualityOption(
                            id = "synced_${badge}_${options.size}",
                            title = title,
                            subtitle = subtitle,
                            badge = badge,
                            formatId = absoluteVariantUrl,
                            isAudio = false,
                            fileSizeFormatted = bwFormatted,
                            isSyncedFromWebsite = true
                        )
                    )

                    currentBandwidth = 0L
                    currentResolution = ""
                    currentWidth = 0
                    currentHeight = 0
                }
            }

            return options.sortedByDescending { opt ->
                opt.badge.removeSuffix("p").toIntOrNull() ?: 0
            }
        }

        fun formatTime(ms: Long): String {
            if (ms <= 0L) return "00:00"
            val totalSeconds = ms / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return ""
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}

data class DownloadQualityOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val formatId: String,
    val isAudio: Boolean = false,
    val fileSizeFormatted: String? = null,
    val isSyncedFromWebsite: Boolean = false,
    val isPlayingNow: Boolean = false
)
