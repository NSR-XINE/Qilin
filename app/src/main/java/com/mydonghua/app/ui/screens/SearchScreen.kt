package com.mydonghua.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mydonghua.app.ui.Spacing
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import com.mydonghua.app.ui.common.ShowCard
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.ColorOSMotion
import com.mydonghua.app.ui.theme.ColorOSTransitions
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.colorOSPressBounce
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.mydonghua.app.ui.LocalHeaderBackdrop
import com.mydonghua.app.ui.LocalLayerBackdrop
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WifiOff
import com.mydonghua.app.ui.common.ColorOSDialog
import com.mydonghua.app.ui.common.ColorOSPillButton
import com.mydonghua.app.ui.common.ColorOSOutlinedButton
import com.mydonghua.app.ui.common.ColorOSTextButton
import com.mydonghua.app.ui.theme.JustSansFamily
import com.mydonghua.app.ui.common.EmptyState
import com.mydonghua.app.ui.common.HighlightedText
import com.mydonghua.app.ui.common.ScrollPerfMonitor
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
    val searchError by viewModel.searchError.collectAsState()
    val autoOpenKeyboard by viewModel.autoOpenKeyboard.collectAsState()
    var showToAdd by remember { mutableStateOf<Show?>(null) }
    val activeFilters by viewModel.activeFilters.collectAsState()
    val distinctGenres by viewModel.distinctGenres.collectAsState()
    var showGenreDialog by remember { mutableStateOf(false) }
    var showStudioDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val composeView = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isKeyboardVisible = WindowInsets.isImeVisible

    var isSearchActive by remember { mutableStateOf(false) }

    val hasActiveFilters = activeFilters.selectedGenre != null ||
        activeFilters.selectedStudio != null ||
        activeFilters.selectedYear != null ||
        activeFilters.minRating > 0

    val resultsListState = rememberLazyListState()
    ScrollPerfMonitor(resultsListState, "search")

    fun showKeyboardNow() {
        val window = (context as? Activity)?.window ?: return
        WindowCompat.getInsetsController(window, composeView)
            .show(WindowInsetsCompat.Type.ime())
    }

    fun isImeVisibleNow(): Boolean {
        val insets = composeView.rootWindowInsets ?: return false
        return WindowInsetsCompat.toWindowInsetsCompat(insets, composeView)
            .getInsets(WindowInsetsCompat.Type.ime()).bottom > 0
    }

    fun focusAndOpenKeyboard() {
        if (isSearchActive) return
        focusRequester.requestFocus()
        scope.launch {
            repeat(4) {
                if (isImeVisibleNow()) {
                    return@launch
                }
                showKeyboardNow()
                delay(150)
            }
        }
    }

    val voiceSearchLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.selectQuery(spokenText)
            }
        }
    }

    fun startVoiceSearch() {
        if (viewModel.startVoiceSearch()) return
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Search shows...")
        }
        try {
            voiceSearchLauncher.launch(intent)
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Voice search is not supported on this device")
            }
        }
    }

    LaunchedEffect(autoOpenKeyboard) {
        if (autoOpenKeyboard) {
            delay(400)
            focusAndOpenKeyboard()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearQuery()
            viewModel.updateFilters(SearchFilters())
        }
    }

    BackHandler(enabled = true) {
        if (isKeyboardVisible) {
            keyboardController?.hide()
            focusManager.clearFocus()
        } else {
            onNavigateBack()
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    val isLight = !isDark
    val searchPillBg = if (isLight) Color.White.copy(alpha = 0.70f) else Color(0xFF1E222D).copy(alpha = 0.65f)
    val searchPillShape = RoundedCornerShape(CornerRadius.surface)
    val headerBackdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerTopPadding = statusBarTop + 80.dp

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Scrollable Suggestions / Results List
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(headerBackdrop)
                .imePadding()
        ) {
            if (query.isEmpty() && !hasActiveFilters) {
                val suggestionsList = suggestions
                if (suggestionsList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                        contentPadding = PaddingValues(top = headerTopPadding, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        val recents = suggestionsList.filterIsInstance<SearchSuggestion.Recent>()
                        val populars = suggestionsList.filterIsInstance<SearchSuggestion.Popular>()

                        if (recents.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SearchSectionTitle("Recent Searches", MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "Clear all",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                                    )
                                }
                            }
                            items(recents) { item ->
                                SuggestionRow(
                                    text = item.text,
                                    isRecent = true,
                                    onClick = { viewModel.selectQuery(item.text) },
                                    onDelete = { viewModel.removeRecentSearch(item.text) }
                                )
                            }
                        }

                        if (populars.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs)) {
                                    SearchSectionTitle("Popular Searches", MaterialTheme.colorScheme.secondary)
                                }
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

                if (searchError && query.trim().isNotEmpty() && !isSearchingOnline && results.isEmpty() && onlineResults.isEmpty()) {
                    EmptyState(
                        title = "Search failed",
                        subtitle = "We couldn't reach the search service. Check your connection and try again.",
                        icon = Icons.Rounded.WifiOff,
                        actionLabel = "Retry",
                        onAction = { viewModel.retryOnlineSearch() },
                        modifier = Modifier.padding(top = headerTopPadding)
                    )
                } else if (results.isEmpty() && onlineResults.isEmpty() && !isSearchingOnline && showMatches.isEmpty()) {
                    EmptyState(
                        title = "No matching shows found",
                        subtitle = "Try searching for another title or browse all shows",
                        icon = Icons.Default.Search,
                        actionLabel = "Browse All",
                        onAction = onNavigateBack,
                        modifier = Modifier.padding(top = headerTopPadding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                        state = resultsListState,
                        contentPadding = PaddingValues(top = headerTopPadding, bottom = 80.dp)
                    ) {
                        item {
                            FilterChipsRow(
                                filters = activeFilters,
                                onGenreClick = { showGenreDialog = true },
                                onStudioClick = { showStudioDialog = true },
                                onYearClick = { showYearDialog = true },
                                onRatingClick = { showRatingDialog = true },
                                onSortClick = { showSortDialog = true },
                                onClearAll = { viewModel.updateFilters(SearchFilters()) }
                            )
                        }

                        if (isSearchActive && showMatches.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs)) {
                                    SearchSectionTitle("Suggestions", MaterialTheme.colorScheme.primary)
                                }
                            }
                            items(showMatches) { match ->
                                SuggestionMatchRow(
                                    match = match,
                                    query = query,
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
                                Box(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                                    SearchSectionTitle("My Library", MaterialTheme.colorScheme.primary)
                                }
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
                                    onCardClick = onShowClickById,
                                    highlightQuery = query
                                )
                            }
                        }

                        if (searchError) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Text(
                                        text = "Couldn't load online results.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ColorOSTextButton(
                                        text = "Retry",
                                        onClick = { viewModel.retryOnlineSearch() }
                                    )
                                }
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
                                Box(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                                    SearchSectionTitle("Online Results (AniList / MyAnimeList)", MaterialTheme.colorScheme.secondary)
                                }
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

        // 2. Floating Frosted Glass Header with Search Bar Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // Frosted Canopy (from pill top up to status bar)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Transparent
                                )
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .drawBackdrop(
                        backdrop = headerBackdrop,
                        shape = { RectangleShape },
                        effects = {
                            blur(24.dp.toPx())
                            vibrancy()
                            colorControls(saturation = 1.35f)
                        },
                        highlight = null,
                        shadow = null,
                    )
                    .background(
                        if (isDark) Color(0xFF0F121C).copy(alpha = 0.55f)
                        else Color.White.copy(alpha = 0.60f)
                    )
            )

            // Borderless Search Bar Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = Spacing.lg, end = Spacing.lg, top = 8.dp, bottom = 8.dp)
                    .height(56.dp)
                    .clip(searchPillShape)
                    .drawBackdrop(
                        backdrop = headerBackdrop,
                        shape = { searchPillShape },
                        effects = {
                            blur(24.dp.toPx())
                            vibrancy()
                            colorControls(saturation = 1.35f)
                        },
                        highlight = null,
                        shadow = null,
                    )
                    .background(searchPillBg)
                    .padding(horizontal = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading icon (back / exit search)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .colorOSPressBounce(pressedScale = 0.88f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onNavigateBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSize.md)
                    )
                }

                // Text input field
                BasicTextField(
                    value = query,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isSearchActive = focusState.isFocused
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.selectQuery(query)
                            keyboardController?.hide()
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search shows...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Trailing icon: voice search placeholder when idle, clear when there's text
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .colorOSPressBounce(pressedScale = 0.88f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { viewModel.clearQuery() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .colorOSPressBounce(pressedScale = 0.88f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { startVoiceSearch() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Voice search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                }
            }
        }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .imePadding()
            .navigationBarsPadding()
            .padding(bottom = 80.dp)
    )
    }

    if (showToAdd != null) {
        ColorOSDialog(
            onDismissRequest = { showToAdd = null },
            title = "Add to Library",
            message = "Would you like to add \"${showToAdd?.title}\" to your library?",
            confirmText = "Add",
            dismissText = "Cancel",
            onConfirm = {
                val s = showToAdd!!
                val newShow = s.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    dateAdded = System.currentTimeMillis()
                )
                showsViewModel.insertOrUpdateShow(newShow)
                showToAdd = null
                onShowClick(newShow.id)
            },
            onDismiss = { showToAdd = null }
        )
    }

    if (showGenreDialog) {
        GenreFilterDialog(
            genres = distinctGenres,
            selected = activeFilters.selectedGenre,
            onDismiss = { showGenreDialog = false },
            onApply = { genre ->
                viewModel.updateFilters(activeFilters.copy(selectedGenre = genre))
            }
        )
    }

    if (showStudioDialog) {
        StudioFilterDialog(
            initial = activeFilters.selectedStudio ?: "",
            onDismiss = { showStudioDialog = false },
            onApply = { studio ->
                viewModel.updateFilters(activeFilters.copy(selectedStudio = studio.trim().ifBlank { null }))
            }
        )
    }

    if (showYearDialog) {
        YearFilterDialog(
            initial = activeFilters.selectedYear?.toString() ?: "",
            onDismiss = { showYearDialog = false },
            onApply = { year ->
                viewModel.updateFilters(activeFilters.copy(selectedYear = year.toIntOrNull()))
            }
        )
    }

    if (showRatingDialog) {
        RatingFilterDialog(
            initial = activeFilters.minRating.toFloat(),
            onDismiss = { showRatingDialog = false },
            onApply = { rating ->
                viewModel.updateFilters(activeFilters.copy(minRating = rating.toInt()))
            }
        )
    }

    if (showSortDialog) {
        SortFilterDialog(
            initial = activeFilters.sortBy,
            onDismiss = { showSortDialog = false },
            onApply = { sort ->
                viewModel.updateFilters(activeFilters.copy(sortBy = sort))
            }
        )
    }


}

