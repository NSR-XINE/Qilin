package com.mydonghua.app.data.repository

import com.mydonghua.app.data.db.WatchHistoryDao
import com.mydonghua.app.data.db.WatchHistoryEntity
import com.mydonghua.app.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WatchHistoryRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao
) : WatchHistoryRepository {
    override fun getWatchHistory(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getWatchHistory()
    override suspend fun getWatchHistoryList(): List<WatchHistoryEntity> = watchHistoryDao.getWatchHistoryList()
    override suspend fun getWatchHistoryLimited(limit: Int): List<WatchHistoryEntity> = watchHistoryDao.getWatchHistoryLimited(limit)

    override suspend fun addWatchHistory(showId: String, episodeNum: Int) {
        watchHistoryDao.insertHistory(
            WatchHistoryEntity(
                showId = showId,
                episodeNum = episodeNum,
                watchedTimestamp = System.currentTimeMillis()
            )
        )
        watchHistoryDao.deleteOldHistoryEntries()
    }

    override suspend fun removeWatchHistory(showId: String, episodeNum: Int) {
        watchHistoryDao.deleteHistoryEntry(showId, episodeNum)
    }
}
