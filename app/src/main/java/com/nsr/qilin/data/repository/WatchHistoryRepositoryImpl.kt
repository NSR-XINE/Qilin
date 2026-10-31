package com.nsr.qilin.data.repository

import com.nsr.qilin.data.db.WatchHistoryDao
import com.nsr.qilin.data.db.WatchHistoryEntity
import com.nsr.qilin.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WatchHistoryRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao
) : WatchHistoryRepository {
    override fun getWatchHistory(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getWatchHistory()
    override suspend fun getWatchHistoryList(): List<WatchHistoryEntity> = watchHistoryDao.getWatchHistoryList()
    override suspend fun getWatchHistoryLimited(limit: Int): List<WatchHistoryEntity> = watchHistoryDao.getWatchHistoryLimited(limit)

    override suspend fun addWatchHistory(showId: String, episodeNum: Int, timestamp: Long) {
        watchHistoryDao.deleteHistoryEntry(showId, episodeNum)
        watchHistoryDao.insertHistory(
            WatchHistoryEntity(
                showId = showId,
                episodeNum = episodeNum,
                watchedTimestamp = timestamp
            )
        )
        watchHistoryDao.deleteOldHistoryEntries()
    }

    override suspend fun bulkInsertHistory(historyList: List<WatchHistoryEntity>) {
        val deduplicated = historyList
            .groupBy { it.showId to it.episodeNum }
            .map { (_, entries) -> entries.maxByOrNull { it.watchedTimestamp }!! }
        for (entry in deduplicated) {
            watchHistoryDao.deleteHistoryEntry(entry.showId, entry.episodeNum)
        }
        watchHistoryDao.bulkInsertHistory(deduplicated)
        watchHistoryDao.deleteOldHistoryEntries()
    }

    override suspend fun removeWatchHistory(showId: String, episodeNum: Int) {
        watchHistoryDao.deleteHistoryEntry(showId, episodeNum)
    }

    override suspend fun clearAllWatchHistory() {
        watchHistoryDao.clearAllWatchHistory()
    }
}
