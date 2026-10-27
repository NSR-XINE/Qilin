package com.mydonghua.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import dev.chrisbanes.haze.hazeChild

@Composable
fun DynamicShowBackground(
    imageUrl: String?,
    themeMode: String,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val isLight = themeMode.lowercase() == "light"
    val isAmoled = themeMode.lowercase() == "amoled"

    val baseBackgroundColor = when {
        isAmoled -> Color.Black
        isLight -> Color(0xFFF6F8FA)
        else -> Color(0xFF090C16)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackgroundColor)
            .hazeChild(state = hazeState)
    ) {
        if (!imageUrl.isNullOrBlank()) {
            Crossfade(
                targetState = imageUrl,
                animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing),
                label = "DynamicShowBackgroundCrossfade"
            ) { activeUrl ->
                Box(modifier = Modifier.fillMaxSize()) {
                    // Show Backdrop Artwork
                    SubcomposeAsyncImage(
                        model = activeUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = when {
                                    isLight -> 0.35f
                                    isAmoled -> 0.38f
                                    else -> 0.45f
                                }
                            }
                    )

                    // Theme-Adaptive Vertical Gradient Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when {
                                    isLight -> Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.40f),
                                            Color.White.copy(alpha = 0.72f),
                                            Color(0xFFF6F8FA).copy(alpha = 0.95f)
                                        )
                                    )
                                    isAmoled -> Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.42f),
                                            Color.Black.copy(alpha = 0.72f),
                                            Color.Black.copy(alpha = 0.98f)
                                        )
                                    )
                                    else -> Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF090C16).copy(alpha = 0.38f),
                                            Color(0xFF0E1424).copy(alpha = 0.70f),
                                            Color(0xFF090C16).copy(alpha = 0.96f)
                                        )
                                    )
                                }
                            )
                    )

                    // Theme-Adaptive Edge Vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when {
                                    isLight -> Brush.horizontalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.40f),
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.40f)
                                        )
                                    )
                                    isAmoled -> Brush.horizontalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.50f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.50f)
                                        )
                                    )
                                    else -> Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF090C16).copy(alpha = 0.45f),
                                            Color.Transparent,
                                            Color(0xFF090C16).copy(alpha = 0.45f)
                                        )
                                    )
                                }
                            )
                    )
                }
            }
        } else {
            GlassBackground(modifier = Modifier.fillMaxSize())
        }
    }
}
