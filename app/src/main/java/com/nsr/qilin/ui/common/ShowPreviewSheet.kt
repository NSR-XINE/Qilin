package com.nsr.qilin.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nsr.qilin.data.service.AniListMedia
import com.nsr.qilin.data.service.AniListService
import com.nsr.qilin.domain.model.Show
import com.nsr.qilin.ui.theme.CornerRadius
import com.nsr.qilin.ui.theme.JustSansFamily

fun AniListMedia.toShow(): Show = Show(
    id = "anilist_${this.id}",
    title = this.preferredTitle,
    titleZh = this.titleNative,
    status = when (this.status?.lowercase()) {
        "completed" -> "completed"
        "releasing" -> "ongoing"
        else -> "ongoing"
    },
    releaseDay = "Sunday",
    releaseTime = "10:00",
    currentEp = 0,
    totalEp = this.episodes,
    poster = this.coverImage,
    notes = this.description,
    genres = this.genres?.joinToString(", "),
    studio = this.studio,
    releaseYear = this.releaseYear ?: 0,
    rating = this.averageScore ?: 0,
    aniListId = this.id
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowPreviewSheet(
    show: Show,
    allShows: List<Show>,
    onDismiss: () -> Unit,
    onAddToLibrary: (Show) -> Unit,
    onWatchClick: (String) -> Unit,
    onShowClick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var extraMedia by remember(show.id) { mutableStateOf<AniListMedia?>(null) }
    var isLoadingExtra by remember(show.id) { mutableStateOf(false) }

    LaunchedEffect(show.id) {
        if (show.genres.isNullOrEmpty() || show.studio.isNullOrEmpty() || show.notes.isNullOrEmpty()) {
            isLoadingExtra = true
            try {
                val results = AniListService.searchAnime(show.title, limit = 1)
                if (results.isNotEmpty()) {
                    extraMedia = results.first()
                }
            } catch (_: Exception) {
            } finally {
                isLoadingExtra = false
            }
        }
    }

    val displayStatus = extraMedia?.status ?: show.status
    val displayEpisodes = if (show.totalEp > 0) show.totalEp else (extraMedia?.episodes ?: 0)
    val displayScore = if (show.rating > 0) show.rating else (extraMedia?.averageScore ?: 0)
    val displayYear = if (show.releaseYear > 0) show.releaseYear else (extraMedia?.releaseYear ?: 0)
    val displayStudio = show.studio ?: extraMedia?.studio
    val displayGenres = remember(show.genres, extraMedia?.genres) {
        if (!show.genres.isNullOrBlank()) {
            show.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            extraMedia?.genres ?: emptyList()
        }
    }
    val rawDescription = extraMedia?.description ?: show.notes
    val displayDescription = remember(rawDescription) {
        rawDescription
            ?.replace(Regex("<[^>]*>"), "")
            ?.replace("&quot;", "\"")
            ?.replace("&amp;", "&")
            ?.replace("&#039;", "'")
            ?.replace("&apos;", "'")
            ?.trim()
            ?.ifEmpty { "No synopsis available for this show." }
            ?: "No synopsis available for this show."
    }

    val isInLibrary = remember(show.id, show.title, show.aniListId, allShows) {
        allShows.any { s ->
            (show.aniListId != null && s.aniListId == show.aniListId) ||
            s.title.equals(show.title, ignoreCase = true) ||
            (!show.titleZh.isNullOrBlank() && s.title.equals(show.titleZh, ignoreCase = true))
        }
    }
    val existingShow = remember(isInLibrary) {
        allShows.firstOrNull { s ->
            (show.aniListId != null && s.aniListId == show.aniListId) ||
            s.title.equals(show.title, ignoreCase = true) ||
            (!show.titleZh.isNullOrBlank() && s.title.equals(show.titleZh, ignoreCase = true))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF101422) else Color(0xFFF8FAFC),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.15f))
            )
        },
        shape = RoundedCornerShape(topStart = CornerRadius.sheet, topEnd = CornerRadius.sheet)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header Row: Close button on right, Preview tag on left
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(13.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF02A9FF))
                    )
                    Text(
                        text = "Show Preview",
                        fontFamily = JustSansFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF02A9FF)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close preview",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Info Row: Poster on left, Identity on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(138.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            0.75.dp,
                            if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    DonghuaPoster(
                        posterUrl = show.poster,
                        title = show.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        text = show.title,
                        fontFamily = JustSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )

                    if (!show.titleZh.isNullOrBlank() && !show.titleZh.equals(show.title, ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = show.titleZh,
                            fontFamily = JustSansFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pill Badges: Status, Episodes, Rating, Year
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isOngoing = displayStatus.equals("ongoing", ignoreCase = true) || displayStatus.equals("releasing", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isOngoing) Color(0xFF00C853).copy(alpha = 0.15f) else Color(0xFF02A9FF).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = if (isOngoing) "Ongoing" else "Completed",
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp,
                                color = if (isOngoing) Color(0xFF00C853) else Color(0xFF02A9FF)
                            )
                        }

                        if (displayEpisodes > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "$displayEpisodes eps",
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (displayScore > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFB300).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "★ $displayScore%",
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFFFB300)
                                )
                            }
                        }

                        if (displayYear > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "$displayYear",
                                    fontFamily = JustSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!displayStudio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Studio: $displayStudio",
                            fontFamily = JustSansFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Genres Chips
            if (displayGenres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displayGenres) { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                .border(
                                    0.5.dp,
                                    if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 9.dp, vertical = 3.5.dp)
                        ) {
                            Text(
                                text = genre,
                                fontFamily = JustSansFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Synopsis Header & Content
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "Synopsis",
                    fontFamily = JustSansFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isLoadingExtra) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 130.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = displayDescription,
                    fontFamily = JustSansFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 17.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInLibrary) {
                    ColorOSPillButton(
                        text = "In Library • Open Details",
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                        onClick = {
                            onDismiss()
                            onShowClick(existingShow?.id ?: show.id)
                        }
                    )
                } else {
                    ColorOSPillButton(
                        text = "+ Add to Library",
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                        onClick = {
                            val newShow = show.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                dateAdded = System.currentTimeMillis(),
                                genres = if (displayGenres.isNotEmpty()) displayGenres.joinToString(", ") else show.genres,
                                studio = displayStudio ?: show.studio,
                                rating = if (displayScore > 0) displayScore else show.rating,
                                releaseYear = if (displayYear > 0) displayYear else show.releaseYear,
                                notes = displayDescription,
                                totalEp = if (displayEpisodes > 0) displayEpisodes else show.totalEp,
                                status = displayStatus
                            )
                            onAddToLibrary(newShow)
                            onDismiss()
                            onShowClick(newShow.id)
                        }
                    )
                }

                ColorOSOutlinedButton(
                    text = "Watch",
                    modifier = Modifier.width(96.dp),
                    height = 46.dp,
                    onClick = {
                        onDismiss()
                        val target = show.watchUrl.takeIf { !it.isNullOrBlank() } ?: show.title
                        onWatchClick(target)
                    }
                )
            }
        }
    }
}
