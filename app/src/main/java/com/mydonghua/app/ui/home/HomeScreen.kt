package com.mydonghua.app.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.mydonghua.app.ui.theme.colorOSAuroraBorder
import com.mydonghua.app.ui.theme.colorOSAuroraShimmer
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
    val effectiveTopPadding = if (topPadding.value > 0f) topPadding + Spacing.md else 88.dp

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: PICTURE (Donghua Poster)
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(184.dp)
                    .clip(posterShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(
                        width = 0.75.dp,
                        color = Color.White.copy(alpha = 0.18f),
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

                // Atmospheric bottom shadow overlay on poster
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.6f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.65f)
                            )
                        )
                )

                // Episode badge pill overlaid at bottom of poster
                val epText = if (show.totalEp > 0) "EP ${show.currentEp + 1}/${show.totalEp}" else "EP ${show.currentEp + 1}"
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = epText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JustSansFamily,
                        color = Color.White
                    )
                }
            }

            // RIGHT SIDE: DETAILS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(184.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Status / Airing Capsule Tag
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                        else -> "FEATURED"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JustSansFamily,
                                    color = if (remainingTime.airingNow) Color(0xFF00E676) else primaryAccent,
                                    letterSpacing = 0.5.sp
                                )
                            }
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

                    // Subtitle / Chinese Title / Genre
                    if (!show.titleZh.isNullOrBlank()) {
                        Text(
                            text = show.titleZh,
                            fontSize = 12.sp,
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Medium,
                            color = primaryAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (!show.genres.isNullOrBlank()) {
                        Text(
                            text = show.genres.split(",").take(2).joinToString(" • ") { it.trim() },
                            fontSize = 11.5.sp,
                            fontFamily = JustSansFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bottom: Action or Countdown Deck
                if (remainingTime.airingNow) {
                    // Watch Ep Now Button (50% capsule in ColorOS 17 Electric Blue)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
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
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Watch Ep ${show.currentEp + 1}",
                                fontSize = 13.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else if (hasSchedule && !remainingTime.elapsed) {
                    // Sleek Compact Liquid Countdown Deck
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isLight) Color.Black.copy(alpha = 0.05f)
                                else Color.Black.copy(alpha = 0.25f)
                            )
                            .border(
                                width = 0.75.dp,
                                color = rememberLiquidGlassChipBorder(isLight),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactTimerPill(value = remainingTime.days, unit = "d")
                        Text(":", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryAccent.copy(alpha = 0.6f))
                        CompactTimerPill(value = remainingTime.hours, unit = "h")
                        Text(":", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryAccent.copy(alpha = 0.6f))
                        CompactTimerPill(value = remainingTime.minutes, unit = "m")
                        Text(":", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryAccent.copy(alpha = 0.6f))
                        CompactTimerPill(value = remainingTime.seconds, unit = "s")
                    }
                } else {
                    // Frosted Details Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
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
                                text = "Show Details",
                                fontSize = 13.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
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
private fun CompactTimerPill(value: Long, unit: String) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val primaryAccent = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = String.format(Locale.getDefault(), "%02d", value),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JustSansFamily,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JustSansFamily,
            color = primaryAccent
        )
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
    val cardShape = RoundedCornerShape(26.dp)

    // ColorOS 17 Specular Highlight Gradient Border
    val cardBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isLight) 0.35f else 0.18f),
            Color.White.copy(alpha = if (isLight) 0.10f else 0.04f)
        )
    )

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
                Color(0xFF1E2638).copy(alpha = 0.48f),
                Color(0xFF121622).copy(alpha = 0.58f)
            )
        )
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items(shows, key = { it.id }) { show ->
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .clip(cardShape)
                    .hazeChild(state = hazeState, shape = cardShape)
                    .background(cardBg)
                    .border(
                        width = 0.85.dp,
                        brush = cardBorder,
                        shape = cardShape
                    )
                    .colorOSPressBounce(
                        pressedScale = 0.98f,
                        onClick = { onShowClick(show.id) }
                    )
                    .padding(Spacing.sm + 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 58.dp, height = 78.dp)
                            .clip(RoundedCornerShape(CornerRadius.md))
                    ) {
                        DonghuaPoster(
                            posterUrl = show.poster,
                            title = show.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            fontSize = 8.sp
                        )

                        // Subtle play icon badge overlay (ColorOS Blue)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0082FB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(Spacing.md))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 78.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = show.title,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        val totalStr = if (show.totalEp > 0) "/${show.totalEp}" else ""
                        Text(
                            text = "Ep ${show.currentEp}$totalStr",
                            fontSize = 12.sp,
                            fontFamily = JustSansFamily,
                            color = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (show.totalEp > 0) {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            val fraction = (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isLight) Color.Black.copy(alpha = 0.08f) else Color(0xFFE2F0FE).copy(alpha = 0.20f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB))
                                )
                            }
                        }
                    }
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
    val lazyListState = rememberLazyListState()
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    LazyRow(
        state = lazyListState,
        flingBehavior = snapFlingBehavior,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items(shows, key = { it.id }) { show ->
            RecentlyUpdatedCard(
                show = show,
                isLight = isLight,
                hazeState = hazeState,
                onCardClick = { onShowClick(show.id) },
                onWatchClick = if (onWatchClick != null) { { onWatchClick(show) } } else null
            )
        }
    }
}

