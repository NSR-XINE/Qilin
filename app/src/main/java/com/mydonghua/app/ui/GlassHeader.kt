package com.mydonghua.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.mydonghua.app.ui.LocalHeaderBackdrop
import com.mydonghua.app.ui.LocalLayerBackdrop
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.mydonghua.app.R
import com.mydonghua.app.ui.theme.Spacing
import com.mydonghua.app.ui.theme.ColorOSMotion
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.NavBarHeight

import androidx.compose.ui.text.style.TextOverflow

@Composable
fun GlassHeader(
    title: String,
    backdrop: Backdrop = LocalHeaderBackdrop.current ?: LocalLayerBackdrop.current ?: rememberLayerBackdrop(),
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() <= 0.5f,
    subtitle: String? = null,
    onDrawerOpen: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val menuInteraction = remember { MutableInteractionSource() }
    val isMenuPressed by menuInteraction.collectIsPressedAsState()
    val menuScale by animateFloatAsState(
        targetValue = if (isMenuPressed) 0.90f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "menuScale"
    )

    val density = LocalDensity.current
    val glowRadius = with(density) { (NavBarHeight.icon * 1.2f).toPx() }

    val contentColor = if (isDark) Color.White else Color(0xFF1E1E1E)
    val menuBtnBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
    val menuBtnPressBg = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)

    val headerInteraction = remember { MutableInteractionSource() }
    val isHeaderPressed by headerInteraction.collectIsPressedAsState()
    val headerPressProgress by animateFloatAsState(
        targetValue = if (isHeaderPressed) 1f else 0f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "headerPressProgress"
    )
    var headerTouchPosition by remember { mutableStateOf(Offset.Zero) }

    val capsuleScale = lerp(1f, 0.985f, headerPressProgress)

    val canopySurfaceColor = if (isDark) Color(0xFF070A10).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.52f)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Surrounding frosted canopy from pill to top of screen
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Black,
                            0.76f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    highlight = null,
                    shadow = null,
                    effects = {
                        vibrancy()
                        colorControls(
                            brightness = if (isDark) 0.03f else 0.01f,
                            contrast = 1.02f,
                            saturation = 1.45f
                        )
                        blur(24.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to canopySurfaceColor,
                                0.76f to canopySurfaceColor.copy(alpha = canopySurfaceColor.alpha * 0.90f),
                                1.0f to Color.Transparent
                            )
                        )
                    }
                )
        )

        // Floating Liquid Glass Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Spacing.lg, end = Spacing.lg, top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                .fillMaxWidth()
                .height(56.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val firstPressed = event.changes.firstOrNull { it.pressed }
                            if (firstPressed != null) {
                                headerTouchPosition = firstPressed.position
                            } else {
                                headerTouchPosition = Offset.Zero
                            }
                        }
                    }
                }
                .clickable(
                    interactionSource = headerInteraction,
                    indication = null,
                    onClick = {}
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    highlight = null,
                    shadow = null,
                    effects = {
                        vibrancy()
                        colorControls(
                            brightness = if (isDark) 0.05f else 0.02f,
                            contrast = 1.05f,
                            saturation = 1.6f
                        )
                        blur((22f + 2f * headerPressProgress).dp.toPx())
                    },
                    onDrawSurface = {
                        val baseColor = if (isDark) Color(0xFF0F141C).copy(alpha = 0.38f) else Color.White.copy(alpha = 0.48f)
                        val radius = size.height / 2f
                        drawRoundRect(
                            color = baseColor,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                        )
                        val press = headerPressProgress
                        if (press > 0f) {
                            val lightColor = if (isDark) Color.White.copy(alpha = 0.18f * press) else Color.White.copy(alpha = 0.35f * press)
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(lightColor, Color.Transparent),
                                    center = if (headerTouchPosition != Offset.Zero) headerTouchPosition else Offset(size.width / 2f, size.height / 2f),
                                    radius = size.minDimension * 1.2f,
                                ),
                                blendMode = BlendMode.Plus,
                            )
                        }
                    },
                    layerBlock = {
                        scaleX = capsuleScale
                        scaleY = capsuleScale
                    }
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = if (onBackClick != null) Spacing.sm else Spacing.xxl, end = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(menuBtnBg)
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = contentColor,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.md)
                ) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = if (subtitle != null) 16.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = TextStyle(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0082FB)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onEditClick != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(menuBtnBg)
                            .scale(menuScale)
                            .clickable(
                                interactionSource = menuInteraction,
                                indication = null,
                                onClick = onEditClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Edit",
                            tint = contentColor,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }

                if (onDrawerOpen != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(menuBtnBg)
                            .scale(menuScale)
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
                                                menuBtnPressBg,
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
                            tint = contentColor,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                }

                if (actions != null) {
                    actions()
                }
            }
        }
    }
}
}
