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
import com.mydonghua.app.ui.theme.colorOSAuroraBorder
import com.mydonghua.app.ui.theme.colorOSAuroraShimmer
import com.mydonghua.app.ui.theme.colorOSPressBounce
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

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
    val context = LocalContext.current
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

    // ColorOS 17 Aurora Rim Gradient Border
    val bannerBorderBrush = colorOSAuroraBorder(
        isDark = !isLight,
        primaryColor = Color(0xFF00E5FF),
        accentGlow = if (isLight) 0.35f else 0.50f
    )

    val bannerShape = RoundedCornerShape(26.dp)

    val textShadow = remember {
        Shadow(
            color = Color.Black.copy(alpha = 0.85f),
            offset = Offset(0f, 2f),
            blurRadius = 6f
        )
    }

    val primaryAccent = if (isLight) Color(0xFF008D99) else Color(0xFF00E5FF)
    val hasSchedule = !show.releaseDay.isNullOrEmpty() && !show.releaseTime.isNullOrEmpty()

    Card(
        shape = bannerShape,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = bannerBorderBrush
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
            .height(275.dp)
            .colorOSPressBounce(pressedScale = 0.985f, onClick = { onBannerClick() })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(bannerShape)
                .colorOSAuroraShimmer(enabled = true, shimmerColor = Color.White.copy(alpha = 0.08f))
        ) {
            // Hero Poster
            DonghuaPoster(
                posterUrl = show.poster,
                title = show.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(bannerShape),
                contentScale = ContentScale.Crop,
                showAcronym = false,
                immediate = true
            )

            // Cinematic Multi-Stop Atmospheric Vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.60f),
                            0.22f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.30f),
                            0.70f to Color.Black.copy(alpha = 0.78f),
                            1.0f to Color.Black.copy(alpha = 0.96f)
                        )
                    )
            )

            // Left soft gradient for title contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to Color.Black.copy(alpha = 0.40f),
                            0.60f to Color.Transparent
                        )
                    )
            )

            // Top Row: Status Pill (Left) and Episode / Genre Badge (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left: "AIRING NOW" or "NEXT UP" Capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .then(
                            if (remainingTime.airingNow) {
                                Modifier.background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF00E676).copy(alpha = 0.88f),
                                            Color(0xFF00B0FF).copy(alpha = 0.88f)
                                        )
                                    )
                                )
                            } else {
                                Modifier
                                    .background(Color.Black.copy(alpha = 0.58f))
                                    .border(
                                        width = 0.75.dp,
                                        color = primaryAccent.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(percent = 50)
                                    )
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (remainingTime.airingNow) Color.White else primaryAccent)
                        )
                        Text(
                            text = when {
                                remainingTime.airingNow -> "AIRING NOW"
                                hasSchedule -> "NEXT UP • ${show.releaseDay?.take(3)?.uppercase()} ${show.releaseTime}"
                                else -> "FEATURED SHOW"
                            },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Top-Right: Episode / Genre Pill
                val topRightTag = when {
                    show.totalEp > 0 -> "EP ${show.currentEp + 1}/${show.totalEp}"
                    !show.genres.isNullOrBlank() -> show.genres.split(",").firstOrNull()?.trim() ?: "EP ${show.currentEp + 1}"
                    else -> "EP ${show.currentEp + 1}"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.52f))
                        .border(
                            width = 0.75.dp,
                            color = Color.White.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(percent = 50)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = topRightTag,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.92f),
                        letterSpacing = 0.4.sp
                    )
                }
            }

            // Bottom Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Main English / Romaji Title
                Text(
                    text = show.title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = textShadow),
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Subtitle Row (Chinese Title & Genres)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!show.titleZh.isNullOrBlank()) {
                        Text(
                            text = show.titleZh,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val detailInfo = if (remainingTime.airingNow) {
                        if (!show.titleZh.isNullOrBlank()) " • New episode live" else "New episode available now"
                    } else if (hasSchedule) {
                        if (!show.titleZh.isNullOrBlank()) " • Ep ${show.currentEp + 1} (${show.releaseDay})"
                        else "Episode ${show.currentEp + 1} • ${show.releaseDay} ${show.releaseTime}"
                    } else {
                        if (!show.titleZh.isNullOrBlank()) " • Episode ${show.currentEp}"
                        else "Episode ${show.currentEp} • Ongoing"
                    }
                    Text(
                        text = detailInfo,
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (remainingTime.airingNow) {
                    // Vibrant Aurora CTA Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF0072FF),
                                        Color(0xFF7F00FF)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(13.dp)
                            )
                            .colorOSPressBounce(pressedScale = 0.95f, onClick = { onWatchClick() })
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "Watch Episode ${show.currentEp + 1} Now",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                } else if (hasSchedule) {
                    // Liquid Glass Countdown Deck
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BannerTimeBox(value = remainingTime.days, label = "DAYS", modifier = Modifier.weight(1f))
                        BannerTimeBox(value = remainingTime.hours, label = "HOURS", modifier = Modifier.weight(1f))
                        BannerTimeBox(value = remainingTime.minutes, label = "MINS", modifier = Modifier.weight(1f))
                        BannerTimeBox(value = remainingTime.seconds, label = "SECS", modifier = Modifier.weight(1f))
                    }
                } else {
                    // Clean Glass Details Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(13.dp)
                            )
                            .colorOSPressBounce(pressedScale = 0.96f, onClick = { onBannerClick() })
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = primaryAccent
                            )
                            Text(
                                text = "View Show Details & Episodes",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerTimeBox(
    value: Long,
    label: String,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val shape = RoundedCornerShape(13.dp)

    // Liquid Glass Gradient for Timer Pillar
    val bgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.Black.copy(alpha = 0.45f)
            )
        )
    }

    val primaryAccent = if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape)
            .background(brush = bgBrush)
            .border(
                width = 0.75.dp,
                color = primaryAccent.copy(alpha = 0.35f),
                shape = shape
            )
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 17.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryAccent,
                letterSpacing = 1.1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xxl, bottom = Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF),
                            Color(0xFF0072FF)
                        )
                    )
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
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
    val cardShape = RoundedCornerShape(CornerRadius.card)

    // Unified liquid glass border across all boxes
    val cardBorder = rememberLiquidGlassBorder(isLight)

    val cardBg = if (isLight) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
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
                        width = 1.dp,
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

                        // Subtle play icon badge overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFF00E5FF)
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
                            fontSize = 11.5.sp,
                            color = if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (show.totalEp > 0) {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            val fraction = (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLight) 0.10f else 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                if (isLight) {
                                                    listOf(Color(0xFF009AA6), Color(0xFF0072FF))
                                                } else {
                                                    listOf(Color(0xFF00E5FF), Color(0xFF0072FF))
                                                }
                                            )
                                        )
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
    val cardShape = RoundedCornerShape(20.dp)
    val posterShape = RoundedCornerShape(13.dp)

    // ColorOS 17 Liquid Glass & Aurora Gradient Background
    val cardBg = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.94f),
                Color(0xFFF1F5F9).copy(alpha = 0.88f)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 400f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF171B2B).copy(alpha = 0.82f),
                Color(0xFF0F121E).copy(alpha = 0.92f)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 400f)
        )
    }

    // ColorOS 17 Aurora Rim Gradient Border
    val borderBrush = colorOSAuroraBorder(
        isDark = !isLight,
        primaryColor = Color(0xFF00E5FF),
        accentGlow = if (isLight) 0.35f else 0.45f
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

    val primaryAccent = if (isLight) Color(0xFF008D99) else Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .width(292.dp)
            .clip(cardShape)
            .hazeChild(state = hazeState, shape = cardShape)
            .background(cardBg)
            .border(width = 1.dp, brush = borderBrush, shape = cardShape)
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

                // Aurora Glow "NEW" / "UPDATED" Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF7F00FF)
                                )
                            )
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEW",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.6.sp
                    )
                }

                // Quick Play Circle Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(enabled = onWatchClick != null) {
                            onWatchClick?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Watch now",
                        modifier = Modifier.size(13.dp),
                        tint = primaryAccent
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
                    // Episode Capsule Pill
                    val epText = if (show.totalEp > 0) "EP ${show.currentEp}/${show.totalEp}" else "EP ${show.currentEp}"
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(primaryAccent.copy(alpha = if (isLight) 0.12f else 0.16f))
                            .border(
                                width = 0.75.dp,
                                color = primaryAccent.copy(alpha = if (isLight) 0.35f else 0.40f),
                                shape = RoundedCornerShape(percent = 50)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(primaryAccent)
                        )
                        Text(
                            text = epText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            letterSpacing = 0.2.sp
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
    val bannerBorderBrush = colorOSAuroraBorder(
        isDark = !isLight,
        primaryColor = Color(0xFF00E5FF),
        accentGlow = if (isLight) 0.35f else 0.50f
    )
    val bannerShape = RoundedCornerShape(26.dp)

    Card(
        shape = bannerShape,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = bannerBorderBrush
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
            .height(275.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(bannerShape)
                .shimmerEffect()
        )
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
