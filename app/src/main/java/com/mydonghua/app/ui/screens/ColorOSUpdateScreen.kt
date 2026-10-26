package com.mydonghua.app.ui.screens

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.ui.theme.ColorOSMotion
import com.mydonghua.app.ui.theme.JustSansFamily
import com.mydonghua.app.ui.theme.colorOSPressBounce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Official ColorOS 17 Software Update Screen.
 * Exact 1:1 replica of the ColorOS 17 System Update UI (OnePlus 15 reference).
 * Features:
 * - Atmospheric dusk/sunset aurora wallpaper background
 * - Bold iconic "17" ColorOS typography header
 * - Signature 32dp squircle frosted liquid glass changelog card
 * - Dual-tone liquid progress capsule action button ("Download & install" -> "Installing... 80.3%")
 */
@Composable
fun ColorOSUpdateScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var updateState by remember { mutableStateOf(UpdateState.IDLE) }
    var installProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val animatedProgress by animateFloatAsState(
        targetValue = installProgress,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "installProgress"
    )

    val deviceModel = remember {
        val model = Build.MODEL
        if (!model.isNullOrBlank() && model != "unknown") model else "OnePlus 15"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080A14))
    ) {
        // 1. ColorOS 17 Atmospheric Dusk / Sunset Aurora Canvas
        ColorOSAtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 2. Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .colorOSPressBounce(pressedScale = 0.90f, onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Software update",
                    fontSize = 20.sp,
                    fontFamily = JustSansFamily,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .colorOSPressBounce(pressedScale = 0.90f) { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Scrollable Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 3. Iconic ColorOS 17 Hero Branding
                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "17",
                        fontSize = 86.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-2).sp,
                        lineHeight = 86.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "ColorOS",
                        fontSize = 21.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = deviceModel,
                        fontSize = 13.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.75f)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 4. Signature 32dp Squircle Frosted Liquid Glass Card
                    val cardShape = RoundedCornerShape(32.dp)
                    val cardBg = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E2638).copy(alpha = 0.52f),
                            Color(0xFF121624).copy(alpha = 0.60f)
                        )
                    )
                    val cardBorder = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(cardShape)
                            .background(cardBg)
                            .border(BorderStroke(0.85.dp, cardBorder), shape = cardShape)
                            .padding(horizontal = 22.dp, vertical = 22.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Section: Software version
                            Text(
                                text = "Software version",
                                fontSize = 18.5.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "CPH2745_17.0.0.102(C10P02) | 3.53 GB",
                                fontSize = 14.sp,
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.70f)
                            )

                            // Subtle Divider
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.10f),
                                thickness = 0.75.dp,
                                modifier = Modifier.padding(vertical = 18.dp)
                            )

                            // Collapsible Section Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { isExpanded = !isExpanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ColorOS 17",
                                    fontSize = 17.5.sp,
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Expandable Content
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(animationSpec = ColorOSMotion.ExpandSizeSpec) + fadeIn(ColorOSMotion.NavFadeInSpec),
                                exit = shrinkVertically(animationSpec = ColorOSMotion.ShrinkSizeSpec) + fadeOut(ColorOSMotion.NavFadeOutSpec)
                            ) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    Text(
                                        text = "ColorOS 17. Silky Smooth. Flow with AI.",
                                        fontSize = 18.5.sp,
                                        fontFamily = JustSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        lineHeight = 25.sp
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Text(
                                        text = "ColorOS 17, a fresh new look.",
                                        fontSize = 14.sp,
                                        fontFamily = JustSansFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Smooth from the start, fluid over time. All-new fluid aesthetics. Smooth in motion, vivid by nature.",
                                        fontSize = 14.sp,
                                        fontFamily = JustSansFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White.copy(alpha = 0.75f),
                                        lineHeight = 21.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Laminar Flow Engine now features smarter memory scheduling to deliver sustained stability and smooth performance in both the foreground and background.",
                                        fontSize = 14.sp,
                                        fontFamily = JustSansFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White.copy(alpha = 0.75f),
                                        lineHeight = 21.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "OnePlus AI is now more personalized and proactive, deeply attuned to your needs and system-level agent capabilities.",
                                        fontSize = 14.sp,
                                        fontFamily = JustSansFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White.copy(alpha = 0.75f),
                                        lineHeight = 21.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Floating Bottom Action Pill (Exact Match to Picture 1 & 2)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF080A14).copy(alpha = 0.85f),
                                    Color(0xFF080A14)
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    ColorOSProgressPill(
                        state = updateState,
                        progress = animatedProgress,
                        idleText = "Download & install",
                        onClick = {
                            if (updateState == UpdateState.IDLE) {
                                updateState = UpdateState.INSTALLING
                                scope.launch {
                                    // Smoothly progress to 80.3% as shown in Picture 2
                                    installProgress = 0.15f
                                    delay(400)
                                    installProgress = 0.38f
                                    delay(450)
                                    installProgress = 0.62f
                                    delay(400)
                                    installProgress = 0.803f
                                    delay(1200)
                                    // Finish up to 100%
                                    installProgress = 0.94f
                                    delay(500)
                                    installProgress = 1.0f
                                    delay(400)
                                    updateState = UpdateState.COMPLETED
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

enum class UpdateState {
    IDLE,
    INSTALLING,
    COMPLETED
}

/**
 * Signature ColorOS 17 Dual-Tone Liquid Progress Pill.
 * Exact match to Picture 1 (solid blue) and Picture 2 (split blue + frosted sky-white fill).
 */
@Composable
fun ColorOSProgressPill(
    state: UpdateState,
    progress: Float,
    idleText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(percent = 50)
    val primaryBlue = Color(0xFF0082FB)
    val remainingWhite = Color(0xFFE2F0FE)

    when (state) {
        UpdateState.IDLE -> {
            Box(
                modifier = modifier
                    .height(52.dp)
                    .clip(pillShape)
                    .background(primaryBlue)
                    .colorOSPressBounce(pressedScale = 0.96f, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = idleText,
                    fontSize = 16.sp,
                    fontFamily = JustSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        UpdateState.INSTALLING -> {
            val progressPercent = (progress * 100).coerceIn(0f, 100f)
            val displayText = "Installing... ${String.format(Locale.US, "%.1f", progressPercent)}%"

            Box(
                modifier = modifier
                    .height(52.dp)
                    .clip(pillShape)
                    .background(remainingWhite),
                contentAlignment = Alignment.Center
            ) {
                // Background Layer Text (Blue text on frosted sky-white background)
                Text(
                    text = displayText,
                    fontSize = 16.sp,
                    fontFamily = JustSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryBlue
                )

                // Filled Section with clipped white text overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val clipWidth = size.width * progress.coerceIn(0f, 1f)
                            drawRect(
                                color = primaryBlue,
                                size = androidx.compose.ui.geometry.Size(clipWidth, size.height)
                            )
                            clipRect(right = clipWidth) {
                                this@drawWithContent.drawContent()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        fontSize = 16.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        UpdateState.COMPLETED -> {
            Box(
                modifier = modifier
                    .height(52.dp)
                    .clip(pillShape)
                    .background(Color(0xFF00C853)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "System is up to date",
                        fontSize = 16.sp,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Atmospheric Dusk / Sunset Aurora background shader matching ColorOS 17 system update wallpaper.
 */
@Composable
fun ColorOSAtmosphereBackground(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()

                    // Top Indigo Core
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF141C38).copy(alpha = 0.70f),
                                Color(0xFF0A0D18).copy(alpha = 0.95f)
                            ),
                            center = Offset(width * 0.5f, height * 0.15f),
                            radius = width * 0.85f
                        )
                    )

                    // Right Edge Warm Sunset Aurora Glow (Matches Pictures 1 & 2)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE65100).copy(alpha = 0.58f),
                                Color(0xFFFF7043).copy(alpha = 0.35f),
                                Color(0xFF8E24AA).copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(width * 1.05f, height * 0.58f),
                            radius = width * 0.95f
                        ),
                        blendMode = BlendMode.Plus
                    )

                    // Bottom-Left Deep Cobalt Flare
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0277BD).copy(alpha = 0.28f),
                                Color(0xFF0D47A1).copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(0f, height * 0.72f),
                            radius = width * 0.80f
                        ),
                        blendMode = BlendMode.Plus
                    )
                }
        )
    }
}
