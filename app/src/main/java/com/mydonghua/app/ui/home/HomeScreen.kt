package com.mydonghua.app.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.mydonghua.app.domain.model.CountdownCalculator
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.GlassSection
import com.mydonghua.app.ui.LocalBackdrop
import com.mydonghua.app.ui.screens.HomeViewModel
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
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
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(bottom = 100.dp)
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
            .padding(16.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onBannerClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(show.poster)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = "Hero Banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.7f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = show.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (remainingTime.airingNow) "New episode available now"
                else "Ep ${show.currentEp + 1} • ${show.releaseDay} ${show.releaseTime ?: ""}",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BannerTimeBox(value = remainingTime.days, label = "d")
                BannerTimeBox(value = remainingTime.hours, label = "h")
                BannerTimeBox(value = remainingTime.minutes, label = "m")
                BannerTimeBox(value = remainingTime.seconds, label = "s")
            }
        }
    }
}

@Composable
private fun BannerTimeBox(value: Long, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(48.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
    ) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = String.format("%02d", value),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 10.dp)
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
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(shows, key = { it.id }) { show ->
            GlassSection(modifier = Modifier.width(240.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowClick(show.id) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 72.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(show.poster).crossfade(true).build(),
                            contentDescription = show.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = show.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
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
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
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
    val context = LocalContext.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(shows, key = { it.id }) { show ->
            GlassSection(modifier = Modifier.width(240.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowClick(show.id) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 72.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(show.poster).crossfade(true).build(),
                            contentDescription = show.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = show.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
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
                                fontSize = 11.sp,
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

@Composable
fun WeeklyScheduleSection(
    shows: List<Show>,
    onShowClick: (String) -> Unit,
    onWatchClick: (String) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalBackdrop.current
    val isDark = isSystemInDarkTheme()
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
            .padding(horizontal = 16.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(16.dp) },
                effects = {
                    vibrancy()
                    colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.5f)
                    blur(8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.Black.copy(alpha = if (isDark) 0.10f else 0.15f))
                }
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(days) { day ->
                    val isSelected = day == selectedDay
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedDay = day }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
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

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredShows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
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
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 8.dp)
                ) {
                    filteredShows.forEach { show ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onShowClick(show.id) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = show.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = show.releaseTime ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
