package com.mydonghua.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun DynamicShowBackground(
    imageUrl: String?,
    themeMode: String,
    modifier: Modifier = Modifier
) {
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
    ) {
        if (!imageUrl.isNullOrBlank()) {
            Crossfade(
                targetState = imageUrl,
                animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing),
                label = "DynamicShowBackgroundCrossfade"
            ) { activeUrl ->
                Box(modifier = Modifier.fillMaxSize()) {
                    SubcomposeAsyncImage(
                        model = activeUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(30.dp)
                            .graphicsLayer {
                                alpha = when {
                                    isLight -> 0.26f
                                    isAmoled -> 0.28f
                                    else -> 0.30f
                                }
                                scaleX = 1.16f
                                scaleY = 1.16f
                            }
                    )

                    SubcomposeAsyncImage(
                        model = activeUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(24.dp)
                            .graphicsLayer {
                                alpha = when {
                                    isLight -> 0.92f
                                    isAmoled -> 0.86f
                                    else -> 0.88f
                                }
                            }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when {
                                    isLight -> Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.10f),
                                            Color.White.copy(alpha = 0.34f),
                                            Color(0xFFF6F8FA).copy(alpha = 0.82f)
                                        )
                                    )
                                    isAmoled -> Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.08f),
                                            Color.Black.copy(alpha = 0.38f),
                                            Color.Black.copy(alpha = 0.90f)
                                        )
                                    )
                                    else -> Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF090C16).copy(alpha = 0.10f),
                                            Color(0xFF0E1424).copy(alpha = 0.38f),
                                            Color(0xFF090C16).copy(alpha = 0.86f)
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
                                            Color.White.copy(alpha = 0.15f),
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.15f)
                                        )
                                    )
                                    isAmoled -> Brush.horizontalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.25f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.25f)
                                        )
                                    )
                                    else -> Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF090C16).copy(alpha = 0.25f),
                                            Color.Transparent,
                                            Color(0xFF090C16).copy(alpha = 0.25f)
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
