package com.mydonghua.app.data.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mydonghua.app.domain.repository.SettingsKeys
import com.mydonghua.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.legacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * One-time migration of the legacy settings stores into the Room-backed
 * [SettingsRepository]. Reads:
 *  - DataStore "settings" (theme mode, accent color, UI visibility, keyboard, stream provider)
 *  - SharedPreferences "notification_prefs" (notification round-trip values)
 * Writes everything into the Room `settings` table, gated by [SettingsKeys.PREFS_MIGRATED].
 */
@Singleton
class LegacySettingsMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private val LEGACY_THEME_MODE = stringPreferencesKey("app_theme")
        private val LEGACY_ACCENT_COLOR = stringPreferencesKey("pref_accent_color")
        private val LEGACY_SHOW_BANNER = booleanPreferencesKey("show_banner")
        private val LEGACY_SHOW_CONTINUE = booleanPreferencesKey("show_continue")
        private val LEGACY_SHOW_RECENT = booleanPreferencesKey("show_recent")
        private val LEGACY_AUTO_OPEN_KEYBOARD = booleanPreferencesKey("auto_open_keyboard")
        private val LEGACY_DEFAULT_STREAM_PROVIDER = stringPreferencesKey("default_stream_provider")
    }

    suspend fun migrate() {
        if (settingsRepository.getSetting(SettingsKeys.PREFS_MIGRATED, "false") == "true") return

        val prefs = context.legacyDataStore.data.first()

        prefs[LEGACY_THEME_MODE]?.let {
            settingsRepository.putSetting(SettingsKeys.THEME_MODE, it)
        }
        prefs[LEGACY_ACCENT_COLOR]?.let {
            settingsRepository.putSetting(SettingsKeys.ACCENT_COLOR, it)
        }
        prefs[LEGACY_SHOW_BANNER]?.let {
            settingsRepository.putSetting(SettingsKeys.SHOW_BANNER, it.toString())
        }
        prefs[LEGACY_SHOW_CONTINUE]?.let {
            settingsRepository.putSetting(SettingsKeys.SHOW_CONTINUE, it.toString())
        }
        prefs[LEGACY_SHOW_RECENT]?.let {
            settingsRepository.putSetting(SettingsKeys.SHOW_RECENT, it.toString())
        }
        prefs[LEGACY_AUTO_OPEN_KEYBOARD]?.let {
            settingsRepository.putSetting(SettingsKeys.AUTO_OPEN_KEYBOARD, it.toString())
        }
        prefs[LEGACY_DEFAULT_STREAM_PROVIDER]?.let {
            settingsRepository.putSetting(SettingsKeys.DEFAULT_STREAM_PROVIDER, it)
        }

        val notifyPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        notifyPrefs.all.forEach { (key, value) ->
            settingsRepository.putSetting(key, value.toString())
        }

        settingsRepository.putSetting(SettingsKeys.PREFS_MIGRATED, "true")
    }
}
