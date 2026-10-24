package com.mydonghua.app.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * ColorOS 17 & OxygenOS 17 Unified Aurora & Tidal Engine 2.0 Motion System.
 * Signature fluid cubic deceleration, viscous sheet transitions, and tactile spring dynamics.
 */
object ColorOSMotion {
    // Timing Tokens (ms) - Tuned for luxurious, fluid ColorOS 17 smoothness
    const val DurationInstant = 0
    const val DurationMicro = 160
    const val DurationQuick = 240
    const val DurationNormal = 360
    const val DurationExpanded = 460
    const val DurationLong = 580

    // ColorOS 17 Signature Easings
    // FluidCubic: signature gentle deceleration curve with ultra-smooth tail
    val FluidCubic = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.85f, 0.15f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.35f, 0.0f, 0.8f, 0.15f)
    val SnappyEase = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    // Springs (ColorOS 17 Aquamorphic & Tidal Engine Physics)
    // Signature fluid spring dynamics: responsive start, gentle physical deceleration, organic settling
    val FluidSpring = spring<Float>(dampingRatio = 0.74f, stiffness = 260f)
    val TouchBounceSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 320f)
    val SnappySpring = spring<Float>(dampingRatio = 0.70f, stiffness = 340f)
    val TactileSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 280f)
    val BouncySpring = spring<Float>(dampingRatio = 0.58f, stiffness = 220f)
    val PopSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 240f)

    // Float Transitions (Physics Springs)
    val FluidFloat = spring<Float>(dampingRatio = 0.75f, stiffness = 260f)
    val QuickFloat = spring<Float>(dampingRatio = 0.70f, stiffness = 340f)
    val FadeInFloat = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)
    val FadeOutFloat = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f)
    val ScaleInFloat = spring<Float>(dampingRatio = 0.72f, stiffness = 260f)
    val ScaleOutFloat = spring<Float>(dampingRatio = 0.82f, stiffness = 340f)

    // Dp Transitions (Physics Springs)
    val FluidDp = spring<Dp>(dampingRatio = 0.74f, stiffness = 260f)
    val SnappyDp = spring<Dp>(dampingRatio = 0.70f, stiffness = 340f)
    val SheetDp = spring<Dp>(dampingRatio = 0.76f, stiffness = 220f)

    // IntOffset Transitions (Sheet, Drawer, Navigation - ColorOS 17 Physics Springs)
    val SheetIntOffset = spring<IntOffset>(dampingRatio = 0.76f, stiffness = 220f)
    val SheetExitIntOffset = spring<IntOffset>(dampingRatio = 0.82f, stiffness = 280f)
    val FluidIntOffset = spring<IntOffset>(dampingRatio = 0.78f, stiffness = 260f)
}

/**
 * ColorOS 17 Interactive Tactile Press Modifier.
 * Provides the signature ColorOS press squish and elastic rebound on any composable.
 */
fun Modifier.colorOSPressBounce(
    pressedScale: Float = 0.95f,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "colorOSScale"
    )

    val pointerMod = if (enabled) {
        Modifier.pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                try {
                    waitForUpOrCancellation()
                } finally {
                    isPressed = false
                }
            }
        }
    } else Modifier

    val clickableMod = if (onClick != null && enabled) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(pointerMod)
        .then(clickableMod)
}

/**
 * ColorOS 17 Aurora Rim Gradient Brush for Liquid Glass surfaces.
 */
fun colorOSAuroraBorder(
    isDark: Boolean,
    primaryColor: Color,
    accentGlow: Float = 0.40f
): Brush {
    return Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.35f),
                primaryColor.copy(alpha = accentGlow),
                Color(0xFF26C6DA).copy(alpha = accentGlow * 0.5f),
                Color.White.copy(alpha = 0.08f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f),
                primaryColor.copy(alpha = accentGlow * 0.7f),
                Color(0xFF0091EA).copy(alpha = accentGlow * 0.3f),
                Color.Black.copy(alpha = 0.08f)
            )
        }
    )
}

/**
 * ColorOS 17 Aurora Shimmer Sweep effect for highlighted glass surfaces.
 */
fun Modifier.colorOSAuroraShimmer(
    enabled: Boolean = true,
    shimmerColor: Color = Color.White.copy(alpha = 0.15f)
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "auroraShimmer")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )

    this.drawWithContent {
        drawContent()
        val width = size.width
        val height = size.height
        val startX = (sweepProgress - 0.3f) * width
        val endX = (sweepProgress + 0.3f) * width

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    shimmerColor,
                    Color.Transparent
                ),
                start = Offset(startX, 0f),
                end = Offset(endX, height)
            ),
            blendMode = BlendMode.Plus
        )
    }
}
