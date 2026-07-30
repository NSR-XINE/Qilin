package com.mydonghua.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.EmptyState
import com.mydonghua.app.ui.common.ShowCard
import com.mydonghua.app.ui.common.shimmerEffect
import com.mydonghua.app.ui.theme.CornerRadius

@Composable
fun AiringScreen(
    viewModel: ShowsViewModel,
    onWatchClick: (String) -> Unit,
    onEditClick: (com.mydonghua.app.domain.model.Show) -> Unit,
    onCardClick: (com.mydonghua.app.domain.model.Show) -> Unit,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(0f),
) {
    val shows by viewModel.airingShows.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(top = topPadding + Spacing.sm, bottom = 80.dp)
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
                contentPadding = PaddingValues(top = topPadding + Spacing.sm, bottom = 80.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Airing",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (shows.size == 1) "1 show" else "${shows.size} shows",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(shows, key = { it.id }) { show ->
                    ShowCard(
                        show = show,
                        onWatchClick = onWatchClick,
                        onEditClick = onEditClick,
                        onDeleteClick = { viewModel.deleteShow(it.id) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onNotifyToggle = { viewModel.toggleNotification(context, it) },
                        onEpIncrement = { viewModel.incrementEpisode(it) },
                        onEpDecrement = { viewModel.decrementEpisode(it) },
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
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(CornerRadius.xs))
                .shimmerEffect()
        )
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(CornerRadius.xs))
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
                    .heightIn(min = 64.dp)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(CornerRadius.sm))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}