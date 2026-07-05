package com.mydonghua.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mydonghua.app.data.db.AppDatabase
import com.mydonghua.app.data.db.SettingEntity
import com.mydonghua.app.data.db.ShowEntity
import com.mydonghua.app.data.db.WatchHistoryEntity
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

        // Insert 505 entries
        for (i in 1..505) {
            val entry = WatchHistoryEntity(
                showId = "history-show",
                episodeNum = i,
                watchedTimestamp = System.currentTimeMillis() + i
            )
            db.watchHistoryDao().insertHistory(entry)
        }

        // Delete old history entries to enforce 500 cap
        db.watchHistoryDao().deleteOldHistoryEntries()

        val history = db.watchHistoryDao().getWatchHistoryList()
        assertEquals(500, history.size)
        // Verify the oldest 5 entries (episodeNum 1 to 5) were removed, so minimum is 6
        val episodeNumbers = history.map { it.episodeNum }
        assertFalse(episodeNumbers.contains(1))
        assertFalse(episodeNumbers.contains(5))
        assertTrue(episodeNumbers.contains(6))
        assertTrue(episodeNumbers.contains(505))
    }
}
