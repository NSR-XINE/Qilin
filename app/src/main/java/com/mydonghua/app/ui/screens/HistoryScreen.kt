package com.mydonghua.app.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.EmptyState
import com.mydonghua.app.ui.common.shimmerEffect
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    showsViewModel: ShowsViewModel,
    onShowClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val historyList by showsViewModel.history.collectAsState()
    val isLoading by showsViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val groupedHistory = remember(historyList) {
        historyList.groupBy { item ->
            getRelativeDateGroup(item.watchedAt)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Watch History Log", fontWeight = FontWeight.SemiBold) },
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
        if (isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Spacing.lg)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                contentPadding = PaddingValues(bottom = Spacing.sm)
            ) {
                items(2) { groupIndex ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(CornerRadius.xs))
                                .shimmerEffect()
                        )
                        repeat(2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(CornerRadius.sm))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(CornerRadius.sm))
                                        .shimmerEffect()
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(CornerRadius.xs))
                                            .shimmerEffect()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.45f)
                                            .height(11.dp)
                                            .clip(RoundedCornerShape(CornerRadius.xs))
                                            .shimmerEffect()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(CornerRadius.xs))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }
            }
        } else if (historyList.isEmpty()) {
            EmptyState(
                title = "No watch history recorded yet",
                subtitle = "Track your episodes to start building your history",
                painter = painterResource(id = R.drawable.ic_history),
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Spacing.lg)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                contentPadding = PaddingValues(bottom = Spacing.sm)
            ) {
                groupedHistory.forEach { (dateHeader, items) ->
                    item {
                        Text(
                            text = dateHeader,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = Spacing.sm)
                        )
                    }

                    items(items, key = { "${it.showId}_${it.watchedAt}" }) { item ->
                        val onItemClick = remember(item.showId) { { onShowClick(item.showId) } }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CornerRadius.sm))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(CornerRadius.sm)
                                )
                                .clickable(onClick = onItemClick)
                                .padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(CornerRadius.sm))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                com.mydonghua.app.ui.common.DonghuaPoster(
                                    posterUrl = item.poster,
                                    title = item.showTitle,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    fontSize = 13.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.showTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Watched Episode ${item.episodeNum}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = formatTimeOfDay(item.watchedAt),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getRelativeDateGroup(timestamp: Long): String {
    return when {
        DateUtils.isToday(timestamp) -> "Today"
        DateUtils.isToday(timestamp + 86400000L) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatTimeOfDay(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}