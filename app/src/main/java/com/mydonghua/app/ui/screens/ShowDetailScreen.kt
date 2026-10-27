package com.mydonghua.app.ui.screens

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
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.mydonghua.app.R
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
    val allShows by viewModel.allShows.collectAsState()
    val show = remember(showId, allShows) { allShows.find { it.id == showId } }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

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
        val headerTopPadding = if (topPadding > 0.dp) topPadding + Spacing.sm else statusBarTop + 84.dp

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
                            fontSize = 11.sp
                        )
                    }

                    // Title & Badges
                    Column(modifier = Modifier.weight(1f)) {
                        val statusColor = when (show.status) {
                            "ongoing" -> Color(0xFF0082FB)
                            "completed" -> Color(0xFF4ADE80)
                            else -> Color(0xFFFFA726)
                        }

                        // Status Chip
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
                            Text(
                                text = "WATCH PROGRESS",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0082FB),
                                letterSpacing = 0.5.sp
                            )
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
    }
}

@Composable
private fun DetailCountdownBox(value: Long, label: String, isLight: Boolean = false) {
    val shape = RoundedCornerShape(16.dp)
    val boxBg = if (isLight) Color(0xFFE8EEF5).copy(alpha = 0.90f) else Color(0xFF1E2638).copy(alpha = 0.45f)
    val numColor = if (isLight) Color(0xFF141824) else Color.White
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
