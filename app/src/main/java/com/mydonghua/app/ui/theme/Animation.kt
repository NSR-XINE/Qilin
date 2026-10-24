package com.mydonghua.app.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

object AnimDuration {
    val instant = 0
    val fast = 100
    val normal = 200
    val medium = 250
    val slow = 300
    val extraSlow = 300
}

object AnimSpec {
    val springDefault = tween<Float>(AnimDuration.normal, easing = FastOutSlowInEasing)
    val springStiff = tween<Float>(AnimDuration.fast, easing = FastOutSlowInEasing)
    val springSmooth = tween<Float>(AnimDuration.slow, easing = FastOutSlowInEasing)
    val fadeIn = tween<Float>(AnimDuration.normal)
    val fadeOut = tween<Float>(AnimDuration.fast)
    val slide = tween<Int>(AnimDuration.medium)
}

/**
 * ColorOS 17 & OxygenOS 17 Unified Aurora & Tidal Engine Motion System.
 * Smooth fluid cubic curves with zero bounce and zero spring wobble.
 */
object ColorOSMotion {
    // Standard Durations
    const val DurationQuick = 180
    const val DurationNormal = 280
    const val DurationExpanded = 380

    // ColorOS 17 Signature Easings
    val FluidCubic = CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /**
     * Aquamorphic Fluid Motion: ColorOS 17 natural fluid curve for surfaces, sheets, and drawers.
     * Soft entry, zero drag, non-wobbly settle.
     */
    val FluidSpring = tween<Float>(
        durationMillis = DurationNormal,
        easing = FluidCubic
    )

    /**
     * Touch Ease: ColorOS 17 smooth tactile press and release curve without spring bounce.
     */
    val TouchBounceSpring = tween<Float>(
        durationMillis = DurationQuick,
        easing = FluidCubic
    )

    /**
     * Snappy Motion: For switches, radio dots, chips, and small controls.
     */
    val SnappySpring = tween<Float>(
        durationMillis = DurationQuick,
        easing = FluidCubic
    )

    /**
     * Sheet & Drawer Motion: Smooth viscous deceleration for expansive overlays.
     */
    val SheetSpring = tween<Float>(
        durationMillis = DurationNormal,
        easing = FluidCubic
    )

    /**
     * Sheet IntOffset Motion: Used for sheet slide transitions.
     */
    val SheetIntOffset = tween<IntOffset>(
        durationMillis = DurationNormal,
        easing = FluidCubic
    )

    /**
     * Fluid IntOffset Motion: Used for parallel slide navigation transitions.
     */
    val FluidIntOffset = tween<IntOffset>(
        durationMillis = DurationNormal,
        easing = FluidCubic
    )

    /**
     * Snappy Dp Motion: For displacement and offsets.
     */
    val SnappyDp = tween<Dp>(
        durationMillis = DurationQuick,
        easing = FluidCubic
    )

    /**
     * Fluid Dp Motion: For smooth offset translations.
     */
    val FluidDp = tween<Dp>(
        durationMillis = DurationNormal,
        easing = FluidCubic
    )
}

/**
 * ColorOS 17 Interactive Elastic Press Modifier.
 * Provides the signature ColorOS press-down squish (scale down) and elastic rebound.
 */
fun Modifier.colorOSPressBounce(
    pressedScale: Float = 0.95f,
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "colorOSScale"
    )

    val mod = this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    if (onClick != null) {
        mod.clickable(
            interactionSource = source,
            indication = null,
            onClick = onClick
        )
    } else {
        mod
    }
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
