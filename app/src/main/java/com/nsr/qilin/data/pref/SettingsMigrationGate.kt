package com.nsr.qilin.data.pref

import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gate that [LegacySettingsMigrator] opens once the one-time migration from
 * the legacy DataStore / SharedPreferences stores into the Room `settings`
 * table has completed (or was skipped). Settings reads await this gate so
 * screens never observe pre-migration defaults on the first launch after an
 * upgrade. Writes are not gated, so the migrator itself never deadlocks.
 */
@Singleton
class SettingsMigrationGate @Inject constructor() {
    private val migrated = CompletableDeferred<Unit>()

    fun markMigrated() {
        if (!migrated.isCompleted) migrated.complete(Unit)
    }

    suspend fun awaitReady() {
        migrated.await()
    }
}
