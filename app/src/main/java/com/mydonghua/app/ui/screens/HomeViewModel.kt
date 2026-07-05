package com.mydonghua.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mydonghua.app.domain.model.CountdownCalculator
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.domain.repository.SettingsRepository
import com.mydonghua.app.domain.repository.ShowRepository
import com.mydonghua.app.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mydonghua.app.data.pref.ThemePreference
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
    private val watchHistoryRepository: WatchHistoryRepository,
    @ApplicationContext private val context: Context,
    private val themePreference: ThemePreference
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
            themePreference.showBannerFlow,
            themePreference.showContinueFlow,
            themePreference.showRecentFlow,
            themePreference.showScheduleFlow,
            themePreference.themeModeFlow
        ) { banner, cont, recent, schedule, theme ->
            DashboardSettings(
                showBanner = banner,
                showContinue = cont,
                showRecent = recent,
                showSchedule = schedule,
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
            val nextUp = state.nextUpShow
            if (nextUp != null) {
                val targetMs = CountdownCalculator.getNextReleaseDateMs(nextUp.releaseDay, nextUp.releaseTime)
                val remaining = CountdownCalculator.calculateTimeRemaining(targetMs)
                val countdownStr = if (remaining.airingNow) {
                    "AIRING NOW"
                } else {
                    "${remaining.days}d ${remaining.hours}h ${remaining.minutes}m"
                }
                val epStr = "Episode ${nextUp.currentEp + 1} releasing ${nextUp.releaseDay}"
                CountdownWidget.pushWidgetData(context, nextUp.title, countdownStr, epStr)
            } else {
                CountdownWidget.pushWidgetData(context, "No shows yet", "--d --h --m", "")
            }
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

    fun setShowBanner(show: Boolean) {
        viewModelScope.launch { themePreference.setShowBanner(show) }
    }

    fun setShowContinue(show: Boolean) {
        viewModelScope.launch { themePreference.setShowContinue(show) }
    }

    fun setShowRecent(show: Boolean) {
        viewModelScope.launch { themePreference.setShowRecent(show) }
    }

    fun setShowSchedule(show: Boolean) {
        viewModelScope.launch { themePreference.setShowSchedule(show) }
    }
}
