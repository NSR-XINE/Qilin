package com.mydonghua.app.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
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
import com.mydonghua.app.ui.common.ScrollPerfMonitor
import com.mydonghua.app.ui.common.shimmerEffect
import com.mydonghua.app.ui.screens.HomeViewModel
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.colorOSPressBounce
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat

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
        contentPadding = PaddingValues(top = effectiveTopPadding, bottom = 80.dp)
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

    val bannerBorderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.12f)
            )
        )
    }
    val overlayBrush = remember {
        Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.25f to Color.Transparent,
            0.55f to Color.Black.copy(alpha = 0.04f),
            0.8f to Color.Black.copy(alpha = 0.15f),
            1.0f to Color.Black.copy(alpha = 0.45f)
        )
    }
    val textShadow = remember {
        Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            offset = Offset(0f, 1f),
            blurRadius = 3f
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
            .height(250.dp)
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, end = 18.dp, bottom = Spacing.xl, top = 10.dp)
            ) {
                Text(
                    text = show.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = textShadow)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (remainingTime.airingNow) "New episode available now"
                    else "Ep ${show.currentEp + 1} • ${show.releaseDay} ${show.releaseTime ?: ""}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    BannerTimeBox(value = remainingTime.days, label = "days")
                    BannerTimeBox(value = remainingTime.hours, label = "hours")
                    BannerTimeBox(value = remainingTime.minutes, label = "mins")
                    BannerTimeBox(value = remainingTime.seconds, label = "secs")
                }
            }
        }
    }
}

@Composable
private fun BannerTimeBox(value: Long, label: String) {
    val hazeState = LocalHazeState.current
    val shape = RoundedCornerShape(CornerRadius.md)
    val bgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.45f),
                Color.Black.copy(alpha = 0.25f)
            )
        )
    }
    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.50f),
                Color.White.copy(alpha = 0.15f)
            )
        )
    }
    Box(
        modifier = Modifier
            .width(54.dp)
            .height(46.dp)
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape)
            .background(brush = bgBrush)
            .border(width = 0.85.dp, brush = borderBrush, shape = shape)
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
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
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
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
    val context = LocalContext.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(shows, key = { it.id }) { show ->
            GlassSection(
                modifier = Modifier.width(240.dp),
                onClick = { onShowClick(show.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 74.dp)
                            .clip(RoundedCornerShape(CornerRadius.md))
                    ) {
                        DonghuaPoster(
                            posterUrl = show.poster,
                            title = show.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            fontSize = 7.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = show.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val totalStr = if (show.totalEp > 0) "/${show.totalEp}" else ""
                        Text(
                            text = "Ep ${show.currentEp}$totalStr",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (show.totalEp > 0) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            LinearProgressIndicator(
                                progress = { (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(CornerRadius.xs)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
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
            Box(modifier = Modifier.fillParentMaxWidth()) {
                GlassSection(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onShowClick(show.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 54.dp, height = 72.dp)
                                .clip(RoundedCornerShape(CornerRadius.md))
                        ) {
                            DonghuaPoster(
                                posterUrl = show.poster,
                                title = show.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                fontSize = 7.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = show.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val relativeTime = if (show.lastUpdated != null) {
                                DateUtils.getRelativeTimeSpanString(show.lastUpdated).toString()
                            } else ""
                            if (relativeTime.isNotEmpty()) {
                                Text(
                                    text = relativeTime,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.xs),
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
            .height(250.dp)
            .clip(RoundedCornerShape(CornerRadius.card))
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(CornerRadius.sm))
                    .shimmerEffect()
            )
        }
    }
}