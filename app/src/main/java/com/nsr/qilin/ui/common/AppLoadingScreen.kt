package com.nsr.qilin.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nsr.qilin.R
import com.nsr.qilin.ui.theme.OnePlusSansFamily

@Composable
fun AppLoadingScreen(
    isDark: Boolean = true,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isAmoled -> Color(0xFF000000)
        isDark -> Color(0xFF060A12)
        else -> Color(0xFFF6F8FC)
    }
    val textColor = if (isDark) Color.White else Color(0xFF121826)
    val accent = Color(0xFF0082FB)
    val accentAlt = Color(0xFF26C6DA)

    val transition = rememberInfiniteTransition(label = "loading")

    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblemPulse"
    )

    val glow by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitSpin"
    )

    val shimmer by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "titleShimmer"
    )

    val dotA by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)
        ),
        label = "dotA"
    )
    val dotB by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(140)
        ),
        label = "dotB"
    )
    val dotC by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(280)
        ),
        label = "dotC"
    )

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enter by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 260f),
        label = "loadingEnter"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer {
                    alpha = glow * enter
                    scaleX = pulse
                    scaleY = pulse
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.30f), accentAlt.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset.Unspecified
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = enter.coerceIn(0f, 1f)
                    val s = 0.88f + 0.12f * enter
                    scaleX = s
                    scaleY = s
                    translationY = (1f - enter) * 36f
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(132.dp)) {
                    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    rotate(orbit) {
                        drawArc(
                            color = accent.copy(alpha = 0.65f),
                            startAngle = 0f,
                            sweepAngle = 104f,
                            useCenter = false,
                            style = stroke
                        )
                    }
                    rotate(-orbit * 0.65f) {
                        drawArc(
                            color = accentAlt.copy(alpha = 0.40f),
                            startAngle = 120f,
                            sweepAngle = 70f,
                            useCenter = false,
                            style = stroke
                        )
                    }
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_dragon_emblem_trimmed),
                    contentDescription = "Qilin",
                    modifier = Modifier
                        .width(80.dp)
                        .height(64.5.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        },
                    contentScale = ContentScale.FillBounds
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Qilin",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            textColor.copy(alpha = 0.55f),
                            textColor,
                            textColor.copy(alpha = 0.55f)
                        ),
                        start = Offset((shimmer - 0.25f) * 260f, 0f),
                        end = Offset((shimmer + 0.25f) * 260f, 40f)
                    ),
                    fontFamily = OnePlusSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(26.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(dotA, dotB, dotC).forEach { a ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer {
                                alpha = a
                                scaleX = a
                                scaleY = a
                            }
                            .background(accent, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Preparing your library",
                fontFamily = OnePlusSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.55f)
            )
        }
    }
}
