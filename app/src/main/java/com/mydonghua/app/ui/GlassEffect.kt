package com.mydonghua.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import com.mydonghua.app.ui.theme.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

val LocalHazeState = staticCompositionLocalOf<HazeState> { error("No HazeState provided") }

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

private val GlassTintAlpha = 0.04f
private val GlassSectionAlpha = 0.05f

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hazeState = LocalHazeState.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceTint = MaterialTheme.colorScheme.surfaceTint

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor)
            .hazeChild(state = hazeState)
    ) {
        // Material 3 Ambient Tonal Radial Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryContainer.copy(alpha = 0.18f),
                            surfaceTint.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                        radius = 2000f
                    )
                )
        )
        content()
    }
}

@Composable
fun GlassSection(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(CornerRadius.card),
    content: @Composable () -> Unit
) {
    val hazeState = LocalHazeState.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceTint = MaterialTheme.colorScheme.surfaceTint

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.55f),
                        surfaceTint.copy(alpha = 0.08f),
                        surfaceColor.copy(alpha = 0.40f)
                    )
                ),
                shape = shape
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                ),
                shape = shape
            )
    ) {
        Box(modifier = Modifier.padding(Spacing.md)) {
            content()
        }
    }
}