@Composable
private fun SuggestionRow(text: String, isRecent: Boolean, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            imageVector = if (isRecent) Icons.Rounded.History else Icons.AutoMirrored.Rounded.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onDelete != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDelete
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Delete recent search",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun SuggestionMatchRow(
    match: SearchSuggestion.ShowMatch,
    query: String,
    onClick: () -> Unit
) {
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
        Column(modifier = Modifier.weight(1f)) {
            HighlightedText(
                text = match.displayText,
                query = query,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!match.subtitle.isNullOrEmpty()) {
                Text(
                    text = match.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
                    fontWeight = FontWeight.SemiBold,
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
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bg = if (isSelected) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF0082FB),
                Color(0xFF1990FF)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                if (isLight) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.10f),
                if (isLight) Color(0xFFF1F5F9).copy(alpha = 0.80f) else Color.White.copy(alpha = 0.05f)
            )
        )
    }
    val borderBrush = if (isSelected) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF60B5FF).copy(alpha = 0.60f),
                Color(0xFF0082FB).copy(alpha = 0.20f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (isLight) 0.35f else 0.20f),
                Color.White.copy(alpha = if (isLight) 0.12f else 0.04f)
            )
        )
    }
    val textColor = if (isSelected) Color.White else (if (isLight) Color(0xFF1E293B) else Color(0xFFE2E8F0))

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(CornerRadius.pill))
            .background(bg, RoundedCornerShape(CornerRadius.pill))
            .border(width = 0.85.dp, brush = borderBrush, shape = RoundedCornerShape(CornerRadius.pill))
            .colorOSPressBounce(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontFamily = JustSansFamily,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor
            )
            Text(
                text = if (isSelected) "\u2715" else "+",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = if (isSelected) 0.9f else 0.6f)
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
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    AnimatedDialog(onDismissRequest = onDismiss) { dismissWithAction ->
        Column {
            Text(
                text = "Filter by Genre",
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (isLight) Color(0xFF0F172A) else Color.White
            )
            Spacer(Modifier.height(Spacing.md))

            if (genres.isEmpty()) {
                Text(
                    "No genres found",
                    fontFamily = JustSansFamily,
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
                                .clip(RoundedCornerShape(CornerRadius.pill))
                                .background(
                                    if (isGenreSelected) Color(0xFF0082FB).copy(alpha = 0.15f)
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
                                fontFamily = JustSansFamily,
                                fontWeight = if (isGenreSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isGenreSelected) Color(0xFF0082FB)
                                else (if (isLight) Color(0xFF0F172A) else Color.White)
                            )
                            if (isGenreSelected) {
                                Text(
                                    "\u2713",
                                    color = Color(0xFF0082FB),
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorOSOutlinedButton(
                    text = "Cancel",
                    onClick = { dismissWithAction(onDismiss) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                ColorOSPillButton(
                    text = "Apply",
                    onClick = { dismissWithAction { onApply(localSelected) } },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
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
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    AnimatedDialog(onDismissRequest = onDismiss) { dismissWithAction ->
        Column {
            Text(
                text = "Filter by Studio",
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (isLight) Color(0xFF0F172A) else Color.White
            )
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Studio name", fontFamily = JustSansFamily) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0082FB),
                    focusedLabelColor = Color(0xFF0082FB)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorOSOutlinedButton(
                    text = "Cancel",
                    onClick = { dismissWithAction(onDismiss) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                ColorOSPillButton(
                    text = "Apply",
                    onClick = { dismissWithAction { onApply(text) } },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
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
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    AnimatedDialog(onDismissRequest = onDismiss) { dismissWithAction ->
        Column {
            Text(
                text = "Filter by Year",
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (isLight) Color(0xFF0F172A) else Color.White
            )
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("Release year (e.g. 2024)", fontFamily = JustSansFamily) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0082FB),
                    focusedLabelColor = Color(0xFF0082FB)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorOSOutlinedButton(
                    text = "Cancel",
                    onClick = { dismissWithAction(onDismiss) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                ColorOSPillButton(
                    text = "Apply",
                    onClick = { dismissWithAction { onApply(text) } },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
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
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    AnimatedDialog(onDismissRequest = onDismiss) { dismissWithAction ->
        Column {
            Text(
                text = "Minimum Rating",
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (isLight) Color(0xFF0F172A) else Color.White
            )
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = if (sliderValue > 0) "\u2265 ${sliderValue.toInt()}" else "No minimum",
                fontSize = 24.sp,
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0082FB),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.md))
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 0f..100f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF0082FB),
                    activeTrackColor = Color(0xFF0082FB)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0", fontSize = 12.sp, fontFamily = JustSansFamily, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("100", fontSize = 12.sp, fontFamily = JustSansFamily, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(Spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorOSOutlinedButton(
                    text = "Cancel",
                    onClick = { dismissWithAction(onDismiss) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                ColorOSPillButton(
                    text = "Apply",
                    onClick = { dismissWithAction { onApply(sliderValue) } },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
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
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val options = listOf(
        "alphabetical" to "Alphabetical",
        "countdown" to "Countdown (next airing)",
        "progress" to "Progress (most episodes)",
        "last_updated" to "Last Updated"
    )

    AnimatedDialog(onDismissRequest = onDismiss) { dismissWithAction ->
        Column {
            Text(
                text = "Sort Order",
                fontFamily = JustSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (isLight) Color(0xFF0F172A) else Color.White
            )
            Spacer(Modifier.height(Spacing.md))
            options.forEach { (value, label) ->
                val isSelected = selected == value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.pill))
                        .background(
                            if (isSelected) Color(0xFF0082FB).copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable { selected = value }
                        .padding(horizontal = Spacing.md, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontFamily = JustSansFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF0082FB)
                        else (if (isLight) Color(0xFF0F172A) else Color.White)
                    )
                    if (isSelected) {
                        Text(
                            "\u2713",
                            color = Color(0xFF0082FB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorOSOutlinedButton(
                    text = "Cancel",
                    onClick = { dismissWithAction(onDismiss) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                ColorOSPillButton(
                    text = "Apply",
                    onClick = { dismissWithAction { onApply(selected) } },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
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
            delay(ColorOSMotion.DurationQuick.toLong()) // matches exit animation duration
            action()
        }
    }

    Dialog(
        onDismissRequest = { dismissWithAction(onDismissRequest) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = animateTrigger,
            enter = ColorOSTransitions.enterDialog,
            exit = ColorOSTransitions.exitDialog
        ) {
            val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
            val dialogShape = RoundedCornerShape(30.dp)
            val dialogBg = if (isLight) {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.98f),
                        Color(0xFFF1F5F9).copy(alpha = 0.94f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF161A2B).copy(alpha = 0.96f),
                        Color(0xFF0F121E).copy(alpha = 0.98f)
                    )
                )
            }
            val dialogBorder = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isLight) 0.35f else 0.25f),
                    Color.White.copy(alpha = if (isLight) 0.10f else 0.05f)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(dialogShape)
                    .background(brush = dialogBg, shape = dialogShape)
                    .border(width = 1.dp, brush = dialogBorder, shape = dialogShape)
                    .padding(24.dp)
            ) {
                content(dismissWithAction)
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(title: String, color: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(13.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}