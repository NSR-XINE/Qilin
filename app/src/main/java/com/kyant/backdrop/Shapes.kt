package com.kyant.backdrop

import androidx.compose.ui.geometry.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

val RectangleShape: Shape = Shape { size: Size, _: LayoutDirection, _: Density ->
    Outline.Rectangle(size)
}
