package com.mydonghua.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mydonghua.app.R
import com.mydonghua.app.ui.Spacing
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.ArrowBack
import com.mydonghua.app.ui.common.ShowCard
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.TrendingUp
import com.mydonghua.app.ui.common.EmptyState
import com.mydonghua.app.ui.screens.SearchSuggestion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val onlineResults by viewModel.onlineResults.collectAsState()
    val isSearchingOnline by viewModel.isSearchingOnline.collectAsState()
    var showToAdd by remember { mutableStateOf<Show?>(null) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val autoOpenKeyboard by viewModel.autoOpenKeyboard.collectAsState()

    LaunchedEffect(autoOpenKeyboard) {
        if (autoOpenKeyboard) {
            delay(200)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    var isSearchActive by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearQuery()
            viewModel.updateFilters(SearchFilters())
        }
    }

    val isKeyboardVisible = WindowInsets.isImeVisible

    BackHandler(enabled = true) {
        if (isKeyboardVisible) {
            keyboardController?.hide()
        } else {
            onNavigateBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // 1. Search Bar Input Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(top = Spacing.xs)
                .clip(RoundedCornerShape(CornerRadius.lg))
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(CornerRadius.lg)
                )
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(CornerRadius.lg))
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.selectQuery(query)
                        keyboardController?.hide()
                    }
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                singleLine = true,
                shape = RoundedCornerShape(CornerRadius.lg),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isSearchActive = focusState.isFocused
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                placeholder = { Text("Search shows...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isSearchActive) {
                                    keyboardController?.hide()
                                } else {
                                    onNavigateBack()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.ArrowBack else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "Back" else "Search",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { viewModel.clearQuery() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("\u2715", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        // 2. Redesigned Suggestions / Results List
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (query.isEmpty()) {
                val suggestionsList = suggestions
                if (suggestionsList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        val recents = suggestionsList.filterIsInstance<SearchSuggestion.Recent>()
                        val populars = suggestionsList.filterIsInstance<SearchSuggestion.Popular>()

                        if (recents.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Recent Searches",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs)
                                )
                            }
                            items(recents) { item ->
                                SuggestionRow(text = item.text, isRecent = true, onClick = { viewModel.selectQuery(item.text) })
                            }
                        }

                        if (populars.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Popular Searches",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs)
                                )
                            }
                            items(populars) { item ->
                                SuggestionRow(text = item.text, isRecent = false, onClick = { viewModel.selectQuery(item.text) })
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
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
                                text = "Search your shows",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                val showMatches = suggestions.filterIsInstance<SearchSuggestion.ShowMatch>()
                val onShowClickById = remember { { s: Show -> onShowClick(s.id) } }
                val onDelete = remember { { s: Show -> showsViewModel.deleteShow(s.id) } }
                val onFavorite = remember { { s: Show -> showsViewModel.toggleFavorite(s) } }
                val onNotify = remember { { s: Show -> showsViewModel.toggleNotification(context, s) } }
                val onIncrement = remember { { s: Show -> showsViewModel.incrementEpisode(s) } }
                val onDecrement = remember { { s: Show -> showsViewModel.decrementEpisode(s) } }

                if (results.isEmpty() && onlineResults.isEmpty() && !isSearchingOnline) {
                    EmptyState(
                        title = "No matching shows found",
                        subtitle = "Try searching for another title or browse all shows",
                        icon = Icons.Default.Search,
                        actionLabel = "Browse All",
                        onAction = onNavigateBack
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (isSearchActive && showMatches.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Suggestions",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs)
                                )
                            }
                            items(showMatches) { match ->
                                SuggestionMatchRow(
                                    match = match,
                                    onClick = {
                                        viewModel.selectQuery(match.displayText)
                                        keyboardController?.hide()
                                        onShowClick(match.show.id)
                                    }
                                )
                            }
                            item {
                                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm, horizontal = Spacing.lg))
                            }
                        }

                        if (results.isNotEmpty()) {
                            item {
                                Text(
                                    text = "My Library",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                                )
                            }
                            items(results, key = { it.id }) { show ->
                                ShowCard(
                                    show = show,
                                    onWatchClick = onWatchClick,
                                    onEditClick = onShowClickById,
                                    onDeleteClick = onDelete,
                                    onFavoriteToggle = onFavorite,
                                    onNotifyToggle = onNotify,
                                    onEpIncrement = onIncrement,
                                    onEpDecrement = onDecrement,
                                    onCardClick = onShowClickById
                                )
                            }
                        }

                        if (isSearchingOnline) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = Spacing.md),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else if (onlineResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Online Results (AniList / MyAnimeList)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                                )
                            }
                            items(onlineResults, key = { it.id }) { show ->
                                ShowCard(
                                    show = show,
                                    onWatchClick = { showToAdd = show },
                                    onEditClick = { showToAdd = show },
                                    onDeleteClick = {},
                                    onFavoriteToggle = {},
                                    onNotifyToggle = {},
                                    onEpIncrement = {},
                                    onEpDecrement = {},
                                    onCardClick = { showToAdd = show }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showToAdd != null) {
        AlertDialog(
            onDismissRequest = { showToAdd = null },
            title = { Text("Add to Library") },
            text = { Text("Would you like to add \"${showToAdd?.title}\" to your library?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = showToAdd!!
                        val newShow = s.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            dateAdded = System.currentTimeMillis()
                        )
                        showsViewModel.insertOrUpdateShow(newShow)
                        showToAdd = null
                        onShowClick(newShow.id)
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showToAdd = null }) {
                    Text("Cancel")
                }
            }
        )
    }


}

