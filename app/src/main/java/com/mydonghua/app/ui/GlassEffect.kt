package com.mydonghua.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
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
import com.mydonghua.app.ui.theme.Spacing
import com.mydonghua.app.ui.theme.colorOSPressBounce
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

val LocalHazeState = compositionLocalOf { HazeState() }
val LocalLayerBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
val LocalHeaderBackdrop = staticCompositionLocalOf<Backdrop?> { null }

val Spacing = com.mydonghua.app.ui.theme.Spacing

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

    // Orb 1: Primary Accent (Top-Left)
    val orb1X = 0.25f
    val orb1Y = 0.15f
    val orb1Scale = 1.2f

    // Orb 2: Secondary / Tertiary Tint (Bottom-Right)
    val orb2X = 0.65f
    val orb2Y = 0.75f
    val orb2Scale = 1.1f

    val isDark = backgroundColor.luminance() < 0.5f
    val orb1Alpha = if (isDark) 0.04f else 0.12f
    val orb2Alpha = if (isDark) 0.03f else 0.08f
    val linearAlpha = if (isDark) 0.015f else 0.04f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = if (isDark) Color(0xFF080A14) else backgroundColor)
            .hazeChild(state = hazeState)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            if (isDark) {
                // Official ColorOS 17 Atmospheric Dusk Aurora Horizon Canvas (Matches Reference Screenshots)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()

                            // Top Twilight Indigo Core
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF141C38).copy(alpha = 0.70f),
                                        Color(0xFF080A14).copy(alpha = 0.95f)
                                    ),
                                    center = Offset(widthPx * 0.5f, heightPx * 0.12f),
                                    radius = widthPx * 0.90f
                                )
                            )

                            // Right Edge Warm Sunset Amber Aurora Glow (Matches Stock ColorOS 17 Reference)
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFE65100).copy(alpha = 0.55f),
                                        Color(0xFFFF7043).copy(alpha = 0.35f),
                                        Color(0xFF8E24AA).copy(alpha = 0.22f),
                                        Color.Transparent
                                    ),
                                    center = Offset(widthPx * 1.06f, heightPx * 0.54f),
                                    radius = widthPx * 1.02f
                                ),
                                blendMode = BlendMode.Plus
                            )

                            // Bottom-Left Deep Cobalt Flare
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF0082FB).copy(alpha = 0.26f),
                                        Color(0xFF0277BD).copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    center = Offset(0f, heightPx * 0.76f),
                                    radius = widthPx * 0.85f
                                ),
                                blendMode = BlendMode.Plus
                            )
                        }
                )
            } else {
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
            }
        }
        content()
    }
}

@Composable
fun rememberLiquidGlassBorder(
    isLight: Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f,
    isAmoled: Boolean = MaterialTheme.colorScheme.background == Color.Black || MaterialTheme.colorScheme.background.luminance() < 0.01f
): Brush {
    return remember(isLight, isAmoled) {
        if (isLight) {
            Brush.verticalGradient(
                listOf(
                    Color.Black.copy(alpha = 0.12f),
                    Color.Black.copy(alpha = 0.04f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isAmoled) 0.24f else 0.20f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        }
    }
}

@Composable
fun rememberLiquidGlassChipBorder(
    isLight: Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f,
    isAmoled: Boolean = MaterialTheme.colorScheme.background == Color.Black || MaterialTheme.colorScheme.background.luminance() < 0.01f
): Color {
    return remember(isLight, isAmoled) {
        when {
            isAmoled -> Color.White.copy(alpha = 0.11f)
            isLight -> Color.Black.copy(alpha = 0.07f)
            else -> Color.White.copy(alpha = 0.08f)
        }
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
        isAmoled -> Color(0xFF101420).copy(alpha = 0.60f)
        isLight -> Color.White.copy(alpha = 0.65f)
        else -> Color(0xFF1E2638).copy(alpha = 0.45f)
    }

    val specularGlassBorder = rememberLiquidGlassBorder(isLight, isAmoled)

    val baseModifier = modifier
        .fillMaxWidth()
        .clip(shape)

    val liquidGlassModifier = if (backdrop != null) {
        baseModifier
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
                highlight = { Highlight.Default.copy(alpha = if (isLight) 0.50f else 0.35f) },
                shadow = { Shadow(radius = 2f.dp, alpha = if (isLight) 0.05f else 0.15f) },
                innerShadow = { InnerShadow(radius = 4f.dp, alpha = if (isLight) 0.08f else 0.15f) },
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
            .hazeChild(state = hazeState, shape = shape)
            .background(
                color = surfaceTint,
                shape = shape
            )
    }

    val interactiveModifier = when {
        onClick != null && onLongClick != null -> {
            liquidGlassModifier.colorOSPressBounce(pressedScale = 0.98f).combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
        }
        onClick != null -> {
            liquidGlassModifier.colorOSPressBounce(pressedScale = 0.98f, onClick = onClick)
        }
        else -> liquidGlassModifier
    }

    Box(
        modifier = interactiveModifier
            .border(
                width = 0.85.dp,
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
        isAmoledMode -> Color.Black.copy(alpha = 0.85f)
        isLightMode -> Color(0xFFF8F9FA).copy(alpha = 0.88f)
        else -> Color(0xFF0C0E14).copy(alpha = 0.80f)
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
                        highlight = null,
                        shadow = null,
                        effects = {
                            vibrancy()
                            colorControls(
                                brightness = if (isLightMode) 0.08f else 0.04f,
                                contrast = 1f,
                                saturation = 1.5f
                            )
                            blur(12f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(color = baseColor)
                        }
                    )
            )
        } else {
            val fallbackBg = when {
                isAmoledMode -> Color.Black.copy(alpha = 0.95f)
                isLightMode -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackBg, shape)
            )
        }

        content()
    }
}
