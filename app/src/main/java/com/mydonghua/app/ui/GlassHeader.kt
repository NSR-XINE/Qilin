package com.mydonghua.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mydonghua.app.R

@Composable
fun GlassHeader(
    currentRoute: String,
    isDark: Boolean,
    onSearchClick: () -> Unit,
    onSearchBackClick: () -> Unit,
    onDrawerOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSearchMode = currentRoute == "search"
    val searchInteraction = remember { MutableInteractionSource() }
    val isSearchPressed by searchInteraction.collectIsPressedAsState()
    val searchScale by animateFloatAsState(
        targetValue = if (isSearchPressed) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "searchScale"
    )

    val menuInteraction = remember { MutableInteractionSource() }
    val isMenuPressed by menuInteraction.collectIsPressedAsState()
    val menuScale by animateFloatAsState(
        targetValue = if (isMenuPressed) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "menuScale"
    )

    val density = LocalDensity.current
    val glowRadius = with(density) { (48.dp * 1.2f).toPx() }

    val searchOffsetX by animateFloatAsState(
        targetValue = if (isSearchMode) with(density) { -(200.dp.toPx()) } else 0f,
        animationSpec = tween(300),
        label = "searchOffset"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSearchMode) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(searchScale)
                    .graphicsLayer { translationX = searchOffsetX }
                    .clip(CircleShape)
                    .background(Color.DarkGray.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = searchInteraction,
                        indication = null,
                        onClick = onSearchBackClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSearchPressed) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color.Transparent
                                    ),
                                    radius = glowRadius
                                )
                            )
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(searchScale)
                    .graphicsLayer { translationX = searchOffsetX }
                    .clip(CircleShape)
                    .background(Color.DarkGray.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = searchInteraction,
                        indication = null,
                        onClick = onSearchClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSearchPressed) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color.Transparent
                                    ),
                                    radius = glowRadius
                                )
                            )
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(menuScale)
                    .clip(CircleShape)
                    .background(Color.DarkGray.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = menuInteraction,
                        indication = null,
                        onClick = onDrawerOpen
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
