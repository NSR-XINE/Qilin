package com.nsr.qilin.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nsr.qilin.R
import com.nsr.qilin.ui.theme.ColorOSMotion
import com.nsr.qilin.ui.theme.OnePlusSansFamily
import kotlinx.coroutines.delay

/**
 * ColorOS 17 / OxygenOS 17 Signature App Loading Experience.
 * Features:
 * - Strictly Monotonic Progress (never decreases, never jumps up & down)
 * - Fixed baseline typography (zero physical vertical shift or jitter)
 * - Ambient Aurora Canvas with breathing multi-layered radial bloom
 * - Specular liquid glass pedestal with dual counter-orbiting celestial arcs
 * - Floating breathing dragon crest with micro-bob physical motion
 * - Metallic specular shimmer sweep across "QILIN" brand typography
 * - Dynamic fluid capsule progress beam with glowing leading point
 * - Bottom flagship engine anchor badge
 */
@Composable
fun AppLoadingScreen(
    isDark: Boolean = true,
    isAmoled: Boolean = false,
    isDataReady: Boolean = false,
    targetDurationMillis: Long = 3200L,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isAmoled -> Color(0xFF000000)
        isDark -> Color(0xFF070B16)
        else -> Color(0xFFF7F9FD)
    }
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val accent = Color(0xFF0082FB)        // ColorOS 17 Official Electric Blue
    val accentAlt = Color(0xFF00F2FE)     // Fluid Cyan
    val accentSecondary = Color(0xFF7000FF) // Aurora Violet

    val transition = rememberInfiniteTransition(label = "loadingAnimations")

    // 1. Dragon Crest Organic Breathing Scale
    val emblemPulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblemPulse"
    )

    // 2. Crest Floating Vertical Float
    val floatY by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // 3. Ambient Aurora Breathing Glow
    val auraGlow by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraGlow"
    )

    // 4. Double Concentric Orbital Rotation
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    // 5. Metallic Shimmer Sweep on Title
    val shimmerOffset by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // 6. Smooth Organic Entrance
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enter by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = spring(
            dampingRatio = ColorOSMotion.ExpressiveDefaultSpatialSpring.dampingRatio,
            stiffness = ColorOSMotion.ExpressiveDefaultSpatialSpring.stiffness
        ),
        label = "loadingEnter"
    )

    // Strictly Monotonic Progress Driver (Mathematically Guaranteed Non-Decreasing)
    var maxPercent by rememberSaveable { mutableIntStateOf(0) }
    var currentProgress by rememberSaveable { mutableFloatStateOf(0f) }
    val startTime = rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(targetDurationMillis) {
        while (maxPercent < 100) {
            val elapsed = (System.currentTimeMillis() - startTime.longValue).coerceAtLeast(0L)
            val raw = (elapsed.toFloat() / targetDurationMillis).coerceIn(0f, 1f)

            // Organic progressive easing curve
            val eased = if (raw < 0.35f) {
                (raw / 0.35f) * 0.40f
            } else if (raw < 0.80f) {
                0.40f + ((raw - 0.35f) / 0.45f) * 0.45f
            } else {
                0.85f + ((raw - 0.80f) / 0.20f) * 0.15f
            }

            val calculated = (eased * 100).toInt().coerceIn(0, 100)
            if (calculated > maxPercent) {
                maxPercent = calculated
            }
            if (eased > currentProgress) {
                currentProgress = eased
            }

            if (raw >= 1f || maxPercent >= 100) {
                maxPercent = 100
                currentProgress = 1f
                break
            }
            delay(16L) // 60fps smooth progression
        }
    }

    // Accelerate to 100% when data is fully ready near the finish
    LaunchedEffect(isDataReady) {
        if (isDataReady && maxPercent >= 88 && maxPercent < 100) {
            maxPercent = 100
            currentProgress = 1f
        }
    }

    val statusText = when {
        maxPercent < 25 -> "Initializing Tidal Engine..."
        maxPercent < 60 -> "Connecting to archive..."
        maxPercent < 90 -> "Syncing episode catalog..."
        else -> "Welcome to Qilin"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Ambient Aurora Canvas Glow Mesh
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height * 0.44f)
            val outerRadius = size.width * 0.80f * auraGlow

            // Deep Azure / Cyan ambient bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (isDark) 0.26f * auraGlow else 0.14f * auraGlow),
                        accentAlt.copy(alpha = if (isDark) 0.10f * auraGlow else 0.06f * auraGlow),
                        accentSecondary.copy(alpha = if (isDark) 0.04f * auraGlow else 0f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = outerRadius
                ),
                center = centerOffset,
                radius = outerRadius
            )

            // Subtle top sheen simulating OLED dynamic back-refraction
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentAlt.copy(alpha = if (isDark) 0.06f else 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, 0f),
                    radius = size.width * 0.65f
                ),
                center = Offset(size.width / 2f, 0f),
                radius = size.width * 0.65f
            )
        }

        // Layer 2: Main Central Presentation (Emblem + Typography + Progress)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = enter.coerceIn(0f, 1f)
                    val s = 0.90f + 0.10f * enter
                    scaleX = s
                    scaleY = s
                    translationY = (1f - enter) * 28f
                }
        ) {
            // Emblem Stage with Specular Glass Pedestal & Double Orbital Celestial Arcs
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(164.dp)
            ) {
                // Soft liquid glass pedestal disc
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (isDark) Color(0xFF141C2E).copy(alpha = 0.70f) else Color.White.copy(alpha = 0.85f),
                                    if (isDark) Color(0xFF0E1424).copy(alpha = 0.40f) else Color.White.copy(alpha = 0.50f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    accent.copy(alpha = 0.40f),
                                    accentAlt.copy(alpha = 0.70f),
                                    Color.White.copy(alpha = if (isDark) 0.50f else 0.80f),
                                    accent.copy(alpha = 0.40f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Concentric Celestial Orbital Halo Arcs
                Canvas(modifier = Modifier.size(164.dp)) {
                    val strokePrimary = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    val strokeSecondary = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)

                    // Outer primary orbit arc (Clockwise)
                    rotate(orbit) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.Transparent,
                                    accent.copy(alpha = 0.20f),
                                    accentAlt,
                                    Color.White
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 120f,
                            useCenter = false,
                            style = strokePrimary
                        )
                    }

                    // Inner counter-orbital arc (Counter-clockwise)
                    rotate(-orbit * 0.70f + 70f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.Transparent,
                                    accentAlt.copy(alpha = 0.20f),
                                    accent.copy(alpha = 0.85f)
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 85f,
                            useCenter = false,
                            style = strokeSecondary
                        )
                    }
                }

                // Dragon Crest with organic floating & breathing physics
                Image(
                    painter = painterResource(id = R.drawable.ic_dragon_emblem_trimmed),
                    contentDescription = "Qilin Crest",
                    modifier = Modifier
                        .width(88.dp)
                        .height(71.dp)
                        .graphicsLayer {
                            scaleX = emblemPulse
                            scaleY = emblemPulse
                            translationY = floatY
                        },
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Brand Typography: "Q I L I N" with specular metallic shimmer
            Text(
                text = "Q I L I N",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            textColor.copy(alpha = 0.65f),
                            textColor,
                            accentAlt,
                            textColor,
                            textColor.copy(alpha = 0.65f)
                        ),
                        start = Offset((shimmerOffset - 0.35f) * 320f, 0f),
                        end = Offset((shimmerOffset + 0.35f) * 320f, 40f)
                    ),
                    fontFamily = OnePlusSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = 6.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Tagline
            Text(
                text = "ULTIMATE DONGHUA EXPERIENCE",
                fontFamily = OnePlusSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 2.4.sp,
                color = accent.copy(alpha = 0.90f)
            )

            Spacer(modifier = Modifier.height(34.dp))

            // Fluid Liquid Progress Capsule Bar
            val capsuleWidth = 240.dp
            val capsuleHeight = 5.dp

            Box(
                modifier = Modifier
                    .width(capsuleWidth)
                    .height(capsuleHeight)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.08f)
                        else Color.Black.copy(alpha = 0.06f)
                    )
            ) {
                // Active gradient fill
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(currentProgress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF0052D4),
                                    Color(0xFF0082FB),
                                    Color(0xFF00F2FE)
                                )
                            )
                        )
                )

                // Glowing specular head point
                if (currentProgress > 0.03f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (capsuleWidth * currentProgress) - 4.dp)
                            .size(8.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White, Color(0xFF00F2FE), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fixed-Height Row for Status & Percentage: Locked coordinates with ZERO vertical shift
            Row(
                modifier = Modifier
                    .width(capsuleWidth)
                    .height(22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Fixed-height Status label using smooth crossfade (no vertical offset/slide)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Crossfade(
                        targetState = statusText,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "statusCrossfade"
                    ) { animatedText ->
                        Text(
                            text = animatedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = OnePlusSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.55f)
                        )
                    }
                }

                // Right: Percentage indicator with locked width and end-alignment (no jumping)
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(22.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "$maxPercent%",
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        fontFamily = OnePlusSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = accent
                    )
                }
            }
        }

        // Layer 3: Bottom Flagship Engine Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = enter * 0.85f }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(
                        color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 0.8.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF00E676), CircleShape)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "TIDAL & AURORA ENGINE • COLOROS 17",
                    fontFamily = OnePlusSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.2.sp,
                    color = textColor.copy(alpha = 0.45f)
                )
            }
        }
    }
}
