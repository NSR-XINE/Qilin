package com.nsr.qilin.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    suspend fun getSetting(key: String, defaultValue: String): String
    fun getSettingFlow(key: String, defaultValue: String): Flow<String>
    suspend fun putSetting(key: String, value: String)
    suspend fun removeSetting(key: String)
    suspend fun getAllSettings(): Map<String, String>

    suspend fun getBool(key: String, default: Boolean): Boolean =
        getSetting(key, default.toString()) == "true"

    fun getBoolFlow(key: String, default: Boolean): Flow<Boolean> =
        getSettingFlow(key, default.toString()).map { it == "true" }

    suspend fun putBool(key: String, value: Boolean) = putSetting(key, value.toString())
}
