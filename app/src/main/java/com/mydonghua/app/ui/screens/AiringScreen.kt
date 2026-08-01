package com.mydonghua.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mydonghua.app.domain.model.Show
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
    val onDelete = remember { { s: Show -> viewModel.deleteShow(s.id) } }
    val onFavorite = remember { { s: Show -> viewModel.toggleFavorite(s) } }
    val onNotify = remember { { s: Show -> viewModel.toggleNotification(context, s) } }
    val onIncrement = remember { { s: Show -> viewModel.incrementEpisode(s) } }
    val onDecrement = remember { { s: Show -> viewModel.decrementEpisode(s) } }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(top = topPadding + Spacing.sm, bottom = 80.dp)
            ) {
                HeaderSkeleton()
                repeat(5) { ShowCardSkeleton() }
            }
        } else if (shows.isEmpty()) {
            EmptyState(
                title = "No ongoing shows tracked",
                subtitle = "Add shows that are currently airing to track them here",
                painter = painterResource(id = R.drawable.ic_airing),
                modifier = Modifier.padding(top = topPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(top = topPadding + Spacing.sm, bottom = 80.dp)
            ) {
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

                shows.forEach { show ->
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