@Composable
private fun SuggestionRow(text: String, isRecent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            imageVector = if (isRecent) Icons.Rounded.History else Icons.Rounded.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SuggestionMatchRow(match: SearchSuggestion.ShowMatch, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = match.displayText,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
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

    AnimatedDialog(onDismissRequest = onDismiss) { dismissWithAction ->
        Surface(shape = RoundedCornerShape(CornerRadius.dialog), tonalElevation = 6.dp) {
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
                    TextButton(onClick = { dismissWithAction(onDismiss) }) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { dismissWithAction { onApply(localSelected) } }) { Text("Apply") }
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

    AnimatedDialog(onDismissRequest = onDismiss) { dismissWithAction ->
        Surface(shape = RoundedCornerShape(CornerRadius.dialog), tonalElevation = 6.dp) {
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
                    TextButton(onClick = { dismissWithAction(onDismiss) }) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { dismissWithAction { onApply(text) } }) { Text("Apply") }
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

    AnimatedDialog(onDismissRequest = onDismiss) { dismissWithAction ->
        Surface(shape = RoundedCornerShape(CornerRadius.dialog), tonalElevation = 6.dp) {
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
                    TextButton(onClick = { dismissWithAction(onDismiss) }) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { dismissWithAction { onApply(text) } }) { Text("Apply") }
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

    AnimatedDialog(onDismissRequest = onDismiss) { dismissWithAction ->
        Surface(shape = RoundedCornerShape(CornerRadius.dialog), tonalElevation = 6.dp) {
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
                    TextButton(onClick = { dismissWithAction(onDismiss) }) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { dismissWithAction { onApply(sliderValue) } }) { Text("Apply") }
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

    AnimatedDialog(onDismissRequest = onDismiss) { dismissWithAction ->
        Surface(shape = RoundedCornerShape(CornerRadius.dialog), tonalElevation = 6.dp) {
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
                    TextButton(onClick = { dismissWithAction(onDismiss) }) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { dismissWithAction { onApply(selected) } }) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun AnimatedDialog(
    onDismissRequest: () -> Unit,
    content: @Composable (dismissWithAction: (action: () -> Unit) -> Unit) -> Unit
) {
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }

    val coroutineScope = rememberCoroutineScope()
    val dismissWithAction: (action: () -> Unit) -> Unit = { action ->
        animateTrigger = false
        coroutineScope.launch {
            delay(150) // matches exit animation duration
            action()
        }
    }

    Dialog(
        onDismissRequest = { dismissWithAction(onDismissRequest) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = animateTrigger,
            enter = scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(150)),
            exit = scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(150)
            ) + fadeOut(animationSpec = tween(150))
        ) {
            content(dismissWithAction)
        }
    }
}