package com.nsr.qilin.ui.screens

import android.content.Context
import android.widget.Toast
import com.nsr.qilin.AlarmScheduler
import com.nsr.qilin.data.service.AniListService
import com.nsr.qilin.domain.model.Show
import com.nsr.qilin.domain.repository.ShowRepository
import com.nsr.qilin.domain.repository.WatchHistoryRepository

object ShowActions {
    suspend fun incrementEpisode(
        show: Show,
        showRepository: ShowRepository,
        watchHistoryRepository: WatchHistoryRepository
    ) {
        val nextEp = show.currentEp + 1
        val clampedEp = if (show.totalEp > 0) nextEp.coerceAtMost(show.totalEp) else nextEp
        if (clampedEp == show.currentEp) return
        val updated = show.copy(currentEp = clampedEp, lastUpdated = System.currentTimeMillis())
        showRepository.insertOrUpdateShow(updated)
        watchHistoryRepository.addWatchHistory(show.id, clampedEp)

        AniListService.syncEpisodeProgress(updated, clampedEp) { linkedShow ->
            showRepository.insertOrUpdateShow(linkedShow)
        }
    }

    suspend fun decrementEpisode(
        show: Show,
        showRepository: ShowRepository,
        watchHistoryRepository: WatchHistoryRepository? = null
    ) {
        if (show.currentEp > 0) {
            val oldEp = show.currentEp
            val prevEp = show.currentEp - 1
            val updated = show.copy(currentEp = prevEp, lastUpdated = System.currentTimeMillis())
            showRepository.insertOrUpdateShow(updated)
            watchHistoryRepository?.removeWatchHistory(show.id, oldEp)

            AniListService.syncEpisodeProgress(updated, prevEp) { linkedShow ->
                showRepository.insertOrUpdateShow(linkedShow)
            }
        }
    }

    suspend fun toggleFavorite(
        show: Show,
        showRepository: ShowRepository
    ) {
        val updated = show.copy(
            isFavorite = if (show.isFavorite == 1) 0 else 1,
            lastUpdated = System.currentTimeMillis()
        )
        showRepository.insertOrUpdateShow(updated)

        if (show.aniListId != null) {
            AniListService.toggleFavorite(show.aniListId)
        }
    }

    suspend fun toggleNotification(
        context: Context,
        show: Show,
        showRepository: ShowRepository
    ) {
        val isAlarmSet = show.alarmRequestCode != null && show.alarmRequestCode!! > 0
        val updatedShow = if (isAlarmSet) {
            AlarmScheduler.cancel(context, show.id, show.alarmRequestCode ?: 0)
            Toast.makeText(context, "${show.title} Notifications: Silent", Toast.LENGTH_SHORT).show()
            show.copy(alarmRequestCode = null, lastUpdated = System.currentTimeMillis())
        } else {
            val canSchedule = show.status == "ongoing" && !show.releaseDay.isNullOrEmpty() && !show.releaseTime.isNullOrEmpty()
            if (canSchedule) {
                val code = (show.id + System.currentTimeMillis()).hashCode() and 0x7FFFFFFF
                AlarmScheduler.schedule(context, show.id, show.title, show.releaseDay, show.releaseTime, code, show.currentEp)
                Toast.makeText(context, "${show.title} Notifications: Normal", Toast.LENGTH_SHORT).show()
                show.copy(alarmRequestCode = code, lastUpdated = System.currentTimeMillis())
            } else {
                Toast.makeText(context, "Notifications need an ongoing show with a set release day/time", Toast.LENGTH_LONG).show()
                show
            }
        }
        showRepository.insertOrUpdateShow(updatedShow)
    }
}
