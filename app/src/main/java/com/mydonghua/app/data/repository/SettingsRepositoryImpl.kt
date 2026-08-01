package com.mydonghua.app.data.repository

import com.mydonghua.app.data.db.SettingEntity
import com.mydonghua.app.data.db.SettingsDao
import com.mydonghua.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {
    override suspend fun getSetting(key: String, defaultValue: String): String {
        return settingsDao.getSetting(key)?.value ?: defaultValue
    }

    override fun getSettingFlow(key: String, defaultValue: String): Flow<String> {
        return settingsDao.getSettingFlow(key).map { it?.value ?: defaultValue }
    }

    override suspend fun putSetting(key: String, value: String) {
        settingsDao.insertSetting(SettingEntity(key, value))
    }

    override suspend fun removeSetting(key: String) {
        settingsDao.deleteSetting(key)
    }

    override suspend fun getAllSettings(): Map<String, String> {
        return settingsDao.getAllSettings().associate { it.key to it.value }
    }
}
