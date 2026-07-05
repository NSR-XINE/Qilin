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
        val SHOW_SCHEDULE = booleanPreferencesKey("show_schedule")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "dark" }
    val accentColorFlow: Flow<String> = context.dataStore.data.map { it[ACCENT_COLOR] ?: "default" }
    
    val showBannerFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_BANNER] ?: true }
    val showContinueFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_CONTINUE] ?: true }
    val showRecentFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_RECENT] ?: true }
    val showScheduleFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_SCHEDULE] ?: true }

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

    suspend fun setShowSchedule(show: Boolean) {
        context.dataStore.edit { it[SHOW_SCHEDULE] = show }
    }
}
