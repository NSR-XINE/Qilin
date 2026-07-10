package com.mydonghua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mydonghua.app.R
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.common.ShowCard
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    showsViewModel: ShowsViewModel,
    onWatchClick: (String) -> Unit,
    onShowClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val results by viewModel.filteredShows.collectAsState()
    val filters by viewModel.activeFilters.collectAsState()
    val genres by viewModel.distinctGenres.collectAsState()
    val context = LocalContext.current

    var isSearchActive by remember { mutableStateOf(false) }
    var showGenreDialog by remember { mutableStateOf(false) }
    var showStudioDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = query,
                onQueryChange = { viewModel.searchQuery.value = it },
                onSearch = {
                    viewModel.selectQuery(it)
                    isSearchActive = false
                },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                placeholder = { Text("Search shows, genres, studios...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.padding(start = Spacing.md)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearQuery() },
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            Text("\u2715", fontSize = 16.sp)
                        }
                    }
                },
                shape = RoundedCornerShape(CornerRadius.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(top = Spacing.xs)
            ) {
                suggestions.forEach { suggestion ->
                    when (suggestion) {
                        is SearchSuggestion.Popular -> {
                            SuggestionItem(
                                text = suggestion.text,
                                onClick = {
                                    viewModel.selectQuery(suggestion.text)
                                    isSearchActive = false
                                }
                            )
                        }
                        is SearchSuggestion.Recent -> {
                            SuggestionItem(
                                text = suggestion.text,
                                onClick = {
                                    viewModel.selectQuery(suggestion.text)
                                    isSearchActive = false
                                }
                            )
                        }
                        is SearchSuggestion.ShowMatch -> {
                            SuggestionItem(
                                text = suggestion.displayText,
                                onClick = {
                                    viewModel.selectQuery(suggestion.displayText)
                                    isSearchActive = false
                                    onShowClick(suggestion.show.id)
                                }
                            )
                        }
                    }
                }
            }

            FilterChipsRow(
                filters = filters,
                onGenreClick = { showGenreDialog = true },
                onStudioClick = { showStudioDialog = true },
                onYearClick = { showYearDialog = true },
                onRatingClick = { showRatingDialog = true },
                onSortClick = { showSortDialog = true },
                onClearAll = { viewModel.updateFilters(SearchFilters()) }
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            if (results.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.hero),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(
                            text = if (query.isEmpty()) "Search your shows" else "No matching shows found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (query.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = "Try adjusting your search or filters",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = Spacing.sm)
                ) {
                    items(results, key = { it.id }) { show ->
                        ShowCard(
                            show = show,
                            onWatchClick = onWatchClick,
                            onEditClick = { onShowClick(it.id) },
                            onDeleteClick = { showsViewModel.deleteShow(it.id) },
                            onFavoriteToggle = { showsViewModel.toggleFavorite(it) },
                            onNotifyToggle = { showsViewModel.toggleNotification(context, it) },
                            onEpIncrement = { showsViewModel.incrementEpisode(it) },
                            onEpDecrement = { showsViewModel.decrementEpisode(it) },
                            onCardClick = { onShowClick(it.id) }
                        )
                    }
                }
            }
        }
    }

    if (showGenreDialog) {
        GenreFilterDialog(
            genres = genres,
            selected = filters.selectedGenre,
            onDismiss = { showGenreDialog = false },
            onApply = { genre ->
                viewModel.updateFilters(filters.copy(selectedGenre = genre))
                showGenreDialog = false
            }
        )
    }
    if (showStudioDialog) {
        StudioFilterDialog(
            initial = filters.selectedStudio ?: "",
            onDismiss = { showStudioDialog = false },
            onApply = { studio ->
                viewModel.updateFilters(filters.copy(selectedStudio = studio.takeIf { it.isNotEmpty() }))
                showStudioDialog = false
            }
        )
    }
    if (showYearDialog) {
        YearFilterDialog(
            initial = filters.selectedYear?.toString() ?: "",
            onDismiss = { showYearDialog = false },
            onApply = { year ->
                viewModel.updateFilters(filters.copy(selectedYear = year.toIntOrNull()))
                showYearDialog = false
            }
        )
    }
    if (showRatingDialog) {
        RatingFilterDialog(
            initial = filters.minRating.toFloat(),
            onDismiss = { showRatingDialog = false },
            onApply = { rating ->
                viewModel.updateFilters(filters.copy(minRating = rating.toInt()))
                showRatingDialog = false
            }
        )
    }
    if (showSortDialog) {
        SortFilterDialog(
            initial = filters.sortBy,
            onDismiss = { showSortDialog = false },
            onApply = { sort ->
                viewModel.updateFilters(filters.copy(sortBy = sort))
                showSortDialog = false
            }
        )
    }
}

