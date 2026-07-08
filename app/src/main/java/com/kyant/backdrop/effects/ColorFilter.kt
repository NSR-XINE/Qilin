package com.kyant.backdrop.effects

import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
import androidx.annotation.FloatRange
import com.kyant.backdrop.BackdropEffectScope

fun BackdropEffectScope.colorFilter(colorFilter: ColorFilter) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            RenderEffect.createColorFilterEffect(colorFilter, currentEffect)
        } else {
            RenderEffect.createColorFilterEffect(colorFilter)
        }
}

fun BackdropEffectScope.opacity(@FloatRange(from = 0.0, to = 1.0) alpha: Float) {
    val colorMatrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, alpha, 0f
        )
    )
    colorFilter(ColorMatrixColorFilter(colorMatrix))
}

fun BackdropEffectScope.vibrancy() {
    colorFilter(VibrantColorFilter)
}

private val VibrantColorFilter = ColorMatrixColorFilter(
    ColorMatrix().apply { setSaturation(1.5f) }
)
