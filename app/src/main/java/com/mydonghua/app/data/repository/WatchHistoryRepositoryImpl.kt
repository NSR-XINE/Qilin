package com.mydonghua.app.data.repository

import com.mydonghua.app.data.db.ShowDao
import com.mydonghua.app.data.db.WatchHistoryEntity
import com.mydonghua.app.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WatchHistoryRepositoryImpl @Inject constructor(
    private val showDao: ShowDao
) : WatchHistoryRepository {
    override fun getWatchHistory(): Flow<List<WatchHistoryEntity>> = showDao.getWatchHistory()
    override suspend fun getWatchHistoryList(): List<WatchHistoryEntity> = showDao.getWatchHistoryList()
    override suspend fun getWatchHistoryLimited(limit: Int): List<WatchHistoryEntity> = showDao.getWatchHistoryLimited(limit)

    override suspend fun addWatchHistory(showId: String, episodeNum: Int) {
        showDao.insertHistory(
            WatchHistoryEntity(
                showId = showId,
                episodeNum = episodeNum,
                watchedTimestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeWatchHistory(showId: String, episodeNum: Int) {
        showDao.deleteHistoryEntry(showId, episodeNum)
    }
}
