package com.mydonghua.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.mydonghua.app.ui.theme.CornerRadius
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

val LocalHazeState = staticCompositionLocalOf<HazeState> { error("No HazeState provided") }
val LocalLayerBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val section = xxxl
}

private val GlassTintAlpha = 0.04f

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hazeState = LocalHazeState.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceTint = MaterialTheme.colorScheme.surfaceTint

    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val glowOffsetX by infiniteTransition.animateFloat(
        initialValue = -0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowX"
    )
    val glowOffsetY by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(21000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor)
            .hazeChild(state = hazeState)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val centerX = widthPx * glowOffsetX
            val centerY = heightPx * glowOffsetY

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryContainer.copy(alpha = 0.18f),
                                surfaceTint.copy(alpha = 0.07f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                            radius = (widthPx * 1.6f).coerceAtLeast(1500f)
                        )
                    )
            )
        }
        content()
    }
}

@Composable
fun GlassSection(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(CornerRadius.card),
    content: @Composable () -> Unit
) {
    val backdrop = LocalLayerBackdrop.current
    val hazeState = LocalHazeState.current
    val density = LocalDensity.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = surfaceColor.luminance() < 0.5f

    val baseSurfaceColor = if (isDark) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.45f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f)
    val blurPx = with(density) { 12.dp.toPx() }
    val cardCornerRadiusPx = with(density) { CornerRadius.card.toPx() }

    if (backdrop != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        colorControls(
                            brightness = 0.05f,
                            contrast = 1f,
                            saturation = 1.5f
                        )
                        blur(blurPx)
                        refraction(size.minDimension / 6f, size.minDimension / 3f, false)
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.35f) },
                    shadow = { Shadow(radius = 4f.dp, alpha = 0.15f) },
                    onDrawSurface = {
                        drawRoundRect(
                            color = baseSurfaceColor,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cardCornerRadiusPx, cardCornerRadiusPx)
                        )
                    }
                )
                .border(
                    width = 0.5.dp,
                    color = borderColor,
                    shape = shape
                )
        ) {
            Box(modifier = Modifier.padding(Spacing.md)) {
                content()
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .hazeChild(state = hazeState, shape = shape)
                .background(
                    color = baseSurfaceColor,
                    shape = shape
                )
                .border(
                    width = 0.5.dp,
                    color = borderColor,
                    shape = shape
                )
        ) {
            Box(modifier = Modifier.padding(Spacing.md)) {
                content()
            }
        }
    }
}
