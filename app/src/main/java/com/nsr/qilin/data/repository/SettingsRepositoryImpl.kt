package com.nsr.qilin.data.repository

import com.nsr.qilin.data.db.SettingEntity
import com.nsr.qilin.data.db.SettingsDao
import com.nsr.qilin.data.pref.SettingsMigrationGate
import com.nsr.qilin.domain.repository.SettingsRepository
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
