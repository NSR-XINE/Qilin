package com.mydonghua.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
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
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.02f),
                            Color.White.copy(alpha = 0.08f)
                        ) else listOf(
                            Color.Black.copy(alpha = 0.03f),
                            Color.Black.copy(alpha = 0.01f),
                            Color.Black.copy(alpha = 0.05f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.04f else 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )
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
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
                        ) else listOf(
                            Color.Black.copy(alpha = 0.06f),
                            Color.Black.copy(alpha = 0.01f),
                            Color.Black.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.05f else 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        Box(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}
