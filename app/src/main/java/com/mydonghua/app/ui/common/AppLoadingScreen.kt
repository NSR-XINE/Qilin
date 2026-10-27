package com.mydonghua.app.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.R
import com.mydonghua.app.ui.theme.OnePlusSansFamily
import kotlinx.coroutines.delay

@Composable
fun AppLoadingScreen(
    isDark: Boolean = true,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingTransition")

    // Breathing pulse for ambient glow and icon
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.965f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Indeterminate shimmer sweep for progress beam (-0.7f to 1.7f)
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.7f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.60f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    // Dynamic cycling status text
    val loadingSteps = remember {
        listOf(
            "Initializing engine...",
            "Loading your library...",
            "Checking airing schedules...",
            "Syncing episodes..."
        )
    }
    var stepIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(800)
            stepIndex = (stepIndex + 1) % loadingSteps.size
        }
    }

    val backgroundColor = when {
        isAmoled -> Color(0xFF000000)
        isDark -> Color(0xFF070A12)
        else -> Color(0xFFF6F8FC)
    }

    val primaryTextColor = if (isDark) Color.White else Color(0xFF141824)
    val progressTrackColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // 1. Atmospheric Ambient Aurora & Radiant Bloom
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w > 0 && h > 0) {
                if (isDark) {
                    // Top-center cobalt nebula
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0082FB).copy(alpha = if (isAmoled) 0.12f else 0.18f),
                                Color(0xFF0052B4).copy(alpha = if (isAmoled) 0.05f else 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.35f),
                            radius = w * 0.65f
                        )
                    )
                    // Center-bottom cyan/violet horizon glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00D2FF).copy(alpha = if (isAmoled) 0.08f else 0.12f),
                                Color(0xFF6C5CE7).copy(alpha = if (isAmoled) 0.04f else 0.07f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.62f),
                            radius = w * 0.55f
                        )
                    )

                    // Stardust Particle Constellation
                    val random = java.util.Random(1337L)
                    for (i in 0 until 40) {
                        val cx = random.nextFloat() * w
                        val cy = random.nextFloat() * h
                        val r = random.nextFloat() * 1.5f + 0.6f
                        val baseA = random.nextFloat() * 0.35f + 0.10f
                        drawCircle(
                            color = Color.White,
                            radius = r,
                            center = Offset(cx, cy),
                            alpha = baseA * glowAlpha
                        )
                    }
                } else {
                    // Soft light-mode luminous blush
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0082FB).copy(alpha = 0.08f),
                                Color(0xFF7C4DFF).copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.42f),
                            radius = w * 0.70f
                        )
                    )
                }
            }
        }

        // 2. Center Brand Presentation Stack
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Ambient Radial Glow + Icon Emblem Stack
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                // Soft Radiant Cyan & Electric Halo
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0082FB).copy(alpha = 0.40f * glowAlpha),
                                    Color(0xFF00D2FF).copy(alpha = 0.20f * glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Outer Specular Glow Ring
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(34.dp))
                        .background(
                            if (isDark) Color(0xFF0082FB).copy(alpha = 0.10f * glowAlpha)
                            else Color(0xFF0082FB).copy(alpha = 0.08f)
                        )
                )

                // Signature Liquid Glass Squircle Emblem Container
                val emblemShape = RoundedCornerShape(28.dp)
                Box(
                    modifier = Modifier
                        .size(98.dp)
                        .scale(pulseScale)
                        .clip(emblemShape)
                        .background(
                            if (isDark) Brush.verticalGradient(
                                listOf(
                                    Color(0xFF182236).copy(alpha = 0.90f),
                                    Color(0xFF0D1424).copy(alpha = 0.95f)
                                )
                            )
                            else Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.98f),
                                    Color(0xFFF0F4FA).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(
                            width = 1.1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.45f else 0.90f),
                                    Color(0xFF0082FB).copy(alpha = if (isDark) 0.25f else 0.20f),
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.40f)
                                )
                            ),
                            shape = emblemShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "My Donghua Emblem",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Title - Official OnePlus Sans Typography
            Text(
                text = "MY DONGHUA",
                fontFamily = OnePlusSansFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 25.sp,
                letterSpacing = 3.sp,
                color = primaryTextColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ColorOS 17 Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (isDark) Color(0xFF0082FB).copy(alpha = 0.14f)
                        else Color(0xFF0082FB).copy(alpha = 0.10f)
                    )
                    .border(
                        width = 0.75.dp,
                        color = Color(0xFF0082FB).copy(alpha = if (isDark) 0.35f else 0.25f),
                        shape = RoundedCornerShape(percent = 50)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pulsing live indicator dot
                    Box(
                        modifier = Modifier
                            .size(5.5.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFF00D2FF))
                    )
                    Text(
                        text = "ANIME & DONGHUA RADAR",
                        fontFamily = OnePlusSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.4.sp,
                        color = Color(0xFF0082FB)
                    )
                }
            }

            Spacer(modifier = Modifier.height(38.dp))

            // ColorOS 17 Fluid Indeterminate Liquid Progress Capsule
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(progressTrackColor)
            ) {
                // Sweeping radiant gradient segment
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.50f)
                        .offset(x = (180.dp * shimmerOffset))
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0082FB).copy(alpha = 0.15f),
                                    Color(0xFF0082FB),
                                    Color(0xFF00D2FF),
                                    Color(0xFF80E5FF),
                                    Color(0xFF0082FB).copy(alpha = 0.15f)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Step Narrative with Smooth Animated Transition
            AnimatedContent(
                targetState = loadingSteps[stepIndex],
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "loadingStepText"
            ) { stepText ->
                Text(
                    text = stepText,
                    fontFamily = OnePlusSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                    letterSpacing = 0.3.sp,
                    color = primaryTextColor.copy(alpha = textAlpha * 0.75f)
                )
            }
        }

        // 3. Bottom Brand / Architecture Tag
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "COLOROS",
                    fontFamily = OnePlusSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.6.sp,
                    color = primaryTextColor.copy(alpha = 0.32f)
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0082FB).copy(alpha = 0.45f))
                )
                Text(
                    text = "FLUID EXPERIENCE",
                    fontFamily = OnePlusSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.6.sp,
                    color = primaryTextColor.copy(alpha = 0.28f)
                )
            }
        }
    }
}
