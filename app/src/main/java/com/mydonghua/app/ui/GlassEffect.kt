package com.mydonghua.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

val LocalHazeState = staticCompositionLocalOf<HazeState> { error("No HazeState provided") }

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hazeState = LocalHazeState.current
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxSize()
            .hazeChild(state = hazeState)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.02f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.10f)
                    ) else listOf(
                        Color.Black.copy(alpha = 0.05f),
                        Color.Black.copy(alpha = 0.02f),
                        Color.Black.copy(alpha = 0.06f)
                    )
                )
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
    val hazeState = LocalHazeState.current
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .hazeChild(state = hazeState, shape = RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
                    ) else listOf(
                        Color.Black.copy(alpha = 0.07f),
                        Color.Black.copy(alpha = 0.02f),
                        Color.Black.copy(alpha = 0.09f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        content()
    }
}
