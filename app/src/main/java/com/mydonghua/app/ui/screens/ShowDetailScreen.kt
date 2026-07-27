package com.mydonghua.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.R
import com.mydonghua.app.domain.model.CountdownCalculator
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.GlassSection
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.DonghuaPoster
import com.mydonghua.app.ui.theme.ButtonHeight
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.JustSansFamily
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun ShowDetailScreen(
    showId: String,
    viewModel: ShowsViewModel,
    onWatchClick: (String) -> Unit,
    onEditClick: (Show) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val allShows by viewModel.allShows.collectAsState()
    val show = remember(showId, allShows) { allShows.find { it.id == showId } }

    if (show == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var countdownTime by remember(show.id) {
        mutableStateOf(CountdownCalculator.calculateTimeRemaining(CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime)))
    }

    LaunchedEffect(show.releaseDay, show.releaseTime) {
        val targetMs = CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime)
        while (true) {
            countdownTime = CountdownCalculator.calculateTimeRemaining(targetMs)
            delay(1000)
        }
    }

    val hazeState = LocalHazeState.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(if (topPadding > 0.dp) topPadding + 12.dp else 88.dp))
            // Hero Backdrop & Title Block
            val detailBannerShape = RoundedCornerShape(24.dp)
            Card(
                shape = detailBannerShape,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                ),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = 4.dp)
                    .height(220.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(detailBannerShape)
                ) {
                    // Background Poster Art
                    DonghuaPoster(
                        posterUrl = show.poster,
                        title = show.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(detailBannerShape),
                        contentScale = ContentScale.Crop,
                        showAcronym = false
                    )

                    // Liquid Gradient Overlays for smooth blend & text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.5f),
                                    0.4f to Color.Transparent,
                                    0.75f to MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                    1.0f to MaterialTheme.colorScheme.background
                                )
                            )
                    )

                    // Content Overlay in Hero
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Floating Poster Thumbnail Card
                        val thumbnailShape = RoundedCornerShape(14.dp)
                        Card(
                            shape = thumbnailShape,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.35f)
                            ),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            modifier = Modifier.size(width = 84.dp, height = 116.dp)
                        ) {
                            DonghuaPoster(
                                posterUrl = show.poster,
                                title = show.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                fontSize = 11.sp
                            )
                        }

                    Spacer(modifier = Modifier.width(Spacing.md))

                    // Title & Metadata Stack
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Status Badge Pill
                        val statusText = when (show.status) {
                            "ongoing" -> "Airing"
                            "completed" -> "Completed"
                            else -> "Hiatus"
                        }
                        val statusBg = when (show.status) {
                            "ongoing" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            "completed" -> Color(0xFF4CAF50).copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val statusColor = when (show.status) {
                            "ongoing" -> MaterialTheme.colorScheme.primary
                            "completed" -> Color(0xFF81C784)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            shape = RoundedCornerShape(CornerRadius.xs),
                            color = statusBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        Text(
                            text = show.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!show.titleZh.isNullOrEmpty()) {
                            Text(
                                text = show.titleZh,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

            // Main Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                // Next Episode Countdown Section (If Airing)
                if (show.status == "ongoing") {
                    GlassSection(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "NEXT RELEASE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (countdownTime.airingNow) "New Episode Available Now!"
                                        else "Episode ${show.currentEp + 1} • ${show.releaseDay} ${show.releaseTime ?: ""}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (!countdownTime.airingNow && !countdownTime.elapsed) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DetailCountdownBox(value = countdownTime.days, label = "days")
                                    DetailCountdownBox(value = countdownTime.hours, label = "hours")
                                    DetailCountdownBox(value = countdownTime.minutes, label = "mins")
                                    DetailCountdownBox(value = countdownTime.seconds, label = "secs")
                                }
                            }
                        }
                    }
                }

                // Interactive Episode Progress Card
                GlassSection(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "WATCH PROGRESS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val totalStr = if (show.totalEp > 0) " / ${show.totalEp}" else ""
                                Text(
                                    text = "Episode ${show.currentEp}$totalStr",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Interactive Episode Increment / Decrement Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Surface(
                                    onClick = { viewModel.decrementEpisode(show) },
                                    modifier = Modifier.size(36.dp),
                                    shape = RoundedCornerShape(CornerRadius.sm),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Surface(
                                    onClick = { viewModel.incrementEpisode(show) },
                                    modifier = Modifier.size(36.dp),
                                    shape = RoundedCornerShape(CornerRadius.sm),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        if (show.totalEp > 0) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            LinearProgressIndicator(
                                progress = { (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(CornerRadius.xs)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                // Info Chips Grid (Language, Episodes, Studio, Year)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    DetailGlassChip(label = "Language", value = show.language ?: "CN", modifier = Modifier.weight(1f))
                    if (show.totalEp > 0) {
                        DetailGlassChip(label = "Episodes", value = "${show.totalEp}", modifier = Modifier.weight(1f))
                    }
                    if (show.releaseYear != null && show.releaseYear!! > 0) {
                        DetailGlassChip(label = "Year", value = "${show.releaseYear}", modifier = Modifier.weight(1f))
                    }
                }

                // Primary Watch Online Glass Button (Placed directly below Language/Info Chips)
                Button(
                    onClick = { onWatchClick(show.watchUrl ?: "") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(CornerRadius.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tv),
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.sm),
                            tint = Color.White
                        )
                        Text(
                            text = "Watch Online",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                // Genres Pill Chips
                if (!show.genres.isNullOrEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "GENRES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            show.genres?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { genre ->
                                Surface(
                                    shape = RoundedCornerShape(CornerRadius.xs),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    Text(
                                        text = genre,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Synopsis Card
                if (!show.notes.isNullOrEmpty()) {
                    GlassSection(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md)
                        ) {
                            Text(
                                text = "SYNOPSIS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = show.notes!!,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 21.sp,
                                fontFamily = JustSansFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xxl))
            }
        }
    }
}

@Composable
private fun DetailCountdownBox(value: Long, label: String) {
    val hazeState = LocalHazeState.current
    val shape = RoundedCornerShape(12.dp)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(42.dp)
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color.Black.copy(alpha = 0.38f),
                        Color.Black.copy(alpha = 0.22f)
                    ) else listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.35f)
                    )
                )
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.12f)
                    ) else listOf(
                        Color.Black.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.08f)
                    )
                ),
                shape = shape
            )
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DetailGlassChip(label: String, value: String, modifier: Modifier = Modifier) {
    val hazeState = LocalHazeState.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.md))
            .hazeChild(state = hazeState, shape = RoundedCornerShape(CornerRadius.md))
            .background(
                color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}