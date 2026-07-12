package com.mydonghua.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.MutatorMutex
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
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.*
import com.mydonghua.app.R
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.NavBarHeight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
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
private val CapsuleShape = CornerRadius.full
private val BlobHeight = NavBarHeight.blob

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

    val capsuleAlpha by animateFloatAsState(
        targetValue = if (isSearchOpen) 0f else 1f,
        animationSpec = tween(durationMillis = 250)
    )



    LaunchedEffect(layer) {
        val buffer = IntBuffer.allocate(25)
        while (isActive) {
            try {
                if (layer.size.width > 0 && layer.size.height > 0) {
                    val imageBitmap = layer.toImageBitmap()
                    withContext(Dispatchers.IO) {
                        val thumbnail = Bitmap.createScaledBitmap(imageBitmap.asAndroidBitmap(), 5, 5, false)
                        buffer.rewind()
                        thumbnail.copyPixelsToBuffer(buffer)
                    }
                    val avg = (0 until 25).sumOf { i ->
                        val c = buffer.get(i)
                        val r = (c shr 16 and 0xFF) / 255.0
                        val g = (c shr 8 and 0xFF) / 255.0
                        val b = (c and 0xFF) / 255.0
                        0.2126 * r + 0.7152 * g + 0.0722 * b
                    } / 25.0
                    luminanceAnimation.animateTo(avg.toFloat().coerceAtMost(0.8f), tween(500))
                }
            } catch (_: Throwable) { }
            delay(1.seconds)
        }
    }

    val searchLayer = rememberGraphicsLayer()
    val searchInteraction = rememberGlassInteraction()
    val density = LocalDensity.current
    var barWidthPx by remember { mutableIntStateOf(0) }
    val navIndex = navRoutes.indexOf(currentRoute).coerceAtLeast(0)
    val totalSlots = 5f
    val slotWidthPx = if (barWidthPx > 0) barWidthPx / totalSlots else 0f
    val coroutineScope = rememberCoroutineScope()
    val barInteraction = rememberGlassInteraction()
    var currentTabIndex by remember { mutableIntStateOf(navIndex) }
    val draggedFlag = remember { booleanArrayOf(false) }

    fun navToValue(index: Int): Float =
        if (index < 2) index.toFloat() else (index + 1).toFloat()

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
    ) {
        val searchButtonSize = NavBarHeight.bar * 0.95f
        val maxCapsuleWidth = maxWidth - searchButtonSize - Spacing.sm
        val searchButtonOffset by animateDpAsState(
            targetValue = if (isSearchOpen) - (maxCapsuleWidth + Spacing.sm) else 0.dp,
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NavBarHeight.bar),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .width(maxCapsuleWidth)
                    .height(NavBarHeight.bar)
                    .graphicsLayer {
                        alpha = capsuleAlpha
                    }
                .clip(RoundedCornerShape(CornerRadius.pill))
                .drawBackdropCustomShape(
                    backdrop = backdrop,
                    layer = layer,
                    luminanceAnimation = luminanceAnimation.value,
                    shape = RoundedCornerShape(CornerRadius.pill),
                    interaction = barInteraction,
                    pressedScale = 1.12f,
                )
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(CornerRadius.pill),
                )
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(CornerRadius.pill),
                )
                .onSizeChanged { barWidthPx = it.width }
                .pointerInput(barInteraction) { barInteraction.detectPress(this) },
            contentAlignment = Alignment.CenterStart,
        ) {
            val dampedDrag =
                remember(coroutineScope, navRoutes.size) {
                    DampedDragAnimation(
                        animationScope = coroutineScope,
                        initialValue = navToValue(navIndex),
                        valueRange = 0f..totalSlots,
                        visibilityThreshold = 0.001f,
                        initialScale = 1f,
                        pressedScale = 60f / 56f,
                        onDragStarted = { draggedFlag[0] = false },
                        onDragStopped = {
                            if (draggedFlag[0]) {
                                val snapped = targetValue.roundToInt().coerceIn(0, totalSlots.toInt() - 1)
                                val targetIndex = when {
                                    snapped <= 1 -> snapped
                                    snapped == 2 -> if (targetValue > 2.5f) 2 else 1
                                    else -> snapped - 1
                                }
                                val safeIndex =
                                    if (targetIndex in navRoutes.indices) targetIndex else currentTabIndex
                                if (safeIndex != currentTabIndex) {
                                    currentTabIndex = safeIndex
                                }
                                updateValue(navToValue(currentTabIndex))
                            }
                        },
                        onDrag = { _, dragAmount ->
                            if (dragAmount.x != 0f) draggedFlag[0] = true
                            val slotWidth = barWidthPx / totalSlots
                            updateValue(
                                (targetValue + dragAmount.x / slotWidth).coerceIn(0f, totalSlots),
                            )
                        },
                    )
                }

            LaunchedEffect(navIndex) {
                currentTabIndex = navIndex
                if (barWidthPx > 0) {
                    dampedDrag.animateToValue(navToValue(navIndex))
                }
            }

            LaunchedEffect(dampedDrag) {
                snapshotFlow { currentTabIndex }
                    .drop(1)
                    .collectLatest { index ->
                        if (draggedFlag[0]) {
                            onNavigate(navRoutes[index])
                        }
                    }
            }

            if (capsuleAlpha > 0.15f) {
                if (barWidthPx > 0) {
                val l = (luminanceAnimation.value * 2f - 1f).let { sign(it) * it * it }
                val pillWidthPx = with(density) { (slotWidthPx - Spacing.sm.toPx()).coerceAtLeast(Spacing.xxxl.toPx()) }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = (dampedDrag.value + 0.5f) * slotWidthPx - pillWidthPx / 2f
                            scaleX = dampedDrag.scaleX
                            scaleY = dampedDrag.scaleY
                            val velocity = dampedDrag.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        }
                        .width(with(density) { pillWidthPx.toDp() })
                        .height(BlobHeight)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CapsuleShape },
                            effects = {
                                vibrancy()
                                colorControls(
                                    brightness = 0.05f,
                                    contrast = 1f,
                                    saturation = 1.5f,
                                )
                                blur(
                                    (if (l > 0f) lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                                    else lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)) + 20f.dp.toPx(),
                                )
                            },
                            highlight = { Highlight.Default.copy(alpha = 0.6f) },
                            shadow = { Shadow(radius = 4f.dp, alpha = 0.4f) },
                            innerShadow = {
                                val progress = dampedDrag.pressProgress
                                InnerShadow(radius = 8f.dp * progress, alpha = progress)
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
                        ),
                )
            }

            Row(
                modifier = Modifier.matchParentSize().then(dampedDrag.modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navRoutes.forEachIndexed { position, route ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        NavBarIcon(
                            iconRes = when (route) {
                                "home" -> R.drawable.ic_home
                                "airing" -> R.drawable.ic_airing
                                "complete" -> R.drawable.ic_complete
                                else -> R.drawable.ic_stopped
                            },
                            selected = currentTabIndex == position,
                            onClick = {
                                currentTabIndex = position
                                onNavigate(route)
                            },
                        )
                    }
                    if (route == "airing") {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(NavBarHeight.addButton)
                                    .clip(CircleShape)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                                        shape = CircleShape,
                                    )
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                    ) { onNavigate("add_edit_show") },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add",
                                    modifier = Modifier.size(IconSize.xxl),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
    }
}

    Spacer(modifier = Modifier.width(Spacing.sm * capsuleAlpha))

    Box(
        modifier = Modifier
            .offset(x = searchButtonOffset)
            .size(searchButtonSize)
            .drawBackdropCustomShape(
                backdrop = backdrop,
                layer = searchLayer,
                luminanceAnimation = luminanceAnimation.value,
                shape = CircleShape,
                interaction = searchInteraction,
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
            tint = Color.White,
        )
    }
}
}
}

