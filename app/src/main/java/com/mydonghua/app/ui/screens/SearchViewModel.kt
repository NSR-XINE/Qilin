package com.mydonghua.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.domain.repository.SettingsKeys
import com.mydonghua.app.domain.repository.SettingsRepository
import com.mydonghua.app.domain.repository.ShowRepository
import com.mydonghua.app.domain.voice.VoiceSearchController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class SearchFilters(
    val selectedGenre: String? = null,
    val selectedStudio: String? = null,
    val selectedYear: Int? = null,
    val minRating: Int = 0,
    val maxEpisodes: Int = 0,
    val sortBy: String = "alphabetical"
)

sealed class SearchSuggestion {
    data class Popular(val text: String) : SearchSuggestion()
    data class Recent(val text: String) : SearchSuggestion()
    data class ShowMatch(val show: Show, val displayText: String, val score: Int, val subtitle: String? = null) : SearchSuggestion()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val settingsRepository: SettingsRepository,
    private val voiceSearchController: VoiceSearchController
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val activeFilters = MutableStateFlow(SearchFilters())
    val autoOpenKeyboard: StateFlow<Boolean> = settingsRepository.getBoolFlow(SettingsKeys.AUTO_OPEN_KEYBOARD, true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val allShows = showRepository.getAllShows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctGenres: StateFlow<List<String>> = allShows
        .map { shows ->
            shows.flatMap { it.genres?.split(",")?.map { g -> g.trim() } ?: emptyList() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestions: StateFlow<List<SearchSuggestion>> = combine(
        searchQuery,
        allShows,
        settingsRepository.getSettingFlow(SettingsKeys.RECENT_SEARCHES, "[]"),
        settingsRepository.getSettingFlow(SettingsKeys.SEARCH_FREQUENCY, "{}")
    ) { query, shows, recentJson, freqJson ->
        val q = query.trim()
        if (q.isEmpty()) {
            val popular = getPopularSearches(freqJson, 3)
            val recent = getRecentSearches(recentJson)
            val list = mutableListOf<SearchSuggestion>()
            popular.forEach { list.add(SearchSuggestion.Popular(it)) }
            recent.forEach { r ->
                if (!popular.contains(r)) {
                    list.add(SearchSuggestion.Recent(r))
                }
            }
            list
        } else {
            val qLower = q.lowercase()
            val scored = mutableListOf<SearchSuggestion.ShowMatch>()
            shows.forEach { show ->
                val score = scoreShow(show, q)
                if (score > 0) {
                    val titleLower = (show.title ?: "").lowercase()
                    val titleZhLower = (show.titleZh ?: "").lowercase()
                    val displayText = when {
                        titleLower.contains(qLower) -> show.title
                        titleZhLower.contains(qLower) || fuzzyMatch(show.titleZh, q) -> show.titleZh ?: show.title
                        else -> show.title
                    }
                    scored.add(SearchSuggestion.ShowMatch(show, displayText, score, matchReason(show, q)))
                }
            }
            scored.sortByDescending { it.score }
            scored.take(8)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _onlineResults = MutableStateFlow<List<Show>>(emptyList())
    val onlineResults: StateFlow<List<Show>> = _onlineResults.asStateFlow()

    private val _isSearchingOnline = MutableStateFlow(false)
    val isSearchingOnline: StateFlow<Boolean> = _isSearchingOnline.asStateFlow()

    private val _searchError = MutableStateFlow(false)
    val searchError: StateFlow<Boolean> = _searchError.asStateFlow()

    private var voiceJob: Job? = null

    val isVoiceSearchAvailable: Boolean
        get() = voiceSearchController.isAvailable

    /**
     * Starts voice input if the [VoiceSearchController] supports it. Returns
     * false when unavailable so the caller can surface a placeholder hint.
     */
    fun startVoiceSearch(): Boolean {
        if (!voiceSearchController.isAvailable) return false
        voiceJob?.cancel()
        voiceJob = viewModelScope.launch {
            voiceSearchController.startListening().collect { partial ->
                searchQuery.value = partial
            }
        }
        return true
    }

    fun stopVoiceSearch() {
        voiceJob?.cancel()
        voiceJob = null
        voiceSearchController.stopListening()
    }

    /**
     * Runs the online (AniList / MyAnimeList) search for [q] and updates
     * [onlineResults] / [searchError]. Shared by the debounced flow and retry.
     */
    private suspend fun performOnlineSearch(q: String): List<Show> {
        if (q.trim().length < 2) return emptyList()
        _searchError.value = false
        _isSearchingOnline.value = true
        try {
            val aniListResults = com.mydonghua.app.data.service.PosterFetchService.searchAniList(q)
            val malResults = com.mydonghua.app.data.service.PosterFetchService.searchMyAnimeList(q)

            val combined = mutableListOf<Show>()
            val addedTitles = mutableSetOf<String>()
            val localTitles = allShows.value.map { it.title.lowercase().trim() }

            fun addResult(title: String, titleZh: String?, status: String?, totalEp: Int, poster: String?, synopsis: String?, source: String) {
                val key = title.lowercase().trim()
                if (localTitles.contains(key) || addedTitles.contains(key)) return
                if (titleZh != null && localTitles.contains(titleZh.lowercase().trim())) return

                addedTitles.add(key)
                combined.add(
                    Show(
                        id = "online_${source}_${title.hashCode()}",
                        title = title,
                        titleZh = titleZh,
                        status = status ?: "ongoing",
                        totalEp = totalEp,
                        poster = poster,
                        notes = synopsis,
                        genres = null,
                        studio = null,
                        releaseYear = 0
                    )
                )
            }

            aniListResults.forEach { r ->
                addResult(r.title, r.titleZh, r.status, r.totalEp, r.poster, r.synopsis, "anilist")
            }
            malResults.forEach { r ->
                addResult(r.title, r.titleZh, r.status, r.totalEp, r.poster, r.synopsis, "mal")
            }

            return combined
        } catch (e: Exception) {
            _searchError.value = true
            return emptyList()
        } finally {
            _isSearchingOnline.value = false
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val onlineSearchJob = searchQuery
        .onEach { q ->
            if (q.trim().length >= 2) {
                _isSearchingOnline.value = true
                _searchError.value = false
            } else {
                _isSearchingOnline.value = false
                _onlineResults.value = emptyList()
            }
        }
        .debounce(300)
        .flatMapLatest { q ->
            flow { emit(performOnlineSearch(q)) }
        }
        .onEach { _onlineResults.value = it }
        .launchIn(viewModelScope)

    fun retryOnlineSearch() {
        viewModelScope.launch {
            val results = performOnlineSearch(searchQuery.value)
            _onlineResults.value = results
        }
    }

    val filteredShows: StateFlow<List<Show>> = combine(
        searchQuery,
        activeFilters,
        allShows
    ) { query, filters, shows ->
        val hasActiveFilters = filters.selectedGenre != null ||
                filters.selectedStudio != null ||
                filters.selectedYear != null ||
                filters.minRating > 0

        if (query.trim().isEmpty() && !hasActiveFilters) {
            return@combine emptyList()
        }

        var list = shows

        if (query.trim().isNotEmpty()) {
            list = list.filter { show ->
                scoreShow(show, query) > 0
            }
        }

        filters.selectedGenre?.let { genre ->
            list = list.filter { show ->
                show.genres?.split(",")?.map { it.trim().lowercase() }?.contains(genre.lowercase()) == true
            }
        }

        filters.selectedStudio?.let { studio ->
            list = list.filter { show ->
                show.studio?.equals(studio, ignoreCase = true) == true
            }
        }

        filters.selectedYear?.let { year ->
            list = list.filter { show ->
                show.releaseYear == year
            }
        }

        if (filters.minRating > 0) {
            list = list.filter { it.rating >= filters.minRating }
        }

        if (filters.maxEpisodes > 0) {
            list = list.filter { it.totalEp <= filters.maxEpisodes }
        }

        list = when (filters.sortBy) {
            "alphabetical" -> list.sortedBy { it.title.lowercase() }
            "progress" -> list.sortedByDescending { it.currentEp }
            "last_updated" -> list.sortedByDescending { it.lastUpdated ?: 0L }
            "countdown" -> list.sortedBy { show ->
                val targetMs = com.mydonghua.app.domain.model.CountdownCalculator.getNextReleaseDateMs(show.releaseDay, show.releaseTime)
                targetMs ?: Long.MAX_VALUE
            }
            else -> list.sortedBy { it.title.lowercase() }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectQuery(query: String) {
        searchQuery.value = query
        addRecentSearch(query)
        incrementSearchFrequency(query)
    }

    fun clearQuery() {
        searchQuery.value = ""
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            val recentJson = settingsRepository.getSetting(SettingsKeys.RECENT_SEARCHES, "[]")
            val list = getRecentSearches(recentJson).toMutableList()
            list.removeIf { it.equals(query, ignoreCase = true) }
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            settingsRepository.putSetting(SettingsKeys.RECENT_SEARCHES, arr.toString())
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            settingsRepository.putSetting(SettingsKeys.RECENT_SEARCHES, "[]")
        }
    }

    fun updateFilters(newFilters: SearchFilters) {
        activeFilters.value = newFilters
    }

    private fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            val recentJson = settingsRepository.getSetting(SettingsKeys.RECENT_SEARCHES, "[]")
            val list = getRecentSearches(recentJson).toMutableList()
            list.removeIf { it.equals(q, ignoreCase = true) }
            list.add(0, q)
            val updated = list.take(10)
            val arr = JSONArray()
            updated.forEach { arr.put(it) }
            settingsRepository.putSetting(SettingsKeys.RECENT_SEARCHES, arr.toString())
        }
    }

    private fun incrementSearchFrequency(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return
        viewModelScope.launch {
            val freqJson = settingsRepository.getSetting(SettingsKeys.SEARCH_FREQUENCY, "{}")
            val obj = JSONObject(freqJson)
            val count = obj.optInt(q, 0)
            obj.put(q, count + 1)
            settingsRepository.putSetting(SettingsKeys.SEARCH_FREQUENCY, obj.toString())
        }
    }

    private fun getRecentSearches(json: String): List<String> {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getPopularSearches(json: String, limit: Int): List<String> {
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getInt(key)
            }
            map.entries.sortedByDescending { it.value }.take(limit).map { it.key }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fuzzyMatch(text: String?, query: String?): Boolean {
        if (text == null || query == null) return false
        val t = text.lowercase().trim()
        val q = query.lowercase().trim()
        if (q.isEmpty()) return true
        if (t.contains(q)) return true
        var qi = 0
        for (i in 0 until t.length) {
            if (qi < q.length && t[i] == q[qi]) {
                qi++
            }
        }
        return qi == q.length
    }

    private fun scoreShow(show: Show, query: String): Int {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return 0

        val title = (show.title ?: "").lowercase().trim()
        val titleZh = (show.titleZh ?: "").lowercase().trim()

        if (title == q) return 100
        if (title.startsWith(q)) return 80
        if (title.contains(q)) return 60
        if (titleZh.contains(q)) return 50

        // Only allow fuzzy (subsequence) match for queries with at least 3 characters
        if (q.length >= 3) {
            if (fuzzyMatch(title, q)) return 40
            if (fuzzyMatch(titleZh, q)) return 30
        }

        // Search across every content type: genres and studio.
        val genres = show.genres?.split(",")
            ?.mapNotNull { it.trim().lowercase().ifEmpty { null } }
            ?: emptyList()
        if (genres.any { it == q }) return 45
        if (genres.any { it.contains(q) }) return 25
        val studio = (show.studio ?: "").lowercase().trim()
        if (studio == q) return 20
        if (studio.isNotEmpty() && studio.contains(q)) return 15

        return 0
    }

    /**
     * Returns a short reason string ("Genre: X" / "Studio: Y") when [query]
     * matched a show via its genres or studio rather than its title, so the UI
     * can explain why the show surfaced. Null when the title matched.
     */
    private fun matchReason(show: Show, query: String): String? {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return null

        val title = (show.title ?: "").lowercase().trim()
        val titleZh = (show.titleZh ?: "").lowercase().trim()
        val titleMatched = title.contains(q) || title.startsWith(q) ||
            (q.length >= 3 && (fuzzyMatch(title, q) || fuzzyMatch(titleZh, q))) ||
            titleZh.contains(q)
        if (titleMatched) return null

        val genres = show.genres?.split(",")
            ?.mapNotNull { it.trim().ifEmpty { null } }
            ?: emptyList()
        genres.firstOrNull { it.lowercase() == q || it.lowercase().contains(q) }?.let {
            return "Genre: $it"
        }

        val studio = show.studio?.trim()?.takeIf { it.isNotEmpty() }
        studio?.takeIf { it.lowercase() == q || it.lowercase().contains(q) }?.let {
            return "Studio: $it"
        }

        return null
    }
}
