package com.nsr.qilin.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nsr.qilin.R
import com.nsr.qilin.data.service.AniListMedia
import com.nsr.qilin.data.service.AniListMediaDetail
import com.nsr.qilin.data.service.AniListService
import com.nsr.qilin.data.service.PosterFetchService
import com.nsr.qilin.domain.model.CountdownCalculator
import com.nsr.qilin.domain.model.Show
import com.nsr.qilin.ui.GlassHeader
import com.nsr.qilin.ui.Spacing
import com.nsr.qilin.ui.common.ColorOSDialog
import com.nsr.qilin.ui.common.ColorOSOutlinedButton
import com.nsr.qilin.ui.common.ColorOSPillButton
import com.nsr.qilin.ui.common.DonghuaPoster
import com.nsr.qilin.ui.common.EmptyState
import com.nsr.qilin.ui.rememberLiquidGlassBorder
import com.nsr.qilin.ui.theme.CornerRadius
import com.nsr.qilin.ui.theme.Haptics
import com.nsr.qilin.ui.theme.JustSansFamily
import com.nsr.qilin.ui.theme.colorOSPressBounce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ShowDetailScreen(
    showId: String,
    viewModel: ShowsViewModel,
    onWatchClick: (String) -> Unit,
    onEditClick: (Show) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    defaultStreamProvider: String = "donghuastream",
    customStreamUrl: String = ""
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allShows by viewModel.allShows.collectAsState()
    val show = remember(showId, allShows) { allShows.find { it.id == showId } }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    LaunchedEffect(Unit) {
        Haptics.init(context)
    }

    var showAniListMenu by remember { mutableStateOf(false) }
    var showLinkAniListDialog by remember { mutableStateOf(false) }
    var showScoreRateDialog by remember { mutableStateOf(false) }
    var userScoreInput by remember { mutableStateOf("") }
    var linkSearchQuery by remember { mutableStateOf("") }
    var isSearchingAniList by remember { mutableStateOf(false) }
    var aniListSearchResults by remember { mutableStateOf<List<AniListMedia>>(emptyList()) }
    var aniListDetail by remember { mutableStateOf<AniListMediaDetail?>(null) }
    var isQuickSyncing by remember { mutableStateOf(false) }

    LaunchedEffect(show?.aniListId) {
        val targetId = show?.aniListId
        if (targetId != null) {
            val res = AniListService.fetchMediaDetails(targetId)
            if (res.isSuccess) {
                aniListDetail = res.getOrNull()
            }
        } else {
            aniListDetail = null
        }
    }

    if (show == null) {
        val isLoading by viewModel.isLoading.collectAsState()
        val emptyBackdrop = rememberLayerBackdrop()
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(emptyBackdrop)
                    .padding(horizontal = Spacing.lg),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(38.dp),
                            strokeWidth = 3.5.dp,
                            color = Color(0xFF0082FB)
                        )
                        Text(
                            text = "Loading show details...",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    EmptyState(
                        title = "Show not found",
                        subtitle = "This show may have been removed from your library",
                        painter = painterResource(id = R.drawable.ic_airing)
                    )
                }
            }
            GlassHeader(
                title = "Show Details",
                backdrop = emptyBackdrop,
                isDark = !isLight,
                onBackClick = onNavigateBack
            )
        }
        return
    }

    var countdownTime by remember(show.id) {
        mutableStateOf(CountdownCalculator.calculateTimeRemaining(CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime)))
    }

    LaunchedEffect(show.releaseDay, show.releaseTime) {
        while (true) {
            countdownTime = CountdownCalculator.calculateTimeRemaining(
                CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime)
            )
            delay(1000)
        }
    }

    var isSynopsisExpanded by remember { mutableStateOf(false) }
    val glassBorder = rememberLiquidGlassBorder(isLight)
    val screenBackdrop = rememberLayerBackdrop()
    val cardBgColor = if (isLight) Color.White.copy(alpha = 0.85f) else Color(0xFF1E2638).copy(alpha = 0.45f)
    val chipBgColor = if (isLight) Color.White.copy(alpha = 0.85f) else Color(0xFF1E2638).copy(alpha = 0.45f)

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlphaAnim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val statusText = when (show.status) {
            "ongoing" -> "AIRING"
            "completed" -> "COMPLETED"
            else -> "HIATUS"
        }
        val detailSubtitle = "$statusText • EP ${show.currentEp}${if (show.totalEp > 0) "/${show.totalEp}" else ""}"
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val headerTopPadding = statusBarTop + 84.dp

        // Scrollable Body Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(screenBackdrop)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md)
                .padding(top = headerTopPadding, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Cinematic Hero Header Card (Backdrop + Floating Poster + Title Badges)
            val bannerShape = RoundedCornerShape(CornerRadius.banner)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(268.dp)
                    .clip(bannerShape)
                    .background(Color(0xFF0C101B))
                    .border(
                        width = 0.75.dp,
                        brush = glassBorder,
                        shape = bannerShape
                    )
            ) {
                // Background Backdrop Art with Fluid Blur Gradient
                val heroBackdropUrl = aniListDetail?.bannerImage ?: show.poster
                DonghuaPoster(
                    posterUrl = heroBackdropUrl,
                    title = show.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    showAcronym = false,
                    crossfade = true,
                    immediate = true
                )

                // Dark Aquamorphic Gradient Vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.28f),
                                    Color.Black.copy(alpha = 0.45f),
                                    Color(0xFF090D16).copy(alpha = 0.94f)
                                )
                            )
                        )
                )

                // Hero Content Row (Floating Squircle Poster + Right Details)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Elevated Floating Squircle Poster Card
                    val thumbShape = RoundedCornerShape(18.dp)
                    Box(
                        modifier = Modifier
                            .size(width = 98.dp, height = 142.dp)
                            .clip(thumbShape)
                            .border(
                                width = 1.dp,
                                brush = rememberLiquidGlassBorder(isLight = false),
                                shape = thumbShape
                            )
                    ) {
                        DonghuaPoster(
                            posterUrl = show.poster,
                            title = show.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            fontSize = 11.sp,
                            immediate = true
                        )
                    }

                    // Title & Badges Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val statusColor = when (show.status) {
                            "ongoing" -> Color(0xFF0082FB)
                            "completed" -> Color(0xFF10B981)
                            else -> Color(0xFFFFA726)
                        }

                        // Status & AniList Badges Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Status Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.22f))
                                    .border(0.6.dp, statusColor.copy(alpha = 0.45f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (show.status == "ongoing") statusColor.copy(alpha = pulseAlpha)
                                            else statusColor
                                        )
                                )
                                Text(
                                    text = when (show.status) {
                                        "ongoing" -> "Airing"
                                        "completed" -> "Completed"
                                        else -> "Hiatus"
                                    },
                                    fontSize = 10.5.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            // AniList Rating Badge (if available)
                            val avgScore = aniListDetail?.averageScore
                            if (avgScore != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF02A9FF).copy(alpha = 0.22f))
                                        .border(0.6.dp, Color(0xFF02A9FF).copy(alpha = 0.50f), CircleShape)
                                        .clickable {
                                            Haptics.tick()
                                            showAniListMenu = true
                                        }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "★ $avgScore% AL",
                                        fontSize = 10.5.sp,
                                        fontFamily = JustSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF02A9FF)
                                    )
                                }
                            }
                        }

                        // Main Title
                        Text(
                            text = show.title,
                            fontSize = 18.5.sp,
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 23.sp
                        )

                        // Chinese Native Title
                        if (!show.titleZh.isNullOrEmpty()) {
                            Text(
                                text = show.titleZh,
                                fontSize = 12.sp,
                                fontFamily = JustSansFamily,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Quick Metadata Badges Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (show.totalEp > 0) {
                                Text(
                                    text = "${show.totalEp} Eps",
                                    fontSize = 11.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            if (show.releaseYear > 0) {
                                if (show.totalEp > 0) {
                                    Text(
                                        text = "•",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                }
                                Text(
                                    text = "${show.releaseYear}",
                                    fontSize = 11.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            val format = aniListDetail?.format
                            if (!format.isNullOrBlank()) {
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.45f)
                                )
                                Text(
                                    text = format,
                                    fontSize = 11.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF02A9FF)
                                )
                            }
                        }
                    }
                }
            }

            // 2. ColorOS 17 Quick Action Dock (Watch Now Hero Pill + Favorite + Notify + Edit + Share)
            val providerLabel = when (defaultStreamProvider.lowercase()) {
                "luciferdonghua" -> "LuciferDonghua ⚡"
                "donghuastream" -> "DonghuaStream 🎬"
                "direct", "custom" -> if (!show.watchUrl.isNullOrBlank()) "Direct Link 🔗" else "Custom Stream 🌐"
                else -> if (!show.watchUrl.isNullOrBlank()) "Direct Link 🔗" else "Watch Stream 🎬"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val watchShimmer = rememberInfiniteTransition(label = "watchShimmerSweep")
                    .animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2400, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "watchSweepProgress"
                    )

                val nextEpToWatch = if (show.currentEp > 0) show.currentEp + 1 else 1
                val isFullyWatched = show.totalEp > 0 && show.currentEp >= show.totalEp
                val watchButtonLabel = if (isFullyWatched) "REWATCH SHOW" else "WATCH EP $nextEpToWatch"

                // Primary Watch Action Capsule Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0072FF), Color(0xFF00A2FF))
                            )
                        )
                        .colorOSPressBounce(
                            pressedScale = 0.96f,
                            onClick = {
                                Haptics.impact()
                                val targetUrl = PosterFetchService.resolveWatchUrl(
                                    showTitle = show.title,
                                    customWatchUrl = show.watchUrl,
                                    providerPreference = defaultStreamProvider,
                                    customProviderUrl = customStreamUrl
                                )
                                onWatchClick(targetUrl)
                            }
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawWithContent {
                                val travel = size.width * 2.4f
                                val x = -size.width * 0.6f + watchShimmer.value * travel
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.28f),
                                            Color.Transparent
                                        ),
                                        start = Offset(x - size.width * 0.22f, 0f),
                                        end = Offset(x + size.width * 0.22f, size.height)
                                    )
                                )
                            }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.24f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = watchButtonLabel,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                            Text(
                                text = providerLabel,
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 10.5.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                val isFavorite = show.isFavorite == 1
                val hasNotification = show.alarmRequestCode != null

                // Favorite Toggle Button (Frosted Circle)
                IconButton(
                    onClick = {
                        Haptics.toggle(!isFavorite)
                        viewModel.toggleFavorite(show)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFavorite) Color(0xFFFF4081).copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                        )
                        .border(
                            width = 1.dp,
                            brush = glassBorder,
                            shape = CircleShape
                        )
                        .colorOSPressBounce(pressedScale = 0.90f)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Notification Bell Button (Frosted Circle)
                IconButton(
                    onClick = {
                        Haptics.toggle(!hasNotification)
                        viewModel.toggleNotification(context, show)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasNotification) Color(0xFF0082FB).copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                        )
                        .border(
                            width = 1.dp,
                            brush = glassBorder,
                            shape = CircleShape
                        )
                        .colorOSPressBounce(pressedScale = 0.90f)
                ) {
                    Icon(
                        imageVector = if (hasNotification) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                        contentDescription = "Toggle Notifications",
                        tint = if (hasNotification) Color(0xFF0082FB) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Edit Show Button (Frosted Circle)
                IconButton(
                    onClick = {
                        Haptics.tick()
                        onEditClick(show)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f))
                        .border(
                            width = 1.dp,
                            brush = glassBorder,
                            shape = CircleShape
                        )
                        .colorOSPressBounce(pressedScale = 0.90f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Show",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share Button (Frosted Circle)
                IconButton(
                    onClick = {
                        Haptics.tick()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            val epText = if (show.totalEp > 0) "${show.currentEp}/${show.totalEp}" else "${show.currentEp}"
                            val shareBody = buildString {
                                append("Watching ${show.title} (Episode $epText) on Qilin!")
                                if (!show.watchUrl.isNullOrBlank()) {
                                    append("\nWatch: ${show.watchUrl}")
                                }
                            }
                            putExtra(Intent.EXTRA_TEXT, shareBody)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share ${show.title}"))
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f))
                        .border(
                            width = 1.dp,
                            brush = glassBorder,
                            shape = CircleShape
                        )
                        .colorOSPressBounce(pressedScale = 0.90f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share Show",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3. Episode Progress Bento Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CornerRadius.card))
                    .background(cardBgColor)
                    .border(
                        width = 0.75.dp,
                        brush = glassBorder,
                        shape = RoundedCornerShape(CornerRadius.card)
                    )
                    .padding(Spacing.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header Row: Watch Progress Title & AniList Integration Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VideoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF0082FB),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "WATCH PROGRESS",
                                fontSize = 11.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0082FB),
                                letterSpacing = 0.5.sp
                            )
                        }

                        // AniList Pill
                        if (show.aniListId != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF02A9FF).copy(alpha = 0.15f))
                                    .border(0.6.dp, Color(0xFF02A9FF).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        Haptics.tick()
                                        showAniListMenu = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudSync,
                                    contentDescription = null,
                                    tint = Color(0xFF02A9FF),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "AniList #${show.aniListId}",
                                    fontSize = 10.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF02A9FF)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .border(0.6.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        Haptics.tick()
                                        linkSearchQuery = show.title
                                        isSearchingAniList = true
                                        coroutineScope.launch {
                                            aniListSearchResults = AniListService.searchAnime(show.title, limit = 8)
                                            isSearchingAniList = false
                                        }
                                        showLinkAniListDialog = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Link AniList",
                                    fontSize = 10.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Large Episode Typography & Steppers Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val totalStr = if (show.totalEp > 0) "of ${show.totalEp} Episodes" else "Ongoing Series"
                            Text(
                                text = "Episode ${show.currentEp}",
                                fontSize = 21.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = totalStr,
                                fontSize = 12.5.sp,
                                fontFamily = JustSansFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Tactile Stepper Controls (- / +)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    Haptics.tick()
                                    viewModel.decrementEpisode(show)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                    .border(
                                        width = 0.75.dp,
                                        brush = rememberLiquidGlassBorder(isLight),
                                        shape = CircleShape
                                    )
                                    .colorOSPressBounce(pressedScale = 0.88f)
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    Haptics.tick()
                                    viewModel.incrementEpisode(show)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF0072FF), Color(0xFF00A2FF))
                                        )
                                    )
                                    .colorOSPressBounce(pressedScale = 0.88f)
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Fluid Progress Bar (if totalEp > 0)
                    if (show.totalEp > 0) {
                        val progressFraction = (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF0082FB), Color(0xFF00D2FF))
                                            )
                                        )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(progressFraction * 100).toInt()}% completed",
                                    fontSize = 11.5.sp,
                                    fontFamily = JustSansFamily,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                val remaining = (show.totalEp - show.currentEp).coerceAtLeast(0)
                                Text(
                                    text = "$remaining episodes left",
                                    fontSize = 11.5.sp,
                                    fontFamily = JustSansFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Direct 1-Tap Cloud Sync Pill (if AniList is linked and token exists)
                    if (show.aniListId != null) {
                        val token = remember { AniListService.getToken() }
                        if (!token.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF02A9FF).copy(alpha = 0.10f))
                                    .border(0.6.dp, Color(0xFF02A9FF).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isQuickSyncing) {
                                        isQuickSyncing = true
                                        Haptics.tick()
                                        coroutineScope.launch {
                                            val res = AniListService.updateProgress(
                                                token = token,
                                                mediaId = show.aniListId,
                                                progress = show.currentEp,
                                                totalEpisodes = show.totalEp
                                            )
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Synced EP ${show.currentEp} to AniList ☁️", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Sync failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            isQuickSyncing = false
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isQuickSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF02A9FF)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudSync,
                                        contentDescription = null,
                                        tint = Color(0xFF02A9FF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isQuickSyncing) "Syncing with AniList..." else "Tap to sync Episode ${show.currentEp} to AniList",
                                    fontSize = 11.5.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF02A9FF)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Live Airing Countdown Bento Card (for ongoing shows)
            if (show.status == "ongoing") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.card))
                        .background(cardBgColor)
                        .border(
                            width = 0.75.dp,
                            brush = glassBorder,
                            shape = RoundedCornerShape(CornerRadius.card)
                        )
                        .padding(Spacing.lg)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFF0082FB),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "NEXT AIRING EPISODE",
                                        fontSize = 11.sp,
                                        fontFamily = JustSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0082FB),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (countdownTime.airingNow) "New Episode Releasing Now!"
                                    else "Episode ${show.currentEp + 1} • Every ${show.releaseDay ?: "TBA"} ${show.releaseTime ?: ""}".trim(),
                                    fontSize = 13.5.sp,
                                    fontFamily = JustSansFamily,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (countdownTime.airingNow) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.20f))
                                        .border(0.6.dp, Color(0xFF10B981).copy(alpha = 0.45f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                                    )
                                    Text(
                                        text = "LIVE NOW",
                                        fontSize = 10.sp,
                                        fontFamily = JustSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }

                        if (!countdownTime.airingNow && !countdownTime.elapsed) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DetailCountdownBox(value = countdownTime.days, label = "days", isLight = isLight)
                                DetailCountdownBox(value = countdownTime.hours, label = "hours", isLight = isLight)
                                DetailCountdownBox(value = countdownTime.minutes, label = "mins", isLight = isLight)
                                DetailCountdownBox(value = countdownTime.seconds, label = "secs", isLight = isLight)
                            }
                        }
                    }
                }
            }

            // 5. Specifications Bento Grid (2 columns x 3 rows)
            val formattedLanguage = when (show.language?.trim()?.lowercase()) {
                "cn", "zh", "chinese", "mandarin" -> "🇨🇳 Chinese"
                "en", "english" -> "🇬🇧 English"
                "jp", "ja", "japanese" -> "🇯🇵 Japanese"
                null, "" -> "🇨🇳 Chinese"
                else -> show.language
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "SPECIFICATIONS & DETAILS",
                    fontSize = 11.sp,
                    fontFamily = JustSansFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0082FB),
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailSpecCard(
                        icon = Icons.Rounded.Language,
                        label = "AUDIO",
                        value = formattedLanguage,
                        isLight = isLight,
                        modifier = Modifier.weight(1f)
                    )
                    DetailSpecCard(
                        icon = Icons.Rounded.VideoLibrary,
                        label = "EPISODES",
                        value = if (show.totalEp > 0) "${show.totalEp} Episodes" else "Ongoing ONA",
                        isLight = isLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val scheduleVal = "${show.releaseDay ?: "TBA"} ${show.releaseTime ?: ""}".trim().ifEmpty { "TBA" }
                    DetailSpecCard(
                        icon = Icons.Rounded.Schedule,
                        label = "SCHEDULE",
                        value = scheduleVal,
                        isLight = isLight,
                        modifier = Modifier.weight(1f)
                    )
                    DetailSpecCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "YEAR",
                        value = if (show.releaseYear > 0) "${show.releaseYear}" else "TBA",
                        isLight = isLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val studioVal = show.studio?.takeIf { it.isNotBlank() } ?: "Original Animation"
                    DetailSpecCard(
                        icon = Icons.Rounded.Movie,
                        label = "STUDIO",
                        value = studioVal,
                        isLight = isLight,
                        modifier = Modifier.weight(1f)
                    )

                    val ratingVal = if (aniListDetail?.averageScore != null) {
                        "${aniListDetail?.averageScore}% (AniList)"
                    } else if (show.rating > 0) {
                        "${show.rating}/10 ★"
                    } else {
                        "Unrated"
                    }
                    DetailSpecCard(
                        icon = Icons.Rounded.Star,
                        label = "SCORE",
                        value = ratingVal,
                        isLight = isLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 6. Genres Tag Cloud
            val genresList = remember(show.genres) {
                show.genres?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            }
            if (genresList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "GENRES",
                        fontSize = 11.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0082FB),
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genresList.forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(chipBgColor)
                                    .border(
                                        width = 0.75.dp,
                                        brush = rememberLiquidGlassBorder(isLight),
                                        shape = RoundedCornerShape(percent = 50)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = genre,
                                    fontSize = 12.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 7. Synopsis & Storyline Card
            val synopsisContent = remember(show.notes, aniListDetail?.description) {
                show.notes?.takeIf { it.isNotBlank() }
                    ?: aniListDetail?.description?.replace(Regex("<[^>]*>"), "")?.trim()
            }
            if (!synopsisContent.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.card))
                        .background(cardBgColor)
                        .border(
                            width = 0.75.dp,
                            brush = glassBorder,
                            shape = RoundedCornerShape(CornerRadius.card)
                        )
                        .padding(Spacing.lg)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.animateContentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                tint = Color(0xFF0082FB),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "SYNOPSIS & STORYLINE",
                                fontSize = 11.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0082FB),
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = synopsisContent,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                            lineHeight = 21.sp,
                            fontFamily = JustSansFamily,
                            maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (synopsisContent.length > 150) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        Haptics.tick()
                                        isSynopsisExpanded = !isSynopsisExpanded
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isSynopsisExpanded) "Show Less" else "Read More",
                                    color = Color(0xFF0082FB),
                                    fontSize = 12.5.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isSynopsisExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF0082FB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Floating Glass Header
        GlassHeader(
            title = show.title,
            subtitle = detailSubtitle,
            backdrop = screenBackdrop,
            isDark = !isLight,
            onBackClick = onNavigateBack,
            onEditClick = { onEditClick(show) }
        )

        // AniList Options Dialog
        if (showAniListMenu) {
            ColorOSDialog(
                onDismissRequest = { showAniListMenu = false },
                title = "AniList Options",
                confirmText = "Done",
                dismissText = null,
                onConfirm = { showAniListMenu = false },
                customContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Linked to AniList #${show.aniListId}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = JustSansFamily,
                            color = Color(0xFF02A9FF)
                        )

                        ColorOSPillButton(
                            text = "Sync Current Progress (EP ${show.currentEp})",
                            onClick = {
                                val token = AniListService.getToken()
                                val targetId = show.aniListId
                                if (token.isNullOrBlank()) {
                                    Toast.makeText(context, "AniList not connected. Log in from Settings.", Toast.LENGTH_SHORT).show()
                                } else if (targetId != null) {
                                    coroutineScope.launch {
                                        val res = AniListService.updateProgress(token, targetId, show.currentEp, show.totalEp)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Synced Episode ${show.currentEp} to AniList!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Sync failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        showAniListMenu = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 44.dp,
                            containerColor = Color(0xFF02A9FF)
                        )

                        val userScore = aniListDetail?.userScore
                        val scoreLabel = if (userScore != null && userScore > 0.0) {
                            "Your Score: $userScore ★ (Change)"
                        } else {
                            "Rate Show on AniList"
                        }

                        ColorOSOutlinedButton(
                            text = scoreLabel,
                            onClick = {
                                userScoreInput = userScore?.toInt()?.toString() ?: ""
                                showScoreRateDialog = true
                                showAniListMenu = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 44.dp
                        )

                        ColorOSOutlinedButton(
                            text = "Open on AniList ↗",
                            onClick = {
                                val url = "https://anilist.co/anime/${show.aniListId}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                                showAniListMenu = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 44.dp
                        )

                        ColorOSOutlinedButton(
                            text = "Change AniList Mapping",
                            onClick = {
                                showAniListMenu = false
                                linkSearchQuery = show.title
                                isSearchingAniList = true
                                coroutineScope.launch {
                                    aniListSearchResults = AniListService.searchAnime(show.title, limit = 8)
                                    isSearchingAniList = false
                                }
                                showLinkAniListDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 44.dp
                        )

                        ColorOSOutlinedButton(
                            text = "Unlink from AniList",
                            onClick = {
                                viewModel.insertOrUpdateShow(show.copy(aniListId = null))
                                showAniListMenu = false
                                Toast.makeText(context, "Unlinked from AniList", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 44.dp,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }

        // AniList Score Submission Dialog
        if (showScoreRateDialog) {
            ColorOSDialog(
                onDismissRequest = { showScoreRateDialog = false },
                title = "Rate on AniList",
                confirmText = "Submit Score",
                dismissText = "Cancel",
                confirmButtonColor = Color(0xFF02A9FF),
                onConfirm = {
                    val score = userScoreInput.toDoubleOrNull()
                    val token = AniListService.getToken()
                    val targetId = show.aniListId
                    if (token.isNullOrBlank()) {
                        Toast.makeText(context, "AniList not connected", Toast.LENGTH_SHORT).show()
                    } else if (score != null && targetId != null) {
                        coroutineScope.launch {
                            val res = AniListService.updateMediaScoreAndStatus(token, targetId, score, null)
                            if (res.isSuccess) {
                                viewModel.insertOrUpdateShow(show.copy(rating = score.toInt()))
                                Toast.makeText(context, "Score submitted to AniList: $score ★", Toast.LENGTH_SHORT).show()
                                val ref = AniListService.fetchMediaDetails(targetId)
                                if (ref.isSuccess) aniListDetail = ref.getOrNull()
                            } else {
                                Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                            showScoreRateDialog = false
                        }
                    }
                },
                customContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter your score for ${show.title} (1 to 10 or 100):",
                            fontSize = 13.sp,
                            fontFamily = JustSansFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = userScoreInput,
                            onValueChange = { userScoreInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Score") },
                            placeholder = { Text("e.g. 9 or 85") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            )
        }

        // AniList Search and Link Dialog
        if (showLinkAniListDialog) {
            ColorOSDialog(
                onDismissRequest = { showLinkAniListDialog = false },
                title = "Link AniList Media",
                confirmText = "Close",
                dismissText = null,
                onConfirm = { showLinkAniListDialog = false },
                customContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = linkSearchQuery,
                                onValueChange = { linkSearchQuery = it },
                                placeholder = { Text("Search show name...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ColorOSPillButton(
                                text = "Search",
                                onClick = {
                                    if (linkSearchQuery.isNotBlank()) {
                                        isSearchingAniList = true
                                        coroutineScope.launch {
                                            aniListSearchResults = AniListService.searchAnime(linkSearchQuery, limit = 8)
                                            isSearchingAniList = false
                                        }
                                    }
                                },
                                height = 50.dp,
                                containerColor = Color(0xFF02A9FF)
                            )
                        }

                        if (isSearchingAniList) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color(0xFF02A9FF))
                            }
                        } else if (aniListSearchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No results found on AniList",
                                    fontSize = 13.sp,
                                    fontFamily = JustSansFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                aniListSearchResults.forEach { media ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                            .clickable {
                                                val updated = show.copy(aniListId = media.id)
                                                viewModel.insertOrUpdateShow(updated)
                                                showLinkAniListDialog = false
                                                Toast.makeText(context, "Linked to ${media.preferredTitle} (#${media.id})!", Toast.LENGTH_SHORT).show()

                                                if (show.currentEp > 0) {
                                                    coroutineScope.launch {
                                                        AniListService.syncEpisodeProgress(updated, show.currentEp)
                                                    }
                                                }
                                            }
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DonghuaPoster(
                                            posterUrl = media.coverImage,
                                            title = media.preferredTitle,
                                            modifier = Modifier
                                                .size(width = 38.dp, height = 52.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = media.preferredTitle,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = JustSansFamily,
                                                fontSize = 13.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "AniList #${media.id} • ${if (media.episodes > 0) "${media.episodes} eps" else "Episodes TBA"}",
                                                fontSize = 11.sp,
                                                fontFamily = JustSansFamily,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailCountdownBox(value: Long, label: String, isLight: Boolean = false) {
    val shape = RoundedCornerShape(18.dp)
    val boxBg = if (isLight) Color(0xFFE8EEF5).copy(alpha = 0.90f) else Color(0xFF1E2638).copy(alpha = 0.45f)
    val numColor = if (isLight) Color.Black else Color.White
    Box(
        modifier = Modifier
            .width(62.dp)
            .height(56.dp)
            .clip(shape)
            .background(boxBg)
            .border(
                width = 0.75.dp,
                brush = rememberLiquidGlassBorder(isLight),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 16.5.sp,
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                color = numColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = label.uppercase(),
                fontSize = 8.5.sp,
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0082FB),
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun DetailSpecCard(
    icon: ImageVector,
    label: String,
    value: String,
    isLight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val cardBg = if (isLight) Color.White.copy(alpha = 0.85f) else Color(0xFF1E2638).copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .clip(shape)
            .background(cardBg)
            .border(
                width = 0.75.dp,
                brush = rememberLiquidGlassBorder(isLight),
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0082FB).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF0082FB),
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    fontSize = 8.5.sp,
                    fontFamily = JustSansFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0082FB),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = value,
                    fontSize = 12.5.sp,
                    fontFamily = JustSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
