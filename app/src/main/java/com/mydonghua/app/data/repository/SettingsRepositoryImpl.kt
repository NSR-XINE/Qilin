package com.mydonghua.app.data.repository

import com.mydonghua.app.data.db.SettingEntity
import com.mydonghua.app.data.db.SettingsDao
import com.mydonghua.app.data.pref.SettingsMigrationGate
import com.mydonghua.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val migrationGate: SettingsMigrationGate
) : SettingsRepository {
    override suspend fun getSetting(key: String, defaultValue: String): String {
        migrationGate.awaitReady()
        return settingsDao.getSetting(key)?.value ?: defaultValue
    }

    override fun getSettingFlow(key: String, defaultValue: String): Flow<String> {
        return flow {
            migrationGate.awaitReady()
            emitAll(settingsDao.getSettingFlow(key).map { it?.value ?: defaultValue })
        }
    }

    override suspend fun putSetting(key: String, value: String) {
        settingsDao.insertSetting(SettingEntity(key, value))
    }

    override suspend fun removeSetting(key: String) {
        settingsDao.deleteSetting(key)
    }

    override suspend fun getAllSettings(): Map<String, String> {
        migrationGate.awaitReady()
        return settingsDao.getAllSettings().associate { it.key to (it.value ?: "") }
    }
}
