package com.mydonghua.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val section = xxxl
}

object CornerRadius {
    val xs = 6.dp
    val sm = 10.dp
    val md = 14.dp
    val lg = 18.dp
    val xl = 22.dp
    val pill = 32.dp
    val full = RoundedCornerShape(percent = 50)

    // ColorOS 17 Aquamorphic Design Tokens (Matches Reference Screenshots)
    val chip = 14.dp
    val surface = 24.dp // Floating capsule and header docks
    val card = 28.dp    // Signature continuous curvature squircle from ColorOS 17
    val banner = 32.dp  // Large hero squircle
    val sheet = 32.dp
    val dialog = 32.dp
}

object IconSize {
    val xs = 16.dp
    val sm = 18.dp
    val md = 20.dp
    val lg = 22.dp
    val xl = 24.dp
    val xxl = 28.dp
    val xxxl = 32.dp
    val hero = 48.dp
}

object ButtonHeight {
    val sm = 36.dp
    val md = 40.dp
    val lg = 48.dp
    val xl = 52.dp
}

object AppElevation {
    val none = 0.dp
    val sm = 1.dp
    val md = 2.dp
    val lg = 4.dp
    val xl = 8.dp
}

object CardImage {
    val posterWidth = 52.dp
    val posterHeight = 72.dp
    val thumbnail = 42.dp
    val miniThumbnail = 32.dp
    val posterPreview = 56.dp
    val heroAspectRatio = 16f / 9f
}

object TopBarHeight {
    val compact = 48.dp
    val standard = 56.dp
}

object NavBarHeight {
    val bar = 64.dp
    val blob = 56.dp
    val icon = 48.dp
    val searchButton = 48.dp
    val addButton = 48.dp
}
