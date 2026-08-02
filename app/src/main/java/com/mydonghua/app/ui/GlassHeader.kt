package com.mydonghua.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.mydonghua.app.R
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.LocalDisplayCornerRadius
import com.mydonghua.app.ui.theme.NavBarHeight

import androidx.compose.ui.text.style.TextOverflow

@Composable
fun GlassHeader(
    title: String,
    backdrop: LayerBackdrop,
    isDark: Boolean = true,
    onDrawerOpen: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val menuInteraction = remember { MutableInteractionSource() }
    val isMenuPressed by menuInteraction.collectIsPressedAsState()
    val menuScale by animateFloatAsState(
        targetValue = if (isMenuPressed) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "menuScale"
    )

    val density = LocalDensity.current
    val glowRadius = with(density) { (NavBarHeight.icon * 1.2f).toPx() }

    val contentColor = if (isDark) Color.White else Color(0xFF1E1E1E)
    val menuBtnBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
    val menuBtnPressBg = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f)

    val headerInteraction = remember { MutableInteractionSource() }
    val isHeaderPressed by headerInteraction.collectIsPressedAsState()
    val headerPressProgress by animateFloatAsState(
        targetValue = if (isHeaderPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "headerPressProgress"
    )
    var headerTouchPosition by remember { mutableStateOf(Offset.Zero) }

    val capsuleScale = lerp(1f, 1.05f, headerPressProgress)

    val displayCornerRadius = LocalDisplayCornerRadius.current
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val topCornerRadiusPx = with(density) { displayCornerRadius.toPx() }
    val bottomCornerRadiusPx = with(density) { 16.dp.toPx() }
    val capsulePaddingStartPx = with(density) { Spacing.xl.toPx() }
    val capsulePaddingEndPx = with(density) { Spacing.xl.toPx() }
    val capsuleTopPaddingPx = with(density) { 2.dp.toPx() }
    val capsuleHeightPx = with(density) { 56.dp.toPx() }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val animScale = capsuleScale
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = {
                        CutoutHeaderShape(
                            statusBarHeightPx = statusBarHeightPx,
                            topCornerRadiusPx = topCornerRadiusPx,
                            bottomCornerRadiusPx = bottomCornerRadiusPx,
                            capsulePaddingStartPx = capsulePaddingStartPx,
                            capsulePaddingEndPx = capsulePaddingEndPx,
                            capsuleTopPaddingPx = capsuleTopPaddingPx,
                            capsuleHeightPx = capsuleHeightPx,
                            scale = lerp(1f, 1.05f, headerPressProgress)
                        )
                    },
                    effects = {
                        vibrancy()
                        colorControls(
                            brightness = 0.05f,
                            contrast = 1f,
                            saturation = 1.5f
                        )
                        blur((12f + 0.01f * headerPressProgress).dp.toPx())
                    },
                    onDrawSurface = {
                        val baseColor = if (isDark) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.25f)
                        drawRect(color = baseColor)
                    }
                )
                .border(
                    width = 0.5.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(topStart = displayCornerRadius, topEnd = displayCornerRadius, bottomStart = 16.dp, bottomEnd = 16.dp)
                )
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(72.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Spacing.xl, end = Spacing.xl, top = 2.dp, bottom = 14.dp),
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
                    effects = {
                        vibrancy()
                        colorControls(
                            brightness = 0.05f,
                            contrast = 1f,
                            saturation = 1.5f
                        )
                        blur((12f + 2f * headerPressProgress).dp.toPx())
                        refraction(size.minDimension / 4f + 2f.dp.toPx() * headerPressProgress, size.minDimension / 2f, false)
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.35f) },
                    shadow = { Shadow(radius = 4f.dp, alpha = 0.15f) },
                    onDrawSurface = {
                        val baseColor = if (isDark) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.45f)
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
                )
                .border(
                    width = 0.5.dp,
                    color = borderColor,
                    shape = CircleShape
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
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = title,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            colors = listOf(contentColor, contentColor.copy(alpha = 0.75f))
                        ),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        letterSpacing = 0.25.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (onEditClick != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(menuBtnBg)
                            .clickable(onClick = onEditClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Edit",
                            tint = contentColor,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                }

                if (onDrawerOpen != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(menuScale)
                            .clip(CircleShape)
                            .background(menuBtnBg)
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
            }
        }
        }
    }
}

class CutoutHeaderShape(
    private val statusBarHeightPx: Float,
    private val topCornerRadiusPx: Float,
    private val bottomCornerRadiusPx: Float,
    private val capsulePaddingStartPx: Float,
    private val capsulePaddingEndPx: Float,
    private val capsuleTopPaddingPx: Float,
    private val capsuleHeightPx: Float,
    private val scale: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val outerPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    topLeft = CornerRadius(topCornerRadiusPx, topCornerRadiusPx),
                    topRight = CornerRadius(topCornerRadiusPx, topCornerRadiusPx),
                    bottomLeft = CornerRadius(bottomCornerRadiusPx, bottomCornerRadiusPx),
                    bottomRight = CornerRadius(bottomCornerRadiusPx, bottomCornerRadiusPx)
                )
            )
        }
        val innerPath = Path().apply {
            val topPaddingPx = with(density) { 2.dp.toPx() }
            val bottomPaddingPx = with(density) { 14.dp.toPx() }
            val capsuleHeightPx = with(density) { 56.dp.toPx() }

            val maxCapsuleWidthPx = with(density) { 420.dp.toPx() }
            val availableWidth = size.width - capsulePaddingStartPx - capsulePaddingEndPx
            val actualCapsuleWidth = minOf(availableWidth, maxCapsuleWidthPx)

            val centerX = size.width / 2f
            val bottom = size.height - bottomPaddingPx
            val top = bottom - capsuleHeightPx
            val centerY = (top + bottom) / 2f

            val halfWidth = actualCapsuleWidth / 2f
            val halfHeight = (bottom - top) / 2f
            
            val scaledLeft = centerX - halfWidth * scale
            val scaledRight = centerX + halfWidth * scale
            val scaledTop = centerY - halfHeight * scale
            val scaledBottom = centerY + halfHeight * scale
            val scaledHeight = scaledBottom - scaledTop
            
            addRoundRect(
                RoundRect(
                    rect = Rect(scaledLeft, scaledTop, scaledRight, scaledBottom),
                    cornerRadius = CornerRadius(scaledHeight / 2f, scaledHeight / 2f)
                )
            )
        }
        val resultPath = Path.combine(
            PathOperation.Difference,
            outerPath,
            innerPath
        )
        return Outline.Generic(resultPath)
    }
}