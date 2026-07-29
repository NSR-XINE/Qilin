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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.RoundedRectRefractionShaderString
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
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
    content: @Composable () -> Unit = {}
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

    val isDark = backgroundColor.luminance() < 0.5f
    val orb1Alpha = if (isDark) 0.04f else 0.12f
    val orb2Alpha = if (isDark) 0.03f else 0.08f
    val orb3Alpha = if (isDark) 0.03f else 0.08f
    val orb4Alpha = if (isDark) 0.02f else 0.06f
    val linearAlpha = if (isDark) 0.015f else 0.04f

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
                                primaryColor.copy(alpha = linearAlpha),
                                secondaryColor.copy(alpha = linearAlpha * 0.7f),
                                tertiaryColor.copy(alpha = linearAlpha)
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
                                primaryColor.copy(alpha = orb1Alpha),
                                primaryContainer.copy(alpha = orb1Alpha * 0.4f),
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
                                tertiaryColor.copy(alpha = orb2Alpha),
                                surfaceTint.copy(alpha = orb2Alpha * 0.4f),
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
                                secondaryColor.copy(alpha = orb3Alpha),
                                primaryColor.copy(alpha = orb3Alpha * 0.4f),
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
                                primaryColor.copy(alpha = orb4Alpha),
                                tertiaryColor.copy(alpha = orb4Alpha * 0.4f),
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
    content: @Composable () -> Unit = {}
) {
    val hazeState = LocalHazeState.current
    val backdrop = LocalLayerBackdrop.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = surfaceColor.luminance() < 0.5f

    val surfaceTint = if (isDark) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.25f)
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.10f)

    val specularGlassBorder = Brush.verticalGradient(
        colors = if (isDark) listOf(
            Color.White.copy(alpha = 0.50f),
            Color.White.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.04f)
        ) else listOf(
            Color.White.copy(alpha = 0.75f),
            Color.White.copy(alpha = 0.35f),
            Color.Black.copy(alpha = 0.12f),
            Color.Black.copy(alpha = 0.06f)
        )
    )

    val baseModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = 6.dp,
            shape = shape,
            clip = false,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )

    val liquidGlassModifier = if (backdrop != null) {
        baseModifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                colorControls(
                    brightness = if (isDark) 0.05f else 0.10f,
                    contrast = 1.0f,
                    saturation = 1.5f
                )
                blur(12f.dp.toPx())
                refraction(size.minDimension / 4f, size.minDimension / 2f, false)
            },
            highlight = { Highlight.Default.copy(alpha = if (isDark) 0.50f else 0.70f) },
            shadow = { Shadow(radius = 4f.dp, alpha = if (isDark) 0.30f else 0.15f) },
            innerShadow = { InnerShadow(radius = 6f.dp, alpha = if (isDark) 0.25f else 0.15f) },
            onDrawSurface = {
                val radius = shape.topStart.toPx(size, density = this)
                drawRoundRect(
                    color = surfaceTint,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
            }
        )
    } else {
        baseModifier
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape)
            .background(
                color = surfaceTint,
                shape = shape
            )
    }

    Box(
        modifier = liquidGlassModifier
            .border(
                width = 1.dp,
                brush = specularGlassBorder,
                shape = shape
            )
    ) {
        Box(modifier = Modifier.padding(Spacing.md)) {
            content()
        }
    }
}
