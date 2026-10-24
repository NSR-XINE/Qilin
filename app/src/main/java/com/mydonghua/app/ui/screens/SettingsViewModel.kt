package com.mydonghua.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mydonghua.app.data.service.AdBlocker
import com.mydonghua.app.domain.repository.SettingsKeys
import com.mydonghua.app.domain.repository.SettingsRepository
import com.mydonghua.app.domain.repository.ShowRepository
import com.mydonghua.app.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val autoOpenKeyboard: Boolean = true,
    val defaultStreamProvider: String = "donghuastream",
    val adBlockerEnabled: Boolean = true
)

data class DashboardPrefs(
    val showBanner: Boolean,
    val showContinue: Boolean,
    val showRecent: Boolean
)

data class StreamPrefs(
    val autoOpenKeyboard: Boolean,
    val defaultStreamProvider: String,
    val adBlockerEnabled: Boolean
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val showRepository: ShowRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getSettingFlow(SettingsKeys.THEME_MODE, "dark"),
        settingsRepository.getSettingFlow(SettingsKeys.ACCENT_COLOR, "default"),
        combine(
            settingsRepository.getBoolFlow(SettingsKeys.AUTO_OPEN_KEYBOARD, true),
            settingsRepository.getSettingFlow(SettingsKeys.DEFAULT_STREAM_PROVIDER, "donghuastream"),
            AdBlocker.enabledFlow(appContext)
        ) { autoOpen, provider, adBlocker ->
            StreamPrefs(autoOpen, provider, adBlocker)
        },
        combine(
            settingsRepository.getBoolFlow(SettingsKeys.SHOW_BANNER, true),
            settingsRepository.getBoolFlow(SettingsKeys.SHOW_CONTINUE, true),
            settingsRepository.getBoolFlow(SettingsKeys.SHOW_RECENT, true)
        ) { banner, cont, recent ->
            DashboardPrefs(banner, cont, recent)
        }
    ) { theme, accent, streamPrefs, prefs ->
        SettingsUiState(
            themeMode = theme,
            accentColor = accent,
            showBanner = prefs.showBanner,
            showContinue = prefs.showContinue,
            showRecent = prefs.showRecent,
            autoOpenKeyboard = streamPrefs.autoOpenKeyboard,
            defaultStreamProvider = streamPrefs.defaultStreamProvider,
            adBlockerEnabled = streamPrefs.adBlockerEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.putSetting(SettingsKeys.THEME_MODE, mode) }
    }

    fun setAccentColor(accent: String) {
        viewModelScope.launch { settingsRepository.putSetting(SettingsKeys.ACCENT_COLOR, accent) }
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

    fun setAutoOpenKeyboard(autoOpen: Boolean) {
        viewModelScope.launch { settingsRepository.putBool(SettingsKeys.AUTO_OPEN_KEYBOARD, autoOpen) }
    }

    fun setAdBlockerEnabled(enabled: Boolean) {
        AdBlocker.setEnabled(appContext, enabled)
        viewModelScope.launch { settingsRepository.putBool(SettingsKeys.AD_BLOCKER_ENABLED, enabled) }
    }

    suspend fun storagePermissionPrompted(): Boolean =
        settingsRepository.getBool(SettingsKeys.STORAGE_PERMISSION_PROMPTED, false)

    fun markStoragePermissionPrompted() {
        viewModelScope.launch {
            settingsRepository.putBool(SettingsKeys.STORAGE_PERMISSION_PROMPTED, true)
        }
    }

    fun setDefaultStreamProvider(provider: String) {
        viewModelScope.launch { settingsRepository.putSetting(SettingsKeys.DEFAULT_STREAM_PROVIDER, provider) }
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

            // All app settings (theme, UI toggles, search history, notifications) live in the Room settings table.
            val settingsMap = settingsRepository.getAllSettings()
            val settingsJson = JSONObject(settingsMap)

            val backup = JSONObject().apply {
                put("version", 2)
                put("exportedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()))
                put("appVersion", "v6.8")
                put("shows", showsArr)
                put("watchHistory", historyArr)
                put("notifyPrefs", settingsJson.toString())
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
                val jsonString = try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                } catch (se: SecurityException) {
                    throw Exception("Permission denied opening file. Please select the file again using the document picker.")
                } ?: throw Exception("Unable to open backup file stream.")

                val parsed = JSONObject(jsonString)
                val showsArr = parsed.optJSONArray("shows") ?: JSONArray()
                val watchHistory = parsed.optJSONArray("watchHistory") ?: JSONArray()
                val notifyPrefsStr = parsed.optString("notifyPrefs", "{}")
                val notifyObj = JSONObject(notifyPrefsStr)
                val keys = notifyObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = notifyObj.get(key)
                    settingsRepository.putSetting(key, value.toString())
                }

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

                com.mydonghua.app.AlarmScheduler.rescheduleAllAfterBoot(context)

                onSuccess("${importedShowsList.size} shows and ${watchHistory.length()} history logs imported successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure(e.localizedMessage ?: "Failed to parse backup JSON")
            }
        }
    }
}


