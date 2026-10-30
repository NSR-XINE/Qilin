package com.mydonghua.app.ui.common

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.delay

import com.mydonghua.app.ui.LocalNavAnimatedVisibilityScope
import com.mydonghua.app.ui.LocalSharedTransitionScope
import com.mydonghua.app.ui.SharedBoundsSpring
import com.mydonghua.app.ui.theme.JustSansFamily

private const val POSTER_LOAD_DELAY_MS = 250L
private const val POSTER_STAGGER_STEP_MS = 30L
private const val POSTER_STAGGER_MAX_ADD_MS = 90L
private const val SWITCH_BURST_WINDOW_MS = 800L

object PosterMemoryCache {
    private const val MAX_SIZE = 1000
    private val loadedSet = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun isLoaded(url: String): Boolean = loadedSet.contains(url)

    fun markLoaded(url: String) {
        if (loadedSet.size > MAX_SIZE) loadedSet.clear()
        loadedSet.add(url)
    }
}

object PosterTrace {
    @Volatile
    var composedCount = 0
        private set

    fun reset() {
        composedCount = 0
    }

    fun inc() {
        composedCount++
    }
}

object PosterStagger {
    private const val MAX_SEEN_URLS = 2000

    private val counter = java.util.concurrent.atomic.AtomicInteger(0)
    private val seen = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val lastResetNanos = java.util.concurrent.atomic.AtomicLong(0L)

    /**
     * Returns a delay in ms before this url's first load, or 0 to load now.
     * First-time loads starting right after a tab switch are deferred past the
     * capsule slide (their decode burst stalled the slide); scroll reveals load
     * once the scroll settles (see ScrollGate in ScrollPerf.kt).
     */
    fun staggerDelay(url: String): Long {
        if (!seen.add(url)) return 0L
        if (seen.size > MAX_SEEN_URLS) seen.clear()
        if (System.nanoTime() - lastResetNanos.get() > SWITCH_BURST_WINDOW_MS * 1_000_000L) return 0L
        val idx = counter.getAndIncrement()
        return POSTER_LOAD_DELAY_MS + minOf(idx * POSTER_STAGGER_STEP_MS, POSTER_STAGGER_MAX_ADD_MS)
    }

    fun reset() {
        counter.set(0)
        lastResetNanos.set(System.nanoTime())
    }
}

/**
 * Extracts initials acronym from title (e.g. "Renegade Immortal" -> "RI", "Soul Land" -> "SL")
 */
fun getTitleInitials(title: String?): String {
    if (title.isNullOrBlank()) return "DH"
    val words = title.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.size == 1) {
        val word = words[0].replace(Regex("[^a-zA-Z0-9]"), "")
        return if (word.length >= 2) word.take(2).uppercase() else word.uppercase()
    }
    val initials = words.take(4)
        .mapNotNull { word ->
            val clean = word.replace(Regex("[^a-zA-Z0-9]"), "")
            clean.firstOrNull()?.uppercaseChar()
        }
        .joinToString("")
    return initials.ifEmpty { title.take(2).uppercase() }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DonghuaPoster(
    posterUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showAcronym: Boolean = true,
    fontSize: TextUnit = TextUnit.Unspecified,
    crossfade: Boolean = true,
    immediate: Boolean = false,
    sharedElementKey: String? = null,
    sharedOverlayShape: Shape? = null
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val sharedModifier = if (sharedElementKey != null && sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = sharedElementKey),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> SharedBoundsSpring },
                clipInOverlayDuringTransition = OverlayClip(
                    sharedOverlayShape ?: RoundedCornerShape(18.dp)
                )
            )
        }
    } else {
        Modifier
    }
    val context = LocalContext.current
    val initials = remember(title) { getTitleInitials(title) }

    remember(posterUrl) {
        PosterTrace.inc()
        true
    }

    val isAlreadyCached = remember(posterUrl) {
        !posterUrl.isNullOrBlank() && PosterMemoryCache.isLoaded(posterUrl)
    }

    val staggerDelayMs = remember(posterUrl, immediate, isAlreadyCached) {
        if (immediate || isAlreadyCached || posterUrl.isNullOrBlank()) 0L else PosterStagger.staggerDelay(posterUrl)
    }
    var posterEnabled by remember(posterUrl, immediate, isAlreadyCached) {
        mutableStateOf(
            immediate || isAlreadyCached || posterUrl.isNullOrBlank() || (staggerDelayMs == 0L && !ScrollGate.scrolling)
        )
    }
    LaunchedEffect(posterUrl, staggerDelayMs, immediate, isAlreadyCached) {
        if (immediate || isAlreadyCached || posterUrl.isNullOrBlank()) {
            posterEnabled = true
            return@LaunchedEffect
        }
        if (staggerDelayMs > 0L) delay(staggerDelayMs)
        while (ScrollGate.scrolling) delay(50L)
        posterEnabled = true
    }

    val imageRequest = remember(posterUrl, context, crossfade, posterEnabled, isAlreadyCached) {
        ImageRequest.Builder(context)
            .data(if (posterEnabled) posterUrl?.takeIf { it.isNotBlank() && it != "null" } else null)
            .crossfade(if (crossfade && !isAlreadyCached) 250 else 0)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = title,
        modifier = modifier.then(sharedModifier),
        contentScale = contentScale
    ) {
        val state = painter.state
        if (state is AsyncImagePainter.State.Success) {
            if (!posterUrl.isNullOrBlank()) {
                PosterMemoryCache.markLoaded(posterUrl)
            }
            SubcomposeAsyncImageContent()
        } else if (state is AsyncImagePainter.State.Loading || !posterEnabled) {
            if (isAlreadyCached) {
                SubcomposeAsyncImageContent()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect()
                )
            }
        } else {
            PosterAcronymPlaceholder(
                initials = initials,
                showAcronym = showAcronym,
                fontSize = fontSize
            )
        }
    }
}

@Composable
fun PosterAcronymPlaceholder(
    initials: String,
    showAcronym: Boolean = true,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.25f),
                        surfaceVariant.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.2f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showAcronym) {
            Text(
                text = initials,
                fontFamily = JustSansFamily,
                fontSize = if (fontSize != TextUnit.Unspecified) fontSize else 7.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}
