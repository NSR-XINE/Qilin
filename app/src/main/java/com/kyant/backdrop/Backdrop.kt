package com.kyant.backdrop

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density

interface Backdrop {
    fun DrawScope.drawBackdrop(density: Density)
}
