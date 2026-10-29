package com.mydonghua.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.theme.ColorOSMotion
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.Haptics
import com.mydonghua.app.ui.theme.JustSansFamily
import com.mydonghua.app.ui.theme.colorOSPressBounce
import dev.chrisbanes.haze.hazeChild

/**
 * Official ColorOS 17 Liquid Glass Squircle Card.
 * 100% free of Material 3 tonal elevation and surface tinting.
 * Replicates the signature frosted liquid glass cards from ColorOS 17 System Update.
 */
@Composable
fun ColorOSCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(CornerRadius.card),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val hazeState = LocalHazeState.current

    val cardBg = if (isLight) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.70f),
                Color(0xFFF0F4FA).copy(alpha = 0.60f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1E2638).copy(alpha = 0.48f),
                Color(0xFF121622).copy(alpha = 0.58f)
            )
        )
    }

    val specularBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isLight) 0.35f else 0.18f),
            Color.White.copy(alpha = if (isLight) 0.12f else 0.04f)
        )
    )

    val clickModifier = if (onClick != null) {
        Modifier
            .colorOSPressBounce(pressedScale = 0.975f, onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape)
            .background(brush = cardBg, shape = shape)
            .border(width = 1.dp, brush = specularBorder, shape = shape)
            .then(clickModifier),
        content = content
    )
}

/**
 * Signature ColorOS 17 50% Capsule Action Pill Button.
 * Replaces Material 3 Button with authentic ColorOS 17 liquid glass physics and tactile bounce.
 */
@Composable
fun ColorOSPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    containerColor: Color = Color(0xFF0082FB),
    contentColor: Color = Color.White,
    icon: (@Composable () -> Unit)? = null
) {
    val pillShape = RoundedCornerShape(percent = 50)

    val actualContainer = if (enabled) containerColor else containerColor.copy(alpha = 0.38f)
    val actualContent = if (enabled) contentColor else contentColor.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .height(height)
            .clip(pillShape)
            .background(actualContainer)
            .then(
                if (enabled) Modifier.colorOSPressBounce(pressedScale = 0.96f, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                icon()
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                color = actualContent
            )
        }
    }
}

/**
 * ColorOS 17 Outlined Frosted Capsule Button.
 */
@Composable
fun ColorOSOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 50.dp,
    contentColor: Color = Color.White,
    icon: (@Composable () -> Unit)? = null
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val pillShape = RoundedCornerShape(percent = 50)

    val borderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isLight) 0.35f else 0.22f),
            Color.White.copy(alpha = if (isLight) 0.12f else 0.06f)
        )
    )

    Box(
        modifier = modifier
            .height(height)
            .clip(pillShape)
            .background(if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.10f))
            .border(width = 1.dp, brush = borderBrush, shape = pillShape)
            .colorOSPressBounce(pressedScale = 0.96f, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                icon()
            }
            Text(
                text = text,
                fontSize = 14.5.sp,
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

/**
 * ColorOS 17 Text Button.
 */
@Composable
fun ColorOSTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0082FB)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .colorOSPressBounce(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.5.sp,
            fontFamily = JustSansFamily,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Official ColorOS 17 Aquamorphic Toggle Switch.
 * 100% custom implementation replacing the generic Material 3 Switch.
 * Replicates stock ColorOS 17 fluid pill track and spring-sliding thumb.
 */
@Composable
fun ColorOSSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = Color(0xFF0082FB)
) {
    val trackWidth = 50.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val thumbPadding = 3.dp

    val maxOffset = trackWidth - thumbSize - (thumbPadding * 2)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 0.dp,
        animationSpec = ColorOSMotion.SwitchThumbSpring,
        label = "thumbOffset"
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (pressed) 1.22f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 900f),
        label = "thumbScale"
    )

    val trackColor = if (checked) {
        if (enabled) activeColor else activeColor.copy(alpha = 0.5f)
    } else {
        Color.White.copy(alpha = 0.16f)
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val inactiveTrackColor = if (isLight) Color.Black.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.16f)
    val animatedTrackColor by animateColorAsState(
        targetValue = if (checked) trackColor else inactiveTrackColor,
        animationSpec = tween(durationMillis = 220, easing = ColorOSMotion.FastOutSlowIn),
        label = "trackColor"
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(animatedTrackColor)
            .then(
                if (enabled && onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        Haptics.toggle(!checked)
                        onCheckedChange(!checked)
                    }
                } else Modifier
            )
            .padding(thumbPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * Official ColorOS 17 Liquid Glass Alert Dialog.
 * 100% replaces Material 3 AlertDialog with 32dp continuous squircle, specular rim border,
 * and signature ColorOS action buttons.
 */
@Composable
fun ColorOSDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String? = null,
    confirmText: String = "Confirm",
    dismissText: String? = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    confirmButtonColor: Color = Color(0xFF0082FB),
    customContent: (@Composable () -> Unit)? = null
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val dialogShape = RoundedCornerShape(30.dp)

    val dialogBg = if (isLight) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.94f),
                Color(0xFFF1F5F9).copy(alpha = 0.90f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1E2638).copy(alpha = 0.88f),
                Color(0xFF121622).copy(alpha = 0.94f)
            )
        )
    }

    val dialogBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isLight) 0.35f else 0.20f),
            Color.White.copy(alpha = if (isLight) 0.12f else 0.04f)
        )
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var dialogShown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { dialogShown = true }
        val dialogEnter by animateFloatAsState(
            targetValue = if (dialogShown) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.60f, stiffness = 300f),
            label = "dialogEnter"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .graphicsLayer {
                    alpha = dialogEnter.coerceIn(0f, 1f)
                    val s = 0.86f + 0.14f * dialogEnter
                    scaleX = s
                    scaleY = s
                    translationY = (1f - dialogEnter) * 48f
                }
                .clip(dialogShape)
                .background(brush = dialogBg, shape = dialogShape)
                .border(width = 1.dp, brush = dialogBorder, shape = dialogShape)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontFamily = JustSansFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) Color(0xFF0F172A) else Color.White
                )

                if (message != null) {
                    Text(
                        text = message,
                        fontSize = 14.5.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.Normal,
                        color = if (isLight) Color(0xFF475569) else Color(0xFFA0A6B5),
                        lineHeight = 21.sp
                    )
                }

                if (customContent != null) {
                    customContent()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissText != null) {
                        ColorOSOutlinedButton(
                            text = dismissText,
                            onClick = { onDismiss?.invoke() ?: onDismissRequest() },
                            modifier = Modifier.weight(1f),
                            height = 46.dp,
                            contentColor = if (isLight) Color(0xFF475569) else Color.White
                        )
                    }

                    ColorOSPillButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                        containerColor = confirmButtonColor,
                        contentColor = Color.White
                    )
                }
            }
        }
    }
}
