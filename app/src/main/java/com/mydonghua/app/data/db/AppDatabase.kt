package com.mydonghua.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ShowEntity::class, WatchHistoryEntity::class, SettingEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun settingsDao(): SettingsDao
    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `shows` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `title_zh` TEXT, `status` TEXT NOT NULL DEFAULT 'ongoing', `release_day` TEXT, `release_time` TEXT, `current_ep` INTEGER NOT NULL DEFAULT 0, `total_ep` INTEGER NOT NULL DEFAULT 0, `poster` TEXT, `watch_url` TEXT, `countdown_url` TEXT, `notes` TEXT, `is_favorite` INTEGER NOT NULL DEFAULT 0, `rating` INTEGER NOT NULL DEFAULT 0, `last_updated` INTEGER, `date_added` INTEGER, `alarm_request_code` INTEGER, `genres` TEXT, `studio` TEXT, `release_year` INTEGER NOT NULL DEFAULT 0, `language` TEXT DEFAULT 'Chinese', `collection_id` TEXT, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `watch_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `show_id` TEXT NOT NULL, `episode_num` INTEGER NOT NULL, `watched_timestamp` INTEGER NOT NULL, FOREIGN KEY(`show_id`) REFERENCES `shows`(`id`) ON DELETE CASCADE)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL, `value` TEXT, PRIMARY KEY(`key`))")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_shows_status` ON `shows` (`status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_shows_favorite` ON `shows` (`is_favorite`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_shows_release_day` ON `shows` (`release_day`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_genres` ON `shows` (`genres`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_collection` ON `shows` (`collection_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_last_updated` ON `shows` (`last_updated`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_history_show_id` ON `watch_history` (`show_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_history_timestamp` ON `watch_history` (`watched_timestamp`)")
        }

        val MIGRATION_2_3 = Migration(2, 3) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `shows` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `title_zh` TEXT, `status` TEXT NOT NULL DEFAULT 'ongoing', `release_day` TEXT, `release_time` TEXT, `current_ep` INTEGER NOT NULL DEFAULT 0, `total_ep` INTEGER NOT NULL DEFAULT 0, `poster` TEXT, `watch_url` TEXT, `countdown_url` TEXT, `notes` TEXT, `is_favorite` INTEGER NOT NULL DEFAULT 0, `rating` INTEGER NOT NULL DEFAULT 0, `last_updated` INTEGER, `date_added` INTEGER, `alarm_request_code` INTEGER, `genres` TEXT, `studio` TEXT, `release_year` INTEGER NOT NULL DEFAULT 0, `language` TEXT DEFAULT 'Chinese', `collection_id` TEXT, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `watch_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `show_id` TEXT NOT NULL, `episode_num` INTEGER NOT NULL, `watched_timestamp` INTEGER NOT NULL, FOREIGN KEY(`show_id`) REFERENCES `shows`(`id`) ON DELETE CASCADE)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL, `value` TEXT, PRIMARY KEY(`key`))")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_shows_status` ON `shows` (`status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_shows_favorite` ON `shows` (`is_favorite`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_shows_release_day` ON `shows` (`release_day`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_genres` ON `shows` (`genres`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_collection` ON `shows` (`collection_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_last_updated` ON `shows` (`last_updated`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_history_show_id` ON `watch_history` (`show_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_history_timestamp` ON `watch_history` (`watched_timestamp`)")
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mydonghua.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
