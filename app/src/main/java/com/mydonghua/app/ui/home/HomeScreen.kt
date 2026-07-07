package com.mydonghua.app.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import com.mydonghua.app.domain.model.CountdownTime
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.screens.HomeViewModel
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
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var ticks by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            ticks++
        }
    }

    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(ticks) {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val now = Date()
        currentTimeString = "${sdf.format(now)} (${dayFormat.format(now)})"
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Hero Countdown Banner
        if (state.showCountdownBanner && state.nextUpShow != null) {
            item(span = { GridItemSpan(2) }) {
                HeroBanner(
                    show = state.nextUpShow!!,
                    ticks = ticks,
                    onWatchClick = { onWatchClick(state.nextUpShow!!.watchUrl ?: "") },
                    onBannerClick = { onShowClick(state.nextUpShow!!.id) }
                )
            }
        }

        // Continue Watching Section
        if (state.showContinueWatching && state.continueWatching.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                SectionHeader(title = "Continue Watching")
            }
            item(span = { GridItemSpan(2) }) {
                ContinueWatchingRow(shows = state.continueWatching, onShowClick = onShowClick)
            }
        }

        // Recently Updated Section
        if (state.showRecentlyUpdated && state.recentlyUpdated.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                SectionHeader(title = "Recently Updated")
            }
            item(span = { GridItemSpan(2) }) {
                RecentlyUpdatedRow(shows = state.recentlyUpdated, onShowClick = onShowClick)
            }
        }

        // Weekly Schedule Section
        if (state.showWeeklySchedule) {
            item(span = { GridItemSpan(2) }) {
                SectionHeader(title = "Weekly Schedule")
            }
            item(span = { GridItemSpan(2) }) {
                WeeklyScheduleSection(
                    shows = state.allShows,
                    onShowClick = onShowClick,
                    onWatchClick = onWatchClick,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun HeroBanner(
    show: Show,
    ticks: Int,
    onWatchClick: () -> Unit,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targetMs = remember(show.id) { CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime) }
    var remainingTime by remember(show.id) { mutableStateOf(CountdownCalculator.calculateTimeRemaining(targetMs)) }

    LaunchedEffect(ticks) {
        remainingTime = CountdownCalculator.calculateTimeRemaining(targetMs)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onBannerClick() }
    ) {
        // Poster Background
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



        // Badge showing status
        Box(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF9D4EDD))),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "NEXT UP RELEASE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Details Block
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = show.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 4f
                    )
                )
            )
            Text(
                text = "Episode ${show.currentEp + 1} releasing on ${show.releaseDay} at ${show.releaseTime}",
                fontSize = 12.sp,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                        blurRadius = 3f
                    )
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Time Display Grid
            if (remainingTime.airingNow) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "📡 AIRING NOW / JUST RELEASED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (!remainingTime.elapsed) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BannerTimeBox(value = remainingTime.days, label = "Days")
                    BannerTimeBox(value = remainingTime.hours, label = "Hours")
                    BannerTimeBox(value = remainingTime.minutes, label = "Mins")
                    BannerTimeBox(value = remainingTime.seconds, label = "Secs")
                }
            }
        }
    }
}

@Composable
private fun BannerTimeBox(value: Long, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(46.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = String.format("%02d", value),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .offset(y = 2.dp)
                .wrapContentHeight(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
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
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp)
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(shows, key = { it.id }) { show ->
            Card(
                modifier = Modifier
                    .width(260.dp)
                    .clickable { onShowClick(show.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(show.poster)
                                .crossfade(true)
                                .build(),
                            contentDescription = show.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = show.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val totalStr = if (show.totalEp > 0) "/${show.totalEp}" else ""
                        Text(
                            text = "Ep ${show.currentEp}$totalStr",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (show.totalEp > 0) {
                            val ratio = show.currentEp.toFloat() / show.totalEp.toFloat()
                            LinearProgressIndicator(
                                progress = ratio.coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(shows, key = { it.id }) { show ->
            Card(
                modifier = Modifier
                    .width(260.dp)
                    .clickable { onShowClick(show.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(show.poster)
                                .crossfade(true)
                                .build(),
                            contentDescription = show.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = show.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val relativeTime = if (show.lastUpdated != null) {
                            DateUtils.getRelativeTimeSpanString(show.lastUpdated).toString()
                        } else {
                            ""
                        }
                        Text(
                            text = relativeTime,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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

    Column(modifier = modifier.fillMaxWidth()) {
        // Day selector tabs
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days) { day ->
                val isSelected = day == selectedDay
                val isToday = day == todayName

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary)
                            else Modifier
                                .hazeChild(state = hazeState, shape = RoundedCornerShape(8.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isDark) listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            Color.White.copy(alpha = 0.03f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.10f)
                                        ) else listOf(
                                            Color.Black.copy(alpha = 0.04f),
                                            Color.Black.copy(alpha = 0.01f),
                                            Color.Black.copy(alpha = 0.06f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        .clickable { selectedDay = day }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.substring(0, 3) + if (isToday) " •" else "",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Schedule list
        if (filteredShows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No shows releasing on this day",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                filteredShows.forEach { show ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .hazeChild(state = hazeState, shape = RoundedCornerShape(8.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (isDark) listOf(
                                        Color.White.copy(alpha = 0.06f),
                                        Color.White.copy(alpha = 0.02f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
                                    ) else listOf(
                                        Color.Black.copy(alpha = 0.04f),
                                        Color.Black.copy(alpha = 0.01f),
                                        Color.Black.copy(alpha = 0.06f)
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onShowClick(show.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                text = show.releaseTime ?: "10:00",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
