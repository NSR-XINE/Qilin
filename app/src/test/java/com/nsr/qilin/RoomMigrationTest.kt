package com.nsr.qilin

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.nsr.qilin.data.db.AppDatabase
import com.nsr.qilin.data.db.SettingEntity
import com.nsr.qilin.data.db.ShowEntity
import com.nsr.qilin.data.db.WatchHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomMigrationTest {

    companion object {
        private const val TEST_DB = "migration-test.db"

        private val CREATE_SHOWS = """
            CREATE TABLE IF NOT EXISTS `shows` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `title_zh` TEXT,
                `status` TEXT NOT NULL DEFAULT 'ongoing',
                `release_day` TEXT,
                `release_time` TEXT,
                `current_ep` INTEGER NOT NULL DEFAULT 0,
                `total_ep` INTEGER NOT NULL DEFAULT 0,
                `poster` TEXT,
                `watch_url` TEXT,
                `countdown_url` TEXT,
                `notes` TEXT,
                `is_favorite` INTEGER NOT NULL DEFAULT 0,
                `rating` INTEGER NOT NULL DEFAULT 0,
                `last_updated` INTEGER,
                `date_added` INTEGER,
                `alarm_request_code` INTEGER,
                `genres` TEXT,
                `studio` TEXT,
                `release_year` INTEGER NOT NULL DEFAULT 0,
                `language` TEXT DEFAULT 'Chinese',
                `collection_id` TEXT,
                PRIMARY KEY(`id`)
            )
        """.trimIndent()

        private val CREATE_WATCH_HISTORY = """
            CREATE TABLE IF NOT EXISTS `watch_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `show_id` TEXT NOT NULL,
                `episode_num` INTEGER NOT NULL,
                `watched_timestamp` INTEGER NOT NULL,
                FOREIGN KEY(`show_id`) REFERENCES `shows`(`id`) ON DELETE CASCADE
            )
        """.trimIndent()

        private val CREATE_SETTINGS = """
            CREATE TABLE IF NOT EXISTS `settings` (
                `key` TEXT NOT NULL,
                `value` TEXT,
                PRIMARY KEY(`key`)
            )
        """.trimIndent()

        private val CREATE_INDICES = listOf(
            "CREATE INDEX IF NOT EXISTS `idx_shows_status` ON `shows` (`status`)",
            "CREATE INDEX IF NOT EXISTS `idx_shows_favorite` ON `shows` (`is_favorite`)",
            "CREATE INDEX IF NOT EXISTS `idx_shows_release_day` ON `shows` (`release_day`)",
            "CREATE INDEX IF NOT EXISTS `idx_genres` ON `shows` (`genres`)",
            "CREATE INDEX IF NOT EXISTS `idx_collection` ON `shows` (`collection_id`)",
            "CREATE INDEX IF NOT EXISTS `idx_last_updated` ON `shows` (`last_updated`)",
            "CREATE INDEX IF NOT EXISTS `idx_history_show_id` ON `watch_history` (`show_id`)",
            "CREATE INDEX IF NOT EXISTS `idx_history_timestamp` ON `watch_history` (`watched_timestamp`)"
        )
    }

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @After
    fun tearDown() {
        ApplicationProvider.getApplicationContext<Context>()
            .deleteDatabase(TEST_DB)
    }

    @Test
    @Throws(Exception::class)
    fun writeShowAndReadInList() = runBlocking {
        val show = ShowEntity(
            id = "test-show-1",
            title = "Test Donghua",
            titleZh = "测试动画",
            status = "ongoing",
            releaseDay = "Sunday",
            releaseTime = "10:00",
            currentEp = 2,
            totalEp = 12,
            poster = "https://example.com/poster.jpg",
            watchUrl = "https://example.com/watch",
            countdownUrl = "https://example.com/countdown",
            notes = "Test notes",
            isFavorite = 1,
            rating = 9,
            lastUpdated = System.currentTimeMillis(),
            dateAdded = System.currentTimeMillis(),
            alarmRequestCode = 123,
            genres = "Action, Fantasy",
            studio = "Sparkly Key",
            releaseYear = 2026,
            language = "Chinese",
            collectionId = "Test Collection"
        )
        db.showDao().insertOrUpdateShow(show)
        val shows = db.showDao().getAllShows().first()
        assertEquals(1, shows.size)
        assertEquals("test-show-1", shows[0].id)
        assertEquals("Test Donghua", shows[0].title)
        assertEquals("测试动画", shows[0].titleZh)
        assertEquals(1, shows[0].isFavorite)
    }

    @Test
    @Throws(Exception::class)
    fun writeSettingAndRead() = runBlocking {
        val setting = SettingEntity("app_theme", "amoled")
        db.settingsDao().insertSetting(setting)
        val fetched = db.settingsDao().getSetting("app_theme")
        assertNotNull(fetched)
        assertEquals("amoled", fetched?.value)
    }

    @Test
    @Throws(Exception::class)
    fun writeWatchHistoryAndCap() = runBlocking {
        val show = ShowEntity(
            id = "history-show",
            title = "History Show",
            titleZh = "历史动画",
            status = "completed",
            releaseDay = "Monday",
            releaseTime = "12:00",
            currentEp = 12,
            totalEp = 12,
            poster = "",
            watchUrl = "",
            countdownUrl = "",
            notes = "",
            isFavorite = 0,
            rating = 0,
            lastUpdated = System.currentTimeMillis(),
            dateAdded = System.currentTimeMillis(),
            alarmRequestCode = 0,
            genres = "",
            studio = "",
            releaseYear = 2026,
            language = "Chinese",
            collectionId = ""
        )
        db.showDao().insertOrUpdateShow(show)

        for (i in 1..505) {
            val entry = WatchHistoryEntity(
                showId = "history-show",
                episodeNum = i,
                watchedTimestamp = System.currentTimeMillis() + i
            )
            db.watchHistoryDao().insertHistory(entry)
        }

        db.watchHistoryDao().deleteOldHistoryEntries()

        val history = db.watchHistoryDao().getWatchHistoryList()
        assertEquals(500, history.size)
        val episodeNumbers = history.map { it.episodeNum }
        assertFalse(episodeNumbers.contains(1))
        assertFalse(episodeNumbers.contains(5))
        assertTrue(episodeNumbers.contains(6))
        assertTrue(episodeNumbers.contains(505))
    }

    @Test
    @Throws(Exception::class)
    fun migration_1_2_to_3_preserves_shows_and_watch_history() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(CREATE_SHOWS)
            CREATE_INDICES.forEach { db.execSQL(it) }
            db.execSQL(CREATE_WATCH_HISTORY)
            db.execSQL(CREATE_SETTINGS)

            db.execSQL(
                "INSERT INTO shows (id, title, status, current_ep, total_ep, release_year, language) " +
                "VALUES ('s1', 'Survived Show', 'ongoing', 3, 24, 2026, 'Chinese')"
            )
            db.execSQL(
                "INSERT INTO shows (id, title, status, current_ep, total_ep) " +
                "VALUES ('s2', 'Another Show', 'completed', 12, 12)"
            )
            db.execSQL(
                "INSERT INTO watch_history (show_id, episode_num, watched_timestamp) " +
                "VALUES ('s1', 1, 1000)"
            )
            db.execSQL(
                "INSERT INTO watch_history (show_id, episode_num, watched_timestamp) " +
                "VALUES ('s1', 2, 2000)"
            )
            db.execSQL(
                "INSERT INTO settings (`key`, `value`) " +
                "VALUES ('pref_key', 'pref_value')"
            )
        }

        val roomDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        val show1 = roomDb.showDao().getShowById("s1")
        assertNotNull(show1)
        assertEquals("Survived Show", show1!!.title)
        assertEquals(3, show1.currentEp)
        assertEquals("ongoing", show1.status)

        val show2 = roomDb.showDao().getShowById("s2")
        assertNotNull(show2)
        assertEquals("Another Show", show2!!.title)
        assertEquals(12, show2.currentEp)

        val history = roomDb.watchHistoryDao().getWatchHistoryList()
        assertEquals(2, history.size)
        val episodes = history.map { it.episodeNum }
        assertTrue(episodes.contains(1))
        assertTrue(episodes.contains(2))

        val setting = roomDb.settingsDao().getSetting("pref_key")
        assertNotNull(setting)
        assertEquals("pref_value", setting!!.value)

        roomDb.close()
    }
}
