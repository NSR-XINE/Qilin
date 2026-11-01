package com.nsr.qilin.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nsr.qilin.AlarmScheduler
import com.nsr.qilin.data.service.AniListMedia
import com.nsr.qilin.data.service.AniListService
import com.nsr.qilin.data.service.PosterFetchService
import com.nsr.qilin.domain.model.Show
import com.nsr.qilin.ui.GlassHeader
import com.nsr.qilin.ui.Spacing
import com.nsr.qilin.ui.common.ColorOSDialog
import com.nsr.qilin.ui.common.ColorOSOutlinedButton
import com.nsr.qilin.ui.common.ColorOSPillButton
import com.nsr.qilin.ui.common.DonghuaPoster
import com.nsr.qilin.ui.rememberLiquidGlassBorder
import com.nsr.qilin.ui.theme.CornerRadius
import com.nsr.qilin.ui.theme.Haptics
import com.nsr.qilin.ui.theme.IconSize
import com.nsr.qilin.ui.theme.JustSansFamily
import com.nsr.qilin.ui.theme.colorOSPressBounce
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

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

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

    var showAniListSearchDialog by remember { mutableStateOf(false) }
    var aniListSearchQuery by remember { mutableStateOf("") }
    var isSearchingAniList by remember { mutableStateOf(false) }
    var aniListSearchResults by remember { mutableStateOf<List<AniListMedia>>(emptyList()) }

    var hasInitialized by remember(showId) { mutableStateOf(false) }

    LaunchedEffect(editingShow) {
        if (!hasInitialized && editingShow != null) {
            hasInitialized = true
            title = editingShow.title
            titleZh = editingShow.titleZh ?: ""
            status = editingShow.status
            releaseDay = editingShow.releaseDay ?: "Sunday"
            releaseTime = editingShow.releaseTime ?: ""
            currentEp = editingShow.currentEp.toString()
            totalEp = editingShow.totalEp.toString()
            poster = editingShow.poster ?: ""
            watchUrl = editingShow.watchUrl ?: ""
            countdownUrl = editingShow.countdownUrl ?: ""
            notes = editingShow.notes ?: ""
            rating = editingShow.rating.toFloat()
            genres = editingShow.genres ?: ""
            studio = editingShow.studio ?: ""
            releaseYear = if (editingShow.releaseYear > 0) editingShow.releaseYear.toString() else ""
            language = editingShow.language ?: ""
            collectionId = editingShow.collectionId ?: ""
            aniListId = editingShow.aniListId?.toString() ?: ""
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerTopPadding = statusBarTop + 84.dp
    val screenBackdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(screenBackdrop)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md)
                .padding(top = headerTopPadding, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top AniList Auto-fill Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CornerRadius.surface))
                    .background(Color(0xFF02A9FF).copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF02A9FF).copy(alpha = 0.35f),
                        shape = RoundedCornerShape(CornerRadius.surface)
                    )
                    .colorOSPressBounce(
                        pressedScale = 0.98f,
                        onClick = {
                            Haptics.tick()
                            aniListSearchQuery = title.trim()
                            showAniListSearchDialog = true
                            if (aniListSearchQuery.isNotBlank()) {
                                isSearchingAniList = true
                                coroutineScope.launch {
                                    aniListSearchResults = AniListService.searchAnime(aniListSearchQuery, limit = 12)
                                    isSearchingAniList = false
                                }
                            }
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF02A9FF).copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF02A9FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Auto-fill with AniList",
                                fontFamily = JustSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF02A9FF)
                            )
                            Text(
                                text = "Search to fill titles, poster, episodes, studio & synopsis",
                                fontFamily = JustSansFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF02A9FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 1. Basic Information Section
            ColorOSFormSection(title = "Basic Info", icon = Icons.Rounded.Info) {
                ColorOSFormField(
                    value = title,
                    onValueChange = { title = it },
                    label = "English Title *",
                    placeholder = "e.g. Battle Through the Heavens"
                )

                ColorOSFormField(
                    value = titleZh,
                    onValueChange = { titleZh = it },
                    label = "Chinese / Alternate Title",
                    placeholder = "e.g. 斗破苍穹"
                )

                // ColorOS 3-Pill Status Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "STATUS",
                        fontFamily = JustSansFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0082FB),
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("ongoing", "Airing", Color(0xFF0082FB)),
                            Triple("completed", "Completed", Color(0xFF10B981)),
                            Triple("stopped", "Hiatus", Color(0xFFFFA726))
                        ).forEach { (optStatus, optLabel, optColor) ->
                            val isSelected = status == optStatus
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(
                                        if (isSelected) optColor.copy(alpha = 0.20f)
                                        else if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.65.dp,
                                        color = if (isSelected) optColor.copy(alpha = 0.60f) else Color.Transparent,
                                        shape = RoundedCornerShape(percent = 50)
                                    )
                                    .clickable {
                                        Haptics.tick()
                                        status = optStatus
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) optColor else Color.Gray.copy(alpha = 0.5f))
                                    )
                                    Text(
                                        text = optLabel,
                                        fontFamily = JustSansFamily,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) optColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Schedule Section (for ongoing shows)
            if (status == "ongoing") {
                ColorOSFormSection(title = "Release Schedule", icon = Icons.Rounded.Schedule) {
                    var dayExpanded by remember { mutableStateOf(false) }
                    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

                    ExposedDropdownMenuBox(
                        expanded = dayExpanded,
                        onExpandedChange = { dayExpanded = it }
                    ) {
                        ColorOSFormField(
                            value = releaseDay,
                            onValueChange = {},
                            readOnly = true,
                            label = "Broadcast Day",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false }
                        ) {
                            days.forEach { day ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = day,
                                            fontFamily = JustSansFamily,
                                            fontSize = 13.5.sp
                                        )
                                    },
                                    onClick = {
                                        Haptics.tick()
                                        releaseDay = day
                                        dayExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ColorOSFormField(
                            value = releaseTime,
                            onValueChange = { releaseTime = it },
                            label = "Time (HH:MM)",
                            placeholder = "10:00",
                            modifier = Modifier.weight(1f)
                        )
                        ColorOSFormField(
                            value = releaseYear,
                            onValueChange = { releaseYear = it.filter { ch -> ch.isDigit() } },
                            label = "Year",
                            placeholder = "2024",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Episode Progress Section
            ColorOSFormSection(title = "Episodes", icon = Icons.Rounded.VideoLibrary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorOSFormField(
                        value = currentEp,
                        onValueChange = { currentEp = it.filter { ch -> ch.isDigit() } },
                        label = "Current Episode",
                        placeholder = "0",
                        modifier = Modifier.weight(1f)
                    )
                    ColorOSFormField(
                        value = totalEp,
                        onValueChange = { totalEp = it.filter { ch -> ch.isDigit() } },
                        label = "Total Episodes",
                        placeholder = "0 (ongoing)",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Media & Streaming Links Section
            ColorOSFormSection(title = "Media & Links", icon = Icons.Rounded.Link) {
                // Integrated Poster Preview Card & URL input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val thumbShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .size(width = 68.dp, height = 96.dp)
                            .clip(thumbShape)
                            .border(
                                width = 1.dp,
                                brush = rememberLiquidGlassBorder(isLight),
                                shape = thumbShape
                            )
                    ) {
                        DonghuaPoster(
                            posterUrl = poster,
                            title = title.ifBlank { "New Show" },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            fontSize = 12.sp,
                            immediate = true
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorOSFormField(
                            value = poster,
                            onValueChange = { poster = it },
                            label = "Poster Image URL",
                            placeholder = "https://..."
                        )

                        ColorOSOutlinedButton(
                            text = "Scrape Poster Art",
                            onClick = {
                                val sourceUrl = watchUrl.takeIf { it.isNotEmpty() } ?: countdownUrl
                                if (sourceUrl.isBlank()) {
                                    Toast.makeText(context, "Enter watch or countdown URL first", Toast.LENGTH_SHORT).show()
                                    return@ColorOSOutlinedButton
                                }
                                coroutineScope.launch {
                                    val scraped = PosterFetchService.scrapePoster(sourceUrl)
                                    if (scraped != null) {
                                        poster = scraped
                                        Toast.makeText(context, "Poster extracted successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Could not scrape poster image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            height = 38.dp,
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Watch URL Input & Provider Quick Presets
                ColorOSFormField(
                    value = watchUrl,
                    onValueChange = { watchUrl = it },
                    label = "Watch URL",
                    placeholder = "https://donghuastream.org/..."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chipBg = if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.08f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(chipBg)
                            .border(0.75.dp, rememberLiquidGlassBorder(isLight), RoundedCornerShape(percent = 50))
                            .clickable {
                                Haptics.tick()
                                val t = title.ifBlank { "donghua" }
                                watchUrl = PosterFetchService.getDonghuaStreamUrl(t)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DonghuaStream 🎬",
                            fontSize = 11.5.sp,
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(chipBg)
                            .border(0.75.dp, rememberLiquidGlassBorder(isLight), RoundedCornerShape(percent = 50))
                            .clickable {
                                Haptics.tick()
                                val t = title.ifBlank { "donghua" }
                                watchUrl = PosterFetchService.getLuciferDonghuaUrl(t)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LuciferDonghua ⚡",
                            fontSize = 11.5.sp,
                            fontFamily = JustSansFamily,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                ColorOSFormField(
                    value = countdownUrl,
                    onValueChange = { countdownUrl = it },
                    label = "Countdown / Calendar URL",
                    placeholder = "Optional schedule link"
                )

                ColorOSFormField(
                    value = collectionId,
                    onValueChange = { collectionId = it },
                    label = "Collection ID",
                    placeholder = "Optional series group ID"
                )
            }

            // 5. Details & Metadata Section
            ColorOSFormSection(title = "Details & Metadata", icon = Icons.Rounded.Description) {
                ColorOSFormField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Synopsis / Notes",
                    placeholder = "Enter story synopsis or personal notes...",
                    singleLine = false,
                    maxLines = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorOSFormField(
                        value = genres,
                        onValueChange = { genres = it },
                        label = "Genres",
                        placeholder = "Action, Fantasy",
                        modifier = Modifier.weight(1f)
                    )
                    ColorOSFormField(
                        value = studio,
                        onValueChange = { studio = it },
                        label = "Studio",
                        placeholder = "e.g. Sparkly Key",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorOSFormField(
                        value = language,
                        onValueChange = { language = it },
                        label = "Language",
                        placeholder = "Chinese",
                        modifier = Modifier.weight(1f)
                    )
                    ColorOSFormField(
                        value = aniListId,
                        onValueChange = { aniListId = it.filter { ch -> ch.isDigit() } },
                        label = "AniList ID",
                        placeholder = "e.g. 101920",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Bottom Action Button
            ColorOSPillButton(
                text = if (editingShow != null) "Update Donghua" else "Save Show",
                onClick = {
                    Haptics.impact()
                    if (title.isBlank()) {
                        Toast.makeText(context, "English title is required", Toast.LENGTH_SHORT).show()
                        return@ColorOSPillButton
                    }

                    val trimmedWatchUrl = watchUrl.trim()
                    val trimmedCountdownUrl = countdownUrl.trim()

                    fun isValidHttpUrl(u: String): Boolean {
                        if (u.isEmpty()) return true
                        return try {
                            val uri = Uri.parse(u)
                            val scheme = uri.scheme?.lowercase()
                            (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
                        } catch (_: Exception) {
                            false
                        }
                    }

                    if (trimmedWatchUrl.isNotEmpty() && !isValidHttpUrl(trimmedWatchUrl)) {
                        Toast.makeText(context, "Watch URL must start with http:// or https:// and include a valid host", Toast.LENGTH_LONG).show()
                        return@ColorOSPillButton
                    }

                    if (trimmedCountdownUrl.isNotEmpty() && !isValidHttpUrl(trimmedCountdownUrl)) {
                        Toast.makeText(context, "Countdown URL must start with http:// or https:// and include a valid host", Toast.LENGTH_LONG).show()
                        return@ColorOSPillButton
                    }

                    var show = Show(
                        id = editingShow?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        titleZh = titleZh.trim().takeIf { it.isNotEmpty() },
                        status = status,
                        releaseDay = releaseDay,
                        releaseTime = releaseTime.trim().takeIf { it.isNotEmpty() },
                        currentEp = currentEp.toIntOrNull() ?: 0,
                        totalEp = totalEp.toIntOrNull() ?: 0,
                        poster = poster.trim().takeIf { it.isNotEmpty() },
                        watchUrl = trimmedWatchUrl.takeIf { it.isNotEmpty() },
                        countdownUrl = trimmedCountdownUrl.takeIf { it.isNotEmpty() },
                        notes = notes.trim().takeIf { it.isNotEmpty() },
                        isFavorite = editingShow?.isFavorite ?: 0,
                        rating = rating.toInt(),
                        lastUpdated = System.currentTimeMillis(),
                        dateAdded = editingShow?.dateAdded ?: System.currentTimeMillis(),
                        alarmRequestCode = editingShow?.alarmRequestCode,
                        genres = genres.trim().takeIf { it.isNotEmpty() },
                        studio = studio.trim().takeIf { it.isNotEmpty() },
                        releaseYear = releaseYear.toIntOrNull() ?: 0,
                        language = language.trim().takeIf { it.isNotEmpty() } ?: "Chinese",
                        collectionId = collectionId.trim().takeIf { it.isNotEmpty() },
                        aniListId = aniListId.toIntOrNull()
                    )

                    if (show.status == "ongoing" && !show.releaseTime.isNullOrEmpty() && !show.releaseDay.isNullOrEmpty()) {
                        val activeCode = editingShow?.alarmRequestCode
                        if (activeCode != null && activeCode > 0) {
                            AlarmScheduler.schedule(
                                context, show.id, show.title, show.releaseDay,
                                show.releaseTime, activeCode, show.currentEp
                            )
                        }
                    } else {
                        val cancelCode = editingShow?.alarmRequestCode ?: 0
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
        }

        // AniList Media Search Modal Dialog
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
                                placeholder = {
                                    Text(
                                        text = "Search donghua title...",
                                        fontFamily = JustSansFamily,
                                        fontSize = 13.sp
                                    )
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = JustSansFamily,
                                    fontSize = 14.sp
                                ),
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
                                    fontFamily = JustSansFamily,
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
                                                if (!media.description.isNullOrBlank()) {
                                                    notes = media.description.replace(Regex("<[^>]*>"), "").trim()
                                                }
                                                if (media.episodes > 0) totalEp = media.episodes.toString()
                                                if (!media.status.isNullOrBlank()) {
                                                    status = when (media.status.lowercase()) {
                                                        "completed", "finished" -> "completed"
                                                        else -> "ongoing"
                                                    }
                                                }
                                                if (!media.genres.isNullOrEmpty()) genres = media.genres.joinToString(", ")
                                                if (!media.studio.isNullOrBlank()) studio = media.studio
                                                if (media.releaseYear != null && media.releaseYear > 0) releaseYear = media.releaseYear.toString()
                                                aniListId = media.id.toString()

                                                showAniListSearchDialog = false
                                                Toast.makeText(context, "Filled details from ${media.preferredTitle}!", Toast.LENGTH_SHORT).show()
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
                                                fontFamily = JustSansFamily,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (!media.titleNative.isNullOrBlank()) {
                                                Text(
                                                    text = media.titleNative,
                                                    fontSize = 11.5.sp,
                                                    fontFamily = JustSansFamily,
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
                                                fontFamily = JustSansFamily,
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

        // Top Floating Glass Header
        GlassHeader(
            title = if (editingShow != null) "Edit Show" else "Add New Show",
            backdrop = screenBackdrop,
            isDark = !isLight,
            onBackClick = onNavigateBack
        )
    }
}

@Composable
private fun ColorOSFormSection(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val cardBg = if (isLight) Color.White.copy(alpha = 0.88f) else Color(0xFF1E2638).copy(alpha = 0.45f)
    val glassBorder = rememberLiquidGlassBorder(isLight)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.card))
            .background(cardBg)
            .border(
                width = 0.75.dp,
                brush = glassBorder,
                shape = RoundedCornerShape(CornerRadius.card)
            )
            .padding(Spacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF0082FB),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF0082FB))
                    )
                }
                Text(
                    text = title.uppercase(),
                    fontFamily = JustSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF0082FB),
                    letterSpacing = 0.5.sp
                )
            }

            content()
        }
    }
}

@Composable
private fun ColorOSFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    readOnly: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        },
        placeholder = if (placeholder != null) {
            {
                Text(
                    text = placeholder,
                    fontFamily = JustSansFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else null,
        textStyle = LocalTextStyle.current.copy(
            fontFamily = JustSansFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = singleLine,
        maxLines = maxLines,
        readOnly = readOnly,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF0082FB),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
            focusedLabelColor = Color(0xFF0082FB),
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.20f)
        )
    )
}
