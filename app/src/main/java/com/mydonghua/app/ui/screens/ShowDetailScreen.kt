package com.mydonghua.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.mydonghua.app.R
import com.mydonghua.app.data.service.AniListMedia
import com.mydonghua.app.data.service.AniListService
import com.mydonghua.app.ui.common.ColorOSDialog
import com.mydonghua.app.ui.common.ColorOSOutlinedButton
import com.mydonghua.app.ui.common.ColorOSPillButton
import com.mydonghua.app.ui.common.EmptyState
import androidx.compose.ui.text.font.FontWeight
import com.mydonghua.app.ui.rememberLiquidGlassBorder
import com.mydonghua.app.ui.rememberLiquidGlassChipBorder
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.domain.model.CountdownCalculator
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.GlassHeader
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.DonghuaPoster
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.JustSansFamily
import com.mydonghua.app.ui.theme.colorOSPressBounce
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
    defaultStreamProvider: String = "donghuastream"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allShows by viewModel.allShows.collectAsState()
    val show = remember(showId, allShows) { allShows.find { it.id == showId } }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    var showAniListMenu by remember { mutableStateOf(false) }
    var showLinkAniListDialog by remember { mutableStateOf(false) }
    var showScoreRateDialog by remember { mutableStateOf(false) }
    var userScoreInput by remember { mutableStateOf("") }
    var linkSearchQuery by remember { mutableStateOf("") }
    var isSearchingAniList by remember { mutableStateOf(false) }
    var aniListSearchResults by remember { mutableStateOf<List<AniListMedia>>(emptyList()) }
    var aniListDetail by remember { mutableStateOf<com.mydonghua.app.data.service.AniListMediaDetail?>(null) }

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
                .padding(top = headerTopPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Backdrop Banner
            val bannerShape = RoundedCornerShape(CornerRadius.banner)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(bannerShape)
                    .background(Color(0xFF0F121C))
                    .border(
                        width = 0.75.dp,
                        brush = glassBorder,
                        shape = bannerShape
                    )
            ) {
                // Background Poster Art with Blur Gradient
                DonghuaPoster(
                    posterUrl = show.poster,
                    title = show.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    showAcronym = false,
                    crossfade = true
                )

                // Dark Liquid Glass Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Content inside Hero (Poster Thumbnail + Title details)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Floating Poster Thumbnail Card
                    val thumbShape = RoundedCornerShape(CornerRadius.md)
                    Box(
                        modifier = Modifier
                            .size(width = 86.dp, height = 120.dp)
                            .clip(thumbShape)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.10f))
                                ),
                                shape = thumbShape
                            )
                    ) {
                        DonghuaPoster(
                            posterUrl = show.poster,
                            title = show.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            fontSize = 11.sp,
                            sharedElementKey = "show-poster-${show.id}"
                        )
                    }

                    // Title & Badges
                    Column(modifier = Modifier.weight(1f)) {
                        val statusColor = when (show.status) {
                            "ongoing" -> Color(0xFF0082FB)
                            "completed" -> Color(0xFF4ADE80)
                            else -> Color(0xFFFFA726)
                        }

                        // Status & AniList Score Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.20f))
                                    .border(0.6.dp, statusColor.copy(alpha = 0.40f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Text(
                                    text = when (show.status) {
                                        "ongoing" -> "Airing"
                                        "completed" -> "Completed"
                                        else -> "Hiatus"
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            if (aniListDetail?.averageScore != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF02A9FF).copy(alpha = 0.22f))
                                        .border(0.6.dp, Color(0xFF02A9FF).copy(alpha = 0.50f), CircleShape)
                                        .clickable { showAniListMenu = true }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "★ ${aniListDetail?.averageScore}% AL",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF02A9FF)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = show.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!show.titleZh.isNullOrEmpty()) {
                            Text(
                                text = show.titleZh,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions Bar (Watch Online Hero Button + Favorite + Notify + Edit)
            val providerLabel = when (defaultStreamProvider.lowercase()) {
                "luciferdonghua" -> "LuciferDonghua ⚡"
                "donghuastream" -> "DonghuaStream 🎬"
                else -> "Direct Link 🔗"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Main "Watch Online" Hero Button (ColorOS 17 50% Capsule Action Pill)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xFF0082FB))
                        .colorOSPressBounce(
                            pressedScale = 0.96f,
                            onClick = {
                                val targetUrl = com.mydonghua.app.data.service.PosterFetchService.resolveWatchUrl(
                                    showTitle = show.title,
                                    customWatchUrl = show.watchUrl,
                                    providerPreference = defaultStreamProvider
                                )
                                onWatchClick(targetUrl)
                            }
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "WATCH NOW",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = providerLabel,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
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
                    onClick = { viewModel.toggleFavorite(show) },
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
                        viewModel.toggleNotification(context, show)
                        Toast.makeText(
                            context,
                            if (hasNotification) "Notifications turned off" else "Notifications turned on for ${show.title}",
                            Toast.LENGTH_SHORT
                        ).show()
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
            }

            // Episode Progress Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CornerRadius.card))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f))
                    .border(
                        width = 0.65.dp,
                        brush = glassBorder,
                        shape = RoundedCornerShape(CornerRadius.card)
                    )
                    .padding(Spacing.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                Text(
                                    text = "WATCH PROGRESS",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0082FB),
                                    letterSpacing = 0.5.sp
                                )

                                if (show.aniListId != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF02A9FF).copy(alpha = 0.15f))
                                            .clickable { showAniListMenu = true }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "AniList #${show.aniListId}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF02A9FF)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            .clickable {
                                                linkSearchQuery = show.title
                                                isSearchingAniList = true
                                                coroutineScope.launch {
                                                    aniListSearchResults = AniListService.searchAnime(show.title, limit = 8)
                                                    isSearchingAniList = false
                                                }
                                                showLinkAniListDialog = true
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "+ Link AniList",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            val totalStr = if (show.totalEp > 0) " / ${show.totalEp}" else ""
                            Text(
                                text = "Episode ${show.currentEp}$totalStr",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Tactile Stepper Buttons (+ / -) in ColorOS 17 style
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { viewModel.decrementEpisode(show) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .border(
                                        width = 0.75.dp,
                                        brush = rememberLiquidGlassBorder(),
                                        shape = CircleShape
                                    )
                                    .colorOSPressBounce(pressedScale = 0.88f)
                            ) {
                                Text(
                                    text = "-",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { viewModel.incrementEpisode(show) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0082FB))
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

                    // Fluid Progress Bar
                    if (show.totalEp > 0) {
                        val progressFraction = (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(Color(0xFF0082FB))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(progressFraction * 100).toInt()}% completed",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            val remaining = (show.totalEp - show.currentEp).coerceAtLeast(0)
                            Text(
                                text = "$remaining episodes left",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Airing Countdown Widget (if status == ongoing)
            if (show.status == "ongoing") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.card))
                        .background(cardBgColor)
                        .border(
                            width = 0.85.dp,
                            brush = glassBorder,
                            shape = RoundedCornerShape(CornerRadius.card)
                        )
                        .padding(Spacing.md)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NEXT AIRING EPISODE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0082FB),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (countdownTime.airingNow) "New Episode Releasing Now!"
                                    else "Episode ${show.currentEp + 1} • Every ${show.releaseDay} ${show.releaseTime ?: ""}",
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (!countdownTime.airingNow && !countdownTime.elapsed) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

            // Quick Info Chips Row
            val formattedLanguage = when (show.language?.trim()?.lowercase()) {
                "cn", "zh", "chinese", "mandarin" -> "🇨🇳 Chinese"
                "en", "english" -> "🇬🇧 English"
                "jp", "ja", "japanese" -> "🇯🇵 Japanese"
                null, "" -> "🇨🇳 Chinese"
                else -> show.language!!
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailGlassChip(label = "Language", value = formattedLanguage, isLight = isLight, modifier = Modifier.weight(1.2f))
                if (show.totalEp > 0) {
                    DetailGlassChip(label = "Episodes", value = "${show.totalEp}", isLight = isLight, modifier = Modifier.weight(0.9f))
                }
                if (show.releaseYear > 0) {
                    DetailGlassChip(label = "Year", value = "${show.releaseYear}", isLight = isLight, modifier = Modifier.weight(0.9f))
                }
            }

            // Genres Chips
            if (!show.genres.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "GENRES",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0082FB),
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        show.genres?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(chipBgColor)
                                    .border(
                                        width = 0.75.dp,
                                        brush = rememberLiquidGlassBorder(isLight),
                                        shape = RoundedCornerShape(percent = 50)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = genre,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Synopsis Section
            if (!show.notes.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.card))
                        .background(cardBgColor)
                        .border(
                            width = 0.85.dp,
                            brush = glassBorder,
                            shape = RoundedCornerShape(CornerRadius.card)
                        )
                        .padding(Spacing.md)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SYNOPSIS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0082FB),
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = show.notes!!,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                            lineHeight = 20.sp,
                            fontFamily = JustSansFamily,
                            maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (show.notes!!.length > 150) {
                            Text(
                                text = if (isSynopsisExpanded) "Show Less" else "Read More",
                                color = Color(0xFF0082FB),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        GlassHeader(
            title = show.title,
            subtitle = detailSubtitle,
            backdrop = screenBackdrop,
            isDark = !isLight,
            onBackClick = onNavigateBack,
            onEditClick = { onEditClick(show) }
        )

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
                            color = Color(0xFF02A9FF)
                        )

                        ColorOSPillButton(
                            text = "Sync Current Progress (EP ${show.currentEp})",
                            onClick = {
                                val token = AniListService.getToken()
                                if (token.isNullOrBlank()) {
                                    Toast.makeText(context, "AniList not connected. Log in from Settings.", Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch {
                                        val res = AniListService.updateProgress(token, show.aniListId!!, show.currentEp, show.totalEp)
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
                            height = 42.dp,
                            containerColor = Color(0xFF02A9FF)
                        )

                        ColorOSOutlinedButton(
                            text = if (aniListDetail?.userScore != null && aniListDetail!!.userScore!! > 0.0) "Your Score: ${aniListDetail!!.userScore} ★ (Change)" else "Rate Show on AniList",
                            onClick = {
                                userScoreInput = aniListDetail?.userScore?.toInt()?.toString() ?: ""
                                showScoreRateDialog = true
                                showAniListMenu = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 42.dp
                        )

                        ColorOSOutlinedButton(
                            text = "Open on AniList",
                            onClick = {
                                val url = "https://anilist.co/anime/${show.aniListId}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                                showAniListMenu = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 42.dp
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
                            height = 42.dp
                        )

                        ColorOSOutlinedButton(
                            text = "Unlink from AniList",
                            onClick = {
                                viewModel.insertOrUpdateShow(show.copy(aniListId = null))
                                showAniListMenu = false
                                Toast.makeText(context, "Unlinked from AniList", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 42.dp,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }

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
                    if (token.isNullOrBlank()) {
                        Toast.makeText(context, "AniList not connected", Toast.LENGTH_SHORT).show()
                    } else if (score != null && show.aniListId != null) {
                        coroutineScope.launch {
                            val res = AniListService.updateMediaScoreAndStatus(token, show.aniListId!!, score, null)
                            if (res.isSuccess) {
                                viewModel.insertOrUpdateShow(show.copy(rating = score.toInt()))
                                Toast.makeText(context, "Score submitted to AniList: $score ★", Toast.LENGTH_SHORT).show()
                                val ref = AniListService.fetchMediaDetails(show.aniListId!!)
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
                                                fontSize = 13.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "AniList #${media.id} • ${if (media.episodes > 0) "${media.episodes} eps" else "Episodes TBA"}",
                                                fontSize = 11.sp,
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
    val shape = RoundedCornerShape(16.dp)
    val boxBg = if (isLight) Color(0xFFE8EEF5).copy(alpha = 0.90f) else Color(0xFF1E2638).copy(alpha = 0.45f)
    val numColor = if (isLight) Color.Black else Color.White
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(50.dp)
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = numColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = label.uppercase(),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0082FB),
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun DetailGlassChip(label: String, value: String, isLight: Boolean = false, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val chipBg = if (isLight) Color.White.copy(alpha = 0.85f) else Color(0xFF1E2638).copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .clip(shape)
            .background(chipBg)
            .border(
                width = 0.75.dp,
                brush = rememberLiquidGlassBorder(isLight),
                shape = shape
            )
            .padding(horizontal = Spacing.md, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0082FB),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
