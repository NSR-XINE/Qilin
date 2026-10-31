package com.nsr.qilin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ShowEntity::class, WatchHistoryEntity::class, SettingEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun settingsDao(): SettingsDao
    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun addColumnIfNotExists(db: SupportSQLiteDatabase, table: String, column: String, typeAndConstraints: String) {
            val cursor = db.query("PRAGMA table_info(`$table`)")
            var exists = false
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex != -1 && cursor.getString(nameIndex) == column) {
                    exists = true
                    break
                }
            }
            cursor.close()
            if (!exists) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $typeAndConstraints")
            }
        }

        val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `shows` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `title_zh` TEXT, `status` TEXT NOT NULL DEFAULT 'ongoing', `release_day` TEXT, `release_time` TEXT, `current_ep` INTEGER NOT NULL DEFAULT 0, `total_ep` INTEGER NOT NULL DEFAULT 0, `poster` TEXT, `watch_url` TEXT, `countdown_url` TEXT, `notes` TEXT, `is_favorite` INTEGER NOT NULL DEFAULT 0, `rating` INTEGER NOT NULL DEFAULT 0, `last_updated` INTEGER, `date_added` INTEGER, `alarm_request_code` INTEGER, `genres` TEXT, `studio` TEXT, `release_year` INTEGER NOT NULL DEFAULT 0, `language` TEXT DEFAULT 'Chinese', `collection_id` TEXT, `anilist_id` INTEGER, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `watch_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `show_id` TEXT NOT NULL, `episode_num` INTEGER NOT NULL, `watched_timestamp` INTEGER NOT NULL, FOREIGN KEY(`show_id`) REFERENCES `shows`(`id`) ON DELETE CASCADE)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL, `value` TEXT, PRIMARY KEY(`key`))")

            addColumnIfNotExists(db, "shows", "last_updated", "INTEGER")
            addColumnIfNotExists(db, "shows", "date_added", "INTEGER")
            addColumnIfNotExists(db, "shows", "alarm_request_code", "INTEGER")
            addColumnIfNotExists(db, "shows", "anilist_id", "INTEGER")

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
            db.execSQL("CREATE TABLE IF NOT EXISTS `shows` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `title_zh` TEXT, `status` TEXT NOT NULL DEFAULT 'ongoing', `release_day` TEXT, `release_time` TEXT, `current_ep` INTEGER NOT NULL DEFAULT 0, `total_ep` INTEGER NOT NULL DEFAULT 0, `poster` TEXT, `watch_url` TEXT, `countdown_url` TEXT, `notes` TEXT, `is_favorite` INTEGER NOT NULL DEFAULT 0, `rating` INTEGER NOT NULL DEFAULT 0, `last_updated` INTEGER, `date_added` INTEGER, `alarm_request_code` INTEGER, `genres` TEXT, `studio` TEXT, `release_year` INTEGER NOT NULL DEFAULT 0, `language` TEXT DEFAULT 'Chinese', `collection_id` TEXT, `anilist_id` INTEGER, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `watch_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `show_id` TEXT NOT NULL, `episode_num` INTEGER NOT NULL, `watched_timestamp` INTEGER NOT NULL, FOREIGN KEY(`show_id`) REFERENCES `shows`(`id`) ON DELETE CASCADE)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL, `value` TEXT, PRIMARY KEY(`key`))")

            addColumnIfNotExists(db, "shows", "genres", "TEXT")
            addColumnIfNotExists(db, "shows", "studio", "TEXT")
            addColumnIfNotExists(db, "shows", "release_year", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "shows", "language", "TEXT DEFAULT 'Chinese'")
            addColumnIfNotExists(db, "shows", "collection_id", "TEXT")
            addColumnIfNotExists(db, "shows", "last_updated", "INTEGER")
            addColumnIfNotExists(db, "shows", "date_added", "INTEGER")
            addColumnIfNotExists(db, "shows", "alarm_request_code", "INTEGER")
            addColumnIfNotExists(db, "shows", "anilist_id", "INTEGER")

            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_shows_status` ON `shows` (`status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_shows_favorite` ON `shows` (`is_favorite`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_shows_release_day` ON `shows` (`release_day`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_genres` ON `shows` (`genres`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_collection` ON `shows` (`collection_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_last_updated` ON `shows` (`last_updated`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_history_show_id` ON `watch_history` (`show_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_history_timestamp` ON `watch_history` (`watched_timestamp`)")
        }

        val MIGRATION_3_4 = Migration(3, 4) { db ->
            addColumnIfNotExists(db, "shows", "anilist_id", "INTEGER")
        }

        const val DATABASE_NAME = "qilin.db"
        const val LEGACY_DATABASE_NAME = "mydonghua.db"

        fun ensureDatabaseMigrated(context: Context) {
            try {
                val oldDb = context.getDatabasePath(LEGACY_DATABASE_NAME)
                val newDb = context.getDatabasePath(DATABASE_NAME)
                if (oldDb.exists() && !newDb.exists()) {
                    newDb.parentFile?.mkdirs()
                    oldDb.renameTo(newDb)
                    val oldWal = java.io.File(oldDb.path + "-wal")
                    val newWal = java.io.File(newDb.path + "-wal")
                    if (oldWal.exists()) oldWal.renameTo(newWal)
                    val oldShm = java.io.File(oldDb.path + "-shm")
                    val newShm = java.io.File(newDb.path + "-shm")
                    if (oldShm.exists()) oldShm.renameTo(newShm)
                }
            } catch (_: Exception) {}
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                ensureDatabaseMigrated(context.applicationContext)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
