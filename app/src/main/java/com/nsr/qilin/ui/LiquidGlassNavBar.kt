package com.nsr.qilin.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.*
import com.nsr.qilin.R
import com.nsr.qilin.ui.common.PosterStagger
import com.nsr.qilin.ui.common.PosterTrace
import com.nsr.qilin.ui.theme.ColorOSMotion
import com.nsr.qilin.ui.theme.CornerRadius
import com.nsr.qilin.ui.theme.Haptics
import com.nsr.qilin.ui.theme.IconSize
import com.nsr.qilin.ui.theme.NavBarHeight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.refractionWithDispersion
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

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
    val targetLuminance = if (isDark) 0.18f else 0.72f
    val luminanceAnimation = remember { Animatable(targetLuminance) }

    LaunchedEffect(isDark) {
        luminanceAnimation.animateTo(
            targetValue = if (isDark) 0.18f else 0.72f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    val currentOnNavigate by rememberUpdatedState(onNavigate)

    val capsuleAlpha by animateFloatAsState(
        targetValue = if (isSearchOpen) 0f else 1f,
        animationSpec = ColorOSMotion.FluidFloat
    )

    var userInteracting by remember { mutableStateOf(false) }

    val searchInteraction = rememberGlassInteraction()
    val density = LocalDensity.current
    var barWidthPx by remember { mutableIntStateOf(0) }
    val navIndex = navRoutes.indexOf(currentRoute).coerceAtLeast(0)
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
        val searchButtonSize = NavBarHeight.bar * 0.95f
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val horizontalPadding = Spacing.sm * 2
        val maxCapsuleWidth = minOf((screenWidth - horizontalPadding - searchButtonSize - Spacing.sm) * 0.9f, 262.dp)
        val targetLeftMargin = 48.dp // doubled margin from display edge
        val initialSearchCenterX = (screenWidth / 2f) + (maxCapsuleWidth / 2f) + Spacing.sm + (searchButtonSize / 2f)
        val targetSearchCenterX = targetLeftMargin + (searchButtonSize / 2f)
        val searchSlideDistance = (initialSearchCenterX - targetSearchCenterX).coerceAtLeast(0.dp)
        val searchButtonOffset by animateDpAsState(
            targetValue = if (isSearchOpen) -searchSlideDistance else 0.dp,
            animationSpec = ColorOSMotion.FluidDp
        )

        var activeTouchPosition by remember { mutableStateOf(Offset.Zero) }
        val dampedDrag =
            remember(coroutineScope, navRoutes.size) {
                DampedDragAnimation(
                    animationScope = coroutineScope,
                    initialValue = navIndex.toFloat(),
                    valueRange = 0f..(totalSlots - 1f),
                    visibilityThreshold = 0.001f,
                    initialScale = 1f,
                    pressedScale = 1.1f,
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
                            Haptics.soft()
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
                        currentTabIndex = slot
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
                    }
                    .onSizeChanged { barWidthPx = it.width },
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBackdropCustomShape(
                            backdrop = backdrop,
                            luminanceAnimation = luminanceAnimation.value,
                            shape = BarShape,
                            pressProgress = unifiedPressProgress,
                            touchPosition = { activeTouchPosition },
                            pressedScale = 1.12f,
                        )
                )

            LaunchedEffect(navIndex) {
                if (!userInteracting) {
                    currentTabIndex = navIndex
                    if (barWidthPx > 0) {
                        dampedDrag.animateToValue(navIndex.toFloat())
                    }
                }
            }

            if (capsuleAlpha > 0.15f) {
                if (barWidthPx > 0) {
                val l = (luminanceAnimation.value * 2f - 1f).let { sign(it) * it * it }
                val progress = dampedDrag.pressProgress
                val restWidth = with(density) { 56.dp.toPx() }
                val pressedWidth = with(density) { 76.dp.toPx() }
                val pillWidthPx = lerp(restWidth, pressedWidth, progress)

                val restHeight = restWidth
                val pressedHeight = with(density) { 56.dp.toPx() }
                val pillHeightPx = lerp(restHeight, pressedHeight, progress)

                val pillCenterPx = (dampedDrag.value + 0.5f) * slotWidthPx
                val translationX = pillCenterPx - pillWidthPx / 2f
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            this.translationX = translationX
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CapsuleShape },
                            effects = {
                                vibrancy()
                                colorControls(
                                    brightness = if (isDark) 0.05f else 0.1f,
                                    contrast = 1f,
                                    saturation = 1.5f,
                                )
                                blur(
                                    (if (l > 0f) lerp(4f.dp.toPx(), 8f.dp.toPx(), l)
                                    else lerp(4f.dp.toPx(), 1f.dp.toPx(), -l)) + 6f.dp.toPx(),
                                )
                                val pressProg = dampedDrag.pressProgress
                                refractionWithDispersion(5f.dp.toPx() * pressProg, 7f.dp.toPx() * pressProg, hasDepthEffect = false)
                            },
                            highlight = { Highlight.Default.copy(alpha = if (isDark) 0.6f else 0.8f) },
                            shadow = { Shadow(radius = 4f.dp, alpha = if (isDark) 0.4f else 0.2f) },
                            innerShadow = {
                                val pressProg = dampedDrag.pressProgress
                                InnerShadow(radius = 8f.dp * pressProg, alpha = pressProg)
                            },
                            layerBlock = {
                                scaleX = dampedDrag.scaleX
                                scaleY = dampedDrag.scaleY
                                val speed = abs(dampedDrag.velocity / 10f)
                                val stretch = (speed * 0.75f).fastCoerceIn(0f, 0.2f)
                                scaleX /= (1f - stretch)
                                scaleY *= (1f - stretch * 0.4f)
                            },
                            onDrawSurface = {
                                val darken = lerp(0.22f, 0.55f, ((luminanceAnimation.value - 0.3f) / 0.5f).coerceIn(0f, 1f))
                                drawRect(Color.Black.copy(alpha = darken))
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
                        val tabDescription = when (route) {
                            "home" -> "Home tab"
                            "airing" -> "Airing shows tab"
                            "complete" -> "Completed shows tab"
                            else -> "Upcoming and stopped shows tab"
                        }
                        NavBarIcon(
                            iconRes = when (route) {
                                "home" -> R.drawable.ic_home
                                "airing" -> R.drawable.ic_airing
                                "complete" -> R.drawable.ic_complete
                                else -> R.drawable.ic_stopped
                            },
                            contentDescription = tabDescription,
                            selected = currentTabIndex == position,
                        )
                    }
                }
            }
    }
}

    Spacer(modifier = Modifier.width(Spacing.sm))

    val searchRotation by animateFloatAsState(
        targetValue = if (isSearchOpen) 90f else 0f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "search_btn_rotate"
    )

    Box(
        modifier = Modifier
            .offset(x = searchButtonOffset)
            .graphicsLayer {
                val press = searchInteraction.pressProgress
                val pressScale = 1f - press * 0.12f
                val capsulePushScale = 1f - unifiedPressProgress * 0.15f
                val combinedScale = pressScale * capsulePushScale
                scaleX = combinedScale
                scaleY = combinedScale
                translationX = unifiedPressProgress * 12.dp.toPx()
            }
            .size(searchButtonSize)
            .clip(CircleShape)
            .drawBackdropCustomShape(
                backdrop = backdrop,
                luminanceAnimation = luminanceAnimation.value,
                shape = CircleShape,
                pressProgress = searchInteraction.pressProgress,
                touchPosition = { searchInteraction.touchPosition },
                activeHighlight = isSearchOpen,
            )
            .pointerInput(searchInteraction) { searchInteraction.detectPress(this) }
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = {
                    Haptics.tick()
                    onSearchClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
            contentDescription = if (isSearchOpen) "Close Search" else "Search",
            modifier = Modifier
                .size(IconSize.xl)
                .graphicsLayer {
                    rotationZ = searchRotation
                },
            tint = if (isSearchOpen) Color(0xFF0082FB) else MaterialTheme.colorScheme.primary,
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
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.16f else 1.0f,
        animationSpec = spring(dampingRatio = 0.46f, stiffness = 320f),
        label = "nav_icon_bounce"
    )
    val lift by animateDpAsState(
        targetValue = if (selected) (-2).dp else 0.dp,
        animationSpec = ColorOSMotion.FluidDp,
        label = "nav_icon_lift"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 220, easing = ColorOSMotion.FastOutSlowIn),
        label = "nav_icon_tint"
    )
    val glow by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = ColorOSMotion.FastOutSlowIn),
        label = "nav_icon_glow"
    )
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (selected) {
            tilt.snapTo(0f)
            tilt.animateTo(-13f, tween(durationMillis = 95, easing = ColorOSMotion.EmphasizedDecelerate))
            tilt.animateTo(0f, spring(dampingRatio = 0.32f, stiffness = 240f))
        }
    }
    Box(
        modifier = Modifier
            .offset(y = lift)
            .size(NavBarHeight.icon)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = tilt.value
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer { alpha = glow * 0.32f }
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                        )
                    ),
                    CircleShape
                )
        )
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(IconSize.xl),
            tint = tint,
        )
    }
}

