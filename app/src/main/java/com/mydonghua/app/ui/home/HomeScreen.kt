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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.mydonghua.app.R
import com.mydonghua.app.domain.model.CountdownCalculator
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.GlassSection
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.DonghuaPoster
import com.mydonghua.app.ui.screens.HomeViewModel
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShowClick: (String) -> Unit,
    onWatchClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(top = topPadding + Spacing.sm, bottom = 80.dp)
    ) {
        if (state.showCountdownBanner && state.nextUpShow != null) {
            item { HeroBanner(show = state.nextUpShow!!, onWatchClick = { onWatchClick(state.nextUpShow!!.watchUrl ?: "") }, onBannerClick = { onShowClick(state.nextUpShow!!.id) }) }
        }

        if (state.showContinueWatching && state.continueWatching.isNotEmpty()) {
            item { SectionHeader(title = "Continue Watching") }
            item { ContinueWatchingRow(shows = state.continueWatching, onShowClick = onShowClick) }
        }

        if (state.showRecentlyUpdated && state.recentlyUpdated.isNotEmpty()) {
            item { SectionHeader(title = "Recently Updated") }
            item { RecentlyUpdatedRow(shows = state.recentlyUpdated, onShowClick = onShowClick) }
        }

        if (state.showWeeklySchedule) {
            item { SectionHeader(title = "Weekly Schedule") }
            item { WeeklyScheduleSection(shows = state.allShows, onShowClick = onShowClick, onWatchClick = onWatchClick, viewModel = viewModel) }
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
    val targetMs = remember(show.id) { CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime) }
    var remainingTime by remember(show.id) { mutableStateOf(CountdownCalculator.calculateTimeRemaining(targetMs)) }

    LaunchedEffect(Unit) {
        while (true) {
            remainingTime = CountdownCalculator.calculateTimeRemaining(targetMs)
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            .height(210.dp)
            .clip(RoundedCornerShape(CornerRadius.banner))
            .clickable { onBannerClick() }
    ) {
        DonghuaPoster(
            posterUrl = show.poster,
            title = show.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            showAcronym = false
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.2f to Color.Transparent,
                        1.0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            Text(
                text = show.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (remainingTime.airingNow) "New episode available now"
                else "Ep ${show.currentEp + 1} • ${show.releaseDay} ${show.releaseTime ?: ""}",
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BannerTimeBox(value = remainingTime.days, label = "days")
                BannerTimeBox(value = remainingTime.hours, label = "hours")
                BannerTimeBox(value = remainingTime.minutes, label = "mins")
                BannerTimeBox(value = remainingTime.seconds, label = "secs")
            }
        }
    }
}

@Composable
private fun BannerTimeBox(value: Long, label: String) {
    val hazeState = LocalHazeState.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(42.dp)
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.15f)
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
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xxl, bottom = Spacing.md)
    )
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
            GlassSection(modifier = Modifier.width(240.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowClick(show.id) }
                        .padding(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 72.dp)
                            .clip(RoundedCornerShape(CornerRadius.sm))
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
                                    .height(3.dp)
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
                GlassSection(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowClick(show.id) }
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 88.dp)
                                .clip(RoundedCornerShape(CornerRadius.sm))
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
                                    modifier = Modifier.padding(top = 4.dp),
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
fun WeeklyScheduleSection(
    shows: List<Show>,
    onShowClick: (String) -> Unit,
    onWatchClick: (String) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val calendar = Calendar.getInstance()
    val todayNum = calendar.get(Calendar.DAY_OF_WEEK)
    val todayName = when (todayNum) {
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Sunday"
    }

    var selectedDay by remember { mutableStateOf(todayName) }

    val filteredShows = remember(shows, selectedDay) {
        shows.filter { it.status == "ongoing" && it.releaseDay == selectedDay }
            .sortedWith(compareBy { it.releaseTime ?: "" })
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg)
            .clip(RoundedCornerShape(CornerRadius.card))
            .hazeChild(state = hazeState, shape = RoundedCornerShape(CornerRadius.card))
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.04f),
                shape = RoundedCornerShape(CornerRadius.card)
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(CornerRadius.card)
            )
            .padding(Spacing.md)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(days) { day ->
                    val isSelected = day == selectedDay
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CornerRadius.chip))
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                shape = RoundedCornerShape(CornerRadius.chip)
                            )
                            .clickable { selectedDay = day }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.substring(0, 3),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            if (filteredShows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xxl),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No shows releasing on this day",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xs)
                        .padding(bottom = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    filteredShows.forEach { show ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CornerRadius.chip))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(CornerRadius.chip)
                                )
                                .clickable { onShowClick(show.id) }
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = show.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = show.releaseTime ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}