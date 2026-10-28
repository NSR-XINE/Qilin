package com.mydonghua.app.ui.common

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.BuildConfig
import com.mydonghua.app.R
import com.mydonghua.app.ui.theme.OnePlusSansFamily
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private data class FloatingParticle(
    val xFraction: Float,
    val yFraction: Float,
    val radius: Float,
    val baseAlpha: Float,
    val speed: Float,
    val phaseOffset: Float
)

@Composable
fun AppLoadingScreen(
    isDark: Boolean = true,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingTransition")

    // Slow ambient time phase for fluid organic motion (aurora & particles)
    val timePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timePhase"
    )

    // Breathing pulse for emblem core and ambient glow
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.965f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Kinetic Orbital Arc rotation around emblem
    val orbitalAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitalAngle"
    )

    // Radar pulse wave expanding from behind emblem
    val radarTransition = rememberInfiniteTransition(label = "radarTransition")
    val radarScale by radarTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarScale"
    )
    val radarAlpha by radarTransition.animateFloat(
        initialValue = if (isAmoled) 0.22f else 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    // Shimmer sweep for fluid progress beam (-0.7f to 1.7f)
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.7f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    // Pre-computed floating stardust particles
    val particles = remember {
        val random = java.util.Random(1337L)
        List(32) {
            FloatingParticle(
                xFraction = random.nextFloat(),
                yFraction = random.nextFloat(),
                radius = random.nextFloat() * 1.5f + 0.6f,
                baseAlpha = random.nextFloat() * 0.32f + 0.10f,
                speed = random.nextFloat() * 0.5f + 0.5f,
                phaseOffset = random.nextFloat() * 6.28f
            )
        }
    }

    // Dynamic cycling status text
    val loadingSteps = remember {
        listOf(
            "Initializing engine...",
            "Loading your library...",
            "Checking airing schedules...",
            "Syncing anime radar..."
        )
    }
    var stepIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1300)
            stepIndex = (stepIndex + 1) % loadingSteps.size
        }
    }

    val backgroundColor = when {
        isAmoled -> Color(0xFF000000)
        isDark -> Color(0xFF060A12)
        else -> Color(0xFFF6F8FC)
    }

    val primaryTextColor = if (isDark) Color.White else Color(0xFF121826)
    val progressTrackColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // 1. Atmospheric Ambient Aurora & Kinetic Particle Depth
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w > 0 && h > 0) {
                if (isDark) {
                    // Primary Aurora: drifting upper-center electric cobalt bloom
                    val primaryDriftX = w * (0.50f + sin(timePhase) * 0.08f)
                    val primaryDriftY = h * (0.36f + cos(timePhase) * 0.04f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0082FB).copy(alpha = if (isAmoled) 0.10f else 0.18f),
                                Color(0xFF0052B4).copy(alpha = if (isAmoled) 0.04f else 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(primaryDriftX, primaryDriftY),
                            radius = w * 0.70f
                        )
                    )

                    // Secondary Aurora: counter-drifting lower cyan & amethyst horizon aura
                    val secDriftX = w * (0.50f - sin(timePhase * 0.8f) * 0.06f)
                    val secDriftY = h * (0.64f - cos(timePhase * 0.8f) * 0.04f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00D2FF).copy(alpha = if (isAmoled) 0.07f else 0.12f),
                                Color(0xFF6C5CE7).copy(alpha = if (isAmoled) 0.03f else 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(secDriftX, secDriftY),
                            radius = w * 0.58f
                        )
                    )

                    // Floating Cosmic Stardust Particles
                    for (p in particles) {
                        val particlePhase = timePhase * p.speed + p.phaseOffset
                        val driftY = sin(particlePhase) * 10.dp.toPx()
                        val currentY = (p.yFraction * h + driftY).mod(h)
                        val currentX = p.xFraction * w
                        val alphaFactor = (sin(particlePhase) * 0.35f + 0.65f).coerceIn(0.1f, 1.0f)
                        drawCircle(
                            color = Color(0xFFE2F0FF),
                            radius = p.radius.dp.toPx(),
                            center = Offset(currentX, currentY),
                            alpha = p.baseAlpha * alphaFactor * glowAlpha * (if (isAmoled) 0.65f else 0.90f)
                        )
                    }
                } else {
                    // Light mode luminous sky-blue blush
                    val lightDriftX = w * (0.50f + sin(timePhase) * 0.05f)
                    val lightDriftY = h * (0.40f + cos(timePhase) * 0.03f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0082FB).copy(alpha = 0.08f),
                                Color(0xFF00D2FF).copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(lightDriftX, lightDriftY),
                            radius = w * 0.72f
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
            // Hero Emblem Stack with Rotating Squircle Border Light & Radiance
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(176.dp)
            ) {
                // Expanding Radial Pulse Wave (Radar effect)
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(radarScale)
                        .clip(RoundedCornerShape(36.dp))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF0082FB).copy(alpha = radarAlpha),
                            shape = RoundedCornerShape(36.dp)
                        )
                )

                // Soft Atmospheric Radial Core Glow
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0082FB).copy(alpha = 0.38f * glowAlpha),
                                    Color(0xFF00D2FF).copy(alpha = 0.18f * glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Signature ColorOS 17 Liquid Glass Squircle Container
                val emblemShape = RoundedCornerShape(30.dp)
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .scale(pulseScale)
                        .clip(emblemShape)
                        .background(
                            if (isDark) Brush.verticalGradient(
                                listOf(
                                    Color(0xFF16233B).copy(alpha = 0.88f),
                                    Color(0xFF0B1322).copy(alpha = 0.94f)
                                )
                            )
                            else Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.98f),
                                    Color(0xFFF0F5FA).copy(alpha = 0.94f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.35f else 0.80f),
                                    Color(0xFF0082FB).copy(alpha = if (isDark) 0.20f else 0.15f),
                                    Color.White.copy(alpha = if (isDark) 0.05f else 0.25f)
                                )
                            ),
                            shape = emblemShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Concentrated inner ambient core glow behind dragon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF00D2FF).copy(alpha = 0.30f * glowAlpha),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Official Transparent Cyan-Cobalt Dragon Emblem
                    Image(
                        painter = painterResource(id = R.drawable.ic_dragon_emblem),
                        contentDescription = "My Donghua Emblem",
                        modifier = Modifier.size(68.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Kinetic Rotating Light Beam directly on the Squircle Border
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.4.dp.toPx()
                        val halfStroke = strokeWidth / 2f
                        val cornerRadiusPx = 30.dp.toPx() - halfStroke

                        // Sweep gradient rotated by orbitalAngle around squircle center
                        val sweepShader = SweepGradient(
                            size.width / 2f,
                            size.height / 2f,
                            intArrayOf(
                                android.graphics.Color.TRANSPARENT,
                                0x000082FB.toInt(),
                                0x330082FB.toInt(),
                                0xFF0082FB.toInt(),
                                0xFF00D2FF.toInt(),
                                0xFFFFFFFF.toInt(),
                                0xFF00D2FF.toInt(),
                                0x330082FB.toInt(),
                                android.graphics.Color.TRANSPARENT
                            ),
                            floatArrayOf(0.0f, 0.40f, 0.65f, 0.82f, 0.92f, 0.96f, 0.98f, 0.99f, 1.0f)
                        )
                        val matrix = Matrix()
                        matrix.postRotate(orbitalAngle, size.width / 2f, size.height / 2f)
                        sweepShader.setLocalMatrix(matrix)

                        val rotatingBrush = ShaderBrush(sweepShader)

                        // 1. Soft outer neon glow along the squircle border
                        val glowWidth = 4.8.dp.toPx()
                        val halfGlow = glowWidth / 2f
                        drawRoundRect(
                            brush = rotatingBrush,
                            topLeft = Offset(halfGlow, halfGlow),
                            size = Size(size.width - glowWidth, size.height - glowWidth),
                            cornerRadius = CornerRadius(
                                (30.dp.toPx() - halfGlow).coerceAtLeast(0f),
                                (30.dp.toPx() - halfGlow).coerceAtLeast(0f)
                            ),
                            style = Stroke(width = glowWidth),
                            alpha = 0.45f
                        )

                        // 2. High-intensity crisp light beam tracing the squircle border
                        drawRoundRect(
                            brush = rotatingBrush,
                            topLeft = Offset(halfStroke, halfStroke),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            cornerRadius = CornerRadius(
                                cornerRadiusPx.coerceAtLeast(0f),
                                cornerRadiusPx.coerceAtLeast(0f)
                            ),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // App Title - Official OnePlus Sans Typography with Specular Contrast
            Text(
                text = "MY DONGHUA",
                fontFamily = OnePlusSansFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 25.sp,
                letterSpacing = 3.5.sp,
                color = primaryTextColor
            )

            Spacer(modifier = Modifier.height(7.dp))

            // ColorOS 17 Pill Badge with Breathing Live Indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (isDark) Color(0xFF0082FB).copy(alpha = 0.12f)
                        else Color(0xFF0082FB).copy(alpha = 0.08f)
                    )
                    .border(
                        width = 0.75.dp,
                        color = Color(0xFF0082FB).copy(alpha = if (isDark) 0.32f else 0.22f),
                        shape = RoundedCornerShape(percent = 50)
                    )
                    .padding(horizontal = 13.dp, vertical = 4.5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.5.dp)
                ) {
                    // Pulsing live indicator dot with halo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF00D2FF).copy(alpha = 0.35f))
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0082FB))
                        )
                    }

                    Text(
                        text = "ANIME & DONGHUA RADAR",
                        fontFamily = OnePlusSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF0082FB)
                    )
                }
            }

            Spacer(modifier = Modifier.height(38.dp))

            // ColorOS 17 Fluid Indeterminate Liquid Progress Capsule
            Box(
                modifier = Modifier
                    .width(190.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(progressTrackColor)
                    .border(
                        width = 0.6.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                        shape = CircleShape
                    )
            ) {
                // Sweeping radiant gradient segment
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.50f)
                        .offset(x = (190.dp * shimmerOffset))
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0082FB).copy(alpha = 0.15f),
                                    Color(0xFF0082FB),
                                    Color(0xFF00D2FF),
                                    Color(0xFF99EFFF),
                                    Color(0xFF0082FB).copy(alpha = 0.15f)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4-Stage Step Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                loadingSteps.indices.forEach { index ->
                    val isActive = index == stepIndex
                    val width by animateDpAsState(
                        targetValue = if (isActive) 18.dp else 6.dp,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                        label = "stepDotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(3.5.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color(0xFF0082FB)
                                else if (isDark) Color.White.copy(alpha = 0.15f)
                                else Color.Black.copy(alpha = 0.12f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Step Narrative with Smooth Vertical Slide + Fade
            AnimatedContent(
                targetState = loadingSteps[stepIndex],
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(280)) { height -> height / 2 } + fadeIn(animationSpec = tween(280))) togetherWith
                    (slideOutVertically(animationSpec = tween(280)) { height -> -height / 2 } + fadeOut(animationSpec = tween(280)))
                },
                label = "loadingStepText"
            ) { stepText ->
                Text(
                    text = stepText,
                    fontFamily = OnePlusSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                    letterSpacing = 0.35.sp,
                    color = primaryTextColor.copy(alpha = textAlpha * 0.78f)
                )
            }
        }

        // 3. Bottom Architecture & Version System Tag
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.5.dp)
            ) {
                Text(
                    text = "COLOROS 17",
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
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0082FB).copy(alpha = 0.45f))
                )
                Text(
                    text = "V${BuildConfig.VERSION_NAME.uppercase()}",
                    fontFamily = OnePlusSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.6.sp,
                    color = Color(0xFF0082FB).copy(alpha = 0.60f)
                )
            }
        }
    }
}
