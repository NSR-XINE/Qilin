package com.mydonghua.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object AnimDuration {
    val instant = 0
    val fast = 100
    val normal = 200
    val medium = 300
    val slow = 500
    val extraSlow = 800
}

object AnimSpec {
    val springDefault = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy)
    val springStiff = spring<Float>(dampingRatio = 0.5f, stiffness = 300f)
    val springSmooth = spring<Float>(dampingRatio = 0.7f, stiffness = 200f)
    val fadeIn = tween<Float>(AnimDuration.normal)
    val fadeOut = tween<Float>(AnimDuration.fast)
    val slide = tween<Int>(AnimDuration.medium)
}
