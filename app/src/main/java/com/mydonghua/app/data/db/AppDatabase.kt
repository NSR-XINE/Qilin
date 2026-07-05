package com.mydonghua.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ShowEntity::class, WatchHistoryEntity::class, SettingEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
}
