package com.mydonghua.app.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.R
import com.mydonghua.app.domain.model.CountdownCalculator
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.DonghuaPoster
import com.mydonghua.app.ui.common.ScrollPerfMonitor
import com.mydonghua.app.ui.common.shimmerEffect
import com.mydonghua.app.ui.rememberLiquidGlassBorder
import com.mydonghua.app.ui.rememberLiquidGlassChipBorder
import com.mydonghua.app.ui.screens.HomeViewModel
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.JustSansFamily
import com.mydonghua.app.ui.theme.colorOSPressBounce
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShowClick: (String) -> Unit,
    onWatchClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    defaultStreamProvider: String = "donghuastream",
) {
    val state by viewModel.uiState.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val effectiveTopPadding = if (topPadding.value > 0f) topPadding + Spacing.md else statusBarTop + 84.dp

    val listState = rememberLazyListState()
    ScrollPerfMonitor(listState, "home")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .navigationBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(top = effectiveTopPadding, bottom = 96.dp)
    ) {
        if (state.isLoading) {
            item { HeroBannerSkeleton() }
            item { SectionHeaderSkeleton() }
            item { HorizontalRowSkeleton() }
            item { SectionHeaderSkeleton() }
            item { RecentlyUpdatedRowSkeleton() }
        } else {
            if (state.showCountdownBanner && state.nextUpShow != null) {
                item(key = "hero_banner_${state.nextUpShow!!.id}") {
                    HeroBanner(
                        show = state.nextUpShow!!,
                        onWatchClick = {
                            val url = com.mydonghua.app.data.service.PosterFetchService.resolveWatchUrl(
                                showTitle = state.nextUpShow!!.title,
                                customWatchUrl = state.nextUpShow!!.watchUrl,
                                providerPreference = defaultStreamProvider
                            )
                            onWatchClick(url)
                        },
                        onBannerClick = { onShowClick(state.nextUpShow!!.id) }
                    )
                }
                item(key = "hero_banner_spacer") { Spacer(modifier = Modifier.height(Spacing.sm)) }
            }

            if (state.showContinueWatching && state.continueWatching.isNotEmpty()) {
                item(key = "section_continue_header") { SectionHeader(title = "Continue Watching") }
                item(key = "section_continue_row") { ContinueWatchingRow(shows = state.continueWatching, onShowClick = onShowClick) }
            }

            if (state.showRecentlyUpdated && state.recentlyUpdated.isNotEmpty()) {
                item(key = "section_recent_header") { SectionHeader(title = "Recently Updated") }
                item(key = "section_recent_row") {
                    RecentlyUpdatedRow(
                        shows = state.recentlyUpdated,
                        onShowClick = onShowClick,
                        onWatchClick = { show ->
                            val url = com.mydonghua.app.data.service.PosterFetchService.resolveWatchUrl(
                                showTitle = show.title,
                                customWatchUrl = show.watchUrl,
                                providerPreference = defaultStreamProvider
                            )
                            onWatchClick(url)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HeroBanner(
    show: Show,
    onWatchClick: () -> Unit,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    var remainingTime by remember(show.id) {
        mutableStateOf(
            CountdownCalculator.calculateTimeRemaining(
                CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime)
            )
        )
    }

    LaunchedEffect(show.id, show.releaseDay, show.releaseTime) {
        while (true) {
            remainingTime = CountdownCalculator.calculateTimeRemaining(
                CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime)
            )
            delay(1000)
        }
    }

    val bannerShape = RoundedCornerShape(28.dp)
    val bannerBorderBrush = rememberLiquidGlassBorder(isLight)
    val primaryAccent = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB)
    val hasSchedule = !show.releaseDay.isNullOrEmpty() && !show.releaseTime.isNullOrEmpty()
    val posterShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
            .clip(bannerShape)
            .hazeChild(state = hazeState, shape = bannerShape)
            .background(
                if (isLight) Color.White.copy(alpha = 0.70f)
                else Color(0xFF161A28).copy(alpha = 0.52f)
            )
            .border(
                width = 0.85.dp,
                brush = bannerBorderBrush,
                shape = bannerShape
            )
            .colorOSPressBounce(pressedScale = 0.985f, onClick = { onBannerClick() })
            .padding(14.dp)
    ) {
        // Ambient Aquamorphic Aurora Glow behind the poster
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-20).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(primaryAccent.copy(alpha = if (isLight) 0.15f else 0.22f), Color.Transparent),
                        radius = 260f
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: PICTURE (Donghua Poster Tile)
            Box(
                modifier = Modifier
                    .width(122.dp)
                    .height(180.dp)
                    .clip(posterShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(
                        width = 0.75.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))
                        ),
                        shape = posterShape
                    )
            ) {
                DonghuaPoster(
                    posterUrl = show.poster,
                    title = show.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    showAcronym = false,
                    immediate = true
                )

                // Atmospheric bottom vignette on poster
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.72f)
                            )
                        )
                )

                // Top-Left Episode Capsule Pill on Poster
                val epText = if (show.totalEp > 0) "EP ${show.currentEp + 1}/${show.totalEp}" else "EP ${show.currentEp + 1}"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.60f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = epText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JustSansFamily,
                        color = Color.White
                    )
                }

                // Bottom-Right Glowing Quick-Play Button on Poster
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(primaryAccent)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .colorOSPressBounce(pressedScale = 0.88f, onClick = onWatchClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Quick Watch",
                        modifier = Modifier.size(19.dp),
                        tint = Color.White
                    )
                }
            }

            // RIGHT SIDE: DETAILS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section: Status pill & meta pill row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Status Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(
                                    if (remainingTime.airingNow) Color(0xFF00E676).copy(alpha = 0.16f)
                                    else primaryAccent.copy(alpha = 0.14f)
                                )
                                .border(
                                    width = 0.75.dp,
                                    color = if (remainingTime.airingNow) Color(0xFF00E676).copy(alpha = 0.40f)
                                    else primaryAccent.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(percent = 50)
                                )
                                .padding(horizontal = 9.dp, vertical = 3.5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (remainingTime.airingNow) Color(0xFF00E676) else primaryAccent)
                                )
                                Text(
                                    text = when {
                                        remainingTime.airingNow -> "AIRING NOW"
                                        hasSchedule -> "${show.releaseDay?.take(3)?.uppercase()} • ${show.releaseTime ?: ""}"
                                        else -> "NEXT UP"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JustSansFamily,
                                    color = if (remainingTime.airingNow) Color(0xFF00E676) else primaryAccent,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Right Meta Pill (Year or Status)
                        val rightTag = when {
                            show.releaseYear > 0 -> "${show.releaseYear}"
                            !show.status.isNullOrBlank() -> show.status.replaceFirstChar { it.uppercase() }
                            else -> "HD"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color.White.copy(alpha = if (isLight) 0.12f else 0.08f))
                                .border(0.6.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(percent = 50))
                                .padding(horizontal = 7.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = rightTag,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = JustSansFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Main Title
                    Text(
                        text = show.title,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JustSansFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 21.sp
                    )

                    // Subtitle / Chinese Title & Mini Genre Pills
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!show.titleZh.isNullOrBlank()) {
                            Text(
                                text = show.titleZh,
                                fontSize = 11.5.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Medium,
                                color = primaryAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val genresList = show.genres?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.take(2) ?: emptyList()
                        if (genresList.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                genresList.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(percent = 50))
                                            .background(
                                                if (isLight) Color.Black.copy(alpha = 0.05f)
                                                else Color.White.copy(alpha = 0.07f)
                                            )
                                            .border(0.6.dp, rememberLiquidGlassChipBorder(isLight), RoundedCornerShape(percent = 50))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            fontSize = 9.5.sp,
                                            fontFamily = JustSansFamily,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom: Action or Countdown Deck
                if (remainingTime.airingNow) {
                    // Watch Ep Now Button (50% capsule in ColorOS 17 Electric Blue)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(primaryAccent)
                            .colorOSPressBounce(pressedScale = 0.95f, onClick = { onWatchClick() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Watch Ep ${show.currentEp + 1} Now",
                                fontSize = 12.5.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else if (hasSchedule && !remainingTime.elapsed) {
                    // Sleek 4-Block Micro-Tile Countdown Deck
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactTimerPill(value = remainingTime.days, label = "DAYS", modifier = Modifier.weight(1f))
                        CompactTimerPill(value = remainingTime.hours, label = "HRS", modifier = Modifier.weight(1f))
                        CompactTimerPill(value = remainingTime.minutes, label = "MIN", modifier = Modifier.weight(1f))
                        CompactTimerPill(value = remainingTime.seconds, label = "SEC", modifier = Modifier.weight(1f))
                    }
                } else {
                    // Frosted Details Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                if (isLight) Color.Black.copy(alpha = 0.06f)
                                else Color.White.copy(alpha = 0.10f)
                            )
                            .border(
                                width = 0.75.dp,
                                color = rememberLiquidGlassChipBorder(isLight),
                                shape = RoundedCornerShape(percent = 50)
                            )
                            .colorOSPressBounce(pressedScale = 0.95f, onClick = { onBannerClick() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "View Details & Episodes",
                                fontSize = 12.5.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = primaryAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTimerPill(
    value: Long,
    label: String,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val primaryAccent = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB)
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(
                if (isLight) Color.Black.copy(alpha = 0.05f)
                else Color(0xFF141926).copy(alpha = 0.65f)
            )
            .border(
                width = 0.75.dp,
                color = rememberLiquidGlassChipBorder(isLight),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%02d", value),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JustSansFamily,
                color = Color.White,
                lineHeight = 15.sp
            )
            Text(
                text = label,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JustSansFamily,
                color = primaryAccent,
                letterSpacing = 0.5.sp,
                lineHeight = 9.sp
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val accent = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xxl, bottom = Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 19.sp,
            fontFamily = JustSansFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ContinueWatchingRow(
    shows: List<Show>,
    onShowClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val configuration = LocalConfiguration.current
    val cardWidth = ((configuration.screenWidthDp.dp - Spacing.lg * 2 - Spacing.md) / 2).coerceIn(152.dp, 195.dp)

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items(shows, key = { it.id }) { show ->
            ContinueWatchingVerticalCard(
                show = show,
                cardWidth = cardWidth,
                isLight = isLight,
                hazeState = hazeState,
                onShowClick = { onShowClick(show.id) }
            )
        }
    }
}

@Composable
fun ContinueWatchingVerticalCard(
    show: Show,
    cardWidth: androidx.compose.ui.unit.Dp,
    isLight: Boolean,
    hazeState: dev.chrisbanes.haze.HazeState = LocalHazeState.current,
    onShowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(26.dp)
    val posterShape = RoundedCornerShape(18.dp)
    val primaryAccent = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB)

    val cardBg = if (isLight) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.70f),
                Color(0xFFF0F4FA).copy(alpha = 0.60f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1E2638).copy(alpha = 0.50f),
                Color(0xFF121622).copy(alpha = 0.60f)
            )
        )
    }

    val cardBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isLight) 0.35f else 0.20f),
            Color.White.copy(alpha = if (isLight) 0.10f else 0.04f)
        )
    )

    Box(
        modifier = modifier
            .width(cardWidth)
            .clip(cardShape)
            .hazeChild(state = hazeState, shape = cardShape)
            .background(cardBg)
            .border(
                width = 0.85.dp,
                brush = cardBorder,
                shape = cardShape
            )
            .colorOSPressBounce(
                pressedScale = 0.97f,
                onClick = onShowClick
            )
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top: Vertical Poster Tile with Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .clip(posterShape)
                    .border(
                        width = 0.75.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))
                        ),
                        shape = posterShape
                    )
            ) {
                DonghuaPoster(
                    posterUrl = show.poster,
                    title = show.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fontSize = 8.5.sp
                )

                // Bottom vignette overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.75f)
                            )
                        )
                )

                // Top-Right: Episode Capsule Pill
                val epText = if (show.totalEp > 0) "EP ${show.currentEp}/${show.totalEp}" else "EP ${show.currentEp}"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.60f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = epText,
                        fontSize = 9.5.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Bottom-Right: ColorOS 17 Electric Blue Quick-Play Circle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(primaryAccent)
                        .border(0.75.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Resume Watching",
                        modifier = Modifier.size(17.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Bottom: Details & Progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = show.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JustSansFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (show.totalEp > 0) {
                    val fraction = (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f)
                    val percent = (fraction * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Episode ${show.currentEp}",
                            fontSize = 11.5.sp,
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryAccent
                        )
                        Text(
                            text = "$percent%",
                            fontSize = 10.5.sp,
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLight) Color.Black.copy(alpha = 0.08f)
                                else Color.White.copy(alpha = 0.12f)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(primaryAccent)
                        )
                    }
                } else {
                    Text(
                        text = "Episode ${show.currentEp} • In Progress",
                        fontSize = 11.5.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryAccent
                    )
                }
            }
        }
    }
}

