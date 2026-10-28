package com.mydonghua.app.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.refractionWithDispersion
import com.kyant.backdrop.effects.vibrancy
import com.mydonghua.app.R
import com.mydonghua.app.ui.common.PosterStagger
import com.mydonghua.app.ui.common.PosterTrace
import com.mydonghua.app.ui.theme.ColorOSMotion
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.NavBarHeight
import com.mydonghua.app.ui.theme.Spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.time.Duration.Companion.seconds

private val navRoutes = listOf("home", "airing", "complete", "stopped")
private val BarShape = RoundedCornerShape(percent = 50)
private val CapsuleShape = RoundedCornerShape(percent = 50)

@Composable
fun LiquidGlassNavBar(
    currentRoute: String,
    isDark: Boolean,
    backdrop: Backdrop,
    isSearchOpen: Boolean,
    onSearchClick: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0f) }
    val currentOnNavigate by rememberUpdatedState(onNavigate)
    val haptic = LocalHapticFeedback.current

    val capsuleAlpha by animateFloatAsState(
        targetValue = if (isSearchOpen) 0f else 1f,
        animationSpec = ColorOSMotion.FluidFloat,
        label = "capsule_alpha"
    )

    var userInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(layer) {
        val thumbnail = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)
        val buffer = IntBuffer.allocate(25)
        val scaleRect = android.graphics.Rect(0, 0, 5, 5)
        while (isActive) {
            try {
                if (!userInteracting && layer.size.width > 0 && layer.size.height > 0) {
                    val avg = withContext(Dispatchers.Default) {
                        val imageBitmap = layer.toImageBitmap()
                        android.graphics.Canvas(thumbnail).drawBitmap(
                            imageBitmap.asAndroidBitmap(), null, scaleRect, null
                        )
                        buffer.rewind()
                        thumbnail.copyPixelsToBuffer(buffer)
                        (0 until 25).sumOf { i ->
                            val c = buffer.get(i)
                            val r = (c shr 16 and 0xFF) / 255.0
                            val g = (c shr 8 and 0xFF) / 255.0
                            val b = (c and 0xFF) / 255.0
                            0.2126 * r + 0.7152 * g + 0.0722 * b
                        } / 25.0
                    }
                    luminanceAnimation.animateTo(
                        avg.toFloat().coerceAtMost(0.8f),
                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                    )
                }
            } catch (_: Throwable) { }
            delay(2.seconds)
        }
    }

    val searchLayer = rememberGraphicsLayer()
    val searchInteraction = rememberGlassInteraction()
    val density = LocalDensity.current
    var barWidthPx by remember { mutableIntStateOf(0) }
    val isRouteInTabs = currentRoute in navRoutes
    val navIndex = navRoutes.indexOf(currentRoute).let { if (it >= 0) it else 0 }
    val totalSlots = 4f
    val slotWidthPx = if (barWidthPx > 0) barWidthPx / totalSlots else 0f
    val coroutineScope = rememberCoroutineScope()
    var currentTabIndex by remember { mutableIntStateOf(navIndex) }
    val currentRouteState = rememberUpdatedState(currentRoute)

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm),
    ) {
        val searchButtonSize = 56.dp
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val horizontalPadding = Spacing.sm * 2
        val maxCapsuleWidth = minOf(screenWidth - horizontalPadding - searchButtonSize - Spacing.md, 304.dp)

        var activeTouchPosition by remember { mutableStateOf(Offset.Zero) }
        val dampedDrag =
            remember(coroutineScope, navRoutes.size) {
                DampedDragAnimation(
                    animationScope = coroutineScope,
                    initialValue = navIndex.toFloat(),
                    valueRange = 0f..(totalSlots - 1f),
                    visibilityThreshold = 0.001f,
                    initialScale = 1f,
                    pressedScale = 1.08f,
                    totalSlots = totalSlots,
                    getBarWidthPx = { barWidthPx },
                    onDragStarted = { position ->
                        userInteracting = true
                        activeTouchPosition = position
                    },
                    onDrag = { currentSlot, position ->
                        activeTouchPosition = position
                        val hoveredIndex = currentSlot.roundToInt().coerceIn(0, totalSlots.toInt() - 1)
                        if (currentTabIndex != hoveredIndex) {
                            currentTabIndex = hoveredIndex
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDragFinished = { targetIndex ->
                        userInteracting = false
                        currentTabIndex = targetIndex
                        val targetRoute = navRoutes[targetIndex]
                        if (targetRoute != currentRouteState.value) {
                            PosterTrace.reset()
                            PosterStagger.reset()
                            currentOnNavigate(targetRoute)
                        }
                    },
                    onTap = { slot ->
                        userInteracting = false
                        val prevIndex = currentTabIndex
                        currentTabIndex = slot
                        if (prevIndex != slot) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        val targetRoute = navRoutes[slot]
                        if (targetRoute != currentRouteState.value) {
                            PosterTrace.reset()
                            PosterStagger.reset()
                            currentOnNavigate(targetRoute)
                        }
                    },
                )
            }

        val unifiedPressProgress = dampedDrag.pressProgress

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NavBarHeight.bar),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(maxCapsuleWidth)
                    .height(NavBarHeight.bar)
                    .graphicsLayer {
                        alpha = capsuleAlpha
                        scaleX = lerp(0.94f, 1f, capsuleAlpha)
                        scaleY = lerp(0.94f, 1f, capsuleAlpha)
                    }
                    .onSizeChanged { barWidthPx = it.width },
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBackdropCustomShape(
                            backdrop = backdrop,
                            layer = layer,
                            luminanceAnimation = luminanceAnimation.value,
                            shape = BarShape,
                            isDark = isDark,
                            pressProgress = unifiedPressProgress,
                            touchPosition = { activeTouchPosition },
                            pressedScale = 1.08f,
                        )
                )

                LaunchedEffect(navIndex, isRouteInTabs) {
                    if (!userInteracting && isRouteInTabs) {
                        currentTabIndex = navIndex
                        if (barWidthPx > 0) {
                            dampedDrag.animateToValue(navIndex.toFloat())
                        }
                    }
                }

                val pillTargetAlpha = if (capsuleAlpha > 0.15f && isRouteInTabs) 1f else 0f
                val pillAlpha by animateFloatAsState(
                    targetValue = pillTargetAlpha,
                    animationSpec = ColorOSMotion.TouchBounceSpring,
                    label = "pill_alpha"
                )

                if (pillAlpha > 0.05f && barWidthPx > 0) {
                    val l = (luminanceAnimation.value * 2f - 1f).let { sign(it) * it * it }
                    val progress = dampedDrag.pressProgress
                    val restWidth = with(density) { 58.dp.toPx() }
                    val pressedWidth = with(density) { 74.dp.toPx() }
                    val pillWidthPx = lerp(restWidth, pressedWidth, progress)

                    val restHeight = with(density) { 48.dp.toPx() }
                    val pressedHeight = with(density) { 50.dp.toPx() }
                    val pillHeightPx = lerp(restHeight, pressedHeight, progress)

                    val pillCenterPx = (dampedDrag.value + 0.5f) * slotWidthPx
                    val translationX = pillCenterPx - pillWidthPx / 2f
                    val translationY = with(density) { (NavBarHeight.bar.toPx() - pillHeightPx) / 2f }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                this.translationX = translationX
                                this.translationY = translationY
                                alpha = pillAlpha
                            }
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { CapsuleShape },
                                effects = {
                                    vibrancy()
                                    colorControls(
                                        brightness = if (isDark) 0.05f else 0.10f,
                                        contrast = 1.05f,
                                        saturation = 1.5f,
                                    )
                                    blur(
                                        (if (l > 0f) lerp(4f.dp.toPx(), 8f.dp.toPx(), l)
                                        else lerp(4f.dp.toPx(), 1f.dp.toPx(), -l)) + 6f.dp.toPx(),
                                    )
                                    val pressProg = dampedDrag.pressProgress
                                    refractionWithDispersion(5f.dp.toPx() * pressProg, 7f.dp.toPx() * pressProg, hasDepthEffect = false)
                                },
                                highlight = null,
                                shadow = null,
                                innerShadow = null,
                                layerBlock = {
                                    scaleX = dampedDrag.scaleX
                                    scaleY = dampedDrag.scaleY
                                    val speed = abs(dampedDrag.velocity / 10f)
                                    val stretch = (speed * 0.75f).fastCoerceIn(0f, 0.2f)
                                    scaleX /= (1f - stretch)
                                    scaleY *= (1f - stretch * 0.4f)
                                },
                                onDrawSurface = {
                                    val pillRadius = size.height / 2f
                                    // 1. Electric Blue Luminous Fill
                                    val glowBrush = if (isDark) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF0082FB).copy(alpha = 0.28f),
                                                Color(0xFF005AC2).copy(alpha = 0.14f),
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF0082FB).copy(alpha = 0.18f),
                                                Color(0xFF007AE6).copy(alpha = 0.08f),
                                            )
                                        )
                                    }
                                    drawRoundRect(
                                        brush = glowBrush,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillRadius, pillRadius),
                                    )

                                    // 2. Central Radiant Glow
                                    drawRoundRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF00D2FF).copy(alpha = if (isDark) 0.22f else 0.14f),
                                                Color.Transparent,
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.width * 0.65f,
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillRadius, pillRadius),
                                        blendMode = BlendMode.Plus,
                                    )

                                    // 3. Ultra-fine Luminous Glass Border
                                    val pillStrokeWidth = 0.85.dp.toPx()
                                    val pillInset = pillStrokeWidth / 2f
                                    val innerPillRadius = (pillRadius - pillInset).coerceAtLeast(0f)
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF00D2FF).copy(alpha = if (isDark) 0.55f else 0.35f),
                                                Color(0xFF0082FB).copy(alpha = if (isDark) 0.20f else 0.12f),
                                                Color.Transparent,
                                            )
                                        ),
                                        topLeft = Offset(pillInset, pillInset),
                                        size = Size(size.width - pillStrokeWidth, size.height - pillStrokeWidth),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(innerPillRadius, innerPillRadius),
                                        style = Stroke(width = pillStrokeWidth),
                                    )
                                },
                            )
                            .width(with(density) { pillWidthPx.toDp() })
                            .height(with(density) { pillHeightPx.toDp() }),
                    )
                }

                Box(
                    modifier = Modifier.matchParentSize().then(dampedDrag.modifier),
                ) {
                    navRoutes.forEachIndexed { position, route ->
                        val iconCenterPx = (position + 0.5f) * slotWidthPx
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(with(density) { slotWidthPx.toDp() })
                                .graphicsLayer {
                                    translationX = iconCenterPx - slotWidthPx / 2f
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            val isSelected = isRouteInTabs && (currentTabIndex == position)
                            val tabDescription = when (route) {
                                "home" -> "Home tab"
                                "airing" -> "Airing shows tab"
                                "complete" -> "Completed shows tab"
                                else -> "Stopped shows tab"
                            }
                            NavBarIcon(
                                iconRes = when (route) {
                                    "home" -> R.drawable.ic_home
                                    "airing" -> R.drawable.ic_airing
                                    "complete" -> R.drawable.ic_complete
                                    else -> R.drawable.ic_stopped
                                },
                                contentDescription = tabDescription,
                                selected = isSelected,
                                isDark = isDark,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            val searchRotation by animateFloatAsState(
                targetValue = if (isSearchOpen) 90f else 0f,
                animationSpec = ColorOSMotion.TouchBounceSpring,
                label = "search_btn_rotate"
            )

            Box(
                modifier = Modifier
                    .size(searchButtonSize)
                    .graphicsLayer {
                        val scale = 1f - unifiedPressProgress * 0.10f
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .drawBackdropCustomShape(
                        backdrop = backdrop,
                        layer = searchLayer,
                        luminanceAnimation = luminanceAnimation.value,
                        shape = CircleShape,
                        isDark = isDark,
                        pressProgress = searchInteraction.pressProgress,
                        touchPosition = { searchInteraction.touchPosition },
                        pressedScale = 1.08f,
                        activeHighlight = isSearchOpen,
                    )
                    .pointerInput(searchInteraction) { searchInteraction.detectPress(this) }
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSearchClick()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = if (isSearchOpen) "Close search" else "Search",
                    modifier = Modifier
                        .size(IconSize.xl)
                        .graphicsLayer {
                            rotationZ = searchRotation
                        },
                    tint = if (isSearchOpen) Color(0xFFFF5252) else Color(0xFF0082FB),
                )
            }
        }
    }
}

@Composable
private fun NavBarIcon(
    iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    isDark: Boolean,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1.0f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "nav_icon_bounce"
    )
    val translationY by animateDpAsState(
        targetValue = if (selected) (-2).dp else 0.dp,
        animationSpec = ColorOSMotion.FluidDp,
        label = "nav_icon_nudge"
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "nav_dot_alpha"
    )
    val dotWidth by animateDpAsState(
        targetValue = if (selected) 8.dp else 0.dp,
        animationSpec = ColorOSMotion.FluidDp,
        label = "nav_dot_width"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationY = translationY.toPx()
            }
    ) {
        val selectedTint = Color(0xFF0082FB) // Official ColorOS 17 Electric Blue
        val unselectedTint = if (isDark) Color(0xFF8E97AB).copy(alpha = 0.70f) else Color(0xFF64748B).copy(alpha = 0.85f)

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(IconSize.xl),
            tint = if (selected) selectedTint else unselectedTint,
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Glowing active micro-capsule indicator
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(dotWidth)
                .graphicsLayer {
                    alpha = dotAlpha
                }
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00D2FF),
                            Color(0xFF0082FB),
                        )
                    ),
                    shape = RoundedCornerShape(percent = 50)
                )
        )
    }
}

