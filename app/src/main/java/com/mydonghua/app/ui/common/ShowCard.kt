package com.mydonghua.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import dev.chrisbanes.haze.hazeChild

@Composable
fun ShowCard(
    show: Show,
    onWatchClick: (String) -> Unit,
    onEditClick: (Show) -> Unit,
    onDeleteClick: (Show) -> Unit,
    onFavoriteToggle: (Show) -> Unit,
    onNotifyToggle: (Show) -> Unit,
    onEpIncrement: (Show) -> Unit,
    onEpDecrement: (Show) -> Unit,
    onCardClick: (Show) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hazeState = LocalHazeState.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val overlayGradient = remember {
        Brush.verticalGradient(
            0.5f to Color.Transparent,
            1.0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
        )
    }
    val statusColor = remember(show.status) {
        when (show.status) {
            "completed" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
            "stopped" -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        }
    }
    val statusText = remember(show.status) {
        when (show.status) {
            "completed" -> "Completed"
            "stopped" -> "Hiatus"
            else -> "Airing"
        }
    }
    val transparentCardColors = remember {
        CardDefaults.cardColors(containerColor = Color.Transparent)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Show") },
            text = { Text("Are you sure you want to delete \"${show.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteClick(show)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            .hazeChild(state = hazeState, shape = RoundedCornerShape(CornerRadius.card))
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.04f),
                shape = RoundedCornerShape(CornerRadius.card)
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(CornerRadius.card)
            )
            .clickable { onCardClick(show) },
        shape = RoundedCornerShape(CornerRadius.card),
        colors = transparentCardColors
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = CornerRadius.card, topEnd = CornerRadius.card))
            ) {
                DonghuaPoster(
                    posterUrl = show.poster,
                    title = show.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = overlayGradient)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm)
                        .background(
                            color = statusColor,
                            shape = RoundedCornerShape(CornerRadius.sm)
                        )
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.md)
                ) {
                    Text(
                        text = show.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!show.titleZh.isNullOrEmpty()) {
                        Text(
                            text = show.titleZh,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val scheduleText = if (show.status == "completed") {
                        "${show.currentEp} episodes"
                    } else {
                        "${show.releaseDay ?: "Sunday"} • ${show.releaseTime ?: "10:00"}"
                    }
                    Text(
                        text = scheduleText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    if (show.totalEp > 0) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        LinearProgressIndicator(
                            progress = { (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(CornerRadius.xs)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Ep ${show.currentEp}/${show.totalEp}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    IconButton(
                        onClick = { onEpDecrement(show) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "Decrement Episode",
                            modifier = Modifier.size(IconSize.sm),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onEpIncrement(show) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Increment Episode",
                            modifier = Modifier.size(IconSize.sm),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onFavoriteToggle(show) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (show.isFavorite == 1) R.drawable.ic_heart_filled
                                else R.drawable.ic_heart_outline
                            ),
                            contentDescription = "Favorite",
                            modifier = Modifier.size(IconSize.sm),
                            tint = if (show.isFavorite == 1) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!show.watchUrl.isNullOrEmpty()) {
                        IconButton(
                            onClick = { onWatchClick(show.watchUrl ?: "") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_circle_play),
                                contentDescription = "Watch",
                                modifier = Modifier.size(IconSize.sm),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { onEditClick(show) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Edit",
                            modifier = Modifier.size(IconSize.xs),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}