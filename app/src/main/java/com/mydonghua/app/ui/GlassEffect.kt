package com.mydonghua.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
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

val LocalHazeState = compositionLocalOf { HazeState() }
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassSection(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(CornerRadius.card),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    val hazeState = LocalHazeState.current
    val backdrop = LocalLayerBackdrop.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLight = surfaceColor.luminance() > 0.5f
    val isAmoled = backgroundColor == Color.Black || backgroundColor.luminance() < 0.01f
    val isDark = !isLight

    val surfaceTint = when {
        isAmoled -> Color(0xFF0B0F17).copy(alpha = 0.45f)
        isLight -> Color.White.copy(alpha = 0.35f)
        else -> Color.Black.copy(alpha = 0.18f)
    }

    val shadowColor = when {
        isAmoled -> Color.Black.copy(alpha = 0.40f)
        isLight -> Color.Black.copy(alpha = 0.10f)
        else -> Color.Black.copy(alpha = 0.25f)
    }

    val specularGlassBorder = Brush.verticalGradient(
        colors = when {
            isAmoled -> listOf(
                Color.White.copy(alpha = 0.60f),
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f)
            )
            isLight -> listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.40f),
                Color.Black.copy(alpha = 0.12f),
                Color.Black.copy(alpha = 0.05f)
            )
            else -> listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.03f)
            )
        }
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
        baseModifier
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    colorControls(
                        brightness = when {
                            isAmoled -> -0.02f
                            isLight -> 0.10f
                            else -> 0.04f
                        },
                        contrast = if (isLight) 1.05f else 1.0f,
                        saturation = if (isLight) 1.25f else 1.5f
                    )
                    blur(if (isAmoled) 14f.dp.toPx() else 12f.dp.toPx())
                    refraction(size.minDimension / 4f, size.minDimension / 2f, false)
                },
                highlight = { Highlight.Default.copy(alpha = if (isLight) 0.70f else 0.55f) },
                shadow = { Shadow(radius = 4f.dp, alpha = if (isLight) 0.15f else 0.30f) },
                innerShadow = { InnerShadow(radius = 6f.dp, alpha = if (isLight) 0.15f else 0.25f) },
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

    val interactiveModifier = when {
        onClick != null && onLongClick != null -> {
            liquidGlassModifier.clip(shape).combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
        }
        onClick != null -> {
            liquidGlassModifier.clip(shape).clickable(onClick = onClick)
        }
        else -> liquidGlassModifier
    }

    Box(
        modifier = interactiveModifier
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

@Composable
fun MenuGlassPanel(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
    isAmoledMode: Boolean = false,
    isLightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val backdrop = LocalLayerBackdrop.current
    val isDark = !isLightMode

    val baseColor = when {
        isAmoledMode -> Color(0xFF04060A).copy(alpha = 0.55f)
        isLightMode -> Color.White.copy(alpha = 0.45f)
        else -> Color.Black.copy(alpha = 0.35f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Box(
        modifier = modifier.clip(shape)
    ) {
        if (backdrop != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            colorControls(
                                brightness = if (isLightMode) 0.08f else 0.04f,
                                contrast = 1f,
                                saturation = 1.5f
                            )
                            blur(12f.dp.toPx())
                        },
                        highlight = { Highlight.Default.copy(alpha = if (isLightMode) 0.60f else 0.35f) },
                        shadow = { Shadow(radius = 6f.dp, alpha = 0.20f) },
                        innerShadow = { InnerShadow(radius = 8f.dp, alpha = 0.15f) },
                        onDrawSurface = {
                            drawRect(color = baseColor)
                        }
                    )
                    .border(
                        width = 0.5.dp,
                        color = borderColor,
                        shape = shape
                    )
            )
        } else {
            val fallbackBg = when {
                isAmoledMode -> Color(0xFF060912).copy(alpha = 0.92f)
                isLightMode -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> Color(0xFF0F172A).copy(alpha = 0.92f)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackBg, shape)
                    .border(
                        width = 0.5.dp,
                        color = borderColor,
                        shape = shape
                    )
            )
        }

        content()
    }
}
