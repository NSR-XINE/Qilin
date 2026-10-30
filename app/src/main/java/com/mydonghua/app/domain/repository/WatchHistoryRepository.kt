package com.mydonghua.app.domain.repository

import com.mydonghua.app.data.db.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>>
    suspend fun getWatchHistoryList(): List<WatchHistoryEntity>
    suspend fun getWatchHistoryLimited(limit: Int): List<WatchHistoryEntity>
    suspend fun addWatchHistory(showId: String, episodeNum: Int, timestamp: Long = System.currentTimeMillis())
    suspend fun bulkInsertHistory(historyList: List<WatchHistoryEntity>)
    suspend fun removeWatchHistory(showId: String, episodeNum: Int)
    suspend fun clearAllWatchHistory()
}
