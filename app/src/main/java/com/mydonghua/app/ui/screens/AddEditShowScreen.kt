package com.mydonghua.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.AlarmScheduler
import com.mydonghua.app.data.service.PosterFetchService
import com.mydonghua.app.domain.model.Show
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
        if (!showId.isNullOrEmpty()) {
            allShows.find { it.id == showId }
        } else {
            null
        }
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
                title = { Text(if (editingShow != null) "Edit Show" else "Add New Show", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title Input with Autofill Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("English Title *") },
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "Enter title first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isAutoFilling = true
                        coroutineScope.launch {
                            // 1. Try Jikan API
                            var result = PosterFetchService.fetchFromJikan(title)
                            if (result == null) {
                                // 2. Fallback to AniList GraphQL
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
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isAutoFilling) "..." else "Auto-Fill")
                }
            }

            OutlinedTextField(
                value = titleZh,
                onValueChange = { titleZh = it },
                label = { Text("Chinese/Alternate Title") },
                modifier = Modifier.fillMaxWidth()
            )

            // Status Selector Dropdown
            var statusExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { statusExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Status: ${status.uppercase()}")
                }
                DropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Ongoing") }, onClick = { status = "ongoing"; statusExpanded = false })
                    DropdownMenuItem(text = { Text("Completed") }, onClick = { status = "completed"; statusExpanded = false })
                    DropdownMenuItem(text = { Text("Stopped / Hiatus") }, onClick = { status = "stopped"; statusExpanded = false })
                }
            }

            // Release Day Selector Dropdown
            if (status == "ongoing") {
                var dayExpanded by remember { mutableStateOf(false) }
                val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { dayExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Broadcast Day: $releaseDay")
                    }
                    DropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        days.forEach { day ->
                            DropdownMenuItem(text = { Text(day) }, onClick = { releaseDay = day; dayExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = releaseTime,
                    onValueChange = { releaseTime = it },
                    label = { Text("Broadcast Time (HH:MM e.g. 10:30)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = currentEp,
                    onValueChange = { currentEp = it },
                    label = { Text("Current Episode") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = totalEp,
                    onValueChange = { totalEp = it },
                    label = { Text("Total Episodes") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Poster image URL + scraper option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = poster,
                    onValueChange = { poster = it },
                    label = { Text("Poster Image URL") },
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        val sourceUrl = watchUrl.takeIf { it.isNotEmpty() } ?: countdownUrl
                        if (sourceUrl.isBlank()) {
                            Toast.makeText(context, "Enter watch or countdown URL first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            val scraped = PosterFetchService.scrapePoster(sourceUrl)
                            if (scraped != null) {
                                poster = scraped
                                Toast.makeText(context, "Poster image extracted!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Could not scrape image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Scrape")
                }
            }

            OutlinedTextField(
                value = watchUrl,
                onValueChange = { watchUrl = it },
                label = { Text("Watch Source URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = countdownUrl,
                onValueChange = { countdownUrl = it },
                label = { Text("Countdown / Calendar URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Synopsis / Personal Notes") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            // Rating
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("User Rating: ${rating.toInt()} / 10", fontSize = 14.sp)
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 0f..10f,
                    steps = 9
                )
            }

            OutlinedTextField(
                value = genres,
                onValueChange = { genres = it },
                label = { Text("Genres (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = studio,
                onValueChange = { studio = it },
                label = { Text("Studio") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = releaseYear,
                    onValueChange = { releaseYear = it },
                    label = { Text("Release Year") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Language") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = collectionId,
                onValueChange = { collectionId = it },
                label = { Text("Collection ID") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save / Submit Button
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
                        alarmRequestCode = editingShow?.alarmRequestCode ?: Math.abs(UUID.randomUUID().toString().hashCode()),
                        genres = genres.takeIf { it.isNotEmpty() },
                        studio = studio.takeIf { it.isNotEmpty() },
                        releaseYear = releaseYear.toIntOrNull() ?: 0,
                        language = language.takeIf { it.isNotEmpty() },
                        collectionId = collectionId.takeIf { it.isNotEmpty() }
                    )

                    // Schedule alarm if ongoing and release schedule details are set
                    if (show.status == "ongoing" && !show.releaseTime.isNullOrEmpty()) {
                        AlarmScheduler.schedule(
                            context,
                            show.id,
                            show.title,
                            show.releaseDay,
                            show.releaseTime,
                            show.alarmRequestCode ?: 0,
                            show.currentEp
                        )
                    } else {
                        // Cancel any scheduled alarm
                        AlarmScheduler.cancel(context, show.id, show.alarmRequestCode ?: 0)
                    }

                    viewModel.insertOrUpdateShow(show)
                    Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Show Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
