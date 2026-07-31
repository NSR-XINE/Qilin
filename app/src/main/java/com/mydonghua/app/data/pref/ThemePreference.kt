package com.mydonghua.app.data.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// TODO: Consolidate with SettingsRepositoryImpl (Room) and notification_prefs (SharedPreferences)
// into a single storage backend (preferably Room) to eliminate three different settings stores.
@Singleton
class ThemePreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val THEME_MODE = stringPreferencesKey("app_theme")
        val ACCENT_COLOR = stringPreferencesKey("pref_accent_color")
        val SHOW_BANNER = booleanPreferencesKey("show_banner")
        val SHOW_CONTINUE = booleanPreferencesKey("show_continue")
        val SHOW_RECENT = booleanPreferencesKey("show_recent")
        val AUTO_OPEN_KEYBOARD = booleanPreferencesKey("auto_open_keyboard")
        val DEFAULT_STREAM_PROVIDER = stringPreferencesKey("default_stream_provider")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "dark" }
    val accentColorFlow: Flow<String> = context.dataStore.data.map { it[ACCENT_COLOR] ?: "default" }
    
    val showBannerFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_BANNER] ?: true }
    val showContinueFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_CONTINUE] ?: true }
    val showRecentFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_RECENT] ?: true }
    val autoOpenKeyboardFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_OPEN_KEYBOARD] ?: true }
    val defaultStreamProviderFlow: Flow<String> = context.dataStore.data.map { it[DEFAULT_STREAM_PROVIDER] ?: "donghuastream" }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setAccentColor(accent: String) {
        context.dataStore.edit { it[ACCENT_COLOR] = accent }
    }

    suspend fun setShowBanner(show: Boolean) {
        context.dataStore.edit { it[SHOW_BANNER] = show }
    }

    suspend fun setShowContinue(show: Boolean) {
        context.dataStore.edit { it[SHOW_CONTINUE] = show }
    }

    suspend fun setShowRecent(show: Boolean) {
        context.dataStore.edit { it[SHOW_RECENT] = show }
    }

    suspend fun setAutoOpenKeyboard(autoOpen: Boolean) {
        context.dataStore.edit { it[AUTO_OPEN_KEYBOARD] = autoOpen }
    }

    suspend fun setDefaultStreamProvider(provider: String) {
        context.dataStore.edit { it[DEFAULT_STREAM_PROVIDER] = provider }
    }
}
