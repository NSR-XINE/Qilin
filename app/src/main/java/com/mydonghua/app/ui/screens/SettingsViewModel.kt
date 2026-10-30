package com.mydonghua.app.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.mydonghua.app.data.db.AppDatabase
import com.mydonghua.app.data.db.WatchHistoryEntity
import com.mydonghua.app.data.service.AdBlocker
import com.mydonghua.app.data.service.AniListService
import com.mydonghua.app.data.service.AniListUser
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.domain.repository.SettingsKeys
import com.mydonghua.app.domain.repository.SettingsRepository
import com.mydonghua.app.domain.repository.ShowRepository
import com.mydonghua.app.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
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
    private val watchHistoryRepository: WatchHistoryRepository,
    private val appDatabase: AppDatabase
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
                    put("aniListId", s.aniListId)
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
                put("appVersion", "v${com.mydonghua.app.BuildConfig.VERSION_NAME}")
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
                        collectionId = sObj.optString("collectionId").takeIf { it.isNotEmpty() && it != "null" },
                        aniListId = sObj.optInt("aniListId").takeIf { it > 0 }
                    )
                    importedShowsList.add(show)
                }

                val importedHistoryList = mutableListOf<WatchHistoryEntity>()
                for (i in 0 until watchHistory.length()) {
                    val hObj = watchHistory.getJSONObject(i)
                    importedHistoryList.add(
                        WatchHistoryEntity(
                            showId = hObj.getString("showId"),
                            episodeNum = hObj.getInt("episodeNum"),
                            watchedTimestamp = hObj.optLong("watchedAt").takeIf { it > 0L } ?: System.currentTimeMillis()
                        )
                    )
                }

                appDatabase.withTransaction {
                    showRepository.clearAllShows()
                    watchHistoryRepository.clearAllWatchHistory()
                    showRepository.bulkInsert(importedShowsList)
                    watchHistoryRepository.bulkInsertHistory(importedHistoryList)
                }

                com.mydonghua.app.AlarmScheduler.rescheduleAllAfterBoot(context)

                onSuccess("${importedShowsList.size} shows and ${watchHistory.length()} history logs imported successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure(e.localizedMessage ?: "Failed to parse backup JSON")
            }
        }
    }

    val aniListUser: StateFlow<AniListUser?> = AniListService.userFlow
    val aniListAutoSync: StateFlow<Boolean> = AniListService.autoSyncFlow

    private val _isImportingAniList = MutableStateFlow(false)
    val isImportingAniList: StateFlow<Boolean> = _isImportingAniList.asStateFlow()

    fun connectAniList(token: String, onResult: (Result<AniListUser>) -> Unit) {
        viewModelScope.launch {
            val result = AniListService.validateTokenAndFetchViewer(token)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                AniListService.saveSession(token, user)
            }
            onResult(result)
        }
    }

    fun disconnectAniList() {
        AniListService.clearSession()
    }

    fun setAniListAutoSync(enabled: Boolean) {
        AniListService.setAutoSync(enabled)
    }

    fun importLibraryFromAniList(onComplete: (imported: Int, updated: Int, error: String?) -> Unit) {
        val user = AniListService.userFlow.value ?: run {
            onComplete(0, 0, "AniList account not connected")
            return
        }
        val token = AniListService.getToken() ?: run {
            onComplete(0, 0, "No access token found")
            return
        }
        viewModelScope.launch {
            _isImportingAniList.value = true
            try {
                val res = AniListService.fetchUserAnimeCollection(token, user.id)
                if (res.isFailure) {
                    onComplete(0, 0, res.exceptionOrNull()?.message ?: "Failed to fetch collection")
                    return@launch
                }

                val entries = res.getOrThrow()
                val currentShows = showRepository.getAllShowsList()
                val showByAniListId = currentShows.filter { it.aniListId != null }.associateBy { it.aniListId!! }
                val showByTitle = currentShows.associateBy { it.title.lowercase().trim() }

                var importedCount = 0
                var updatedCount = 0

                for (entry in entries) {
                    val existing = showByAniListId[entry.mediaId]
                        ?: showByTitle[entry.media.preferredTitle.lowercase().trim()]
                        ?: (entry.media.titleRomaji?.let { showByTitle[it.lowercase().trim()] })
                        ?: (entry.media.titleEnglish?.let { showByTitle[it.lowercase().trim()] })
                        ?: (entry.media.titleNative?.let { showByTitle[it.lowercase().trim()] })

                    val status = when (entry.status) {
                        "COMPLETED" -> "completed"
                        "DROPPED", "PAUSED" -> "stopped"
                        else -> "ongoing"
                    }

                    if (existing != null) {
                        val targetEp = maxOf(existing.currentEp, entry.progress)
                        val updatedShow = existing.copy(
                            aniListId = entry.mediaId,
                            currentEp = targetEp,
                            totalEp = if (existing.totalEp <= 0 && entry.media.episodes > 0) entry.media.episodes else existing.totalEp,
                            poster = existing.poster ?: entry.media.coverImage,
                            notes = existing.notes ?: entry.media.description,
                            genres = existing.genres ?: entry.media.genres?.joinToString(", "),
                            studio = existing.studio ?: entry.media.studio,
                            releaseYear = if (existing.releaseYear <= 0 && (entry.media.releaseYear ?: 0) > 0) entry.media.releaseYear!! else existing.releaseYear,
                            status = if (existing.status == "ongoing" && status == "completed") "completed" else existing.status,
                            rating = if (existing.rating <= 0 && entry.score > 0) entry.score.toInt() else existing.rating,
                            lastUpdated = System.currentTimeMillis()
                        )
                        showRepository.insertOrUpdateShow(updatedShow)
                        if (entry.progress > 0) {
                            watchHistoryRepository.addWatchHistory(existing.id, targetEp)
                        }
                        updatedCount++
                    } else {
                        val newId = UUID.randomUUID().toString()
                        val newShow = Show(
                            id = newId,
                            title = entry.media.preferredTitle,
                            titleZh = entry.media.titleNative,
                            status = status,
                            releaseDay = "Sunday",
                            releaseTime = "10:00",
                            currentEp = entry.progress,
                            totalEp = entry.media.episodes,
                            poster = entry.media.coverImage,
                            notes = entry.media.description,
                            genres = entry.media.genres?.joinToString(", "),
                            studio = entry.media.studio,
                            releaseYear = entry.media.releaseYear ?: 0,
                            rating = entry.score.toInt(),
                            dateAdded = System.currentTimeMillis(),
                            lastUpdated = System.currentTimeMillis(),
                            aniListId = entry.mediaId
                        )
                        showRepository.insertOrUpdateShow(newShow)
                        if (entry.progress > 0) {
                            watchHistoryRepository.addWatchHistory(newId, entry.progress)
                        }
                        importedCount++
                    }
                }

                _lastSyncedTime.value = System.currentTimeMillis()
                onComplete(importedCount, updatedCount, null)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Import from AniList failed", e)
                onComplete(0, 0, e.localizedMessage ?: "Import failed")
            } finally {
                _isImportingAniList.value = false
            }
        }
    }

    private val _isPushingToAniList = MutableStateFlow(false)
    val isPushingToAniList: StateFlow<Boolean> = _isPushingToAniList.asStateFlow()

    private val _isTwoWaySyncing = MutableStateFlow(false)
    val isTwoWaySyncing: StateFlow<Boolean> = _isTwoWaySyncing.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow(0L)
    val lastSyncedTime: StateFlow<Long> = _lastSyncedTime.asStateFlow()

    fun pushLocalLibraryToAniList(onComplete: (syncedCount: Int, failedCount: Int, error: String?) -> Unit) {
        val token = AniListService.getToken() ?: run {
            onComplete(0, 0, "AniList not connected")
            return
        }
        viewModelScope.launch {
            _isPushingToAniList.value = true
            try {
                val localShows = showRepository.getAllShowsList()
                var synced = 0
                var failed = 0

                for (show in localShows) {
                    var targetId = show.aniListId
                    if (targetId == null) {
                        targetId = AniListService.findBestMatchId(show.title, show.titleZh)
                        if (targetId != null) {
                            showRepository.insertOrUpdateShow(show.copy(aniListId = targetId))
                        }
                    }

                    if (targetId != null) {
                        val res = AniListService.updateProgress(token, targetId, show.currentEp, show.totalEp)
                        if (res.isSuccess) synced++ else failed++
                    }
                }
                _lastSyncedTime.value = System.currentTimeMillis()
                onComplete(synced, failed, null)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Push to AniList failed", e)
                onComplete(0, 0, e.localizedMessage ?: "Push error")
            } finally {
                _isPushingToAniList.value = false
            }
        }
    }

    fun twoWaySyncAll(onComplete: (pushed: Int, pulled: Int, imported: Int, error: String?) -> Unit) {
        val user = AniListService.userFlow.value ?: run {
            onComplete(0, 0, 0, "AniList not connected")
            return
        }
        val token = AniListService.getToken() ?: run {
            onComplete(0, 0, 0, "No access token found")
            return
        }

        viewModelScope.launch {
            _isTwoWaySyncing.value = true
            try {
                val res = AniListService.fetchUserAnimeCollection(token, user.id)
                if (res.isFailure) {
                    onComplete(0, 0, 0, res.exceptionOrNull()?.message ?: "Failed to fetch cloud collection")
                    return@launch
                }

                val entries = res.getOrThrow()
                val currentShows = showRepository.getAllShowsList()
                val showByAniListId = currentShows.filter { it.aniListId != null }.associateBy { it.aniListId!! }.toMutableMap()
                val showByTitle = currentShows.associateBy { it.title.lowercase().trim() }.toMutableMap()

                var pushedCount = 0
                var pulledCount = 0
                var importedCount = 0

                // 1. Process cloud entries against local
                for (entry in entries) {
                    val existing = showByAniListId[entry.mediaId]
                        ?: showByTitle[entry.media.preferredTitle.lowercase().trim()]
                        ?: (entry.media.titleRomaji?.let { showByTitle[it.lowercase().trim()] })
                        ?: (entry.media.titleEnglish?.let { showByTitle[it.lowercase().trim()] })
                        ?: (entry.media.titleNative?.let { showByTitle[it.lowercase().trim()] })

                    if (existing != null) {
                        if (existing.currentEp > entry.progress) {
                            // Local has newer episode -> Push to AniList
                            val pushRes = AniListService.updateProgress(token, entry.mediaId, existing.currentEp, existing.totalEp)
                            if (pushRes.isSuccess) pushedCount++
                        } else if (entry.progress > existing.currentEp) {
                            // AniList has newer episode -> Pull to Local
                            val updated = existing.copy(
                                aniListId = entry.mediaId,
                                currentEp = entry.progress,
                                lastUpdated = System.currentTimeMillis()
                            )
                            showRepository.insertOrUpdateShow(updated)
                            watchHistoryRepository.addWatchHistory(existing.id, entry.progress)
                            pulledCount++
                        } else if (existing.aniListId == null) {
                            showRepository.insertOrUpdateShow(existing.copy(aniListId = entry.mediaId))
                        }
                    } else {
                        // Show exists in cloud but not locally -> Import
                        val newId = UUID.randomUUID().toString()
                        val newShow = Show(
                            id = newId,
                            title = entry.media.preferredTitle,
                            titleZh = entry.media.titleNative,
                            status = when (entry.status) {
                                "COMPLETED" -> "completed"
                                "DROPPED", "PAUSED" -> "stopped"
                                else -> "ongoing"
                            },
                            releaseDay = "Sunday",
                            releaseTime = "10:00",
                            currentEp = entry.progress,
                            totalEp = entry.media.episodes,
                            poster = entry.media.coverImage,
                            notes = entry.media.description,
                            genres = entry.media.genres?.joinToString(", "),
                            studio = entry.media.studio,
                            releaseYear = entry.media.releaseYear ?: 0,
                            rating = entry.score.toInt(),
                            dateAdded = System.currentTimeMillis(),
                            lastUpdated = System.currentTimeMillis(),
                            aniListId = entry.mediaId
                        )
                        showRepository.insertOrUpdateShow(newShow)
                        if (entry.progress > 0) {
                            watchHistoryRepository.addWatchHistory(newId, entry.progress)
                        }
                        importedCount++
                    }
                }

                // 2. Check remaining local shows not yet in cloud collection
                val cloudMediaIds = entries.map { it.mediaId }.toSet()
                for (localShow in currentShows) {
                    if (localShow.aniListId != null && !cloudMediaIds.contains(localShow.aniListId)) {
                        // Push to AniList
                        val pushRes = AniListService.updateProgress(token, localShow.aniListId, localShow.currentEp, localShow.totalEp)
                        if (pushRes.isSuccess) pushedCount++
                    }
                }

                // Refresh viewer statistics
                val viewerRes = AniListService.validateTokenAndFetchViewer(token)
                if (viewerRes.isSuccess) {
                    AniListService.saveSession(token, viewerRes.getOrThrow())
                }

                _lastSyncedTime.value = System.currentTimeMillis()
                onComplete(pushedCount, pulledCount, importedCount, null)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "2-Way sync failed", e)
                onComplete(0, 0, 0, e.localizedMessage ?: "Sync error")
            } finally {
                _isTwoWaySyncing.value = false
            }
        }
    }
}


