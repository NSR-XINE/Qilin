package com.mydonghua.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.ui.rememberLiquidGlassBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.R
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.DonghuaPoster
import com.mydonghua.app.ui.common.EmptyState
import com.mydonghua.app.ui.common.shimmerEffect
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.colorOSPressBounce

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
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val glassBorder = rememberLiquidGlassBorder(isLight)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ColorOS 17 Floating Liquid Glass Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Spacing.md, end = Spacing.md, top = 8.dp, bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CornerRadius.surface))
                    .background(
                        if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
                            Color.White.copy(alpha = 0.85f)
                        else Color(0xFF0F121C).copy(alpha = 0.88f)
                    )
                    .border(
                        width = 0.65.dp,
                        brush = glassBorder,
                        shape = RoundedCornerShape(CornerRadius.surface)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .colorOSPressBounce(pressedScale = 0.90f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Collections",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${sortedCollections.size} folder groups",
                            fontSize = 11.5.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                contentPadding = PaddingValues(top = Spacing.sm, bottom = 96.dp)
            ) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CornerRadius.card))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(Spacing.md)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(CornerRadius.xs))
                                    .shimmerEffect()
                            )
                            repeat(2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(CornerRadius.sm))
                                            .shimmerEffect()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(CornerRadius.xs))
                                            .shimmerEffect()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (sortedCollections.isEmpty()) {
            EmptyState(
                title = "No Collections Created Yet",
                subtitle = "Edit any show and set its Collection name to organize your donghua into custom folders.",
                painter = painterResource(id = R.drawable.ic_collections),
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 40.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                contentPadding = PaddingValues(top = Spacing.sm, bottom = 96.dp)
            ) {
                items(sortedCollections, key = { it.key }) { (collectionName, showsList) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CornerRadius.card))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(
                                width = 0.65.dp,
                                brush = glassBorder,
                                shape = RoundedCornerShape(CornerRadius.card)
                            )
                            .padding(Spacing.md)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Folder Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Folder,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    Text(
                                        text = collectionName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${showsList.size} shows",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Shows List in this Collection
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                showsList.forEach { show ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(CornerRadius.md))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.40f))
                                            .border(
                                                width = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(CornerRadius.md)
                                            )
                                            .colorOSPressBounce(
                                                pressedScale = 0.98f,
                                                onClick = { onShowClick(show.id) }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 38.dp, height = 48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            DonghuaPoster(
                                                posterUrl = show.poster,
                                                title = show.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = show.title,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val totalStr = if (show.totalEp > 0) "/${show.totalEp}" else ""
                                            Text(
                                                text = "Episode ${show.currentEp}$totalStr",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        val statusColor = when (show.status) {
                                            "ongoing" -> Color(0xFF00E5FF)
                                            "completed" -> Color(0xFF4ADE80)
                                            else -> Color(0xFFFFA726)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
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
