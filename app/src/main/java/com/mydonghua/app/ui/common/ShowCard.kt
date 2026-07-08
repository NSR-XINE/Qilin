package com.mydonghua.app.ui.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mydonghua.app.domain.model.CountdownCalculator
import com.mydonghua.app.domain.model.Show
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.mydonghua.app.ui.LocalBackdrop
import kotlinx.coroutines.delay
import androidx.compose.foundation.isSystemInDarkTheme

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
    val backdrop = LocalBackdrop.current
    val isDark = isSystemInDarkTheme()
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(16.dp) },
                effects = {
                    vibrancy()
                    blur(24.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(
                        color = if (isDark) Color.White.copy(alpha = 0.04f)
                        else Color.Black.copy(alpha = 0.02f)
                    )
                }
            )
            .clickable { onCardClick(show) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            // Poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(show.poster)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = show.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Bottom gradient fade for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.6f)
                            )
                        )
                )

                // Status badge top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = when (show.status) {
                                "completed" -> Color(0xFF2EA043).copy(alpha = 0.85f)
                                "stopped" -> Color(0xFFFF2A5F).copy(alpha = 0.85f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (show.status) {
                            "completed" -> "Completed"
                            "stopped" -> "Hiatus"
                            else -> "Airing"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Title overlay at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = show.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
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

            // Info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Ep ${show.currentEp}/${show.totalEp}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            modifier = Modifier.size(18.dp),
                            tint = if (show.isFavorite == 1) Color(0xFFFF2A5F)
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
                                modifier = Modifier.size(18.dp),
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
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
