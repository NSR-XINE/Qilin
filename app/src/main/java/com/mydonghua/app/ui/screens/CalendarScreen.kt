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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.shimmerEffect
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R
import com.mydonghua.app.ui.common.EmptyState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    showsViewModel: ShowsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shows by showsViewModel.allShows.collectAsState()
    val isLoading by showsViewModel.isLoading.collectAsState()
    val ongoingShows = remember(shows) { shows.filter { it.status == "ongoing" } }
    val scrollState = rememberScrollState()

    val now = Calendar.getInstance()
    val monthName = now.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    val yearName = now.get(Calendar.YEAR).toString()

    val weekDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    val upcomingWeekList = remember(ongoingShows) {
        val list = mutableListOf<UpcomingDayGroup>()
        for (i in 0 until 7) {
            val d = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            val dayName = weekDays[d.get(Calendar.DAY_OF_WEEK) - 1]
            val dayShows = ongoingShows.filter { it.releaseDay == dayName }
            if (dayShows.isNotEmpty()) {
                list.add(UpcomingDayGroup(dayName, d.time, dayShows))
            }
        }
        list
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("$monthName $yearName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.lg)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    repeat(3) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CornerRadius.sm))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(CornerRadius.xs))
                                    .shimmerEffect()
                            )
                            repeat(2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(13.dp)
                                            .clip(RoundedCornerShape(CornerRadius.xs))
                                            .shimmerEffect()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(CornerRadius.xs))
                                            .shimmerEffect()
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (upcomingWeekList.isEmpty()) {
                EmptyState(
                    title = "No shows scheduled for release this week",
                    subtitle = "Add shows that are currently airing with release days set",
                    painter = painterResource(id = R.drawable.ic_calendar)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    upcomingWeekList.forEach { group ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CornerRadius.sm))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(CornerRadius.sm)
                                )
                                .padding(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Text(
                                text = group.dayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            group.shows.forEach { show ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

data class UpcomingDayGroup(
    val dayName: String,
    val date: Date,
    val shows: List<Show>
)