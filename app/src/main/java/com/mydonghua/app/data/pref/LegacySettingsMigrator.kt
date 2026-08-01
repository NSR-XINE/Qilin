package com.mydonghua.app.data.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mydonghua.app.data.db.SettingEntity
import com.mydonghua.app.data.db.SettingsDao
import com.mydonghua.app.domain.repository.SettingsKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.legacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * One-time migration of the legacy settings stores into the Room-backed
 * `settings` table. Reads:
 *  - DataStore "settings" (theme mode, accent color, UI visibility, keyboard, stream provider)
 *  - SharedPreferences "notification_prefs" (notification round-trip values)
 * Writes everything via [SettingsDao] (bypassing the settings repository
 * migration gate to avoid a deadlock), gated by [SettingsKeys.PREFS_MIGRATED].
 */
@Singleton
class LegacySettingsMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao,
    private val migrationGate: SettingsMigrationGate
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
        try {
            if (settingsDao.getSetting(SettingsKeys.PREFS_MIGRATED)?.value == "true") return

            val prefs = context.legacyDataStore.data.first()

            prefs[LEGACY_THEME_MODE]?.let {
                settingsDao.insertSetting(SettingEntity(SettingsKeys.THEME_MODE, it))
            }
            prefs[LEGACY_ACCENT_COLOR]?.let {
                settingsDao.insertSetting(SettingEntity(SettingsKeys.ACCENT_COLOR, it))
            }
            prefs[LEGACY_SHOW_BANNER]?.let {
                settingsDao.insertSetting(SettingEntity(SettingsKeys.SHOW_BANNER, it.toString()))
            }
            prefs[LEGACY_SHOW_CONTINUE]?.let {
                settingsDao.insertSetting(SettingEntity(SettingsKeys.SHOW_CONTINUE, it.toString()))
            }
            prefs[LEGACY_SHOW_RECENT]?.let {
                settingsDao.insertSetting(SettingEntity(SettingsKeys.SHOW_RECENT, it.toString()))
            }
            prefs[LEGACY_AUTO_OPEN_KEYBOARD]?.let {
                settingsDao.insertSetting(SettingEntity(SettingsKeys.AUTO_OPEN_KEYBOARD, it.toString()))
            }
            prefs[LEGACY_DEFAULT_STREAM_PROVIDER]?.let {
                settingsDao.insertSetting(SettingEntity(SettingsKeys.DEFAULT_STREAM_PROVIDER, it))
            }

            val notifyPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            notifyPrefs.all.forEach { (key, value) ->
                settingsDao.insertSetting(SettingEntity(key, value.toString()))
            }

            settingsDao.insertSetting(SettingEntity(SettingsKeys.PREFS_MIGRATED, "true"))
        } finally {
            migrationGate.markMigrated()
        }
    }
}