private fun Modifier.drawBackdropCustomShape(
    backdrop: Backdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
    isDark: Boolean,
    pressProgress: Float = 0f,
    touchPosition: () -> Offset = { Offset.Zero },
    pressedScale: Float = 1.08f,
    activeHighlight: Boolean = false,
): Modifier =
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
            val press = pressProgress
            vibrancy()
            colorControls(
                brightness = if (isDark) 0.04f else 0.08f,
                contrast = 1.02f,
                saturation = 1.4f,
            )
            blur(
                (if (l > 0f) {
                    lerp(5f.dp.toPx(), 10f.dp.toPx(), l)
                } else {
                    lerp(5f.dp.toPx(), 2f.dp.toPx(), -l)
                }) + 2f.dp.toPx() * press,
            )
            refraction(size.minDimension / 8f + 2f.dp.toPx() * press, size.minDimension / 4f, false)
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            layer.record { drawBackdrop() }
        },
        onDrawSurface = {
            val radius = size.height / 2f
            // 1. Translucent frosted surface tint
            val baseColor = if (isDark) {
                val darken = lerp(0.48f, 0.72f, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
                Color(0xFF0A0E18).copy(alpha = darken)
            } else {
                val lighten = lerp(0.65f, 0.82f, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
                Color.White.copy(alpha = lighten)
            }
            drawRoundRect(
                color = baseColor,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )

            // Active Highlight (e.g. search mode)
            if (activeHighlight) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0082FB).copy(alpha = 0.22f),
                            Color(0xFF005AC2).copy(alpha = 0.12f),
                        )
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                )
            }

            // 2. Specular Top Rim Reflection (Signature ColorOS 17 / VisionOS optical glass rim)
            val strokeWidth = 0.85.dp.toPx()
            val inset = strokeWidth / 2f
            val innerRadius = (radius - inset).coerceAtLeast(0f)
            val rimBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.30f else 0.75f),
                    Color.White.copy(alpha = if (isDark) 0.08f else 0.20f),
                    Color.White.copy(alpha = if (isDark) 0.02f else 0.05f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = size.height,
            )
            drawRoundRect(
                brush = rimBrush,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius, innerRadius),
                style = Stroke(width = strokeWidth),
            )

            // 3. Touch ripple caustic bloom
            val press = pressProgress
            if (press > 0f) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0082FB).copy(alpha = 0.16f * press),
                            Color.White.copy(alpha = 0.12f * press),
                            Color.Transparent,
                        ),
                        center = let {
                            val pos = touchPosition()
                            if (pos != Offset.Zero) pos else Offset(size.width / 2f, size.height / 2f)
                        },
                        radius = size.minDimension * 1.3f,
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    blendMode = BlendMode.Plus,
                )
            }
        },
        layerBlock = {
            val scale = lerp(1f, pressedScale, pressProgress)
            scaleX = scale
            scaleY = scale
        },
    )

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val totalSlots: Float,
    val getBarWidthPx: () -> Int,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDrag: DampedDragAnimation.(currentSlot: Float, position: Offset) -> Unit,
    val onDragFinished: DampedDragAnimation.(targetIndex: Int) -> Unit,
    val onTap: DampedDragAnimation.(slot: Int) -> Unit,
) {
    private val snapAnimationSpec = ColorOSMotion.NavPillSpring
    private val pressProgressAnimationSpec = ColorOSMotion.ExpressiveDefaultSpatialSpring
    private val scaleXAnimationSpec = ColorOSMotion.ExpressiveDefaultSpatialSpring
    private val scaleYAnimationSpec = ColorOSMotion.ExpressiveDefaultSpatialSpring
    private val velocityAnimationSpec = ColorOSMotion.ExpressiveSlowSpatialSpring

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)
    private val velocityAnimation = Animatable(0f, 5f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier =
        Modifier.pointerInput(totalSlots) {
            val touchSlop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val startTouchX = down.position.x
                val startSlotValue = valueAnimation.value
                val startSlotIndex = startSlotValue.roundToInt().coerceIn(0, totalSlots.toInt() - 1)
                var hasDragged = false

                velocityTracker.resetTracking()
                velocityTracker.addPosition(down.uptimeMillis, down.position)

                onDragStarted(down.position)
                press()

                val pointerId = down.id
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId }
                    if (change == null) {
                        if (hasDragged) {
                            val currentSlot = valueAnimation.value
                            val targetIndex = currentSlot.roundToInt().coerceIn(0, totalSlots.toInt() - 1)
                            animateToValue(targetIndex.toFloat())
                            onDragFinished(targetIndex)
                        }
                        release()
                        break
                    }

                    velocityTracker.addPosition(change.uptimeMillis, change.position)

                    if (change.changedToUpIgnoreConsumed()) {
                        change.consume()
                        val barWidth = getBarWidthPx()
                        val slotWidth = if (totalSlots > 0 && barWidth > 0) barWidth / totalSlots else 0f

                        if (!hasDragged) {
                            val slot = if (slotWidth > 0f) {
                                (down.position.x / slotWidth).toInt().coerceIn(0, totalSlots.toInt() - 1)
                            } else 0
                            animateToValue(slot.toFloat())
                            onTap(slot)
                        } else {
                            val totalDeltaX = change.position.x - startTouchX
                            val currentSlot = if (slotWidth > 0f) {
                                (startSlotValue + totalDeltaX / slotWidth).coerceIn(0f, totalSlots - 1f)
                            } else startSlotValue

                            val vx = velocityTracker.calculateVelocity().x
                            val flingThreshold = 400f
                            val targetIndex = when {
                                vx > flingThreshold -> {
                                    val minAdvance = (startSlotIndex + 1).coerceAtMost(totalSlots.toInt() - 1)
                                    maxOf(minAdvance, currentSlot.roundToInt()).coerceIn(0, totalSlots.toInt() - 1)
                                }
                                vx < -flingThreshold -> {
                                    val maxRetreat = (startSlotIndex - 1).coerceAtLeast(0)
                                    minOf(maxRetreat, currentSlot.roundToInt()).coerceIn(0, totalSlots.toInt() - 1)
                                }
                                else -> {
                                    currentSlot.roundToInt().coerceIn(0, totalSlots.toInt() - 1)
                                }
                            }
                            animateToValue(targetIndex.toFloat())
                            onDragFinished(targetIndex)
                        }
                        release()
                        break
                    }

                    val totalDeltaX = change.position.x - startTouchX
                    if (!hasDragged && abs(totalDeltaX) > touchSlop) {
                        hasDragged = true
                    }

                    if (hasDragged) {
                        change.consume()
                        val barWidth = getBarWidthPx()
                        val slotWidth = if (totalSlots > 0 && barWidth > 0) barWidth / totalSlots else 0f
                        if (slotWidth > 0f) {
                            val currentSlot = (startSlotValue + totalDeltaX / slotWidth).coerceIn(0f, totalSlots - 1f)
                            snapValue(currentSlot)
                            val vx = velocityTracker.calculateVelocity().x
                            val slotVelocity = vx / slotWidth
                            val targetVelocity = (slotVelocity / 3f).coerceIn(-2.5f, 2.5f)
                            animationScope.launch {
                                velocityAnimation.snapTo(targetVelocity)
                            }
                            onDrag(currentSlot, change.position)
                        }
                    }
                }
            }
        }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
            launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
        }
    }

    fun snapValue(value: Float) {
        val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
        animationScope.launch {
            mutatorMutex.mutate(MutatePriority.UserInput) {
                valueAnimation.snapTo(target)
            }
        }
    }

    fun animateToValue(value: Float) {
        val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
        animationScope.launch {
            mutatorMutex.mutate(MutatePriority.Default) {
                valueAnimation.animateTo(target, snapAnimationSpec)
            }
        }
    }
}
