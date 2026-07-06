package com.mydonghua.app.domain.model

import java.util.Calendar
import java.util.TimeZone

data class CountdownTime(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val elapsed: Boolean,
    val airingNow: Boolean
)

object CountdownCalculator {

    private val DAYS_MAP = mapOf(
        "Sunday" to Calendar.SUNDAY,
        "Monday" to Calendar.MONDAY,
        "Tuesday" to Calendar.TUESDAY,
        "Wednesday" to Calendar.WEDNESDAY,
        "Thursday" to Calendar.THURSDAY,
        "Friday" to Calendar.FRIDAY,
        "Saturday" to Calendar.SATURDAY
    )

    fun getNextReleaseDateMs(releaseDay: String?, releaseTime: String?): Long? {
        if (releaseDay == null || releaseTime == null || !releaseTime.contains(":")) return null
        val parts = releaseTime.split(":")
        if (parts.size < 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        val targetDayNum = DAYS_MAP[releaseDay] ?: return null

        val tz = TimeZone.getDefault()
        val now = Calendar.getInstance(tz)

        val currentDayNum = now.get(Calendar.DAY_OF_WEEK)
        val daysUntil = (targetDayNum - currentDayNum + 7) % 7

        val target = now.clone() as Calendar
        target.add(Calendar.DAY_OF_MONTH, daysUntil)
        target.set(Calendar.HOUR_OF_DAY, hours)
        target.set(Calendar.MINUTE, minutes)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)

        if (daysUntil == 0 && now.after(target)) {
            val diffMs = now.timeInMillis - target.timeInMillis
            val airDurationMs = 90 * 60 * 1000
            if (diffMs < airDurationMs) {
                return target.timeInMillis
            } else {
                target.add(Calendar.DAY_OF_MONTH, 7)
            }
        }
        return target.timeInMillis
    }

    fun calculateTimeRemaining(targetDateMs: Long?): CountdownTime {
        if (targetDateMs == null) {
            return CountdownTime(0, 0, 0, 0, elapsed = true, airingNow = false)
        }
        val now = System.currentTimeMillis()
        val diffMs = targetDateMs - now

        if (diffMs <= 0) {
            val absDiff = -diffMs
            val airDurationMs = 90 * 60 * 1000
            if (absDiff < airDurationMs) {
                return CountdownTime(0, 0, 0, 0, elapsed = false, airingNow = true)
            }
            return CountdownTime(0, 0, 0, 0, elapsed = true, airingNow = false)
        }

        val seconds = (diffMs / 1000) % 60
        val minutes = (diffMs / (1000 * 60)) % 60
        val hours = (diffMs / (1000 * 60 * 60)) % 24
        val days = diffMs / (1000 * 60 * 60 * 24)

        return CountdownTime(days, hours, minutes, seconds, elapsed = false, airingNow = false)
    }

    fun getNextUpShow(shows: List<Show>): Show? {
        val ongoing = shows.filter { it.status == "ongoing" }
        if (ongoing.isEmpty()) return null

        val mapped = ongoing.mapNotNull { show ->
            val targetMs = getNextReleaseDateMs(show.releaseDay, show.releaseTime) ?: return@mapNotNull null
            val now = System.currentTimeMillis()
            val timeDiff = targetMs - now
            val isAiringNow = timeDiff <= 0 && -timeDiff < 90 * 60 * 1000
            Triple(show, targetMs, isAiringNow)
        }

        if (mapped.isEmpty()) return null

        val sorted = mapped.sortedWith(Comparator { a, b ->
            if (a.third && !b.third) return@Comparator -1
            if (!a.third && b.third) return@Comparator 1
            if (a.third && b.third) {
                return@Comparator b.second.compareTo(a.second)
            }
            val aDiff = a.second - System.currentTimeMillis()
            val bDiff = b.second - System.currentTimeMillis()
            aDiff.compareTo(bDiff)
        })

        return sorted.firstOrNull()?.first
    }
}
