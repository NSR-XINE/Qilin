package com.mydonghua.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

val LocalBackdrop = staticCompositionLocalOf<LayerBackdrop> { error("No Backdrop provided") }

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backdrop = LocalBackdrop.current
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { androidx.compose.ui.graphics.RectangleShape },
                effects = {
                    vibrancy()
                    colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.5f)
                    blur(12f.dp.toPx())
                    lens(24f.dp.toPx(), size.minDimension / 2f, true)
                },
                onDrawSurface = {
                    drawRect(Color.Black.copy(alpha = if (isDark) 0.12f else 0.18f))
                }
            )
    ) {
        content()
    }
}

@Composable
fun GlassSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backdrop = LocalBackdrop.current
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(12.dp) },
                effects = {
                    vibrancy()
                    colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.5f)
                    blur(8f.dp.toPx())
                    lens(10f.dp.toPx(), size.minDimension / 2f, false)
                },
                onDrawSurface = {
                    drawRect(
                        Color.Black.copy(
                            alpha = if (isDark) 0.12f else 0.18f
                        )
                    )
                }
            )
            .padding(14.dp)
    ) {
        content()
    }
}
