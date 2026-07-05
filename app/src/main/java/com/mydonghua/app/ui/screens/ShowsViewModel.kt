package com.mydonghua.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.domain.repository.ShowRepository
import com.mydonghua.app.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchHistoryItem(
    val showId: String,
    val showTitle: String,
    val poster: String?,
    val episodeNum: Int,
    val watchedAt: Long
)

@HiltViewModel
class ShowsViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {

    val allShows: StateFlow<List<Show>> = showRepository.getAllShows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<WatchHistoryItem>> = combine(
        watchHistoryRepository.getWatchHistory(),
        allShows
    ) { historyList, showsList ->
        historyList.map { h ->
            val show = showsList.find { it.id == h.showId }
            WatchHistoryItem(
                showId = h.showId,
                showTitle = show?.title ?: "Unknown Show",
                poster = show?.poster,
                episodeNum = h.episodeNum,
                watchedAt = h.watchedAt
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<Map<String, List<Show>>> = allShows
        .map { shows ->
            shows.filter { !it.collectionId.isNullOrBlank() }
                .groupBy { it.collectionId!! }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val airingShows: StateFlow<List<Show>> = allShows
        .map { list -> list.filter { it.status == "ongoing" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedShows: StateFlow<List<Show>> = allShows
        .map { list -> list.filter { it.status == "completed" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stoppedShows: StateFlow<List<Show>> = allShows
        .map { list -> list.filter { it.status == "stopped" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteShows: StateFlow<List<Show>> = allShows
        .map { list -> list.filter { it.isFavorite == 1 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertOrUpdateShow(show: Show) {
        viewModelScope.launch {
            showRepository.insertOrUpdateShow(show)
        }
    }

    fun deleteShow(showId: String) {
        viewModelScope.launch {
            showRepository.deleteShow(showId)
        }
    }

    fun incrementEpisode(show: Show) {
        viewModelScope.launch {
            val nextEp = show.currentEp + 1
            val updated = show.copy(currentEp = nextEp, lastUpdated = System.currentTimeMillis())
            showRepository.insertOrUpdateShow(updated)
            watchHistoryRepository.addWatchHistory(show.id, nextEp)
        }
    }

    fun decrementEpisode(show: Show) {
        viewModelScope.launch {
            if (show.currentEp > 0) {
                val prevEp = show.currentEp - 1
                val updated = show.copy(currentEp = prevEp, lastUpdated = System.currentTimeMillis())
                showRepository.insertOrUpdateShow(updated)
            }
        }
    }

    fun toggleFavorite(show: Show) {
        viewModelScope.launch {
            val updated = show.copy(
                isFavorite = if (show.isFavorite == 1) 0 else 1,
                lastUpdated = System.currentTimeMillis()
            )
            showRepository.insertOrUpdateShow(updated)
        }
    }
}
