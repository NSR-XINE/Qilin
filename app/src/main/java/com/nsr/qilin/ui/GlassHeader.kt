package com.nsr.qilin.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.nsr.qilin.ui.theme.OnePlusSansFamily
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nsr.qilin.ui.LocalHeaderBackdrop
import com.nsr.qilin.ui.LocalLayerBackdrop
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.nsr.qilin.R
import com.nsr.qilin.ui.theme.Spacing
import com.nsr.qilin.ui.theme.ColorOSMotion
import com.nsr.qilin.ui.theme.colorOSPressBounce
import com.nsr.qilin.ui.theme.IconSize
import com.nsr.qilin.ui.theme.Haptics
import kotlinx.coroutines.launch

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.nsr.qilin.data.service.AniListService
import com.nsr.qilin.domain.model.Show
import com.nsr.qilin.domain.model.CountdownCalculator
import kotlinx.coroutines.delay

@Composable
fun GlassHeader(
    title: String,
    backdrop: Backdrop = LocalHeaderBackdrop.current ?: LocalLayerBackdrop.current ?: rememberLayerBackdrop(),
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() <= 0.5f,
    subtitle: String? = null,
    avatarUrl: String? = null,
    countdownShow: Show? = null,
    onCountdownClick: (() -> Unit)? = null,
    onDrawerOpen: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val aniListUser by AniListService.userFlow.collectAsState()
    val effectiveAvatarUrl = avatarUrl ?: aniListUser?.avatarUrl

    var remainingTime by remember(countdownShow?.id, countdownShow?.releaseDay, countdownShow?.releaseTime) {
        mutableStateOf(
            countdownShow?.let {
                CountdownCalculator.calculateTimeRemaining(
                    CountdownCalculator.getNextReleaseDateMs(it.releaseDay, it.releaseTime)
                )
            }
        )
    }

    LaunchedEffect(countdownShow?.id, countdownShow?.releaseDay, countdownShow?.releaseTime) {
        if (countdownShow != null) {
            while (true) {
                remainingTime = CountdownCalculator.calculateTimeRemaining(
                    CountdownCalculator.getNextReleaseDateMs(countdownShow.releaseDay, countdownShow.releaseTime)
                )
                delay(1000)
            }
        }
    }

    val contentColor = if (isDark) Color.White else Color(0xFF1E1E1E)
    val menuBtnBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)

    val headerInteraction = remember { MutableInteractionSource() }
    val isHeaderPressed by headerInteraction.collectIsPressedAsState()
    val headerPressProgress by animateFloatAsState(
        targetValue = if (isHeaderPressed) 1f else 0f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "headerPressProgress"
    )
    var headerTouchPosition by remember { mutableStateOf(Offset.Zero) }

    val capsuleScale = lerp(1f, 0.985f, headerPressProgress)

    val canopySurfaceColor = if (isDark) Color(0xFF070A10).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.52f)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Surrounding frosted canopy from pill to top of screen
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Black,
                            0.76f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    highlight = null,
                    shadow = null,
                    effects = {
                        vibrancy()
                        colorControls(
                            brightness = if (isDark) 0.03f else 0.01f,
                            contrast = 1.02f,
                            saturation = 1.45f
                        )
                        blur(24.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to canopySurfaceColor,
                                0.76f to canopySurfaceColor.copy(alpha = canopySurfaceColor.alpha * 0.90f),
                                1.0f to Color.Transparent
                            )
                        )
                    }
                )
        )

        // Floating Liquid Glass Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Spacing.lg, end = Spacing.lg, top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                .fillMaxWidth()
                .height(56.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val firstPressed = event.changes.firstOrNull { it.pressed }
                            if (firstPressed != null) {
                                headerTouchPosition = firstPressed.position
                            } else {
                                headerTouchPosition = Offset.Zero
                            }
                        }
                    }
                }
                .clickable(
                    interactionSource = headerInteraction,
                    indication = null,
                    onClick = {}
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    highlight = null,
                    shadow = null,
                    effects = {
                        vibrancy()
                        colorControls(
                            brightness = if (isDark) 0.05f else 0.02f,
                            contrast = 1.05f,
                            saturation = 1.6f
                        )
                        blur((22f + 2f * headerPressProgress).dp.toPx())
                    },
                    onDrawSurface = {
                        val baseColor = if (isDark) Color(0xFF0F141C).copy(alpha = 0.38f) else Color.White.copy(alpha = 0.48f)
                        val radius = size.height / 2f
                        drawRoundRect(
                            color = baseColor,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                        )
                        val press = headerPressProgress
                        if (press > 0f) {
                            val lightColor = if (isDark) Color.White.copy(alpha = 0.18f * press) else Color.White.copy(alpha = 0.35f * press)
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(lightColor, Color.Transparent),
                                    center = if (headerTouchPosition != Offset.Zero) headerTouchPosition else Offset(size.width / 2f, size.height / 2f),
                                    radius = size.minDimension * 1.2f,
                                ),
                                blendMode = BlendMode.Plus,
                            )
                        }
                    },
                    layerBlock = {
                        scaleX = capsuleScale
                        scaleY = capsuleScale
                    }
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = if (onBackClick != null || countdownShow != null) Spacing.sm else Spacing.xxl, end = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(menuBtnBg)
                            .colorOSPressBounce(pressedScale = 0.90f, onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = contentColor,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                }

                if (countdownShow != null && remainingTime != null) {
                    val rem = remainingTime!!
                    val isAiring = rem.airingNow

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = Spacing.xs, end = Spacing.xs)
                            .colorOSPressBounce(pressedScale = 0.96f) {
                                onCountdownClick?.invoke()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Mini Poster Thumbnail Circular Avatar
                        val posterShape = CircleShape
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(posterShape)
                                .background(if (isDark) Color(0xFF161F2E) else Color(0xFFE4E9F2))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = if (isDark) 0.50f else 0.85f),
                                            Color(0xFF0082FB).copy(alpha = 0.30f),
                                            Color.White.copy(alpha = if (isDark) 0.08f else 0.35f)
                                        )
                                    ),
                                    shape = posterShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!countdownShow.poster.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = countdownShow.poster,
                                    contentDescription = countdownShow.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                ) {
                                    val state = painter.state
                                    if (state is AsyncImagePainter.State.Success) {
                                        SubcomposeAsyncImageContent()
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_tv),
                                            contentDescription = null,
                                            tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_tv),
                                    contentDescription = null,
                                    tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // 2. Title & Countdown Details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.5.dp)
                            ) {
                                // Pulsing status indicator dot
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background((if (isAiring) Color(0xFFFF3B30) else Color(0xFF0082FB)).copy(alpha = 0.30f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(4.5.dp)
                                            .clip(CircleShape)
                                            .background(if (isAiring) Color(0xFFFF3B30) else Color(0xFF0082FB))
                                    )
                                }

                                Text(
                                    text = countdownShow.title,
                                    fontFamily = OnePlusSansFamily,
                                    style = TextStyle(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                if (isAiring) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF3B30).copy(alpha = 0.16f))
                                            .border(
                                                width = 0.5.dp,
                                                color = Color(0xFFFF3B30).copy(alpha = 0.40f),
                                                shape = CircleShape
                                            )
                                            .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            fontFamily = OnePlusSansFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 8.5.sp,
                                            letterSpacing = 0.5.sp,
                                            color = Color(0xFFFF3B30)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.5.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Episode Chip Pill (Circular capsule)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF0082FB).copy(alpha = if (isDark) 0.18f else 0.12f))
                                        .border(
                                            width = 0.6.dp,
                                            color = Color(0xFF0082FB).copy(alpha = if (isDark) 0.35f else 0.25f),
                                            shape = CircleShape
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "EP ${countdownShow.currentEp + 1}",
                                        fontFamily = OnePlusSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF0082FB)
                                    )
                                }

                                if (isAiring) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                                            .padding(horizontal = 7.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = "Air Now",
                                            fontFamily = OnePlusSansFamily,
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF3B30)
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                } else if (!rem.elapsed) {
                                    val countdownText = buildString {
                                        if (rem.days > 0) append("${rem.days}d ")
                                        append(rem.hours.toString().padStart(2, '0')).append("h ")
                                        append(rem.minutes.toString().padStart(2, '0')).append("m ")
                                        append(rem.seconds.toString().padStart(2, '0')).append("s")
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isDark) Color.White.copy(alpha = 0.07f)
                                                else Color.Black.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = 0.6.dp,
                                                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                                shape = CircleShape
                                            )
                                            .padding(horizontal = 7.dp, vertical = 1.5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_clock),
                                                contentDescription = null,
                                                tint = Color(0xFF0082FB),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = countdownText,
                                                fontFamily = OnePlusSansFamily,
                                                style = TextStyle(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = contentColor.copy(alpha = 0.90f)
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Soon",
                                        fontFamily = OnePlusSansFamily,
                                        style = TextStyle(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = contentColor.copy(alpha = 0.70f)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = Spacing.md)
                    ) {
                        AnimatedContent(
                            targetState = title,
                            transitionSpec = {
                                (fadeIn(animationSpec = ColorOSMotion.TabFadeInSpec) +
                                    slideInVertically(
                                        animationSpec = ColorOSMotion.TabSlideInSpec,
                                        initialOffsetY = { it / 3 }
                                    )) togetherWith
                                    (fadeOut(animationSpec = ColorOSMotion.TabFadeOutSpec) +
                                        slideOutVertically(
                                            animationSpec = ColorOSMotion.TabSlideOutSpec,
                                            targetOffsetY = { -it / 3 }
                                        ))
                            },
                            label = "headerTitle"
                        ) { animatedTitle ->
                            Text(
                                text = animatedTitle,
                                style = TextStyle(
                                    fontSize = if (subtitle != null) 16.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0082FB)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (onEditClick != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(menuBtnBg)
                            .colorOSPressBounce(pressedScale = 0.90f, onClick = onEditClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Edit",
                            tint = contentColor,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }

                if (onDrawerOpen != null) {
                    val hasAvatar = !effectiveAvatarUrl.isNullOrBlank()
                    val menuSpin = remember { Animatable(0f) }
                    val menuPulse = remember { Animatable(0f) }
                    val menuScope = rememberCoroutineScope()
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(menuBtnBg)
                            .colorOSPressBounce(pressedScale = 0.90f, onClick = {
                                Haptics.impact()
                                menuScope.launch {
                                    menuPulse.snapTo(0f)
                                    menuPulse.animateTo(1f, tween(durationMillis = 460, easing = ColorOSMotion.EmphasizedDecelerate))
                                    menuPulse.snapTo(0f)
                                }
                                menuScope.launch {
                                    menuSpin.snapTo(0f)
                                    menuSpin.animateTo(1f, tween(durationMillis = 170, easing = ColorOSMotion.EmphasizedDecelerate))
                                    menuSpin.animateTo(0f, spring(dampingRatio = 0.34f, stiffness = 260f))
                                }
                                onDrawerOpen()
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    val p = menuPulse.value
                                    scaleX = 1f + p * 0.85f
                                    scaleY = 1f + p * 0.85f
                                    alpha = (1f - p) * 0.5f
                                }
                                .border(1.4.dp, contentColor.copy(alpha = 0.8f), CircleShape)
                        )
                        if (hasAvatar) {
                            SubcomposeAsyncImage(
                                model = effectiveAvatarUrl,
                                contentDescription = "AniList Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .graphicsLayer {
                                        val p = menuPulse.value
                                        scaleX = 1f + p * 0.08f
                                        scaleY = 1f + p * 0.08f
                                    },
                                contentScale = ContentScale.Crop
                            ) {
                                val state = painter.state
                                if (state is AsyncImagePainter.State.Success) {
                                    SubcomposeAsyncImageContent()
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_menu),
                                        contentDescription = "Menu",
                                        tint = contentColor,
                                        modifier = Modifier
                                            .size(IconSize.md)
                                            .graphicsLayer {
                                                rotationZ = menuSpin.value * 90f
                                                val s = 1f - menuSpin.value * 0.12f
                                                scaleX = s
                                                scaleY = s
                                            }
                                    )
                                }
                            }
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_menu),
                                contentDescription = "Menu",
                                tint = contentColor,
                                modifier = Modifier
                                    .size(IconSize.md)
                                    .graphicsLayer {
                                        rotationZ = menuSpin.value * 90f
                                        val s = 1f - menuSpin.value * 0.12f
                                        scaleX = s
                                        scaleY = s
                                    }
                            )
                        }
                    }
                }

                if (actions != null) {
                    actions()
                }
            }
        }
    }
}
}
