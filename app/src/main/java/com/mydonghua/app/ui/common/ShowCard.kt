package com.mydonghua.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.rememberLiquidGlassBorder
import com.mydonghua.app.ui.rememberLiquidGlassChipBorder
import com.mydonghua.app.ui.theme.ColorOSMotion
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.colorOSPressBounce
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
    modifier: Modifier = Modifier,
    highlightQuery: String? = null
) {
    val context = LocalContext.current
    val hazeState = LocalHazeState.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    // Status aesthetics
    val (statusDotColor, statusText) = remember(show.status) {
        when (show.status) {
            "completed" -> Color(0xFF00E676) to "COMPLETED"
            "stopped" -> Color(0xFFFF9100) to "HIATUS"
            else -> (if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF)) to "AIRING"
        }
    }

    val scheduleText = remember(show.status, show.releaseDay, show.releaseTime, show.currentEp) {
        if (show.status == "completed") {
            "${show.currentEp} EPS"
        } else {
            val day = show.releaseDay?.take(3) ?: "Sun"
            val time = show.releaseTime ?: "10:00"
            "$day • $time"
        }
    }

    // Unified liquid glass border across all boxes
    val cardBorderBrush = rememberLiquidGlassBorder(isLight)
    val chipBorderColor = rememberLiquidGlassChipBorder(isLight)

    val cardBgColor = if (isLight) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    }

    // Multi-stop liquid gradient overlay for cinematic poster readability
    val posterOverlayBrush = remember {
        Brush.verticalGradient(
            0.0f to Color.Black.copy(alpha = 0.40f),
            0.25f to Color.Transparent,
            0.50f to Color.Transparent,
            0.75f to Color.Black.copy(alpha = 0.60f),
            1.0f to Color.Black.copy(alpha = 0.92f)
        )
    }

    val titleShadow = remember {
        Shadow(
            color = Color.Black.copy(alpha = 0.70f),
            offset = Offset(0f, 1.5f),
            blurRadius = 4f
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(CornerRadius.dialog),
            title = {
                Text(
                    text = "Delete Show",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${show.title}\"? This cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick(show)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    val cardShape = RoundedCornerShape(CornerRadius.card)
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.98f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "showCardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .hazeChild(state = hazeState, shape = cardShape)
            .background(
                color = cardBgColor,
                shape = cardShape
            )
            .border(
                width = 1.dp,
                brush = cardBorderBrush,
                shape = cardShape
            )
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = { onCardClick(show) }
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            // Hero Poster Header (16:9 Aspect Ratio)
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

                // Atmospheric cinematic gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = posterOverlayBrush)
                )

                // Top-Left: Status Capsule with Glowing Dot
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(CornerRadius.chip))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(CornerRadius.chip))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Text(
                            text = statusText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                // Top-Right: Schedule / Episodes Capsule
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(CornerRadius.chip))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(CornerRadius.chip))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scheduleText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f),
                        letterSpacing = 0.2.sp
                    )
                }

                // Title & Subtitle inside bottom vignette
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (highlightQuery.isNullOrBlank()) {
                        Text(
                            text = show.title,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(shadow = titleShadow)
                        )
                    } else {
                        HighlightedText(
                            text = show.title,
                            query = highlightQuery,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                shadow = titleShadow
                            ),
                            highlightColor = if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF),
                            highlightWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!show.titleZh.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = show.titleZh,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.80f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Card Bottom Controls & Metadata Dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Tier 1: Episode Counter + Progress Bar + Quick Episode Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Episode count & dual-color gradient progress bar
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 34.dp)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val totalStr = if (show.totalEp > 0) " / ${show.totalEp}" else ""
                        Text(
                            text = "Episode ${show.currentEp}$totalStr",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        if (show.totalEp > 0) {
                            val progressFraction = (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLight) 0.10f else 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                if (isLight) {
                                                    listOf(Color(0xFF009AA6), Color(0xFF0072FF))
                                                } else {
                                                    listOf(Color(0xFF00E5FF), Color(0xFF0072FF))
                                                }
                                            )
                                        )
                                )
                            }
                        } else {
                            Text(
                                text = "Ongoing series",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Right: Tactile Quick Stepper Dock [ − ] EP X [ + ] (Liquid glass border)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isLight) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                            .border(
                                width = 0.5.dp,
                                color = chipBorderColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(3.dp)
                    ) {
                        // Decrement Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (isLight) Color.Black.copy(alpha = 0.05f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = chipBorderColor,
                                    shape = RoundedCornerShape(7.dp)
                                )
                                .clickable(enabled = show.currentEp > 0) { onEpDecrement(show) }
                                .colorOSPressBounce(pressedScale = 0.90f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrement episode",
                                modifier = Modifier.size(15.dp),
                                tint = if (show.currentEp > 0) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                            )
                        }

                        // Episode badge
                        Text(
                            text = "EP ${show.currentEp}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF),
                            modifier = Modifier.padding(horizontal = 6.dp),
                            textAlign = TextAlign.Center
                        )

                        // Increment Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (isLight) Color.Black.copy(alpha = 0.05f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = chipBorderColor,
                                    shape = RoundedCornerShape(7.dp)
                                )
                                .clickable { onEpIncrement(show) }
                                .colorOSPressBounce(pressedScale = 0.90f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increment episode",
                                modifier = Modifier.size(15.dp),
                                tint = if (isLight) MaterialTheme.colorScheme.onSurface else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Faint divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(
                            if (isLight) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tier 2: Action Chips Row (Same Liquid Glass Border Across All Chips)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val chipBgColor = if (isLight) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

                    // Left Hero Action: Watch pill if URL exists, or Details pill if no direct stream URL
                    if (!show.watchUrl.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(chipBgColor)
                                .border(0.5.dp, chipBorderColor, RoundedCornerShape(10.dp))
                                .clickable { onWatchClick(show.watchUrl ?: "") }
                                .colorOSPressBounce(pressedScale = 0.92f)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayCircle,
                                    contentDescription = "Watch",
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF)
                                )
                                Text(
                                    text = "Watch",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLight) Color(0xFF009AA6) else Color.White
                                )
                            }
                        }
                    } else {
                        // Clean Details pill
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(chipBgColor)
                                .border(0.5.dp, chipBorderColor, RoundedCornerShape(10.dp))
                                .clickable { onCardClick(show) }
                                .colorOSPressBounce(pressedScale = 0.92f)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Details",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Action Squircle Chips (Favorite, Notify, Edit, Delete) - exact same liquid glass border
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorite Heart Chip
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (show.isFavorite == 1) Color(0xFFFF2D55).copy(alpha = if (isLight) 0.12f else 0.20f)
                                    else chipBgColor
                                )
                                .border(0.5.dp, chipBorderColor, RoundedCornerShape(10.dp))
                                .clickable { onFavoriteToggle(show) }
                                .colorOSPressBounce(pressedScale = 0.88f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (show.isFavorite == 1) Icons.Outlined.Favorite
                                else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                modifier = Modifier.size(16.dp),
                                tint = if (show.isFavorite == 1) Color(0xFFFF2D55)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Notification Bell Chip
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (show.alarmRequestCode != null) (if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF)).copy(alpha = if (isLight) 0.12f else 0.20f)
                                    else chipBgColor
                                )
                                .border(0.5.dp, chipBorderColor, RoundedCornerShape(10.dp))
                                .clickable { onNotifyToggle(show) }
                                .colorOSPressBounce(pressedScale = 0.88f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (show.alarmRequestCode != null) Icons.Outlined.Notifications
                                else Icons.Outlined.NotificationsNone,
                                contentDescription = "Notifications",
                                modifier = Modifier.size(16.dp),
                                tint = if (show.alarmRequestCode != null) (if (isLight) Color(0xFF009AA6) else Color(0xFF00E5FF))
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Edit Pencil Chip
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(chipBgColor)
                                .border(0.5.dp, chipBorderColor, RoundedCornerShape(10.dp))
                                .clickable { onEditClick(show) }
                                .colorOSPressBounce(pressedScale = 0.88f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Delete Trash Chip
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = if (isLight) 0.08f else 0.12f))
                                .border(0.5.dp, chipBorderColor, RoundedCornerShape(10.dp))
                                .clickable { showDeleteDialog = true }
                                .colorOSPressBounce(pressedScale = 0.88f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
