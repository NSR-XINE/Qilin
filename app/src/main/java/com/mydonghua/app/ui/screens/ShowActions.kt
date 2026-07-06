package com.mydonghua.app.ui.screens

import android.content.Context
import android.widget.Toast
import com.mydonghua.app.AlarmScheduler
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.domain.repository.ShowRepository
import com.mydonghua.app.domain.repository.WatchHistoryRepository

object ShowActions {
    suspend fun incrementEpisode(
        show: Show,
        showRepository: ShowRepository,
        watchHistoryRepository: WatchHistoryRepository
    ) {
        val nextEp = show.currentEp + 1
        val updated = show.copy(currentEp = nextEp, lastUpdated = System.currentTimeMillis())
        showRepository.insertOrUpdateShow(updated)
        watchHistoryRepository.addWatchHistory(show.id, nextEp)
    }

    suspend fun decrementEpisode(
        show: Show,
        showRepository: ShowRepository
    ) {
        if (show.currentEp > 0) {
            val prevEp = show.currentEp - 1
            val updated = show.copy(currentEp = prevEp, lastUpdated = System.currentTimeMillis())
            showRepository.insertOrUpdateShow(updated)
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
            val code = Math.abs((show.id + System.currentTimeMillis()).hashCode())
            if (show.status == "ongoing" && !show.releaseDay.isNullOrEmpty() && !show.releaseTime.isNullOrEmpty()) {
                AlarmScheduler.schedule(context, show.id, show.title, show.releaseDay, show.releaseTime, code, show.currentEp)
            }
            Toast.makeText(context, "${show.title} Notifications: Normal", Toast.LENGTH_SHORT).show()
            show.copy(alarmRequestCode = code, lastUpdated = System.currentTimeMillis())
        }
        showRepository.insertOrUpdateShow(updatedShow)
    }
}
