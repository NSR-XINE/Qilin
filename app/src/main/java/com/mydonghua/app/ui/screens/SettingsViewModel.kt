package com.mydonghua.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mydonghua.app.data.pref.ThemePreference
import com.mydonghua.app.domain.repository.SettingsRepository
import com.mydonghua.app.domain.repository.ShowRepository
import com.mydonghua.app.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: String = "dark",
    val accentColor: String = "default",
    val showBanner: Boolean = true,
    val showContinue: Boolean = true,
    val showRecent: Boolean = true,
    val showSchedule: Boolean = true,
    val autoOpenKeyboard: Boolean = true,
    val defaultStreamProvider: String = "donghuastream"
)

data class DashboardPrefs(
    val showBanner: Boolean,
    val showContinue: Boolean,
    val showRecent: Boolean,
    val showSchedule: Boolean
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreference: ThemePreference,
    private val showRepository: ShowRepository,
    private val settingsRepository: SettingsRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        themePreference.themeModeFlow,
        themePreference.accentColorFlow,
        themePreference.autoOpenKeyboardFlow,
        themePreference.defaultStreamProviderFlow,
        combine(
            themePreference.showBannerFlow,
            themePreference.showContinueFlow,
            themePreference.showRecentFlow,
            themePreference.showScheduleFlow
        ) { banner, cont, recent, schedule ->
            DashboardPrefs(banner, cont, recent, schedule)
        }
    ) { theme, accent, autoOpen, provider, prefs ->
        SettingsUiState(
            themeMode = theme,
            accentColor = accent,
            showBanner = prefs.showBanner,
            showContinue = prefs.showContinue,
            showRecent = prefs.showRecent,
            showSchedule = prefs.showSchedule,
            autoOpenKeyboard = autoOpen,
            defaultStreamProvider = provider
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: String) {
        viewModelScope.launch { themePreference.setThemeMode(mode) }
    }

    fun setAccentColor(accent: String) {
        viewModelScope.launch { themePreference.setAccentColor(accent) }
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

    fun setAutoOpenKeyboard(autoOpen: Boolean) {
        viewModelScope.launch { themePreference.setAutoOpenKeyboard(autoOpen) }
    }

    fun setDefaultStreamProvider(provider: String) {
        viewModelScope.launch { themePreference.setDefaultStreamProvider(provider) }
    }

    suspend fun exportBackup(context: Context): File? {
        return try {
            val showsList = showRepository.getAllShowsList()
            val watchHistory = watchHistoryRepository.getWatchHistoryList()

            val showsArr = JSONArray()
            showsList.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("titleZh", s.titleZh)
                    put("status", s.status)
                    put("releaseDay", s.releaseDay)
                    put("releaseTime", s.releaseTime)
                    put("currentEp", s.currentEp)
                    put("totalEp", s.totalEp)
                    put("poster", s.poster)
                    put("watchUrl", s.watchUrl)
                    put("countdownUrl", s.countdownUrl)
                    put("notes", s.notes)
                    put("isFavorite", s.isFavorite)
                    put("rating", s.rating)
                    put("lastUpdated", s.lastUpdated)
                    put("dateAdded", s.dateAdded)
                    put("alarmRequestCode", s.alarmRequestCode)
                    put("genres", s.genres)
                    put("studio", s.studio)
                    put("releaseYear", s.releaseYear)
                    put("language", s.language)
                    put("collectionId", s.collectionId)
                }
                showsArr.put(obj)
            }

            val historyArr = JSONArray()
            watchHistory.forEach { h ->
                val obj = JSONObject().apply {
                    put("showId", h.showId)
                    put("episodeNum", h.episodeNum)
                    put("watchedAt", h.watchedTimestamp)
                }
                historyArr.put(obj)
            }

            // TODO: Move to SettingsRepository (Room) alongside ThemePreference migration
            // to eliminate the third settings store (SharedPreferences notification_prefs).
            val notifyPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            val prefsMap = notifyPrefs.all
            val notifyJson = JSONObject(prefsMap)

            val backup = JSONObject().apply {
                put("version", 2)
                put("exportedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()))
                put("appVersion", "v6.8")
                put("shows", showsArr)
                put("watchHistory", historyArr)
                put("notifyPrefs", notifyJson.toString())
            }

            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()
            val file = File(backupDir, "mydonghua_backup.json")
            file.writeText(backup.toString(2))
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importBackup(context: Context, uri: Uri, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: throw Exception("Unable to open backup file")

                val parsed = JSONObject(jsonString)
                val showsArr = parsed.optJSONArray("shows") ?: JSONArray()
                val watchHistory = parsed.optJSONArray("watchHistory") ?: JSONArray()
                val notifyPrefsStr = parsed.optString("notifyPrefs", "{}")

                val importedShowsList = mutableListOf<com.mydonghua.app.domain.model.Show>()
                for (i in 0 until showsArr.length()) {
                    val sObj = showsArr.getJSONObject(i)
                    val show = com.mydonghua.app.domain.model.Show(
                        id = sObj.getString("id"),
                        title = sObj.getString("title"),
                        titleZh = sObj.optString("titleZh").takeIf { it.isNotEmpty() && it != "null" },
                        status = sObj.optString("status", "ongoing"),
                        releaseDay = sObj.optString("releaseDay").takeIf { it.isNotEmpty() && it != "null" },
                        releaseTime = sObj.optString("releaseTime").takeIf { it.isNotEmpty() && it != "null" },
                        currentEp = sObj.optInt("currentEp", 0),
                        totalEp = sObj.optInt("totalEp", 0),
                        poster = sObj.optString("poster").takeIf { it.isNotEmpty() && it != "null" },
                        watchUrl = sObj.optString("watchUrl").takeIf { it.isNotEmpty() && it != "null" },
                        countdownUrl = sObj.optString("countdownUrl").takeIf { it.isNotEmpty() && it != "null" },
                        notes = sObj.optString("notes").takeIf { it.isNotEmpty() && it != "null" },
                        isFavorite = sObj.optInt("isFavorite", 0),
                        rating = sObj.optInt("rating", 0),
                        lastUpdated = sObj.optLong("lastUpdated").takeIf { it > 0L },
                        dateAdded = sObj.optLong("dateAdded").takeIf { it > 0L },
                        alarmRequestCode = sObj.optInt("alarmRequestCode").takeIf { it > 0 },
                        genres = sObj.optString("genres").takeIf { it.isNotEmpty() && it != "null" },
                        studio = sObj.optString("studio").takeIf { it.isNotEmpty() && it != "null" },
                        releaseYear = sObj.optInt("releaseYear", 0),
                        language = sObj.optString("language", "Chinese"),
                        collectionId = sObj.optString("collectionId").takeIf { it.isNotEmpty() && it != "null" }
                    )
                    importedShowsList.add(show)
                }

                showRepository.clearAllShows()
                watchHistoryRepository.clearAllWatchHistory()
                importedShowsList.forEach { s ->
                    showRepository.insertOrUpdateShow(s)
                }

                for (i in 0 until watchHistory.length()) {
                    val hObj = watchHistory.getJSONObject(i)
                    watchHistoryRepository.addWatchHistory(
                        hObj.getString("showId"),
                        hObj.getInt("episodeNum")
                    )
                }

                val notifyPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                val editor = notifyPrefs.edit()
                val notifyObj = JSONObject(notifyPrefsStr)
                val keys = notifyObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = notifyObj.get(key)
                    if (value is Boolean) {
                        editor.putBoolean(key, value)
                    } else if (value is Long) {
                        editor.putLong(key, value)
                    } else if (value is Int) {
                        editor.putInt(key, value)
                    } else if (value is String) {
                        editor.putString(key, value)
                    }
                }
                editor.apply()

                com.mydonghua.app.AlarmScheduler.rescheduleAllAfterBoot(context)

                onSuccess("${importedShowsList.size} shows and ${watchHistory.length()} history logs imported successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure(e.localizedMessage ?: "Failed to parse backup JSON")
            }
        }
    }
}


