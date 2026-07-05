package com.mydonghua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.domain.model.StatsCalculator
import com.mydonghua.app.domain.model.Show
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    showsViewModel: ShowsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shows by showsViewModel.allShows.collectAsState()
    val stats = remember(shows) { StatsCalculator.computeStats(shows) }
    val scrollState = rememberScrollState()

    val now = Calendar.getInstance()
    val todayName = getDayOfWeekName(now.get(Calendar.DAY_OF_WEEK))
    val todayIndex = getDayOfWeekIndex(todayName)

    val ongoingShows = remember(shows) { shows.filter { it.status == "ongoing" } }
    val todayShows = remember(ongoingShows) { ongoingShows.filter { it.releaseDay == todayName } }

    val daysList = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val thisWeekShows = remember(ongoingShows) {
        ongoingShows.filter { show ->
            val dayIndex = daysList.indexOf(show.releaseDay)
            dayIndex >= todayIndex
        }.sortedBy { daysList.indexOf(it.releaseDay) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detailed Statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First 8-card grid
            Text("Core Track Statistics", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            StatsGrid(
                items = listOf(
                    StatItem("Total Tracked", stats.total.toString()),
                    StatItem("Currently Airing", stats.airing.toString()),
                    StatItem("Completed", stats.completed.toString()),
                    StatItem("Hiatus / Stopped", stats.stopped.toString()),
                    StatItem("Total Episodes", stats.totalEpisodes.toString()),
                    StatItem("Hours Watched", stats.hoursWatched.toString()),
                    StatItem("Favorites", stats.favorites.toString()),
                    StatItem("Airing Today", todayShows.size.toString())
                )
            )

            // Second 8-card grid
            Text("Completion & Activity Metrics", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            StatsGrid(
                items = listOf(
                    StatItem("Completion Rate", "${stats.completionRate}%"),
                    StatItem("Day Streak", stats.streak.toString()),
                    StatItem("Active This Week", stats.weeklyActivity.toString()),
                    StatItem("Active This Month", stats.monthlyActivity.toString()),
                    StatItem("Avg Rating", stats.avgRating),
                    StatItem("Longest Series", stats.longestSeries?.title ?: "—", subText = "${stats.longestEpCount} eps")
                ),
                columns = 2
            )

            // Top Genres
            if (stats.topGenres.isNotEmpty()) {
                Text("Top Genres", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stats.topGenres.forEach { (genre, count) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$genre ($count)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Top Studios
            if (stats.topStudios.isNotEmpty()) {
                Text("Top Studios", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stats.topStudios.forEach { (studio, count) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$studio ($count)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Weekly Releases Preview
            Text("This Week's Remaining Releases", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (thisWeekShows.isEmpty()) {
                Text("No upcoming releases this week", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    thisWeekShows.forEach { show ->
                        val isToday = show.releaseDay == todayName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = show.releaseDay?.substring(0, 3) ?: "Sun",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.width(48.dp)
                            )
                            Text(
                                text = show.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                text = show.releaseTime ?: "10:00",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

data class StatItem(
    val label: String,
    val value: String,
    val subText: String? = null
)

@Composable
fun StatsGrid(items: List<StatItem>, columns: Int = 2) {
    val chunked = items.chunked(columns)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(item.value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(item.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            if (item.subText != null) {
                                Text(item.subText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                if (rowItems.size < columns) {
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// FlowRow implementation helper
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val layoutWidth = constraints.maxWidth
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0

        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width > layoutWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + horizontalArrangement.spacing.roundToPx()
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        var totalHeight = 0
        rows.forEachIndexed { index, row ->
            val rowHeight = row.maxOf { it.height }
            totalHeight += rowHeight
            if (index < rows.size - 1) {
                totalHeight += verticalArrangement.spacing.roundToPx()
            }
        }

        layout(layoutWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOf { it.height }
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + horizontalArrangement.spacing.roundToPx()
                }
                y += rowHeight + verticalArrangement.spacing.roundToPx()
            }
        }
    }
}

private fun getDayOfWeekName(dayIndex: Int): String {
    return when (dayIndex) {
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Sunday"
    }
}

private fun getDayOfWeekIndex(name: String): Int {
    val list = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    return list.indexOf(name)
}
