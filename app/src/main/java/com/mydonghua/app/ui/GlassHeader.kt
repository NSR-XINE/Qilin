package com.mydonghua.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mydonghua.app.R
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.NavBarHeight

@Composable
fun GlassHeader(
    isDark: Boolean,
    onDrawerOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val menuInteraction = remember { MutableInteractionSource() }
    val isMenuPressed by menuInteraction.collectIsPressedAsState()
    val menuScale by animateFloatAsState(
        targetValue = if (isMenuPressed) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "menuScale"
    )

    val density = LocalDensity.current
    val glowRadius = with(density) { (NavBarHeight.icon * 1.2f).toPx() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(NavBarHeight.icon)
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
            if (isMenuPressed) {
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
                painter = painterResource(id = R.drawable.ic_menu),
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(IconSize.lg)
            )
        }
    }
}