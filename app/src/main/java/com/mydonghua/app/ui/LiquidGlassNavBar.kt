package com.mydonghua.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.dp
import com.mydonghua.app.R
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sign
import java.nio.IntBuffer
import kotlin.time.Duration.Companion.seconds

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

    Row(
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
                color = if (isDark) Color.White.copy(alpha = 0.06f)
                else Color.Black.copy(alpha = 0.04f),
                shape = RoundedCornerShape(30.dp),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavBarIcon(
            iconRes = R.drawable.ic_home,
            selected = currentRoute == "home",
            onClick = { onNavigate("home") },
        )
        NavBarIcon(
            iconRes = R.drawable.ic_airing,
            selected = currentRoute == "airing",
            onClick = { onNavigate("airing") },
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    color = if (isDark) Color.White.copy(alpha = 0.1f)
                    else Color.Black.copy(alpha = 0.05f),
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
        NavBarIcon(
            iconRes = R.drawable.ic_complete,
            selected = currentRoute == "complete",
            onClick = { onNavigate("complete") },
        )
        NavBarIcon(
            iconRes = R.drawable.ic_stopped,
            selected = currentRoute == "stopped",
            onClick = { onNavigate("stopped") },
        )
    }
}

@Composable
private fun RowScope.NavBarIcon(
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
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
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
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
