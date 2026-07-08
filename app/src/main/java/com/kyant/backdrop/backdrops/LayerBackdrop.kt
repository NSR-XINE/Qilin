package com.kyant.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop

@Composable
fun rememberLayerBackdrop(
    onDraw: ContentDrawScope.() -> Unit = { drawContent() }
): LayerBackdrop {
    return remember(onDraw) { LayerBackdrop(onDraw) }
}

@Stable
class LayerBackdrop internal constructor(
    internal val onDraw: ContentDrawScope.() -> Unit
) : Backdrop {
    override fun DrawScope.drawBackdrop(density: Density) {}

    fun capture(onDraw: ContentDrawScope.() -> Unit) {}
}
