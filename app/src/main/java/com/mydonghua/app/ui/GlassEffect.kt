package com.mydonghua.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                color = if (isDark) Color.White.copy(alpha = 0.03f)
                else Color.Black.copy(alpha = 0.02f)
            )
    ) {
        content()
    }
}

@Composable
fun GlassBackgroundHeavy(
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
                color = if (isDark) Color.White.copy(alpha = 0.06f)
                else Color.Black.copy(alpha = 0.04f)
            )
    ) {
        content()
    }
}
