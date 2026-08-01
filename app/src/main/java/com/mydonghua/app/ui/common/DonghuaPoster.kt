package com.mydonghua.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest

import com.mydonghua.app.ui.theme.JustSansFamily

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

@Composable
fun DonghuaPoster(
    posterUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showAcronym: Boolean = true,
    fontSize: TextUnit = TextUnit.Unspecified,
    crossfade: Boolean = false
) {
    val context = LocalContext.current
    val initials = remember(title) { getTitleInitials(title) }

    val imageRequest = remember(posterUrl, context, crossfade) {
        ImageRequest.Builder(context)
            .data(posterUrl?.takeIf { it.isNotBlank() && it != "null" })
            .crossfade(if (crossfade) 250 else 0)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = title,
        modifier = modifier,
        contentScale = contentScale
    ) {
        val state = painter.state
        if (state is AsyncImagePainter.State.Success) {
            SubcomposeAsyncImageContent()
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
