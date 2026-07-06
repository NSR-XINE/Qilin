package com.mydonghua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.mydonghua.app.ui.common.ShowCard

import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun AiringScreen(
    viewModel: ShowsViewModel,
    onWatchClick: (String) -> Unit,
    onEditClick: (com.mydonghua.app.domain.model.Show) -> Unit,
    onCardClick: (com.mydonghua.app.domain.model.Show) -> Unit,
    modifier: Modifier = Modifier
) {
    val shows by viewModel.airingShows.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier
    ) { paddingValues ->
        if (shows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No ongoing shows tracked",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Airing",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val showCountText = if (shows.size == 1) "1 show" else "${shows.size} shows"
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = showCountText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
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
