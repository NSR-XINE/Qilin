package com.kyant.backdrop

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

sealed interface BackdropEffectScope : Density {
    val size: Size
    val layoutDirection: LayoutDirection
    var renderEffect: RenderEffect?
    var padding: Float
}

internal class BackdropEffectScopeImpl : BackdropEffectScope {
    override var density: Float = 1f
    override var fontScale: Float = 1f
    override var size: Size = Size.Unspecified
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override var renderEffect: RenderEffect? = null
    override var padding: Float = 0f

    fun update(scope: DrawScope): Boolean {
        val changed = scope.density != density ||
                scope.fontScale != fontScale ||
                scope.size != size ||
                scope.layoutDirection != layoutDirection
        if (changed) {
            density = scope.density
            fontScale = scope.fontScale
            size = scope.size
            layoutDirection = scope.layoutDirection
        }
        return changed
    }

    fun apply(effects: BackdropEffectScope.() -> Unit) {
        renderEffect = null
        effects()
    }

    fun reset() {
        density = 1f
        fontScale = 1f
        size = Size.Unspecified
        layoutDirection = LayoutDirection.Ltr
        renderEffect = null
        padding = 0f
    }
}
