package com.mydonghua.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.BiasAlignment
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.*
import com.mydonghua.app.R
import com.mydonghua.app.ui.common.PosterStagger
import com.mydonghua.app.ui.common.PosterTrace
import com.mydonghua.app.ui.theme.ColorOSMotion
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.NavBarHeight
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import java.nio.IntBuffer
import kotlin.time.Duration.Companion.seconds

private val navRoutes = listOf("home", "airing", "complete", "stopped")
private val BlobHeight = NavBarHeight.blob
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

    val capsuleAlpha by animateFloatAsState(
        targetValue = if (isSearchOpen) 0f else 1f,
        animationSpec = tween(durationMillis = 250)
    )

    var userInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(layer) {
        val thumbnail = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)
        val buffer = IntBuffer.allocate(25)
        val scaleRect = android.graphics.Rect(0, 0, 5, 5)
        while (isActive) {
            try {
                if (!userInteracting && layer.size.width > 0 && layer.size.height > 0) {
                    val readT0 = SystemClock.elapsedRealtime()
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
                    val readMs = SystemClock.elapsedRealtime() - readT0
                    if (readMs > 8) {
                        android.util.Log.d("NavLag", "readback took ${readMs}ms")
                    }
                    luminanceAnimation.animateTo(avg.toFloat().coerceAtMost(0.8f), tween(500))
                }
            } catch (_: Throwable) { }
            delay(2.seconds)
        }
    }

    val searchLayer = rememberGraphicsLayer()
    val searchInteraction = rememberGlassInteraction()
    val density = LocalDensity.current
    var barWidthPx by remember { mutableIntStateOf(0) }
    val navIndex = navRoutes.indexOf(currentRoute).coerceAtLeast(0)
    val totalSlots = 4f
    val slotWidthPx = if (barWidthPx > 0) barWidthPx / totalSlots else 0f
    val coroutineScope = rememberCoroutineScope()
    var currentTabIndex by remember { mutableIntStateOf(navIndex) }

    LaunchedEffect(Unit) {
        android.util.Log.e("NavLag", "diagnostic build running")
    }

    fun navToValue(index: Int): Float = index.toFloat()

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
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing
            )
        )

        val draggedFlag = remember { booleanArrayOf(false) }
        var activeTouchPosition by remember { mutableStateOf(Offset.Zero) }
        val dampedDrag =
            remember(coroutineScope, navRoutes.size) {
                DampedDragAnimation(
                    animationScope = coroutineScope,
                    initialValue = navToValue(navIndex),
                    valueRange = 0f..(totalSlots - 1f),
                    visibilityThreshold = 0.001f,
                    initialScale = 1f,
                    pressedScale = 1.1f,
                    totalSlots = totalSlots,
                    getBarWidthPx = { barWidthPx },
                    onDragStarted = { position ->
                        draggedFlag[0] = false
                        userInteracting = true
                        activeTouchPosition = position
                    },
                    onDragStopped = {
                        userInteracting = false
                        activeTouchPosition = Offset.Zero
                        if (draggedFlag[0]) {
                            val vel = velocity
                            val targetIndex = when {
                                vel > 0.4f -> (value + 0.4f).toInt().coerceIn(0, totalSlots.toInt() - 1)
                                vel < -0.4f -> (value - 0.4f).toInt().coerceIn(0, totalSlots.toInt() - 1)
                                else -> value.roundToInt().coerceIn(0, totalSlots.toInt() - 1)
                            }
                            val safeIndex =
                                if (targetIndex in navRoutes.indices) targetIndex else currentTabIndex
                            currentTabIndex = safeIndex
                            animateToValue(navToValue(safeIndex))
                            if (safeIndex != navIndex) {
                                PosterTrace.reset()
                                PosterStagger.reset()
                                currentOnNavigate(navRoutes[safeIndex])
                            }
                        }
                    },
                    onDrag = { _, dragAmount, position ->
                        if (dragAmount.x != 0f) draggedFlag[0] = true
                        activeTouchPosition = position
                        val slotWidth = if (totalSlots > 0) barWidthPx / totalSlots else 0f
                        if (slotWidth > 0f) {
                            val next = (value + dragAmount.x / slotWidth).coerceIn(0f, totalSlots - 1f)
                            snapValue(next)
                            currentTabIndex = next.roundToInt().coerceIn(0, totalSlots.toInt() - 1)
                        }
                    },
                    onTap = { slot ->
                        activeTouchPosition = Offset.Zero
                        if (slot != currentTabIndex) {
                            PosterTrace.reset()
                            PosterStagger.reset()
                            currentTabIndex = slot
                            currentOnNavigate(navRoutes[slot])
                        }
                        animateToValue(navToValue(slot))
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
                            layer = layer,
                            luminanceAnimation = luminanceAnimation.value,
                            shape = BarShape,
                            pressProgress = unifiedPressProgress,
                            touchPosition = { activeTouchPosition },
                            pressedScale = 1.12f,
                        )
                )

            LaunchedEffect(navIndex) {
                currentTabIndex = navIndex
                android.util.Log.e(
                    "NavLag",
                    "navIndex=$navIndex barWidthPx=$barWidthPx"
                )
                if (barWidthPx > 0) {
                    userInteracting = true
                    val slideT0 = System.nanoTime()
                    dampedDrag.animateToValue(navToValue(navIndex))
                    val target = navToValue(navIndex)
                    var last = 0L
                    var maxGap = 0L
                    var frames = 0
                    while (abs(dampedDrag.value - target) > 0.01f) {
                        val now = withFrameNanos { it }
                        if (last != 0L) maxGap = maxOf(maxGap, now - last)
                        frames++
                        last = now
                    }
                    val slideMs = (System.nanoTime() - slideT0) / 1_000_000
                    android.util.Log.e(
                        "NavLag",
                        "slide=${slideMs}ms frames=$frames maxFrameGapMs=${maxGap / 1_000_000L} posters=${PosterTrace.composedCount}"
                    )
                    userInteracting = false
                }
            }

            LaunchedEffect(dampedDrag) {
                snapshotFlow { currentTabIndex }
                    .drop(1)
                    .collectLatest { index ->
                        if (draggedFlag[0] && !userInteracting) {
                            PosterTrace.reset()
                            PosterStagger.reset()
                            currentOnNavigate(navRoutes[index])
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
                                val velocity = dampedDrag.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
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

    Box(
        modifier = Modifier
            .offset(x = searchButtonOffset)
            .graphicsLayer {
                val scale = 1f - unifiedPressProgress * 0.15f
                scaleX = scale
                scaleY = scale
                translationX = unifiedPressProgress * 12.dp.toPx()
            }
            .size(searchButtonSize)
            .clip(CircleShape)
            .drawBackdropCustomShape(
                backdrop = backdrop,
                layer = searchLayer,
                luminanceAnimation = luminanceAnimation.value,
                shape = CircleShape,
                pressProgress = searchInteraction.pressProgress,
                touchPosition = { searchInteraction.touchPosition },
            )
            .pointerInput(searchInteraction) { searchInteraction.detectPress(this) }
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onSearchClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search",
            modifier = Modifier.size(IconSize.xl),
            tint = MaterialTheme.colorScheme.primary,
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
        targetValue = if (selected) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "nav_icon_bounce"
    )
    Box(
        modifier = Modifier
            .size(NavBarHeight.icon)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CapsuleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(IconSize.xl),
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Modifier.drawBackdropCustomShape(
    backdrop: Backdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
    pressProgress: Float = 0f,
    touchPosition: () -> Offset = { Offset.Zero },
    pressedScale: Float = 1.12f,
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
            val recT0 = SystemClock.elapsedRealtime()
            drawBackdrop()
            layer.record { drawBackdrop() }
            val recMs = SystemClock.elapsedRealtime() - recT0
            if (recMs > 6) {
                android.util.Log.d("NavLag", "backdrop record took ${recMs}ms")
            }
        },
        onDrawSurface = {
            val darken = lerp(0.12f, 0.5f, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
            val radius = size.height / 2f
            drawRoundRect(
                color = Color.Black.copy(alpha = darken),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
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
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset, position: Offset) -> Unit,
    val onTap: DampedDragAnimation.(slot: Int) -> Unit,
) {
    private val valueAnimationSpec = spring<Float>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
    private val smoothAnimationSpec = spring<Float>(dampingRatio = 0.60f, stiffness = Spring.StiffnessMediumLow)
    private val velocityAnimationSpec = spring<Float>(dampingRatio = 0.50f, stiffness = 300f)
    private val pressProgressAnimationSpec = spring<Float>(dampingRatio = 0.65f, stiffness = 400f)
    private val scaleXAnimationSpec = spring<Float>(dampingRatio = 0.55f, stiffness = 250f)
    private val scaleYAnimationSpec = spring<Float>(dampingRatio = 0.65f, stiffness = 250f)
    private val snapAnimationSpec = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

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
                val startX = down.position.x
                var hasDragged = false

                onDragStarted(down.position)
                press()

                val pointerId = down.id
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                    if (change.changedToUpIgnoreConsumed()) {
                        change.consume()
                        val barWidth = getBarWidthPx()
                        val slotWidth = if (totalSlots > 0 && barWidth > 0) barWidth / totalSlots else 0f
                        if (!hasDragged) {
                            val slot = if (slotWidth > 0f) {
                                (down.position.x / slotWidth).toInt().coerceIn(0, totalSlots.toInt() - 1)
                            } else 0
                            onTap(slot)
                        } else {
                            onDragStopped()
                        }
                        release()
                        break
                    }

                    val dragAmountX = change.positionChange().x
                    if (!hasDragged && abs(change.position.x - startX) > touchSlop) {
                        hasDragged = true
                    }

                    if (hasDragged) {
                        change.consume()
                        if (dragAmountX != 0f) {
                            onDrag(size, Offset(dragAmountX, 0f), change.position)
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
            withFrameNanos {}
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                while (abs(valueAnimation.value - valueAnimation.targetValue) >= threshold) {
                    withFrameNanos {}
                }
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun snapValue(value: Float) {
        val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
        animationScope.launch {
            mutatorMutex.mutate(MutatePriority.UserInput) {
                valueAnimation.snapTo(target)
                updateVelocity()
            }
        }
    }

    fun updateValue(value: Float) {
        snapValue(value)
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate(MutatePriority.Default) {
                press()
                val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
                launch { valueAnimation.animateTo(target, snapAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(SystemClock.uptimeMillis(), Offset(value, 0f))
        val targetVelocity =
            velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}
