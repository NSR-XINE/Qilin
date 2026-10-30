package com.mydonghua.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.mydonghua.app.AlarmScheduler
import com.mydonghua.app.data.service.AniListMedia
import com.mydonghua.app.data.service.AniListService
import com.mydonghua.app.data.service.PosterFetchService
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.GlassHeader
import com.mydonghua.app.ui.GlassSection
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.ColorOSDialog
import com.mydonghua.app.ui.common.ColorOSPillButton
import com.mydonghua.app.ui.common.DonghuaPoster
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.colorOSPressBounce
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditShowScreen(
    showId: String?,
    viewModel: ShowsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allShows by viewModel.allShows.collectAsState()
    val editingShow = remember(showId, allShows) {
        if (!showId.isNullOrEmpty()) allShows.find { it.id == showId } else null
    }

    var title by remember { mutableStateOf("") }
    var titleZh by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ongoing") }
    var releaseDay by remember { mutableStateOf("Sunday") }
    var releaseTime by remember { mutableStateOf("") }
    var currentEp by remember { mutableStateOf("0") }
    var totalEp by remember { mutableStateOf("0") }
    var poster by remember { mutableStateOf("") }
    var watchUrl by remember { mutableStateOf("") }
    var countdownUrl by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0f) }
    var genres by remember { mutableStateOf("") }
    var studio by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var collectionId by remember { mutableStateOf("") }
    var aniListId by remember { mutableStateOf("") }
    var isAutoFilling by remember { mutableStateOf(false) }
    var showAniListSearchDialog by remember { mutableStateOf(false) }
    var aniListSearchQuery by remember { mutableStateOf("") }
    var isSearchingAniList by remember { mutableStateOf(false) }
    var aniListSearchResults by remember { mutableStateOf<List<AniListMedia>>(emptyList()) }

    LaunchedEffect(editingShow) {
        editingShow?.let { show ->
            title = show.title
            titleZh = show.titleZh ?: ""
            status = show.status
            releaseDay = show.releaseDay ?: "Sunday"
            releaseTime = show.releaseTime ?: ""
            currentEp = show.currentEp.toString()
            totalEp = show.totalEp.toString()
            poster = show.poster ?: ""
            watchUrl = show.watchUrl ?: ""
            countdownUrl = show.countdownUrl ?: ""
            notes = show.notes ?: ""
            rating = show.rating.toFloat()
            genres = show.genres ?: ""
            studio = show.studio ?: ""
            releaseYear = if (show.releaseYear > 0) show.releaseYear.toString() else ""
            language = show.language ?: ""
            collectionId = show.collectionId ?: ""
            aniListId = show.aniListId?.toString() ?: ""
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerTopPadding = statusBarTop + 84.dp
    val screenBackdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(screenBackdrop)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(top = headerTopPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Spacer(modifier = Modifier.height(Spacing.sm))

            GlassSection {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    AddEditSectionTitle("Basic Info")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("English Title *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.md)
                        )

                        FilledTonalButton(
                            onClick = {
                                aniListSearchQuery = title.trim()
                                showAniListSearchDialog = true
                                if (aniListSearchQuery.isNotBlank()) {
                                    isSearchingAniList = true
                                    coroutineScope.launch {
                                        aniListSearchResults = AniListService.searchAnime(aniListSearchQuery, limit = 12)
                                        isSearchingAniList = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(CornerRadius.md),
                            modifier = Modifier
                                .height(56.dp)
                                .colorOSPressBounce(pressedScale = 0.92f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF02A9FF),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AniList", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = titleZh,
                        onValueChange = { titleZh = it },
                        label = { Text("Chinese / Alternate Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.md)
                    )

                    var statusExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = status.replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(CornerRadius.md)
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            listOf("ongoing", "completed", "stopped").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.replaceFirstChar { it.uppercase() }) },
                                    onClick = { status = option; statusExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            if (status == "ongoing") {
                GlassSection {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        AddEditSectionTitle("Schedule")

                        var dayExpanded by remember { mutableStateOf(false) }
                        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                        ExposedDropdownMenuBox(
                            expanded = dayExpanded,
                            onExpandedChange = { dayExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = releaseDay,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Broadcast Day") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(CornerRadius.md)
                            )
                            ExposedDropdownMenu(
                                expanded = dayExpanded,
                                onDismissRequest = { dayExpanded = false }
                            ) {
                                days.forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text(day) },
                                        onClick = { releaseDay = day; dayExpanded = false }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            OutlinedTextField(
                                value = releaseTime,
                                onValueChange = { releaseTime = it },
                                label = { Text("Time (HH:MM)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(CornerRadius.md)
                            )
                            OutlinedTextField(
                                value = releaseYear,
                                onValueChange = { releaseYear = it },
                                label = { Text("Year") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(CornerRadius.md)
                            )
                        }
                    }
                }
            }

            GlassSection {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    AddEditSectionTitle("Episodes")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        OutlinedTextField(
                            value = currentEp,
                            onValueChange = { currentEp = it },
                            label = { Text("Current") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.md)
                        )
                        OutlinedTextField(
                            value = totalEp,
                            onValueChange = { totalEp = it },
                            label = { Text("Total") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.md)
                        )
                    }
                }
            }

            GlassSection {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    AddEditSectionTitle("Media & Links")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = poster,
                            onValueChange = { poster = it },
                            label = { Text("Poster URL") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.md)
                        )

                        FilledTonalButton(
                            onClick = {
                                val sourceUrl = watchUrl.takeIf { it.isNotEmpty() } ?: countdownUrl
                                if (sourceUrl.isBlank()) {
                                    Toast.makeText(context, "Enter watch or countdown URL first", Toast.LENGTH_SHORT).show()
                                    return@FilledTonalButton
                                }
                                coroutineScope.launch {
                                    val scraped = PosterFetchService.scrapePoster(sourceUrl)
                                    if (scraped != null) {
                                        poster = scraped
                                        Toast.makeText(context, "Poster extracted!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Could not scrape image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(CornerRadius.md),
                            modifier = Modifier
                                .height(56.dp)
                                .colorOSPressBounce(pressedScale = 0.92f),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scrape", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        com.mydonghua.app.ui.common.DonghuaPoster(
                            posterUrl = poster,
                            title = title.ifBlank { "New Show" },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(CornerRadius.md)),
                            contentScale = ContentScale.Crop,
                            fontSize = 16.sp
                        )
                    }

                    OutlinedTextField(
                        value = watchUrl,
                        onValueChange = { watchUrl = it },
                        label = { Text("Watch URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.md)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        AssistChip(
                            onClick = {
                                val t = title.ifBlank { "donghua" }
                                watchUrl = com.mydonghua.app.data.service.PosterFetchService.getDonghuaStreamUrl(t)
                            },
                            shape = RoundedCornerShape(CornerRadius.pill),
                            label = { Text("Set DonghuaStream", fontSize = 11.sp) }
                        )
                        AssistChip(
                            onClick = {
                                val t = title.ifBlank { "donghua" }
                                watchUrl = com.mydonghua.app.data.service.PosterFetchService.getLuciferDonghuaUrl(t)
                            },
                            shape = RoundedCornerShape(CornerRadius.pill),
                            label = { Text("Set LuciferDonghua", fontSize = 11.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = countdownUrl,
                        onValueChange = { countdownUrl = it },
                        label = { Text("Countdown / Calendar URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.md)
                    )

                    OutlinedTextField(
                        value = collectionId,
                        onValueChange = { collectionId = it },
                        label = { Text("Collection ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.md)
                    )
                }
            }

            GlassSection {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    AddEditSectionTitle("Details")

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Synopsis / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(CornerRadius.md)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        OutlinedTextField(
                            value = genres,
                            onValueChange = { genres = it },
                            label = { Text("Genres") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.md)
                        )
                        OutlinedTextField(
                            value = studio,
                            onValueChange = { studio = it },
                            label = { Text("Studio") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.md)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        OutlinedTextField(
                            value = language,
                            onValueChange = { language = it },
                            label = { Text("Language") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.md)
                        )
                        OutlinedTextField(
                            value = aniListId,
                            onValueChange = { aniListId = it },
                            label = { Text("AniList ID") },
                            placeholder = { Text("e.g. 101920") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.md)
                        )
                    }
                }
            }



            Spacer(modifier = Modifier.height(Spacing.sm))

            ColorOSPillButton(
                text = if (editingShow != null) "Update Donghua" else "Save Show",
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "English title is required", Toast.LENGTH_SHORT).show()
                        return@ColorOSPillButton
                    }

                    val newAlarmCode = UUID.randomUUID().toString().hashCode() and 0x7FFFFFFF

                    var show = Show(
                        id = editingShow?.id ?: UUID.randomUUID().toString(),
                        title = title,
                        titleZh = titleZh.takeIf { it.isNotEmpty() },
                        status = status,
                        releaseDay = releaseDay,
                        releaseTime = releaseTime.takeIf { it.isNotEmpty() },
                        currentEp = currentEp.toIntOrNull() ?: 0,
                        totalEp = totalEp.toIntOrNull() ?: 0,
                        poster = poster.takeIf { it.isNotEmpty() },
                        watchUrl = watchUrl.takeIf { it.isNotEmpty() },
                        countdownUrl = countdownUrl.takeIf { it.isNotEmpty() },
                        notes = notes.takeIf { it.isNotEmpty() },
                        isFavorite = editingShow?.isFavorite ?: 0,
                        rating = rating.toInt(),
                        lastUpdated = System.currentTimeMillis(),
                        dateAdded = editingShow?.dateAdded ?: System.currentTimeMillis(),
                        alarmRequestCode = editingShow?.alarmRequestCode,
                        genres = genres.takeIf { it.isNotEmpty() },
                        studio = studio.takeIf { it.isNotEmpty() },
                        releaseYear = releaseYear.toIntOrNull() ?: 0,
                        language = language.takeIf { it.isNotEmpty() },
                        collectionId = collectionId.takeIf { it.isNotEmpty() },
                        aniListId = aniListId.toIntOrNull()
                    )

                    if (show.status == "ongoing" && !show.releaseTime.isNullOrEmpty() && !show.releaseDay.isNullOrEmpty()) {
                        val scheduleCode = show.alarmRequestCode ?: newAlarmCode
                        show = show.copy(alarmRequestCode = scheduleCode)
                        AlarmScheduler.schedule(
                            context, show.id, show.title, show.releaseDay,
                            show.releaseTime, scheduleCode, show.currentEp
                        )
                    } else {
                        val cancelCode = show.alarmRequestCode ?: 0
                        if (cancelCode > 0) {
                            AlarmScheduler.cancel(context, show.id, cancelCode)
                        }
                        show = show.copy(alarmRequestCode = null)
                    }

                    viewModel.insertOrUpdateShow(show)
                    if (show.aniListId != null && show.currentEp > 0) {
                        coroutineScope.launch {
                            AniListService.syncEpisodeProgress(show, show.currentEp)
                        }
                    }
                    Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                containerColor = Color(0xFF0082FB),
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = if (editingShow != null) Icons.Rounded.Check else Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.md),
                        tint = Color.White
                    )
                }
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }

        if (showAniListSearchDialog) {
            ColorOSDialog(
                onDismissRequest = { showAniListSearchDialog = false },
                title = "Search AniList Media",
                confirmText = "Close",
                dismissText = null,
                onConfirm = { showAniListSearchDialog = false },
                customContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = aniListSearchQuery,
                                onValueChange = { aniListSearchQuery = it },
                                placeholder = { Text("Search donghua title...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ColorOSPillButton(
                                text = "Search",
                                onClick = {
                                    if (aniListSearchQuery.isNotBlank()) {
                                        isSearchingAniList = true
                                        coroutineScope.launch {
                                            aniListSearchResults = AniListService.searchAnime(aniListSearchQuery, limit = 12)
                                            isSearchingAniList = false
                                        }
                                    }
                                },
                                height = 50.dp,
                                containerColor = Color(0xFF02A9FF)
                            )
                        }

                        if (isSearchingAniList) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color(0xFF02A9FF))
                            }
                        } else if (aniListSearchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (aniListSearchQuery.isBlank()) "Type a title and tap Search" else "No matching anime found",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                aniListSearchResults.forEach { media ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                            .clickable {
                                                title = media.preferredTitle
                                                if (!media.titleNative.isNullOrBlank()) titleZh = media.titleNative
                                                if (!media.coverImage.isNullOrBlank()) poster = media.coverImage
                                                if (!media.description.isNullOrBlank()) notes = media.description
                                                if (media.episodes > 0) totalEp = media.episodes.toString()
                                                if (!media.status.isNullOrBlank()) {
                                                    status = when (media.status.lowercase()) {
                                                        "completed" -> "completed"
                                                        "releasing" -> "ongoing"
                                                        else -> "ongoing"
                                                    }
                                                }
                                                if (!media.genres.isNullOrEmpty()) genres = media.genres.joinToString(", ")
                                                if (!media.studio.isNullOrBlank()) studio = media.studio
                                                if (media.releaseYear != null && media.releaseYear > 0) releaseYear = media.releaseYear.toString()
                                                aniListId = media.id.toString()

                                                showAniListSearchDialog = false
                                                Toast.makeText(context, "Filled from ${media.preferredTitle}!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DonghuaPoster(
                                            posterUrl = media.coverImage,
                                            title = media.preferredTitle,
                                            modifier = Modifier
                                                .size(width = 46.dp, height = 64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = media.preferredTitle,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (!media.titleNative.isNullOrBlank()) {
                                                Text(
                                                    text = media.titleNative,
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = buildString {
                                                    if (media.episodes > 0) append("${media.episodes} eps")
                                                    if (media.releaseYear != null && media.releaseYear > 0) {
                                                        if (isNotEmpty()) append(" • ")
                                                        append(media.releaseYear)
                                                    }
                                                    if (!media.status.isNullOrBlank()) {
                                                        if (isNotEmpty()) append(" • ")
                                                        append(media.status.replaceFirstChar { it.uppercase() })
                                                    }
                                                },
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        GlassHeader(
            title = if (editingShow != null) "Edit Show" else "Add New Show",
            backdrop = screenBackdrop,
            onBackClick = onNavigateBack
        )
    }
}

@Composable
private fun AddEditSectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}