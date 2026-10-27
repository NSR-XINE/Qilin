package com.mydonghua.app.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.R
import com.mydonghua.app.ui.theme.JustSansFamily

@Composable
fun AppLoadingScreen(
    isDark: Boolean = true,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingTransition")

    // Breathing pulse for ambient glow and icon
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Indeterminate shimmer translation (-1f to 2f for smooth sweep)
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    val backgroundColor = when {
        isAmoled -> Color(0xFF000000)
        isDark -> Color(0xFF090D15)
        else -> Color(0xFFF7F9FC)
    }

    val primaryTextColor = if (isDark) Color.White else Color(0xFF141824)
    val subtitleColor = Color(0xFF0082FB)
    val progressTrackColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Starfield / Particles for Dark Mode
        if (isDark) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val random = java.util.Random(42L)
                val w = size.width
                val h = size.height
                if (w > 0 && h > 0) {
                    for (i in 0 until 45) {
                        val cx = random.nextFloat() * w
                        val cy = random.nextFloat() * h
                        val r = random.nextFloat() * 1.4f + 0.5f
                        val a = random.nextFloat() * 0.45f + 0.15f
                        drawCircle(
                            color = Color.White,
                            radius = r,
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                            alpha = a
                        )
                    }
                }
            }
        }

        // Center Content Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Ambient Radial Glow + Icon Emblem Stack
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Soft Radiant Cyan Halo
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0082FB).copy(alpha = 0.35f * glowAlpha),
                                    Color(0xFF00D2FF).copy(alpha = 0.15f * glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Liquid Glass Squircle Emblem Container
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (isDark) Color(0xFF131926).copy(alpha = 0.85f)
                            else Color.White.copy(alpha = 0.95f)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.32f else 0.85f),
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.30f)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "My Donghua Emblem",
                        modifier = Modifier.size(70.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // App Title
            Text(
                text = "MY DONGHUA",
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 2.8.sp,
                color = primaryTextColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "ANIME & DONGHUA TRACKING",
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = subtitleColor
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ColorOS 17 Fluid Indeterminate Progress Bar
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(progressTrackColor)
            ) {
                // Sweeping glowing progress segment
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.45f)
                        .offset(x = (160.dp * shimmerOffset))
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0082FB).copy(alpha = 0.2f),
                                    Color(0xFF00B0FF),
                                    Color(0xFF00E5FF),
                                    Color(0xFF0082FB).copy(alpha = 0.2f)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic status message
            Text(
                text = "Loading your library...",
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 0.3.sp,
                color = primaryTextColor.copy(alpha = textAlpha * 0.65f)
            )
        }

        // Bottom Brand / Version Tag
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "COLOROS FLUID EXPERIENCE",
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.5.sp,
                letterSpacing = 1.6.sp,
                color = primaryTextColor.copy(alpha = 0.30f)
            )
        }
    }
}
