package com.mydonghua.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
private val CapsuleShape = RoundedCornerShape(percent = 50)
private val BlobHeight = 56.dp

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

    LaunchedEffect(layer) {
        val buffer = IntBuffer.allocate(25)
        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    val imageBitmap = layer.toImageBitmap()
                    val thumbnail = Bitmap.createScaledBitmap(imageBitmap.asAndroidBitmap(), 5, 5, false)
                    buffer.rewind()
                    thumbnail.copyPixelsToBuffer(buffer)
                }
            } catch (_: Exception) { continue }
            val avg = (0 until 25).sumOf { i ->
                val c = buffer.get(i)
                val r = (c shr 16 and 0xFF) / 255.0
                val g = (c shr 8 and 0xFF) / 255.0
                val b = (c and 0xFF) / 255.0
                0.2126 * r + 0.7152 * g + 0.0722 * b
            } / 25.0
            luminanceAnimation.animateTo(avg.toFloat().coerceAtMost(0.8f), tween(500))
            delay(1.seconds)
        }
    }

    val searchLayer = rememberGraphicsLayer()
    val searchInteraction = rememberGlassInteraction()
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val currentIsSearchOpen by rememberUpdatedState(isSearchOpen)
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

    val searchOffsetX by animateFloatAsState(
        targetValue = if (isSearchOpen) 0f else (containerWidthPx - with(density) { 56.dp.toPx() }).coerceAtLeast(0f),
        animationSpec = tween(300),
        label = "searchOffset",
    )

    Box(
        modifier = modifier
            .wrapContentHeight()
            .onSizeChanged { if (!currentIsSearchOpen) containerWidthPx = it.width },
    ) {
        AnimatedVisibility(
            visible = !isSearchOpen,
            enter = expandHorizontally(expandFrom = Alignment.Start, animationSpec = tween(300)) + fadeIn(tween(200)),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = tween(300)) + fadeOut(tween(200)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .drawBackdropCustomShape(
                        backdrop = backdrop,
                        layer = layer,
                        luminanceAnimation = luminanceAnimation.value,
                        shape = RoundedCornerShape(30.dp),
                        interaction = barInteraction,
                        pressedScale = 1.12f,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(30.dp),
                    )
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(30.dp),
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

            if (barWidthPx > 0) {
                val l = (luminanceAnimation.value * 2f - 1f).let { sign(it) * it * it }
                val pillWidthPx = with(density) { (slotWidthPx - 8.dp.toPx()).coerceAtLeast(32.dp.toPx()) }
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
                                    .size(48.dp)
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
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .graphicsLayer { translationX = searchOffsetX }
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
                modifier = Modifier.size(24.dp),
                tint = Color.White,
            )
        }
    }
}