@Composable
fun RecentlyUpdatedCard(
    show: Show,
    isLight: Boolean,
    hazeState: dev.chrisbanes.haze.HazeState = LocalHazeState.current,
    onCardClick: () -> Unit,
    onWatchClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(28.dp)
    val posterShape = RoundedCornerShape(18.dp)

    // Official ColorOS 17 Translucent Frosted Glass Squircle Card Background
    val cardBg = if (isLight) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.70f),
                Color(0xFFF0F4FA).copy(alpha = 0.60f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E2638).copy(alpha = 0.48f),
                Color(0xFF121622).copy(alpha = 0.58f)
            )
        )
    }

    // ColorOS 17 Specular Highlight Gradient Border
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isLight) 0.35f else 0.18f),
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

    val primaryAccent = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB)

    Box(
        modifier = modifier
            .width(292.dp)
            .clip(cardShape)
            .hazeChild(state = hazeState, shape = cardShape)
            .background(cardBg)
            .border(width = 0.85.dp, brush = borderBrush, shape = cardShape)
            .colorOSPressBounce(
                pressedScale = 0.96f,
                onClick = onCardClick
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Column with Badges
            Box(
                modifier = Modifier
                    .size(width = 74.dp, height = 100.dp)
                    .clip(posterShape)
                    .border(
                        width = 0.75.dp,
                        color = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.16f),
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

                // ColorOS 17 Capsule Pill Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0082FB),
                                    Color(0xFF1A90FF)
                                )
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
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

                // Quick Play Circle Overlay (ColorOS 17 Blue)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(primaryAccent)
                        .border(0.75.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                        .colorOSPressBounce(pressedScale = 0.88f)
                        .clickable(enabled = onWatchClick != null) {
                            onWatchClick?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Watch now",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Episode Pill + Day/Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Signature ColorOS 17 Episode Capsule Pill
                    val epText = if (show.totalEp > 0) "EP ${show.currentEp}/${show.totalEp}" else "EP ${show.currentEp}"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(primaryAccent)
                            .padding(horizontal = 8.dp, vertical = 2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = epText,
                            fontSize = 10.5.sp,
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Release day or status
                    val statusLabel = when (show.status) {
                        "completed" -> "END"
                        else -> show.releaseDay?.take(3)?.uppercase() ?: "AIRING"
                    }
                    Text(
                        text = statusLabel,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        letterSpacing = 0.5.sp
                    )
                }

                // Middle: Show Title (and Chinese subtitle if present)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = show.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = if (!show.titleZh.isNullOrBlank()) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )

                    if (!show.titleZh.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = show.titleZh,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bottom Row: Relative timestamp + Quick action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock),
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
                        )
                        Text(
                            text = relativeTime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Details",
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    .width(128.dp)
                    .height(184.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .shimmerEffect()
            )

            // Right Details Skeleton
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(184.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(86.dp)
                    .clip(RoundedCornerShape(CornerRadius.card))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
private fun RecentlyUpdatedRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .width(292.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .shimmerEffect()
            )
        }
    }
}
