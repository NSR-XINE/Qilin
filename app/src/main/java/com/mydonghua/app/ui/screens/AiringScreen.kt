package com.mydonghua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.R
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.rememberLiquidGlassChipBorder
import com.mydonghua.app.ui.common.EmptyState
import com.mydonghua.app.ui.common.ScrollPerfMonitor
import com.mydonghua.app.ui.common.ShowCard
import com.mydonghua.app.ui.common.shimmerEffect
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.colorOSPressBounce

@Composable
fun AiringScreen(
    viewModel: ShowsViewModel,
    onWatchClick: (String) -> Unit,
    onEditClick: (Show) -> Unit,
    onCardClick: (Show) -> Unit,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val shows by viewModel.airingShows.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val context = LocalContext.current
    val onDelete = remember { { s: Show -> viewModel.deleteShow(s.id) } }
    val onFavorite = remember { { s: Show -> viewModel.toggleFavorite(s) } }
    val onNotify = remember { { s: Show -> viewModel.toggleNotification(context, s) } }
    val onIncrement = remember { { s: Show -> viewModel.incrementEpisode(s) } }
    val onDecrement = remember { { s: Show -> viewModel.decrementEpisode(s) } }
    val listState = rememberLazyListState()
    ScrollPerfMonitor(listState, "airing")

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                state = listState,
                contentPadding = PaddingValues(top = topPadding + Spacing.sm, bottom = 96.dp)
            ) {
                item { HeaderSkeleton() }
                items(5) { ShowCardSkeleton() }
            }
        } else if (shows.isEmpty()) {
            EmptyState(
                title = "No ongoing shows tracked",
                subtitle = "Add shows that are currently airing to track them here",
                painter = painterResource(id = R.drawable.ic_airing),
                modifier = Modifier.padding(top = topPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                state = listState,
                contentPadding = PaddingValues(top = topPadding + Spacing.sm, bottom = 96.dp)
            ) {
                item(key = "airing_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            if (isLight) {
                                                listOf(Color(0xFF009AA6), Color(0xFF0072FF))
                                            } else {
                                                listOf(Color(0xFF00E5FF), Color(0xFF0072FF))
                                            }
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Airing Schedule",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Frosted pill show counter badge (adaptive to light/dark mode)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(CornerRadius.chip))
                                .colorOSPressBounce(pressedScale = 0.94f)
                                .background(
                                    if (isLight) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = rememberLiquidGlassChipBorder(isLight),
                                    shape = RoundedCornerShape(CornerRadius.chip)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (shows.size == 1) "1 show" else "${shows.size} shows",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF)
                            )
                        }
                    }
                }

                items(shows, key = { it.id }) { show ->
                    ShowCard(
                        show = show,
                        onWatchClick = onWatchClick,
                        onEditClick = onEditClick,
                        onDeleteClick = onDelete,
                        onFavoriteToggle = onFavorite,
                        onNotifyToggle = onNotify,
                        onEpIncrement = onIncrement,
                        onEpDecrement = onDecrement,
                        onCardClick = onCardClick
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(CornerRadius.xs))
                .shimmerEffect()
        )
        Box(
            modifier = Modifier
                .width(58.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(CornerRadius.chip))
                .shimmerEffect()
        )
    }
}

@Composable
private fun ShowCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        shape = RoundedCornerShape(CornerRadius.card),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = CornerRadius.card, topEnd = CornerRadius.card))
                    .shimmerEffect()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(CornerRadius.xs))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(CornerRadius.xs))
                            .shimmerEffect()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}
