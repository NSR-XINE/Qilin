package com.mydonghua.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mydonghua.app.domain.model.CountdownCalculator
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.domain.repository.SettingsRepository
import com.mydonghua.app.domain.repository.ShowRepository
import com.mydonghua.app.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val nextUpShow: Show? = null,
    val continueWatching: List<Show> = emptyList(),
    val recentlyUpdated: List<Show> = emptyList(),
    val allShows: List<Show> = emptyList(),
    val showCountdownBanner: Boolean = true,
    val showContinueWatching: Boolean = true,
    val showRecentlyUpdated: Boolean = true,
    val showWeeklySchedule: Boolean = true,
    val themeMode: String = "dark",
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val settingsRepository: SettingsRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private data class DashboardSettings(
        val showBanner: Boolean,
        val showContinue: Boolean,
        val showRecent: Boolean,
        val showSchedule: Boolean,
        val theme: String
    )

    private fun loadHomeData() {
        val settingsFlow = combine(
            settingsRepository.getSettingFlow("show_countdown_banner", "true"),
            settingsRepository.getSettingFlow("show_continue_watching", "true"),
            settingsRepository.getSettingFlow("show_recently_updated", "true"),
            settingsRepository.getSettingFlow("show_weekly_schedule", "true"),
            settingsRepository.getSettingFlow("app_theme", "dark")
        ) { banner, cont, recent, schedule, theme ->
            DashboardSettings(
                showBanner = banner.toBoolean(),
                showContinue = cont.toBoolean(),
                showRecent = recent.toBoolean(),
                showSchedule = schedule.toBoolean(),
                theme = theme
            )
        }

        combine(
            showRepository.getAllShows(),
            watchHistoryRepository.getWatchHistory(),
            settingsFlow
        ) { shows, history, settings ->
            val nextUp = CountdownCalculator.getNextUpShow(shows)

            val continueWatchingList = history
                .distinctBy { it.showId }
                .mapNotNull { hist -> shows.find { it.id == hist.showId } }
                .filter { it.status == "ongoing" && it.currentEp > 0 }

            val recentlyUpdatedList = shows
                .filter { it.lastUpdated != null }
                .sortedByDescending { it.lastUpdated ?: 0L }
                .take(10)

            HomeUiState(
                nextUpShow = nextUp,
                continueWatching = continueWatchingList,
                recentlyUpdated = recentlyUpdatedList,
                allShows = shows,
                showCountdownBanner = settings.showBanner,
                showContinueWatching = settings.showContinue,
                showRecentlyUpdated = settings.showRecent,
                showWeeklySchedule = settings.showSchedule,
                themeMode = settings.theme,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
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
