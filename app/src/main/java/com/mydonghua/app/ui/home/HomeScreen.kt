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
import androidx.compose.material.icons.outlined.PlayArrow
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
            item { HorizontalRowSkeleton() }
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
                item(key = "section_recent_row") { RecentlyUpdatedRow(shows = state.recentlyUpdated, onShowClick = onShowClick) }
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

    // Unified liquid glass border across all boxes
    val bannerBorderBrush = rememberLiquidGlassBorder(isLight)

    val overlayBrush = remember {
        Brush.verticalGradient(
            0.0f to Color.Black.copy(alpha = 0.40f),
            0.20f to Color.Transparent,
            0.45f to Color.Black.copy(alpha = 0.28f),
            0.75f to Color.Black.copy(alpha = 0.72f),
            1.0f to Color.Black.copy(alpha = 0.92f)
        )
    }

    val textShadow = remember {
        Shadow(
            color = Color.Black.copy(alpha = 0.70f),
            offset = Offset(0f, 1.5f),
            blurRadius = 4f
        )
    }

    val bannerShape = RoundedCornerShape(CornerRadius.banner)

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
            .height(260.dp)
            .colorOSPressBounce(pressedScale = 0.98f)
            .clickable { onBannerClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(bannerShape)
        ) {
            DonghuaPoster(
                posterUrl = show.poster,
                title = show.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(bannerShape),
                contentScale = ContentScale.Crop,
                showAcronym = false
            )

            // Multi-stop liquid gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
            )

            // Top-Left: "NEXT UP" / "AIRING" Pill Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(CornerRadius.chip))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(CornerRadius.chip))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
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
                            .background(if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF))
                    )
                    Text(
                        text = if (remainingTime.airingNow) "AIRING NOW" else "NEXT UP",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.6.sp
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
                Text(
                    text = show.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = textShadow)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (remainingTime.airingNow) "New episode available now"
                    else "Ep ${show.currentEp + 1} • ${show.releaseDay ?: "Sunday"} ${show.releaseTime ?: ""}",
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (remainingTime.airingNow) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isLight) {
                                        listOf(Color(0xFF009AA6), Color(0xFF0072FF))
                                    } else {
                                        listOf(Color(0xFF00E5FF), Color(0xFF0072FF))
                                    }
                                )
                            )
                            .border(0.5.dp, rememberLiquidGlassChipBorder(isLight), RoundedCornerShape(12.dp))
                            .clickable { onWatchClick() }
                            .colorOSPressBounce(pressedScale = 0.92f)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "Watch Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Symmetrical countdown boxes filling the available width evenly
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
    val shape = RoundedCornerShape(CornerRadius.md)
    val bgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.48f),
                Color.Black.copy(alpha = 0.28f)
            )
        )
    }
    // Unified liquid glass border
    val borderBrush = rememberLiquidGlassBorder(isLight)

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape)
            .background(brush = bgBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF).copy(alpha = 0.92f),
                letterSpacing = 0.5.sp,
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
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val lazyListState = rememberLazyListState()
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
    val cardShape = RoundedCornerShape(CornerRadius.card)

    // Unified liquid glass border across all boxes
    val cardBorder = rememberLiquidGlassBorder(isLight)

    val cardBg = if (isLight) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    }

    LazyRow(
        state = lazyListState,
        flingBehavior = snapFlingBehavior,
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

                        val relativeTime = if (show.lastUpdated != null) {
                            DateUtils.getRelativeTimeSpanString(show.lastUpdated).toString()
                        } else ""

                        if (relativeTime.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_clock),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = relativeTime,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
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
private fun HeroBannerSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
            .height(260.dp)
            .clip(RoundedCornerShape(CornerRadius.banner))
            .shimmerEffect()
    )
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