@Composable
fun RecentlyUpdatedRow(
    shows: List<Show>,
    onShowClick: (String) -> Unit,
    onWatchClick: ((Show) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val configuration = LocalConfiguration.current
    val cardWidth = ((configuration.screenWidthDp.dp - Spacing.lg * 2 - Spacing.md) / 2).coerceIn(152.dp, 195.dp)

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items(shows, key = { it.id }) { show ->
            RecentlyUpdatedVerticalCard(
                show = show,
                cardWidth = cardWidth,
                isLight = isLight,
                hazeState = hazeState,
                onCardClick = { onShowClick(show.id) },
                onWatchClick = if (onWatchClick != null) { { onWatchClick(show) } } else null
            )
        }
    }
}

@Composable
fun RecentlyUpdatedVerticalCard(
    show: Show,
    cardWidth: androidx.compose.ui.unit.Dp,
    isLight: Boolean,
    hazeState: dev.chrisbanes.haze.HazeState = LocalHazeState.current,
    onCardClick: () -> Unit,
    onWatchClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(26.dp)
    val posterShape = RoundedCornerShape(18.dp)
    val primaryAccent = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB)

    val cardBg = if (isLight) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.70f),
                Color(0xFFF0F4FA).copy(alpha = 0.60f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1E2638).copy(alpha = 0.50f),
                Color(0xFF121622).copy(alpha = 0.60f)
            )
        )
    }

    val borderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isLight) 0.35f else 0.20f),
            Color.White.copy(alpha = if (isLight) 0.10f else 0.04f)
        )
    )

    val relativeTime = remember(show.lastUpdated) {
        if (show.lastUpdated != null && show.lastUpdated > 0L) {
            DateUtils.getRelativeTimeSpanString(
                show.lastUpdated,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } else {
            "Recently"
        }
    }

    Box(
        modifier = modifier
            .width(cardWidth)
            .clip(cardShape)
            .hazeChild(state = hazeState, shape = cardShape)
            .background(cardBg)
            .border(width = 0.85.dp, brush = borderBrush, shape = cardShape)
            .colorOSPressBounce(
                pressedScale = 0.97f,
                onClick = onCardClick
            )
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top: Vertical Poster Tile with Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .clip(posterShape)
                    .border(
                        width = 0.75.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))
                        ),
                        shape = posterShape
                    )
            ) {
                DonghuaPoster(
                    posterUrl = show.poster,
                    title = show.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fontSize = 8.5.sp,
                    showAcronym = true
                )

                // Bottom vignette overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.75f)
                            )
                        )
                )

                // Top-Left: NEW Capsule Pill Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(primaryAccent)
                        .padding(horizontal = 6.5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEW",
                        fontSize = 8.5.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                // Top-Right: Status / Day Pill Badge
                val statusLabel = when (show.status) {
                    "completed" -> "END"
                    else -> show.releaseDay?.take(3)?.uppercase() ?: "UP"
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.60f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 6.5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 8.5.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.90f),
                        letterSpacing = 0.4.sp
                    )
                }

                // Bottom-Right: Quick Play Circle Button (ColorOS 17 Blue)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(primaryAccent)
                        .border(0.75.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .colorOSPressBounce(pressedScale = 0.88f)
                        .clickable(enabled = onWatchClick != null) {
                            onWatchClick?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Watch now",
                        modifier = Modifier.size(17.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Bottom: Title & Meta Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = show.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JustSansFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ep ${show.currentEp}",
                        fontSize = 11.5.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock),
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
                        )
                        Text(
                            text = relativeTime,
                            fontSize = 10.5.sp,
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBannerSkeleton() {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bannerBorderBrush = rememberLiquidGlassBorder(isLight)
    val bannerShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
            .clip(bannerShape)
            .background(
                if (isLight) Color.White.copy(alpha = 0.70f)
                else Color(0xFF161A28).copy(alpha = 0.52f)
            )
            .border(
                width = 0.85.dp,
                brush = bannerBorderBrush,
                shape = bannerShape
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Poster Skeleton
            Box(
                modifier = Modifier
                    .width(122.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .shimmerEffect()
            )

            // Right Details Skeleton
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(CornerRadius.xs))
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(CornerRadius.xs))
                            .shimmerEffect()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderSkeleton() {
    Box(
        modifier = Modifier
            .padding(start = Spacing.lg, top = Spacing.xxl, bottom = Spacing.md)
            .width(160.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(CornerRadius.xs))
            .shimmerEffect()
    )
}

@Composable
private fun HorizontalRowSkeleton() {
    val configuration = LocalConfiguration.current
    val cardWidth = ((configuration.screenWidthDp.dp - Spacing.lg * 2 - Spacing.md) / 2).coerceIn(152.dp, 195.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(232.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
private fun RecentlyUpdatedRowSkeleton() {
    val configuration = LocalConfiguration.current
    val cardWidth = ((configuration.screenWidthDp.dp - Spacing.lg * 2 - Spacing.md) / 2).coerceIn(152.dp, 195.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(232.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .shimmerEffect()
            )
        }
    }
}
