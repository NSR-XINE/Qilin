package com.nsr.qilin.ui.common

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nsr.qilin.ui.theme.ColorOSMotion

/**
 * Official ColorOS 17 Fluid Aurora Shimmer Effect.
 * Executes purely in the Draw phase to eliminate layout and composition thrashing.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    val transition = rememberInfiniteTransition(label = "colorOSShimmer")
    val progress by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = ColorOSMotion.FastOutSlowIn),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    this.drawWithContent {
        drawRect(color = baseColor)
        val width = size.width
        val height = size.height
        val startX = (progress - 0.4f) * width
        val endX = (progress + 0.4f) * width

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    highlightColor,
                    Color.Transparent
                ),
                start = Offset(startX, 0f),
                end = Offset(endX, height)
            ),
            blendMode = BlendMode.Plus
        )
        drawContent()
    }
}