@Composable
private fun SuggestionItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FilterChipsRow(
    filters: SearchFilters,
    onGenreClick: () -> Unit,
    onStudioClick: () -> Unit,
    onYearClick: () -> Unit,
    onRatingClick: () -> Unit,
    onSortClick: () -> Unit,
    onClearAll: () -> Unit
) {
    val hasActiveFilters = filters.selectedGenre != null || filters.selectedStudio != null ||
        filters.selectedYear != null || filters.minRating > 0 || filters.sortBy != "alphabetical"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Filters",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp
            )
            if (hasActiveFilters) {
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "Clear all",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { onClearAll() }
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChipButton(
                    label = filters.selectedGenre ?: "Genre",
                    isSelected = filters.selectedGenre != null,
                    onClick = onGenreClick
                )
            }
            item {
                FilterChipButton(
                    label = filters.selectedStudio ?: "Studio",
                    isSelected = filters.selectedStudio != null,
                    onClick = onStudioClick
                )
            }
            item {
                FilterChipButton(
                    label = filters.selectedYear?.toString() ?: "Year",
                    isSelected = filters.selectedYear != null,
                    onClick = onYearClick
                )
            }
            item {
                FilterChipButton(
                    label = if (filters.minRating > 0) "${filters.minRating}+" else "Rating",
                    isSelected = filters.minRating > 0,
                    onClick = onRatingClick
                )
            }
            item {
                val sortLabel = when (filters.sortBy) {
                    "countdown" -> "Countdown"
                    "progress" -> "Progress"
                    "last_updated" -> "Updated"
                    else -> "Sort"
                }
                FilterChipButton(
                    label = sortLabel,
                    isSelected = filters.sortBy != "alphabetical",
                    onClick = onSortClick
                )
            }
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(CornerRadius.xl))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = if (isSelected) "\u2715" else "+",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun GenreFilterDialog(
    genres: List<String>,
    selected: String?,
    onDismiss: () -> Unit,
    onApply: (String?) -> Unit
) {
    var localSelected by remember { mutableStateOf(selected) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(CornerRadius.lg), tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text("Filter by Genre", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(Spacing.md))

                if (genres.isEmpty()) {
                    Text(
                        "No genres found",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        genres.forEach { genre ->
                            val isGenreSelected = localSelected == genre
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(CornerRadius.md))
                                    .background(
                                        if (isGenreSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        localSelected = if (isGenreSelected) null else genre
                                    }
                                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = genre,
                                    fontWeight = if (isGenreSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isGenreSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (isGenreSelected) {
                                    Text(
                                        "\u2713",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { onApply(localSelected) }) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun StudioFilterDialog(
    initial: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(CornerRadius.lg), tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text("Filter by Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Studio name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { onApply(text) }) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun YearFilterDialog(
    initial: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(CornerRadius.lg), tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text("Filter by Year", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Release year (e.g. 2024)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { onApply(text) }) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun RatingFilterDialog(
    initial: Float,
    onDismiss: () -> Unit,
    onApply: (Float) -> Unit
) {
    var sliderValue by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(CornerRadius.lg), tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text("Minimum Rating", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = if (sliderValue > 0) "\u2265 ${sliderValue.toInt()}" else "No minimum",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.md))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("100", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Spacer(Modifier.height(Spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { onApply(sliderValue) }) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun SortFilterDialog(
    initial: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var selected by remember { mutableStateOf(initial) }
    val options = listOf(
        "alphabetical" to "Alphabetical",
        "countdown" to "Countdown (next airing)",
        "progress" to "Progress (most episodes)",
        "last_updated" to "Last Updated"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(CornerRadius.lg), tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text("Sort Order", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(Spacing.md))
                options.forEach { (value, label) ->
                    val isSelected = selected == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CornerRadius.md))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable { selected = value }
                            .padding(horizontal = Spacing.md, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Text(
                                "\u2713",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { onApply(selected) }) { Text("Apply") }
                }
            }
        }
    }
}