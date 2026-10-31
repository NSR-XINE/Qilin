package com.nsr.qilin.domain.model

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
    fun computeStats(shows: List<Show>, historyTimestamps: List<Long> = emptyList()): ShowStats {
        val total = shows.size
        val airing = shows.count { it.status == "ongoing" }
        val completed = shows.count { it.status == "completed" }
        val stopped = shows.count { it.status == "stopped" }

        val totalEpisodes = shows.sumOf { it.currentEp }
        val favorites = shows.count { it.isFavorite == 1 }
        val totalEpsPossible = shows.sumOf { it.totalEp }

        val hoursWatched = Math.round((totalEpisodes * 24.0) / 60.0).toInt()
        val completionRate = if (total > 0) {
            Math.round((completed.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val ratings = shows.filter { it.rating > 0 }
        val avgRating = if (ratings.isNotEmpty()) {
            val sum = ratings.sumOf { it.rating }
            val avg = sum.toDouble() / ratings.size
            String.format(java.util.Locale.US, "%.1f", avg)
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

        val timestamps = if (historyTimestamps.isNotEmpty()) {
            historyTimestamps.sortedDescending()
        } else {
            shows.mapNotNull { it.lastUpdated }.sortedDescending()
        }

        val weeklyActivity = if (historyTimestamps.isNotEmpty()) {
            timestamps.count { (now - it) < weekMs }
        } else {
            shows.count { it.lastUpdated != null && (now - (it.lastUpdated ?: 0L)) < weekMs }
        }
        val monthlyActivity = if (historyTimestamps.isNotEmpty()) {
            timestamps.count { (now - it) < monthMs }
        } else {
            shows.count { it.lastUpdated != null && (now - (it.lastUpdated ?: 0L)) < monthMs }
        }

        var streak = 0
        if (timestamps.isNotEmpty()) {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            fun getDayRange(c: java.util.Calendar): Pair<Long, Long> {
                val start = c.timeInMillis
                val next = (c.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_MONTH, 1) }
                return Pair(start, next.timeInMillis)
            }

            val (todayStart, todayEnd) = getDayRange(cal)
            val hasTodayActivity = timestamps.any { it in todayStart until todayEnd }
            if (!hasTodayActivity) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
            }

            for (i in 0 until 365) {
                val (dayStart, dayEnd) = getDayRange(cal)
                val hasActivity = timestamps.any { it in dayStart until dayEnd }
                if (hasActivity) {
                    streak++
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
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
