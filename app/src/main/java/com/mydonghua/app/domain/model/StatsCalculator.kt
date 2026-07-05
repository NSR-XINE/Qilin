package com.mydonghua.app.domain.model

data class ShowStats(
    val total: Int,
    val airing: Int,
    val completed: Int,
    val stopped: Int,
    val totalEpisodes: Int,
    val totalEpsPossible: Int,
    val favorites: Int,
    val hoursWatched: Int,
    val completionRate: Int,
    val avgRating: String,
    val topGenres: List<Pair<String, Int>>,
    val topStudios: List<Pair<String, Int>>,
    val longestSeries: Show?,
    val longestEpCount: Int,
    val weeklyActivity: Int,
    val monthlyActivity: Int,
    val streak: Int
)

object StatsCalculator {
    fun computeStats(shows: List<Show>): ShowStats {
        val total = shows.size
        val airing = shows.count { it.status == "ongoing" }
        val completed = shows.count { it.status == "completed" }
        val stopped = shows.count { it.status == "stopped" }

        val totalEpisodes = shows.sumOf { it.currentEp }
        val favorites = shows.count { it.isFavorite == 1 }
        val totalEpsPossible = shows.sumOf { it.totalEp }

        val hoursWatched = Math.round((totalEpisodes * 24.0) / 60.0).toInt()
        val completionRate = if (totalEpsPossible > 0) {
            Math.round((totalEpisodes.toDouble() / totalEpsPossible.toDouble()) * 100.0).toInt()
        } else {
            0
        }

        val ratings = shows.filter { it.rating > 0 }
        val avgRating = if (ratings.isNotEmpty()) {
            val sum = ratings.sumOf { it.rating }
            val avg = sum.toDouble() / ratings.size
            String.format("%.1f", avg)
        } else {
            "—"
        }

        val genreCounts = mutableMapOf<String, Int>()
        val studioCounts = mutableMapOf<String, Int>()

        shows.forEach { s ->
            s.genres?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { g ->
                genreCounts[g] = genreCounts.getOrDefault(g, 0) + 1
            }
            s.studio?.let { studio ->
                if (studio.isNotEmpty()) {
                    studioCounts[studio] = studioCounts.getOrDefault(studio, 0) + 1
                }
            }
        }

        val topGenres = genreCounts.entries.sortedByDescending { it.value }.take(5).map { Pair(it.key, it.value) }
        val topStudios = studioCounts.entries.sortedByDescending { it.value }.take(5).map { Pair(it.key, it.value) }

        var longestSeries: Show? = null
        var longestEpCount = 0
        shows.forEach { s ->
            val eps = if (s.totalEp > 0) s.totalEp else if (s.currentEp > 0) s.currentEp else 0
            if (eps > longestEpCount) {
                longestEpCount = eps
                longestSeries = s
            }
        }

        val now = System.currentTimeMillis()
        val weekMs = 7L * 24 * 60 * 60 * 1000
        val monthMs = 30L * 24 * 60 * 60 * 1000

        val weeklyActivity = shows.count { it.lastUpdated != null && (now - (it.lastUpdated ?: 0L)) < weekMs }
        val monthlyActivity = shows.count { it.lastUpdated != null && (now - (it.lastUpdated ?: 0L)) < monthMs }

        val sortedByLastUpdated = shows.filter { it.lastUpdated != null }.sortedByDescending { it.lastUpdated ?: 0L }
        var streak = 0
        if (sortedByLastUpdated.isNotEmpty()) {
            val checkDate = java.util.Calendar.getInstance()
            for (i in 0 until 365) {
                checkDate.set(java.util.Calendar.HOUR_OF_DAY, 0)
                checkDate.set(java.util.Calendar.MINUTE, 0)
                checkDate.set(java.util.Calendar.SECOND, 0)
                checkDate.set(java.util.Calendar.MILLISECOND, 0)
                val dayStart = checkDate.timeInMillis
                val dayEnd = dayStart + 86400000L

                val hasActivity = sortedByLastUpdated.any { (it.lastUpdated ?: 0L) in dayStart until dayEnd }
                if (hasActivity) {
                    streak++
                    checkDate.add(java.util.Calendar.DAY_OF_MONTH, -1)
                } else {
                    break
                }
            }
        }

        return ShowStats(
            total = total,
            airing = airing,
            completed = completed,
            stopped = stopped,
            totalEpisodes = totalEpisodes,
            totalEpsPossible = totalEpsPossible,
            favorites = favorites,
            hoursWatched = hoursWatched,
            completionRate = completionRate,
            avgRating = avgRating,
            topGenres = topGenres,
            topStudios = topStudios,
            longestSeries = longestSeries,
            longestEpCount = longestEpCount,
            weeklyActivity = weeklyActivity,
            monthlyActivity = monthlyActivity,
            streak = streak
        )
    }
}