private fun Modifier.drawBackdropCustomShape(
    backdrop: Backdrop,
    luminanceAnimation: Float,
    shape: Shape,
    pressProgress: Float = 0f,
    touchPosition: () -> Offset = { Offset.Zero },
    pressedScale: Float = 1.12f,
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
                brightness = 0.05f,
                contrast = 1f,
                saturation = 1.5f,
            )
            blur(
                (if (l > 0f) {
                    lerp(4f.dp.toPx(), 8f.dp.toPx(), l)
                } else {
                    lerp(4f.dp.toPx(), 1f.dp.toPx(), -l)
                }) + 2f.dp.toPx() * press,
            )
            refraction(size.minDimension / 8f + 2f.dp.toPx() * press, size.minDimension / 4f, false)
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
        },
        onDrawSurface = {
            val darken = lerp(0.12f, 0.5f, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
            val radius = size.height / 2f
            drawRoundRect(
                color = Color.Black.copy(alpha = darken),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
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
            val press = pressProgress
            if (press > 0f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f * press),
                            Color.Transparent,
                        ),
                        center = let {
                            val pos = touchPosition()
                            if (pos != Offset.Zero) pos else Offset(size.width / 2f, size.height / 2f)
                        },
                        radius = size.minDimension * 1.2f,
                    ),
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

class GlassInteraction(
    private val animationScope: CoroutineScope,
) {
    private val pressSpec = spring<Float>(dampingRatio = 0.65f, stiffness = 220f, visibilityThreshold = 0.001f)
    private val pressAnimation = Animatable(0f, 0.001f)

    val pressProgress: Float get() = pressAnimation.value

    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    suspend fun detectPress(pointer: PointerInputScope) =
        with(pointer) {
            inspectDragGestures(
                onDragStart = { down ->
                    touchPosition = down.position
                    animationScope.launch { pressAnimation.animateTo(1f, pressSpec) }
                },
                onDragEnd = {
                    animationScope.launch { pressAnimation.animateTo(0f, pressSpec) }
                },
                onDragCancel = { animationScope.launch { pressAnimation.animateTo(0f, pressSpec) } },
            ) { change, _ ->
                touchPosition = change.position
            }
        }
}

@Composable
fun rememberGlassInteraction(): GlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlassInteraction(scope) }
}

internal suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        onDragStart(down)
        onDrag(down, Offset.Zero)
        val upEvent =
            drag(
                pointerId = down.id,
                onDrag = { onDrag(it, it.positionChange()) },
            )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) {
        return null
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) {
            return null
        }
        if (change.changedToUpIgnoreConsumed()) {
            return change
        }
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) {
                return dragEvent
            }
        }
    }
}
