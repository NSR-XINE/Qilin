package com.mydonghua.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mydonghua.app.R
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sign
import java.nio.IntBuffer
import kotlin.time.Duration.Companion.seconds

private val navRoutes = listOf("home", "airing", "complete", "stopped")
private val CapsuleShape = RoundedCornerShape(percent = 50)
private val PillWidth = 52.dp
private val PillHeight = 48.dp

@Composable
fun LiquidGlassNavBar(
    currentRoute: String,
    isDark: Boolean,
    backdrop: Backdrop,
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

    val density = LocalDensity.current
    var barWidthPx by remember { mutableIntStateOf(0) }
    val navIndex = navRoutes.indexOf(currentRoute).coerceAtLeast(0)
    val totalSlots = 5f
    val slotWidthPx = if (barWidthPx > 0) barWidthPx / totalSlots else 0f
    val blobWidthPx = with(density) { PillWidth.toPx() }
    val blobAnimatable = remember { Animatable(0f) }
    val blobOffsetDp by remember { derivedStateOf { with(density) { blobAnimatable.value.toDp() } } }
    val coroutineScope = rememberCoroutineScope()

    fun targetOffset(index: Int): Float {
        if (barWidthPx <= 0) return 0f
        val slot = if (index < 2) index else index + 1
        val centerPx = slotWidthPx * (slot + 0.5f)
        return centerPx - blobWidthPx / 2f
    }

    fun findNearestTab(offsetPx: Float): Int {
        val centers = navRoutes.indices.map { i ->
            val slot = if (i < 2) i else i + 1
            slotWidthPx * (slot + 0.5f)
        }
        val blobCenter = offsetPx + blobWidthPx / 2f
        return centers.indices.minByOrNull { abs(centers[it] - blobCenter) } ?: navIndex
    }

    LaunchedEffect(navIndex, barWidthPx) {
        if (barWidthPx > 0) {
            blobAnimatable.animateTo(targetOffset(navIndex), spring(dampingRatio = 0.7f, stiffness = 400f))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .drawBackdropCustomShape(
                backdrop = backdrop,
                layer = layer,
                luminanceAnimation = luminanceAnimation.value,
                shape = RoundedCornerShape(30.dp),
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
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val target = findNearestTab(blobAnimatable.value)
                        if (target >= 0 && target < navRoutes.size) {
                            onNavigate(navRoutes[target])
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            blobAnimatable.animateTo(targetOffset(navIndex), spring(0.7f, 400f))
                        }
                    },
                ) { _, dragAmount ->
                    val maxOffset = barWidthPx.toFloat() - blobWidthPx
                    val newPos = (blobAnimatable.value + dragAmount).coerceIn(0f, maxOffset)
                    coroutineScope.launch {
                        blobAnimatable.snapTo(newPos)
                    }
                }
            },
    ) {
        if (barWidthPx > 0) {
            val l = (luminanceAnimation.value * 2f - 1f).let { sign(it) * it * it }
            Box(
                modifier = Modifier
                    .offset(x = blobOffsetDp)
                    .width(PillWidth)
                    .height(PillHeight)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CapsuleShape },
                        effects = {
                            colorControls(
                                brightness = if (l > 0f) lerp(0.15f, 0.5f, l) else lerp(0.15f, -0.15f, -l),
                                contrast = 1f,
                                saturation = 1.5f,
                            )
                            blur(if (l > 0f) lerp(10f.dp.toPx(), 18f.dp.toPx(), l) else lerp(10f.dp.toPx(), 4f.dp.toPx(), -l))
                        },
                        highlight = { Highlight.Default.copy(alpha = 0.5f) },
                        shadow = { Shadow(radius = 4f.dp, alpha = 0.3f) },
                    )
                    .background(
                        color = Color.Black.copy(
                            alpha = lerp(0.15f, 0.4f, ((luminanceAnimation.value - 0.3f) / 0.5f).coerceIn(0f, 1f))
                        ),
                        shape = CapsuleShape,
                    ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            navRoutes.forEachIndexed { _, route ->
                NavBarIcon(
                    iconRes = when (route) {
                        "home" -> R.drawable.ic_home
                        "airing" -> R.drawable.ic_airing
                        "complete" -> R.drawable.ic_complete
                        else -> R.drawable.ic_stopped
                    },
                    selected = navRoutes.indexOf(currentRoute) == navRoutes.indexOf(route),
                    onClick = { onNavigate(route) },
                )
                if (route == "airing") {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                                shape = CircleShape,
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
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

@Composable
private fun NavBarIcon(
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Modifier.drawBackdropCustomShape(
    backdrop: Backdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
): Modifier =
    this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
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
                if (l > 0f) lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                else lerp(8f.dp.toPx(), 2f.dp.toPx(), -l),
            )
            refraction(24f.dp.toPx(), size.minDimension / 2f, true)
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            layer.record { drawBackdrop() }
        },
        shape = { shape },
    )