@Composable
private fun NavBarIcon(
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(NavBarHeight.icon)
            .clip(CapsuleShape)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
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
    interaction: GlassInteraction? = null,
    pressedScale: Float = 1.08f,
): Modifier =
    this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
            val press = interaction?.pressProgress ?: 0f
            colorControls(
                brightness =
                    if (l > 0f) lerp(0.1f, 0.5f, l)
                    else lerp(0.1f, -0.2f, -l),
                contrast =
                    if (l > 0f) lerp(1f, 0f, l)
                    else 1f,
                saturation = 1.5f,
            )
            blur(
                (if (l > 0f) lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                else lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)) + 2f.dp.toPx() * press,
            )
            refraction(24f.dp.toPx(), size.minDimension / 2f, true)
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            layer.record { drawBackdrop() }
        },
        onDrawSurface = {
            val darken = lerp(0.12f, 0.5f, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
            drawRect(Color.Black.copy(alpha = darken))
            val press = interaction?.pressProgress ?: 0f
            if (press > 0f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f * press), Color.Transparent),
                        center = interaction?.touchPosition ?: Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 1.2f,
                    ),
                    blendMode = BlendMode.Plus,
                )
            }
        },
        layerBlock = if (interaction != null) {
            {
                val scale = lerp(1f, pressedScale, interaction.pressProgress)
                scaleX = scale
                scaleY = scale
            }
        } else null,
        shape = { shape },
    )

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {
    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val smoothAnimationSpec = spring(0.55f, 200f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

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
        Modifier.pointerInput(Unit) {
            inspectDragGestures(
                onDragStart = { down ->
                    onDragStarted(down.position)
                    press()
                },
                onDragEnd = {
                    onDragStopped()
                    release()
                },
                onDragCancel = {
                    onDragStopped()
                    release()
                },
            ) { _, dragAmount ->
                onDrag(size, dragAmount)
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

    fun updateValue(value: Float) {
        val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
        animationScope.launch {
            valueAnimation.animateTo(target, valueAnimationSpec) { updateVelocity() }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
                launch { valueAnimation.animateTo(target, smoothAnimationSpec) }
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
