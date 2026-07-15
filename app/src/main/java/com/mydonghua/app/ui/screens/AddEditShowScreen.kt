package com.mydonghua.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.mydonghua.app.R
import com.mydonghua.app.AlarmScheduler
import com.mydonghua.app.data.service.PosterFetchService
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.GlassSection
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.theme.ButtonHeight
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
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
    var isAutoFilling by remember { mutableStateOf(false) }

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
            releaseYear = show.releaseYear?.toString() ?: ""
            language = show.language ?: ""
            collectionId = show.collectionId ?: ""
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editingShow != null) "Edit Show" else "Add New Show",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Spacer(modifier = Modifier.height(Spacing.sm))

            GlassSection {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        text = "Basic Info",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

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
                            shape = RoundedCornerShape(CornerRadius.sm)
                        )

                        FilledTonalButton(
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "Enter title first", Toast.LENGTH_SHORT).show()
                                    return@FilledTonalButton
                                }
                                isAutoFilling = true
                                coroutineScope.launch {
                                    var result = PosterFetchService.fetchFromJikan(title)
                                    if (result == null) {
                                        result = PosterFetchService.fetchFromAniList(title)
                                    }
                                    if (result != null) {
                                        titleZh = result.titleZh ?: titleZh
                                        notes = result.synopsis ?: notes
                                        poster = result.poster ?: poster
                                        if (result.totalEp > 0) totalEp = result.totalEp.toString()
                                        status = result.status ?: status
                                        Toast.makeText(context, "Details auto-filled!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No matching show found", Toast.LENGTH_SHORT).show()
                                    }
                                    isAutoFilling = false
                                }
                            },
                            enabled = !isAutoFilling,
                            shape = RoundedCornerShape(CornerRadius.sm)
                        ) {
                            Text(if (isAutoFilling) "\u2026" else "Auto-Fill", fontSize = 13.sp)
                        }
                    }

                    OutlinedTextField(
                        value = titleZh,
                        onValueChange = { titleZh = it },
                        label = { Text("Chinese / Alternate Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.sm)
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
                                .menuAnchor(),
                            shape = RoundedCornerShape(CornerRadius.sm)
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
                        Text(
                            text = "Schedule",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

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
                                    .menuAnchor(),
                                shape = RoundedCornerShape(CornerRadius.sm)
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
                                shape = RoundedCornerShape(CornerRadius.sm)
                            )
                            OutlinedTextField(
                                value = releaseYear,
                                onValueChange = { releaseYear = it },
                                label = { Text("Year") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(CornerRadius.sm)
                            )
                        }
                    }
                }
            }

            GlassSection {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        text = "Episodes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

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
                            shape = RoundedCornerShape(CornerRadius.sm)
                        )
                        OutlinedTextField(
                            value = totalEp,
                            onValueChange = { totalEp = it },
                            label = { Text("Total") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.sm)
                        )
                    }
                }
            }

            GlassSection {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        text = "Media & Links",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = poster,
                            onValueChange = { poster = it },
                            label = { Text("Poster URL") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.sm)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                shape = RoundedCornerShape(CornerRadius.sm),
                                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm)
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(IconSize.xs))
                                Spacer(Modifier.width(Spacing.xs))
                                Text("Scrape", fontSize = 13.sp)
                            }

                            if (poster.isNotBlank()) {
                                Spacer(Modifier.height(Spacing.sm))
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(poster)
                                        .placeholder(R.drawable.ic_tv)
                                        .error(R.drawable.ic_tv)
                                        .fallback(R.drawable.ic_tv)
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = "Poster preview",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(CornerRadius.sm)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = watchUrl,
                        onValueChange = { watchUrl = it },
                        label = { Text("Watch URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.sm)
                    )

                    OutlinedTextField(
                        value = countdownUrl,
                        onValueChange = { countdownUrl = it },
                        label = { Text("Countdown / Calendar URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.sm)
                    )

                    OutlinedTextField(
                        value = collectionId,
                        onValueChange = { collectionId = it },
                        label = { Text("Collection ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.sm)
                    )
                }
            }

            GlassSection {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        text = "Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Synopsis / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(CornerRadius.sm)
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
                            shape = RoundedCornerShape(CornerRadius.sm)
                        )
                        OutlinedTextField(
                            value = studio,
                            onValueChange = { studio = it },
                            label = { Text("Studio") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(CornerRadius.sm)
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
                            shape = RoundedCornerShape(CornerRadius.sm)
                        )
                    }
                }
            }



            Spacer(modifier = Modifier.height(Spacing.sm))

            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "English title is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val show = Show(
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
                        alarmRequestCode = if (editingShow != null) editingShow.alarmRequestCode else Math.abs(UUID.randomUUID().toString().hashCode()),
                        genres = genres.takeIf { it.isNotEmpty() },
                        studio = studio.takeIf { it.isNotEmpty() },
                        releaseYear = releaseYear.toIntOrNull() ?: 0,
                        language = language.takeIf { it.isNotEmpty() },
                        collectionId = collectionId.takeIf { it.isNotEmpty() }
                    )

                    if (show.status == "ongoing" && !show.releaseTime.isNullOrEmpty() && show.alarmRequestCode != null) {
                        AlarmScheduler.schedule(
                            context, show.id, show.title, show.releaseDay,
                            show.releaseTime, show.alarmRequestCode!!, show.currentEp
                        )
                    } else {
                        val cancelCode = show.alarmRequestCode ?: editingShow?.alarmRequestCode ?: 0
                        AlarmScheduler.cancel(context, show.id, cancelCode)
                    }

                    viewModel.insertOrUpdateShow(show)
                    Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonHeight.xl),
                shape = RoundedCornerShape(CornerRadius.md)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(IconSize.md))
                Spacer(Modifier.width(Spacing.sm))
                Text("Save Show", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}