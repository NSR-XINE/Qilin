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
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceTint = MaterialTheme.colorScheme.surfaceTint

    val infiniteTransition = rememberInfiniteTransition(label = "appleMusicFluidGlow")

    // Orb 1: Primary Accent (Top-Left Lissajous Orbit)
    val orb1X by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(18000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb1X"
    )
    val orb1Y by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(27000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb1Y"
    )
    val orb1Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb1Scale"
    )

    // Orb 2: Secondary / Tertiary Tint (Bottom-Right Orbit)
    val orb2X by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(23000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb2X"
    )
    val orb2Y by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb2Y"
    )
    val orb2Scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb2Scale"
    )

    // Orb 3: Complementary Floating Glare (Center-Right Drift)
    val orb3X by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = -0.1f,
        animationSpec = infiniteRepeatable(tween(31000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb3X"
    )
    val orb3Y by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(22000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb3Y"
    )

    // Orb 4: Center Plasma Ambient Core (Swell & Pulse)
    val orb4X by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(25000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb4X"
    )
    val orb4Y by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(33000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb4Y"
    )
    val orb4Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.40f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb4Scale"
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
            val baseRadius = (widthPx * 1.5f).coerceAtLeast(1400f)

            // Linear background mesh sweep
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.12f),
                                secondaryColor.copy(alpha = 0.08f),
                                tertiaryColor.copy(alpha = 0.12f)
                            ),
                            start = Offset(widthPx * orb1X, 0f),
                            end = Offset(widthPx * orb2X, heightPx)
                        )
                    )
            )

            // Orb 1: Vibrant Primary Core
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.38f),
                                primaryContainer.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(widthPx * orb1X, heightPx * orb1Y),
                            radius = baseRadius * orb1Scale
                        )
                    )
            )

            // Orb 2: Deep Tertiary Swell
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tertiaryColor.copy(alpha = 0.32f),
                                surfaceTint.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(widthPx * orb2X, heightPx * orb2Y),
                            radius = baseRadius * orb2Scale
                        )
                    )
            )

            // Orb 3: Secondary Floating Flare
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = 0.28f),
                                primaryColor.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(widthPx * orb3X, heightPx * orb3Y),
                            radius = baseRadius * 1.25f
                        )
                    )
            )

            // Orb 4: Center Plasma Swell
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.22f),
                                tertiaryColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(widthPx * orb4X, heightPx * orb4Y),
                            radius = baseRadius * orb4Scale
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
    val hazeState = LocalHazeState.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceTint = MaterialTheme.colorScheme.surfaceTint
    val isDark = surfaceColor.luminance() < 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.55f),
                        surfaceTint.copy(alpha = 0.08f),
                        surfaceColor.copy(alpha = 0.40f)
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.2.dp,
                color = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.15f),
                shape = shape
            )
    ) {
        Box(modifier = Modifier.padding(Spacing.md)) {
            content()
        }
    }
}
