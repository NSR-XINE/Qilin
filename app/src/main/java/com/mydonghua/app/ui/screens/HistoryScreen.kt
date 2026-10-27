package com.mydonghua.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.mydonghua.app.ui.rememberLiquidGlassBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.R
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.GlassHeader
import com.mydonghua.app.ui.common.DonghuaPoster
import com.mydonghua.app.ui.common.EmptyState
import com.mydonghua.app.ui.common.shimmerEffect
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.JustSansFamily
import com.mydonghua.app.ui.theme.colorOSPressBounce
import java.text.SimpleDateFormat
import java.util.*

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
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val glassBorder = rememberLiquidGlassBorder(isLight)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerTopPadding = statusBarTop + 84.dp
    val screenBackdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(screenBackdrop)
        ) {
            if (isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                contentPadding = PaddingValues(top = headerTopPadding, bottom = 96.dp)
            ) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CornerRadius.card))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(Spacing.md)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(CornerRadius.sm))
                                    .shimmerEffect()
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(CornerRadius.xs))
                                        .shimmerEffect()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.35f)
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
                title = "No Watch History Yet",
                subtitle = "Episodes you watch and update will be logged here automatically.",
                painter = painterResource(id = R.drawable.ic_history),
                modifier = Modifier.padding(top = headerTopPadding).padding(horizontal = Spacing.lg, vertical = 40.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = headerTopPadding, bottom = 96.dp)
            ) {
                groupedHistory.forEach { (dateHeader, items) ->
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0082FB))
                            )
                            Text(
                                text = dateHeader.uppercase(),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0082FB),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    itemsIndexed(items, key = { index, item -> "${item.showId}_${item.watchedAt}_$index" }) { _, item ->
                        val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                        val formattedTime = remember(item.watchedAt) {
                            timeFormat.format(Date(item.watchedAt))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CornerRadius.card))
                                .background(
                                    if (isLight) Color.White.copy(alpha = 0.65f)
                                    else Color(0xFF1E2638).copy(alpha = 0.48f)
                                )
                                .border(
                                    width = 0.85.dp,
                                    brush = glassBorder,
                                    shape = RoundedCornerShape(CornerRadius.card)
                                )
                                .colorOSPressBounce(
                                    pressedScale = 0.98f,
                                    onClick = { onShowClick(item.showId) }
                                )
                                .padding(Spacing.sm + 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DonghuaPoster(
                                        posterUrl = item.poster,
                                        title = item.showTitle,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        fontSize = 11.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.showTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Episode ${item.episodeNum}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = JustSansFamily,
                                            color = Color(0xFF0082FB)
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formattedTime,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    GlassHeader(
            title = "Watch History",
            subtitle = "${historyList.size} logged episodes",
            backdrop = screenBackdrop,
            isDark = !isLight,
            onBackClick = onNavigateBack
        )
    }
}

private fun getRelativeDateGroup(timestamp: Long): String {
    val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val itemDate = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val diffMillis = now.timeInMillis - itemDate.timeInMillis
    val diffDays = (diffMillis / (1000L * 60 * 60 * 24)).toInt()

    return when {
        diffDays <= 0 -> "Today"
        diffDays == 1 -> "Yesterday"
        diffDays in 2..6 -> "${diffDays} days ago"
        now.get(Calendar.YEAR) == itemDate.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
