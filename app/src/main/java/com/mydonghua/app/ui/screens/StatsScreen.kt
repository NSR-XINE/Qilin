package com.mydonghua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import com.mydonghua.app.ui.GlassSection
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.domain.model.StatsCalculator
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.shimmerEffect
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.colorOSPressBounce
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    showsViewModel: ShowsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shows by showsViewModel.allShows.collectAsState()
    val isLoading by showsViewModel.isLoading.collectAsState()
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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Detailed Statistics", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.colorOSPressBounce(pressedScale = 0.90f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(IconSize.md)
                        )
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
                .padding(horizontal = Spacing.lg)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            if (isLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    repeat(2) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(CornerRadius.xs))
                                    .shimmerEffect()
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            StatsGrid(
                                items = (0 until 4).map {
                                    StatItem("Loading", "---")
                                }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(CornerRadius.xs))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(CornerRadius.sm))
                            .shimmerEffect()
                    )
                }
            } else {
                StatsSectionHeader("Core Track Statistics")
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

                StatsSectionHeader("Completion & Activity Metrics")
                StatsGrid(
                    items = listOf(
                        StatItem("Completion Rate", "${stats.completionRate}%"),
                        StatItem("Day Streak", stats.streak.toString()),
                        StatItem("Active This Week", stats.weeklyActivity.toString()),
                        StatItem("Active This Month", stats.monthlyActivity.toString()),
                        StatItem("Avg Rating", stats.avgRating),
                        StatItem("Longest Series", stats.longestSeries?.title ?: "\u2014", subText = "${stats.longestEpCount} eps")
                    ),
                    columns = 2
                )

                    if (stats.topGenres.isNotEmpty()) {
                        StatsSectionHeader("Top Genres")
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            stats.topGenres.forEach { (genre, count) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(CornerRadius.pill))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                                ) {
                                    Text(
                                        text = "$genre ($count)",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (stats.topStudios.isNotEmpty()) {
                        StatsSectionHeader("Top Studios")
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            stats.topStudios.forEach { (studio, count) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(CornerRadius.pill))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                                ) {
                                    Text(
                                        text = "$studio ($count)",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    StatsSectionHeader("This Week's Remaining Releases")
                    if (thisWeekShows.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "All caught up!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "No upcoming releases this week — add more shows to see their schedule here",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = Spacing.xxxl)
                            )
                        }
                    } else {
                        GlassSection(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                thisWeekShows.forEach { show ->
                                    val isToday = show.releaseDay == todayName
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(CornerRadius.sm))
                                            .background(
                                                if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.40f)
                                            )
                                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = show.releaseDay?.substring(0, 3) ?: "Sun",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        Spacer(modifier = Modifier.width(Spacing.sm))
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
                    }

                    Spacer(modifier = Modifier.height(Spacing.xxxl))
            }
        }
    }
}

@Composable
private fun StatsSectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(14.dp)
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.1).sp,
            color = MaterialTheme.colorScheme.primary
        )
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
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .defaultMinSize(minHeight = 64.dp)
                            .clip(RoundedCornerShape(CornerRadius.card))
                            .colorOSPressBounce(pressedScale = 0.96f)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
                            .border(
                                width = 0.75.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(CornerRadius.card)
                            )
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = item.value,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.subText != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.subText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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