package com.nsr.qilin.ui.common

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
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
import com.nsr.qilin.domain.model.Show
import com.nsr.qilin.ui.Spacing
import com.nsr.qilin.ui.rememberLiquidGlassBorder
import com.nsr.qilin.ui.rememberLiquidGlassChipBorder
import com.nsr.qilin.ui.theme.ColorOSMotion
import com.nsr.qilin.ui.theme.CornerRadius
import com.nsr.qilin.ui.theme.JustSansFamily
import com.nsr.qilin.ui.theme.colorOSPressBounce

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

    // Status aesthetics (Official ColorOS 17 Palette)
    val (statusDotColor, statusText) = remember(show.status) {
        when (show.status) {
            "completed" -> Color(0xFF00E676) to "COMPLETED"
            "stopped" -> Color(0xFFFF9100) to "HIATUS"
            else -> (if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB)) to "AIRING"
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

    // Official ColorOS 17 Translucent Liquid Frosted Glass (Matches Reference Screenshots)
    val cardBgBrush = remember(isLight) {
        if (isLight) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.70f),
                    Color(0xFFF0F4FA).copy(alpha = 0.60f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF1E2638).copy(alpha = 0.48f),
                    Color(0xFF121622).copy(alpha = 0.58f)
                )
            )
        }
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
        ColorOSDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "Delete Show",
            message = "Are you sure you want to delete \"${show.title}\"? This cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            confirmButtonColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                showDeleteDialog = false
                onDeleteClick(show)
            },
            onDismiss = { showDeleteDialog = false }
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(cardShape)
            .background(
                brush = cardBgBrush,
                shape = cardShape
            )
            .border(
                width = 0.85.dp,
                brush = cardBorderBrush,
                shape = cardShape
            )
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = { onCardClick(show) }
            )
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
                    contentScale = ContentScale.Crop,
                    sharedElementKey = "show-poster-card-${show.id}",
                    sharedOverlayShape = RoundedCornerShape(
                        topStart = CornerRadius.card,
                        topEnd = CornerRadius.card
                    )
                )

                // Atmospheric cinematic gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = posterOverlayBrush)
                )

                // Top-Left: Status Capsule with Glowing Dot + Optional AniList Indicator
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(
                                width = 0.75.dp,
                                brush = Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
                                ),
                                shape = RoundedCornerShape(percent = 50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.5.dp),
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
                                fontSize = 10.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    if (show.aniListId != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0xFF02A9FF).copy(alpha = 0.85f))
                                .border(
                                    width = 0.75.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.10f))
                                    ),
                                    shape = RoundedCornerShape(percent = 50)
                                )
                                .padding(horizontal = 7.dp, vertical = 4.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AL",
                                fontSize = 9.5.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Top-Right: Schedule / Episodes Capsule (50% Capsule Pill)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(
                            width = 0.75.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
                            ),
                            shape = RoundedCornerShape(percent = 50)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scheduleText,
                        fontSize = 10.5.sp,
                        fontFamily = JustSansFamily,
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
                            fontSize = 17.sp,
                            fontFamily = JustSansFamily,
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
                                fontSize = 17.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Bold,
                                shadow = titleShadow
                            ),
                            highlightColor = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB),
                            highlightWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!show.titleZh.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = show.titleZh,
                            fontSize = 12.sp,
                            fontFamily = JustSansFamily,
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
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Tier 1: Episode Counter + Progress Bar + Quick Episode Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Episode count & progress bar
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 36.dp)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val totalStr = if (show.totalEp > 0) " / ${show.totalEp}" else ""
                        Text(
                            text = "Episode ${show.currentEp}$totalStr",
                            fontSize = 13.5.sp,
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isLight) Color(0xFF0F172A) else Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (show.totalEp > 0) {
                            val progressFraction = (show.currentEp.toFloat() / show.totalEp.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB))
                                )
                            }
                        } else {
                            Text(
                                text = "Ongoing series",
                                fontSize = 11.5.sp,
                                fontFamily = JustSansFamily,
                                color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Right: Tactile Quick Stepper Capsule Dock [ − ] EP X [ + ]
                    val stepperBorder = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isLight) 0.30f else 0.18f),
                            Color.White.copy(alpha = if (isLight) 0.10f else 0.04f)
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                if (isLight) Color.Black.copy(alpha = 0.05f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                width = 0.75.dp,
                                brush = stepperBorder,
                                shape = RoundedCornerShape(percent = 50)
                            )
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                    ) {
                        // Decrement Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isLight) Color.White.copy(alpha = 0.70f)
                                    else Color.White.copy(alpha = 0.10f)
                                )
                                .clickable(enabled = show.currentEp > 0) { onEpDecrement(show) }
                                .colorOSPressBounce(pressedScale = 0.88f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrement episode",
                                modifier = Modifier.size(14.dp),
                                tint = if (show.currentEp > 0) (if (isLight) Color(0xFF0F172A) else Color.White)
                                else Color.White.copy(alpha = 0.28f)
                            )
                        }

                        // Episode badge
                        Text(
                            text = "EP ${show.currentEp}",
                            fontSize = 11.5.sp,
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB),
                            modifier = Modifier.padding(horizontal = 6.dp),
                            textAlign = TextAlign.Center
                        )

                        // Increment Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isLight) Color.White.copy(alpha = 0.70f)
                                    else Color.White.copy(alpha = 0.10f)
                                )
                                .clickable { onEpIncrement(show) }
                                .colorOSPressBounce(pressedScale = 0.88f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increment episode",
                                modifier = Modifier.size(14.dp),
                                tint = if (isLight) Color(0xFF0F172A) else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Faint specular divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(
                            if (isLight) Color.Black.copy(alpha = 0.06f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tier 2: Action Row (ColorOS 17 50% Capsule Action Buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val actionButtonBorder = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isLight) 0.30f else 0.18f),
                            Color.White.copy(alpha = if (isLight) 0.10f else 0.04f)
                        )
                    )
                    val actionButtonBg = if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f)

                    // Left Hero Action: Signature ColorOS 17 50% Capsule Watch Pill
                    if (!show.watchUrl.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(if (isLight) Color(0xFF0072E5) else Color(0xFF0082FB))
                                .clickable { onWatchClick(show.watchUrl ?: "") }
                                .colorOSPressBounce(pressedScale = 0.94f)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Watch",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Text(
                                    text = "Watch",
                                    fontSize = 13.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Clean Details pill
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(actionButtonBg)
                                .border(0.75.dp, actionButtonBorder, RoundedCornerShape(percent = 50))
                                .clickable { onCardClick(show) }
                                .colorOSPressBounce(pressedScale = 0.94f)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "Details",
                                    fontSize = 12.5.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLight) Color(0xFF0F172A) else Color.White
                                )
                            }
                        }
                    }

                    // Action Circular Buttons (Favorite, Notify, Edit, Delete)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorite Heart Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (show.isFavorite == 1) Color(0xFFFF2D55).copy(alpha = if (isLight) 0.15f else 0.22f)
                                    else actionButtonBg
                                )
                                .border(
                                    0.75.dp,
                                    if (show.isFavorite == 1) Brush.verticalGradient(listOf(Color(0xFFFF2D55).copy(0.40f), Color(0xFFFF2D55).copy(0.15f)))
                                    else actionButtonBorder,
                                    CircleShape
                                )
                                .clickable { onFavoriteToggle(show) }
                                .colorOSPressBounce(pressedScale = 0.88f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (show.isFavorite == 1) Icons.Outlined.Favorite
                                else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                modifier = Modifier.size(17.dp),
                                tint = if (show.isFavorite == 1) Color(0xFFFF2D55)
                                else (if (isLight) Color(0xFF64748B) else Color(0xFFA0A6B5))
                            )
                        }

                        // Notification Bell Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (show.alarmRequestCode != null) Color(0xFF0082FB).copy(alpha = if (isLight) 0.15f else 0.22f)
                                    else actionButtonBg
                                )
                                .border(
                                    0.75.dp,
                                    if (show.alarmRequestCode != null) Brush.verticalGradient(listOf(Color(0xFF0082FB).copy(0.40f), Color(0xFF0082FB).copy(0.15f)))
                                    else actionButtonBorder,
                                    CircleShape
                                )
                                .clickable { onNotifyToggle(show) }
                                .colorOSPressBounce(pressedScale = 0.88f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (show.alarmRequestCode != null) Icons.Outlined.Notifications
                                else Icons.Outlined.NotificationsNone,
                                contentDescription = "Notifications",
                                modifier = Modifier.size(17.dp),
                                tint = if (show.alarmRequestCode != null) Color(0xFF0082FB)
                                else (if (isLight) Color(0xFF64748B) else Color(0xFFA0A6B5))
                            )
                        }

                        // Edit Pencil Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(actionButtonBg)
                                .border(0.75.dp, actionButtonBorder, CircleShape)
                                .clickable { onEditClick(show) }
                                .colorOSPressBounce(pressedScale = 0.88f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(17.dp),
                                tint = if (isLight) Color(0xFF64748B) else Color(0xFFA0A6B5)
                            )
                        }

                        // Delete Trash Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30).copy(alpha = if (isLight) 0.08f else 0.12f))
                                .border(
                                    0.75.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFFF3B30).copy(0.25f),
                                            Color(0xFFFF3B30).copy(0.08f)
                                        )
                                    ),
                                    CircleShape
                                )
                                .clickable { showDeleteDialog = true }
                                .colorOSPressBounce(pressedScale = 0.88f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(17.dp),
                                tint = Color(0xFFFF3B30).copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}
