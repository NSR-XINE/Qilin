package com.mydonghua.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun getSetting(key: String, defaultValue: String): String
    fun getSettingFlow(key: String, defaultValue: String): Flow<String>
    suspend fun putSetting(key: String, value: String)
    suspend fun removeSetting(key: String)
}
