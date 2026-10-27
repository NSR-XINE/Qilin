package com.mydonghua.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mydonghua.app.domain.model.CountdownCalculator
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.domain.repository.SettingsKeys
import com.mydonghua.app.domain.repository.SettingsRepository
import com.mydonghua.app.domain.repository.ShowRepository
import com.mydonghua.app.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mydonghua.app.CountdownWidget
import com.mydonghua.app.data.service.AniListService
import com.mydonghua.app.data.service.AniListMedia
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val nextUpShow: Show? = null,
    val continueWatching: List<Show> = emptyList(),
    val recentlyUpdated: List<Show> = emptyList(),
    val allShows: List<Show> = emptyList(),
    val trendingDonghua: List<AniListMedia> = emptyList(),
    val isTrendingLoading: Boolean = false,
    val showCountdownBanner: Boolean = true,
    val showContinueWatching: Boolean = true,
    val showRecentlyUpdated: Boolean = true,
    val themeMode: String = "dark",
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val settingsRepository: SettingsRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        loadTrendingDonghua()
    }

    private data class DashboardSettings(
        val showBanner: Boolean,
        val showContinue: Boolean,
        val showRecent: Boolean,
        val theme: String
    )

    private fun loadHomeData() {
        val settingsFlow = combine(
            settingsRepository.getBoolFlow(SettingsKeys.SHOW_BANNER, true),
            settingsRepository.getBoolFlow(SettingsKeys.SHOW_CONTINUE, true),
            settingsRepository.getBoolFlow(SettingsKeys.SHOW_RECENT, true),
            settingsRepository.getSettingFlow(SettingsKeys.THEME_MODE, "dark")
        ) { banner, cont, recent, theme ->
            DashboardSettings(
                showBanner = banner,
                showContinue = cont,
                showRecent = recent,
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
                .filter { show ->
                    if (show.totalEp > 0) show.currentEp < show.totalEp else show.status != "completed"
                }

            val recentlyUpdatedList = shows
                .sortedByDescending { it.lastUpdated ?: 0L }
                .take(10)

            HomeUiState(
                nextUpShow = nextUp,
                continueWatching = continueWatchingList,
                recentlyUpdated = recentlyUpdatedList,
                allShows = shows,
                trendingDonghua = _uiState.value.trendingDonghua,
                isTrendingLoading = _uiState.value.isTrendingLoading,
                showCountdownBanner = settings.showBanner,
                showContinueWatching = settings.showContinue,
                showRecentlyUpdated = settings.showRecent,
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
        viewModelScope.launch { ShowActions.incrementEpisode(show, showRepository, watchHistoryRepository) }
    }

    fun decrementEpisode(show: Show) {
        viewModelScope.launch { ShowActions.decrementEpisode(show, showRepository) }
    }

    fun toggleFavorite(show: Show) {
        viewModelScope.launch { ShowActions.toggleFavorite(show, showRepository) }
    }

    fun toggleNotification(context: android.content.Context, show: Show) {
        viewModelScope.launch { ShowActions.toggleNotification(context, show, showRepository) }
    }

    fun setShowBanner(show: Boolean) {
        viewModelScope.launch { settingsRepository.putBool(SettingsKeys.SHOW_BANNER, show) }
    }

    fun setShowContinue(show: Boolean) {
        viewModelScope.launch { settingsRepository.putBool(SettingsKeys.SHOW_CONTINUE, show) }
    }

    fun setShowRecent(show: Boolean) {
        viewModelScope.launch { settingsRepository.putBool(SettingsKeys.SHOW_RECENT, show) }
    }

    fun loadTrendingDonghua() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isTrendingLoading = true) }
                val trending = AniListService.fetchTrendingDonghua(page = 1, perPage = 10)
                _uiState.update { it.copy(trendingDonghua = trending, isTrendingLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isTrendingLoading = false) }
            }
        }
    }

    fun addTrendingShow(media: AniListMedia, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val existing = _uiState.value.allShows.find { s ->
                s.aniListId == media.id ||
                s.title.equals(media.preferredTitle, ignoreCase = true) ||
                (media.titleEnglish != null && s.title.equals(media.titleEnglish, ignoreCase = true)) ||
                (media.titleRomaji != null && s.title.equals(media.titleRomaji, ignoreCase = true))
            }
            if (existing != null) {
                onComplete(existing.id)
                return@launch
            }
            val id = UUID.randomUUID().toString()
            val show = Show(
                id = id,
                title = media.preferredTitle,
                titleZh = media.titleNative,
                status = when (media.status?.lowercase()) {
                    "completed" -> "completed"
                    "releasing" -> "ongoing"
                    else -> "ongoing"
                },
                releaseDay = "Sunday",
                releaseTime = "10:00",
                currentEp = 0,
                totalEp = media.episodes,
                poster = media.coverImage,
                notes = media.description,
                genres = media.genres?.joinToString(", "),
                studio = media.studio,
                releaseYear = media.releaseYear ?: 0,
                rating = media.averageScore ?: 0,
                aniListId = media.id,
                dateAdded = System.currentTimeMillis(),
                lastUpdated = System.currentTimeMillis()
            )
            showRepository.insertOrUpdateShow(show)
            onComplete(id)
        }
    }
}
