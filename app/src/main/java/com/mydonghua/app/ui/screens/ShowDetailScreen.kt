package com.mydonghua.app.ui.screens

import androidx.compose.foundation.background

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.GlassSection
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.mydonghua.app.ui.LocalBackdrop
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    showId: String,
    viewModel: ShowsViewModel,
    onWatchClick: (String) -> Unit,
    onEditClick: (Show) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allShows by viewModel.allShows.collectAsState()
    val show = remember(showId, allShows) { allShows.find { it.id == showId } }

    if (show == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(show) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(show.poster)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = show.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.3f to Color.Transparent,
                                1.0f to MaterialTheme.colorScheme.background.copy(alpha = 1f)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = show.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!show.titleZh.isNullOrEmpty()) {
                        Text(
                            text = show.titleZh,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status chips with glass
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassChip(
                        label = "Status",
                        value = when (show.status) {
                            "ongoing" -> "Airing"
                            "completed" -> "Completed"
                            else -> "Hiatus"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GlassChip(
                        label = "Language",
                        value = show.language ?: "CN",
                        modifier = Modifier.weight(1f)
                    )
                    if (show.totalEp > 0) {
                        GlassChip(
                            label = "Episodes",
                            value = "${show.totalEp}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Countdown
                if (show.status == "ongoing") {
                    GlassSection {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Next Episode",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (countdownTime.airingNow) "Available now"
                                    else "Ep ${show.currentEp + 1} • ${show.releaseDay}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (!countdownTime.airingNow && !countdownTime.elapsed) {
                                Text(
                                    text = "${countdownTime.days}d ${countdownTime.hours}h ${countdownTime.minutes}m",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                // Progress
                GlassSection {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Progress",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val totalStr = if (show.totalEp > 0) " / ${show.totalEp}" else ""
                            Text(
                                text = "Episode ${show.currentEp}$totalStr",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { viewModel.decrementEpisode(show) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(
                                onClick = { viewModel.incrementEpisode(show) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // Synopsis
                if (!show.notes.isNullOrEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Synopsis", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = show.notes!!,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        )
                    }
                }

                // Meta
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!show.studio.isNullOrEmpty()) Meta(label = "Studio", value = show.studio!!)
                    if (show.releaseYear != null && show.releaseYear!! > 0) Meta(label = "Year", value = show.releaseYear.toString())
                    if (!show.genres.isNullOrEmpty()) Meta(label = "Genres", value = show.genres!!)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onWatchClick(show.watchUrl ?: "") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tv),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Watch Online", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun GlassChip(label: String, value: String, modifier: Modifier = Modifier) {
    val backdrop = LocalBackdrop.current
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(10.dp) },
                effects = {
                    vibrancy()
                    blur(24.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.02f),
                                surfaceColor.copy(alpha = 0.15f)
                            ) else listOf(
                                Color.Black.copy(alpha = 0.06f),
                                Color.Black.copy(alpha = 0.02f),
                                Color.Black.copy(alpha = 0.08f)
                            )
                        )
                    )
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Meta(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("$label:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
