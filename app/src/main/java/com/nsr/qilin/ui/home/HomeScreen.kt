package com.nsr.qilin.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nsr.qilin.R
import com.nsr.qilin.domain.model.CountdownCalculator
import com.nsr.qilin.domain.model.Show
import com.nsr.qilin.ui.LocalHazeState
import com.nsr.qilin.ui.Spacing
import com.nsr.qilin.ui.common.DonghuaPoster
import com.nsr.qilin.ui.common.EmptyState
import com.nsr.qilin.ui.common.ScrollPerfMonitor
import com.nsr.qilin.ui.common.shimmerEffect
import com.nsr.qilin.ui.rememberLiquidGlassBorder
import com.nsr.qilin.ui.rememberLiquidGlassChipBorder
import com.nsr.qilin.ui.screens.HomeViewModel
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.SubcomposeAsyncImage
import com.nsr.qilin.data.service.AniListMedia
import com.nsr.qilin.ui.theme.ColorOSMotion
import com.nsr.qilin.ui.theme.CornerRadius
import com.nsr.qilin.ui.theme.JustSansFamily
import com.nsr.qilin.ui.theme.colorOSPressBounce
import com.nsr.qilin.ui.theme.RevealOnEnter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    customStreamUrl: String = "",
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
            } else if (state.allShows.isEmpty()) {
                if (state.showCountdownBanner && state.trendingDonghua.isNotEmpty()) {
                    item(key = "trending_hero_carousel_empty") {
                        TrendingHeroCarousel(
                            trendingList = state.trendingDonghua,
                            allShows = state.allShows,
                            themeMode = state.themeMode,
                            onPageChanged = { page -> viewModel.setActiveTrendingIndex(page) },
                            onTrendingClick = { media ->
                                viewModel.addTrendingShow(media) { targetId ->
                                    onShowClick(targetId)
                                }
                            },
                            onWatchClick = { media ->
                                viewModel.addTrendingShow(media) { targetId ->
                                    val url = com.nsr.qilin.data.service.PosterFetchService.resolveWatchUrl(
                                        showTitle = media.preferredTitle,
                                        customWatchUrl = null,
                                        providerPreference = defaultStreamProvider,
                                        customProviderUrl = customStreamUrl
                                    )
                                    onWatchClick(url)
                                }
                            }
                        )
                    }
                    item(key = "trending_empty_spacer") { Spacer(modifier = Modifier.height(Spacing.sm)) }
                }
                item {
                    EmptyState(
                        title = "No shows tracked yet",
                        subtitle = "Tap any trending show above or search to start tracking your favorite donghua",
                        painter = painterResource(id = R.drawable.ic_airing),
                        modifier = Modifier.padding(top = if (state.showCountdownBanner && state.trendingDonghua.isNotEmpty()) Spacing.md else effectiveTopPadding)
                    )
                }
            } else {
                if (state.showCountdownBanner) {
                    if (state.trendingDonghua.isNotEmpty()) {
                        item(key = "trending_hero_carousel") {
                            RevealOnEnter(index = 0) {
                                TrendingHeroCarousel(
                                    trendingList = state.trendingDonghua,
                                    allShows = state.allShows,
                                    themeMode = state.themeMode,
                                    onPageChanged = { page -> viewModel.setActiveTrendingIndex(page) },
                                    onTrendingClick = { media ->
                                        viewModel.addTrendingShow(media) { targetId ->
                                            onShowClick(targetId)
                                        }
                                    },
                                    onWatchClick = { media ->
                                        viewModel.addTrendingShow(media) { targetId ->
                                            val url = com.nsr.qilin.data.service.PosterFetchService.resolveWatchUrl(
                                                showTitle = media.preferredTitle,
                                                customWatchUrl = null,
                                                providerPreference = defaultStreamProvider,
                                                customProviderUrl = customStreamUrl
                                            )
                                            onWatchClick(url)
                                        }
                                    }
                                )
                            }
                        }
                        item(key = "trending_spacer") { Spacer(modifier = Modifier.height(Spacing.sm)) }
                    } else if (state.isTrendingLoading) {
                        item(key = "trending_skeleton") { HeroBannerSkeleton() }
                        item(key = "trending_skeleton_spacer") { Spacer(modifier = Modifier.height(Spacing.sm)) }
                    }
                }

                if (state.showContinueWatching && state.continueWatching.isNotEmpty()) {
                    item(key = "section_continue_header") {
                        RevealOnEnter(index = 1) {
                            SectionHeader(title = "Continue Watching")
                        }
                    }
                    item(key = "section_continue_row") {
                        RevealOnEnter(index = 2) {
                            ContinueWatchingRow(
                                shows = state.continueWatching,
                                onShowClick = onShowClick
                            )
                        }
                    }
                }

                if (state.showRecentlyUpdated && state.recentlyUpdated.isNotEmpty()) {
                    item(key = "section_recent_header") {
                        RevealOnEnter(index = 3) {
                            SectionHeader(title = "Recently Updated")
                        }
                    }
                    item(key = "section_recent_row") {
                        RevealOnEnter(index = 4) {
                            RecentlyUpdatedRow(
                                shows = state.recentlyUpdated,
                                onShowClick = onShowClick,
                                onWatchClick = { show ->
                                    val url = com.nsr.qilin.data.service.PosterFetchService.resolveWatchUrl(
                                        showTitle = show.title,
                                        customWatchUrl = show.watchUrl,
                                        providerPreference = defaultStreamProvider,
                                        customProviderUrl = customStreamUrl
                                    )
                                    onWatchClick(url)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
fun TrendingHeroCarousel(
    trendingList: List<AniListMedia>,
    allShows: List<Show>,
    themeMode: String = "dark",
    onPageChanged: ((Int) -> Unit)? = null,
    onTrendingClick: (AniListMedia) -> Unit,
    onWatchClick: (AniListMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    if (trendingList.isEmpty()) return

    val displayList = remember(trendingList) { trendingList.take(6) }
    val pagerState = rememberPagerState(pageCount = { displayList.size })
    val coroutineScope = rememberCoroutineScope()
    val isLight = themeMode.lowercase() == "light"
    val isAmoled = themeMode.lowercase() == "amoled"

    // Notify parent whenever active page changes
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged?.invoke(pagerState.currentPage)
    }

    // Auto-advance every 6 seconds
    LaunchedEffect(pagerState.pageCount) {
        while (true) {
            delay(6000)
            if (displayList.size > 1 && !pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % displayList.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val media = displayList[page]
            val isAlreadyInLibrary = remember(media.id, allShows) {
                allShows.any { s ->
                    s.aniListId == media.id ||
                    s.title.equals(media.preferredTitle, ignoreCase = true) ||
                    (media.titleEnglish != null && s.title.equals(media.titleEnglish, ignoreCase = true)) ||
                    (media.titleRomaji != null && s.title.equals(media.titleRomaji, ignoreCase = true))
                }
            }
            TrendingHeroCard(
                media = media,
                rank = page + 1,
                isAlreadyInLibrary = isAlreadyInLibrary,
                isLight = isLight,
                isAmoled = isAmoled,
                onClick = { onTrendingClick(media) },
                onWatchClick = { onWatchClick(media) }
            )
        }

        if (displayList.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                displayList.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 18.dp else 6.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else if (isLight) Color.Black.copy(alpha = 0.20f)
                                else Color.White.copy(alpha = 0.25f)
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingHeroCard(
    media: AniListMedia,
    rank: Int,
    isAlreadyInLibrary: Boolean,
    isLight: Boolean,
    isAmoled: Boolean,
    onClick: () -> Unit,
    onWatchClick: () -> Unit
) {
    val hazeState = LocalHazeState.current
    val bannerShape = RoundedCornerShape(26.dp)
    val bannerBorderBrush = rememberLiquidGlassBorder(isLight = isLight, isAmoled = isAmoled)
    val primaryAccent = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB)
    val posterShape = RoundedCornerShape(16.dp)
    val chipBorder = rememberLiquidGlassChipBorder(isLight)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
            .clip(bannerShape)
            .background(
                when {
                    isLight -> Color.White.copy(alpha = 0.85f)
                    isAmoled -> Color.Black.copy(alpha = 0.82f)
                    else -> Color(0xFF131726).copy(alpha = 0.65f)
                }
            )
            .border(
                width = 0.85.dp,
                brush = bannerBorderBrush,
                shape = bannerShape
            )
            .colorOSPressBounce(pressedScale = 0.985f, onClick = onClick)
            .padding(14.dp)
    ) {
        // Foreground Content Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: Donghua Poster Tile
            Box(
                modifier = Modifier
                    .width(118.dp)
                    .height(176.dp)
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
                    posterUrl = media.coverImage,
                    title = media.preferredTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    showAcronym = false,
                    immediate = true
                )

                // Top-Left Rank Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "#$rank",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Top-Right Score Pill
                if (media.averageScore != null && media.averageScore > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "★ ${media.averageScore}%",
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp
                        )
                    }
                }

                // Bottom Tag if In Library
                if (isAlreadyInLibrary) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color(0xFF00C853).copy(alpha = 0.88f))
                            .padding(vertical = 2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "In Library",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            // RIGHT SIDE: Details & Action Button
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(176.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    // Title
                    Text(
                        text = media.preferredTitle,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )

                    // Year • Status • Episodes
                    val statusText = when (media.status?.lowercase()) {
                        "releasing" -> "Airing"
                        "completed" -> "Completed"
                        else -> media.status?.replaceFirstChar { it.uppercase() } ?: "Ongoing"
                    }
                    val epCountText = if (media.episodes > 0) "${media.episodes} eps" else "Series"
                    val metaLine = buildString {
                        if (media.releaseYear != null && media.releaseYear > 0) {
                            append("${media.releaseYear} • ")
                        }
                        append("$statusText • $epCountText")
                    }
                    Text(
                        text = metaLine,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Genres
                    if (!media.genres.isNullOrEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            media.genres.take(2).forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(
                                            if (isLight) Color.Black.copy(alpha = 0.05f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(0.6.dp, chipBorder, RoundedCornerShape(percent = 50))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Button (50% capsule in ColorOS 17 Electric Blue)
                val watchPulse = rememberInfiniteTransition(label = "homeWatchPulse")
                    .animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.12f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 950, easing = ColorOSMotion.FastOutSlowIn),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "homeWatchPulseValue"
                    )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
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
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = watchPulse.value
                                    scaleY = watchPulse.value
                                }
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = Color.White
                            )
                        }
                        Text(
                            text = if (isAlreadyInLibrary) "Watch Now" else "Track & Watch",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val accent = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB)
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { grown = true }
    val grow by animateFloatAsState(
        targetValue = if (grown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 360f),
        label = "sectionHeaderGrow"
    )
    val accentHeight = if (subtitle.isNullOrBlank()) 18.dp else 28.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = Spacing.lg, top = Spacing.lg, bottom = Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(accentHeight)
                .graphicsLayer {
                    scaleY = grow
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = grow.coerceIn(0f, 1f)
                translationX = (1f - grow) * 14f
            }
        ) {
            Text(
                text = title,
                fontSize = 19.sp,
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    fontFamily = JustSansFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
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
        itemsIndexed(shows, key = { _, show -> show.id }) { index, show ->
            RevealOnEnter(
                index = index.coerceAtMost(6),
                distance = 22.dp,
                scaleFrom = 0.94f,
                onceKey = "home-cw-${show.id}"
            ) {
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
        itemsIndexed(shows, key = { _, show -> show.id }) { index, show ->
            RevealOnEnter(
                index = index.coerceAtMost(6),
                distance = 24.dp,
                scaleFrom = 0.94f,
                onceKey = "home-recent-${show.id}"
            ) {
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
