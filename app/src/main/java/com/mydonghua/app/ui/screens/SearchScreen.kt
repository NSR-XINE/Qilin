package com.mydonghua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.mydonghua.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.common.ShowCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    showsViewModel: ShowsViewModel,
    onWatchClick: (String) -> Unit,
    onShowClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val results by viewModel.filteredShows.collectAsState()
    val filters by viewModel.activeFilters.collectAsState()
    val context = LocalContext.current

    var showFiltersSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Search & Filters", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showFiltersSheet = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Show Filters",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
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
        ) {
            // Search Input Row
            SearchBar(
                query = query,
                onQueryChange = { viewModel.searchQuery.value = it },
                onSearch = {
                    viewModel.selectQuery(it)
                    isSearchActive = false
                },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                placeholder = { Text("Search show titles, synopsis...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.padding(start = 8.dp).size(20.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearQuery() }) {
                            Text("✕")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Suggestions inside search bar overlay
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(suggestions) { suggestion ->
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Filters Tags
            ActiveFiltersTags(filters = filters, onClearFilters = { viewModel.updateFilters(SearchFilters()) })

            // Results Grid list
            if (results.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching shows found",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 100.dp)
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

        // Filter Sheet Dialog
        if (showFiltersSheet) {
            FilterBottomSheet(
                currentFilters = filters,
                onDismiss = { showFiltersSheet = false },
                onApply = { newFilters ->
                    viewModel.updateFilters(newFilters)
                    showFiltersSheet = false
                }
            )
        }
    }
}

@Composable
private fun SuggestionItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ActiveFiltersTags(filters: SearchFilters, onClearFilters: () -> Unit) {
    val activeList = mutableListOf<String>()
    filters.selectedGenre?.let { activeList.add("Genre: $it") }
    filters.selectedStudio?.let { activeList.add("Studio: $it") }
    filters.selectedYear?.let { activeList.add("Year: $it") }
    if (filters.minRating > 0) activeList.add("Rating >= ${filters.minRating}")
    if (filters.maxEpisodes > 0) activeList.add("Max Ep: ${filters.maxEpisodes}")
    if (filters.sortBy != "alphabetical") activeList.add("Sort: ${filters.sortBy.uppercase()}")

    if (activeList.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Filters:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(activeList) { tag ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(tag, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(
                text = "Clear ✕",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { onClearFilters() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilters: SearchFilters,
    onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit
) {
    var genre by remember { mutableStateOf(currentFilters.selectedGenre ?: "") }
    var studio by remember { mutableStateOf(currentFilters.selectedStudio ?: "") }
    var year by remember { mutableStateOf(currentFilters.selectedYear?.toString() ?: "") }
    var minRating by remember { mutableStateOf(currentFilters.minRating.toFloat()) }
    var maxEpisodes by remember { mutableStateOf(currentFilters.maxEpisodes.toString()) }
    var sortBy by remember { mutableStateOf(currentFilters.sortBy) }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Advanced Filtering Options", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)

            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("Filter by Genre") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = studio,
                onValueChange = { studio = it },
                label = { Text("Filter by Studio") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Release Year") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = maxEpisodes,
                    onValueChange = { maxEpisodes = it },
                    label = { Text("Max Episodes") },
                    modifier = Modifier.weight(1f)
                )
            }



            // Sorting Selection row
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Sorting Order Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("alphabetical", "countdown", "progress", "last_updated").forEach { mode ->
                        val isSelected = sortBy == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { sortBy = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.substring(0, 4).uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onApply(
                        SearchFilters(
                            selectedGenre = genre.takeIf { it.isNotEmpty() },
                            selectedStudio = studio.takeIf { it.isNotEmpty() },
                            selectedYear = year.toIntOrNull(),
                            minRating = minRating.toInt(),
                            maxEpisodes = maxEpisodes.toIntOrNull() ?: 0,
                            sortBy = sortBy
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply Search Filters", fontWeight = FontWeight.Bold)
            }
        }
    }
}
