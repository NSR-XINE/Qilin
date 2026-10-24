package com.mydonghua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.GlassSection
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.EmptyState
import com.mydonghua.app.ui.common.shimmerEffect
import com.mydonghua.app.ui.theme.ButtonHeight
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.colorOSPressBounce
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    showsViewModel: ShowsViewModel,
    onShowClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collectionsMap by showsViewModel.collections.collectAsState()
    val isLoading by showsViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val sortedCollections = remember(collectionsMap) {
        collectionsMap.entries.sortedBy { it.key.lowercase() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Collections folders", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Spacing.lg)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                contentPadding = PaddingValues(top = Spacing.md, bottom = Spacing.sm)
            ) {
                items(3) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CornerRadius.sm))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(CornerRadius.xs))
                                    .shimmerEffect()
                            )
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(CornerRadius.xs))
                                    .shimmerEffect()
                            )
                        }
                        repeat(2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(CornerRadius.sm))
                                    .padding(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(CornerRadius.sm))
                                        .shimmerEffect()
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(12.dp)
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
                    }
                }
            }
        } else if (sortedCollections.isEmpty()) {
            EmptyState(
                title = "No collections created yet",
                subtitle = "Edit a show and set its Collection field to start grouping them.",
                painter = painterResource(id = R.drawable.ic_collections),
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Spacing.lg)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                contentPadding = PaddingValues(top = Spacing.md, bottom = Spacing.sm)
            ) {
                items(sortedCollections, key = { it.key }) { (collectionName, showsList) ->
                    GlassSection(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Text(
                                        text = collectionName,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(CornerRadius.pill))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                                ) {
                                    Text(
                                        text = "${showsList.size} shows",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                showsList.forEach { show ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(CornerRadius.sm))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                                            .colorOSPressBounce(pressedScale = 0.98f)
                                            .clickable { onShowClick(show.id) }
                                            .padding(Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(CornerRadius.sm))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            com.mydonghua.app.ui.common.DonghuaPoster(
                                                posterUrl = show.poster,
                                                title = show.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                fontSize = 11.sp
                                            )
                                        }

                                    Text(
                                        text = show.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )

                                    val statusColor = when (show.status) {
                                        "ongoing" -> MaterialTheme.colorScheme.primary
                                        "completed" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(CornerRadius.sm))
                                            .background(statusColor.copy(alpha = 0.15f))
                                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                                    ) {
                                        Text(
                                            text = show.status.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = statusColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}