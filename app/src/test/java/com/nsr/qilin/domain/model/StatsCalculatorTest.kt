package com.nsr.qilin.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsCalculatorTest {

    private val now = System.currentTimeMillis()

    private fun show(
        id: String,
        status: String = "ongoing",
        currentEp: Int = 0,
        totalEp: Int = 0,
        rating: Int = 0,
        isFavorite: Int = 0,
        genres: String? = null,
        studio: String? = null,
        lastUpdated: Long? = null
    ) = Show(
        id = id,
        title = id,
        status = status,
        currentEp = currentEp,
        totalEp = totalEp,
        rating = rating,
        isFavorite = isFavorite,
        genres = genres,
        studio = studio,
        lastUpdated = lastUpdated
    )

    @Test
    fun emptyShows_yieldZeroStats() {
        val stats = StatsCalculator.computeStats(emptyList())
        assertEquals(0, stats.total)
        assertEquals(0, stats.airing)
        assertEquals(0, stats.completed)
        assertEquals(0, stats.stopped)
        assertEquals("—", stats.avgRating)
        assertEquals(0, stats.streak)
        assertEquals(0, stats.hoursWatched)
    }

    @Test
    fun mixedShows_produceCorrectAggregates() {
        val dayMs = 24L * 60 * 60 * 1000
        val shows = listOf(
            show(
                id = "a", status = "ongoing", currentEp = 12, totalEp = 24,
                rating = 8, isFavorite = 1, genres = "Action, Fantasy",
                studio = "Studio X", lastUpdated = now
            ),
            show(
                id = "b", status = "completed", currentEp = 24, totalEp = 24,
                rating = 7, genres = "Fantasy", studio = "Studio Y",
                lastUpdated = now - 3 * dayMs
            ),
            show(
                id = "c", status = "stopped", currentEp = 5, totalEp = 0,
                genres = "Action", lastUpdated = now - 40 * dayMs
            ),
            show(id = "d", status = "completed", rating = 9)
        )

        val s = StatsCalculator.computeStats(shows)

        assertEquals(4, s.total)
        assertEquals(1, s.airing)
        assertEquals(2, s.completed)
        assertEquals(1, s.stopped)
        assertEquals(41, s.totalEpisodes)
        assertEquals(1, s.favorites)
        assertEquals(48, s.totalEpsPossible)
        assertEquals(16, s.hoursWatched)
        assertEquals(50, s.completionRate)
        assertEquals("8.0", s.avgRating)
        assertEquals(listOf("Action" to 2, "Fantasy" to 2), s.topGenres)
        assertEquals(listOf("Studio X" to 1, "Studio Y" to 1), s.topStudios)
        assertEquals("a", s.longestSeries?.id)
        assertEquals(24, s.longestEpCount)
        assertEquals(2, s.weeklyActivity)
        assertEquals(2, s.monthlyActivity)
        assertEquals(1, s.streak)
    }

    @Test
    fun unreleasedEpisodeCount_fallsBackToCurrentEp() {
        val shows = listOf(
            show(id = "x", status = "ongoing", currentEp = 6, totalEp = 0)
        )
        val s = StatsCalculator.computeStats(shows)
        assertEquals("x", s.longestSeries?.id)
        assertEquals(6, s.longestEpCount)
    }
}
