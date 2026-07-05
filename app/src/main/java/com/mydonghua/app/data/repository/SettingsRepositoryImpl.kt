package com.mydonghua.app.data.repository

import com.mydonghua.app.data.db.SettingEntity
import com.mydonghua.app.data.db.ShowDao
import com.mydonghua.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val showDao: ShowDao
) : SettingsRepository {
    override suspend fun getSetting(key: String, defaultValue: String): String {
        return showDao.getSetting(key)?.value ?: defaultValue
    }

    override fun getSettingFlow(key: String, defaultValue: String): Flow<String> {
        return showDao.getSettingFlow(key).map { it?.value ?: defaultValue }
    }

    override suspend fun putSetting(key: String, value: String) {
        showDao.insertSetting(SettingEntity(key, value))
    }

    override suspend fun removeSetting(key: String) {
        showDao.deleteSetting(key)
    }
}
