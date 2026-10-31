package com.nsr.qilin.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY watched_timestamp DESC")
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY watched_timestamp DESC")
    suspend fun getWatchHistoryList(): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history ORDER BY watched_timestamp DESC LIMIT :limit")
    suspend fun getWatchHistoryLimited(limit: Int): List<WatchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsertHistory(historyList: List<WatchHistoryEntity>)

    @Query("DELETE FROM watch_history WHERE id NOT IN (SELECT id FROM watch_history ORDER BY watched_timestamp DESC LIMIT 500)")
    suspend fun deleteOldHistoryEntries()

    @Query("DELETE FROM watch_history WHERE show_id = :showId AND episode_num = :episodeNum")
    suspend fun deleteHistoryEntry(showId: String, episodeNum: Int)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllWatchHistory()
}
