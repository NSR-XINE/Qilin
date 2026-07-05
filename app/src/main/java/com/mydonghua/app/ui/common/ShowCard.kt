package com.mydonghua.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mydonghua.app.domain.model.CountdownCalculator
import com.mydonghua.app.domain.model.CountdownTime
import com.mydonghua.app.domain.model.Show
import kotlinx.coroutines.delay

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
    var countdownTime by remember(show.id) {
        mutableStateOf(CountdownCalculator.calculateTimeRemaining(CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime)))
    }

    LaunchedEffect(show.releaseDay, show.releaseTime) {
        val targetMs = CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime)
        while (true) {
            countdownTime = CountdownCalculator.calculateTimeRemaining(targetMs)
            delay(1000)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onCardClick(show) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Poster Image
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 135.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
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
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title and Chinese Title
                Text(
                    text = show.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!show.titleZh.isNullOrEmpty()) {
                    Text(
                        text = show.titleZh,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Schedule release text or Complete State
                if (show.status == "completed") {
                    Text(
                        text = "✅ Completed Series",
                        fontSize = 11.sp,
                        color = Color(0xFF2EA043),
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    val timeStr = if (!show.releaseTime.isNullOrEmpty()) " at ${show.releaseTime}" else ""
                    Text(
                        text = "📅 ${show.releaseDay ?: "Sunday"}$timeStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Indicator & Episode controller
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        val totalStr = if (show.totalEp > 0) "/${show.totalEp}" else ""
                        Text(
                            text = "Episode: ${show.currentEp}$totalStr",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (show.totalEp > 0) {
                            val ratio = show.currentEp.toFloat() / show.totalEp.toFloat()
                            LinearProgressIndicator(
                                progress = ratio.coerceIn(0f, 1f),
                                modifier = Modifier
                                    .width(80.dp)
                                    .padding(top = 4.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        }
                    }

                    // Increment & Decrement Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onEpDecrement(show) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        ) {
                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }

                        IconButton(
                            onClick = { onEpIncrement(show) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        ) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Countdown Ticker
                if (show.status == "ongoing") {
                    if (countdownTime.airingNow) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1A00F2FE), RoundedCornerShape(6.dp))
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🚀 AIRING NOW / RELEASED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00F2FE)
                            )
                        }
                    } else if (!countdownTime.elapsed) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CountdownBox(value = countdownTime.days, label = "D")
                            CountdownBox(value = countdownTime.hours, label = "H")
                            CountdownBox(value = countdownTime.minutes, label = "M")
                            CountdownBox(value = countdownTime.seconds, label = "S")
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

        // Toolbars and Watch Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Watch Button
            Button(
                onClick = { onWatchClick(show.watchUrl ?: "") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📺 Watch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Toolbar Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = { onNotifyToggle(show) }, modifier = Modifier.size(32.dp)) {
                    Text(if (show.alarmRequestCode != null && show.alarmRequestCode!! > 0) "🔔" else "🔕", fontSize = 14.sp)
                }
                IconButton(onClick = { onEditClick(show) }, modifier = Modifier.size(32.dp)) {
                    Text("✏️", fontSize = 14.sp)
                }
                IconButton(onClick = { onFavoriteToggle(show) }, modifier = Modifier.size(32.dp)) {
                    Text(if (show.isFavorite == 1) "❤️" else "🖤", fontSize = 14.sp)
                }
                IconButton(onClick = { onDeleteClick(show) }, modifier = Modifier.size(32.dp)) {
                    Text("🗑️", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun RowScope.CountdownBox(value: Long, label: String) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
