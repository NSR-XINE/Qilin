package com.mydonghua.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.domain.repository.SettingsRepository
import com.mydonghua.app.domain.repository.ShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    data class ShowMatch(val show: Show, val displayText: String, val score: Int) : SearchSuggestion()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val activeFilters = MutableStateFlow(SearchFilters())

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
        settingsRepository.getSettingFlow("recent_searches", "[]"),
        settingsRepository.getSettingFlow("search_frequency", "{}")
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
                    scored.add(SearchSuggestion.ShowMatch(show, displayText, score))
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val onlineSearchJob = searchQuery
        .debounce(600)
        .flatMapLatest { q ->
            flow {
                if (q.trim().length < 2) {
                    emit(emptyList())
                    return@flow
                }
                _isSearchingOnline.value = true
                try {
                    val searchResults = com.mydonghua.app.data.service.PosterFetchService.searchAniList(q)
                    val localTitles = allShows.value.map { it.title.lowercase().trim() }
                    val mapped = searchResults.mapNotNull { result ->
                        val isAlreadyAdded = localTitles.contains(result.title.lowercase().trim()) || 
                                             (result.titleZh != null && localTitles.contains(result.titleZh.lowercase().trim()))
                        if (isAlreadyAdded) return@mapNotNull null
                        
                        Show(
                            id = "online_${result.title.hashCode()}",
                            title = result.title,
                            titleZh = result.titleZh,
                            status = result.status ?: "ongoing",
                            totalEp = result.totalEp,
                            poster = result.poster,
                            notes = result.synopsis,
                            genres = null,
                            studio = null,
                            releaseYear = 0
                        )
                    }
                    emit(mapped)
                } catch (e: Exception) {
                    emit(emptyList())
                } finally {
                    _isSearchingOnline.value = false
                }
            }
        }
        .onEach { _onlineResults.value = it }
        .launchIn(viewModelScope)

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

    fun updateFilters(newFilters: SearchFilters) {
        activeFilters.value = newFilters
    }

    private fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            val recentJson = settingsRepository.getSetting("recent_searches", "[]")
            val list = getRecentSearches(recentJson).toMutableList()
            list.removeIf { it.equals(q, ignoreCase = true) }
            list.add(0, q)
            val updated = list.take(10)
            val arr = JSONArray()
            updated.forEach { arr.put(it) }
            settingsRepository.putSetting("recent_searches", arr.toString())
        }
    }

    private fun incrementSearchFrequency(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return
        viewModelScope.launch {
            val freqJson = settingsRepository.getSetting("search_frequency", "{}")
            val obj = JSONObject(freqJson)
            val count = obj.optInt(q, 0)
            obj.put(q, count + 1)
            settingsRepository.putSetting("search_frequency", obj.toString())
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
        
        if (fuzzyMatch(title, q)) return 40
        if (fuzzyMatch(titleZh, q)) return 30
        
        if (show.notes != null && show.notes.lowercase().contains(q)) return 20
        return 0
    }
}
