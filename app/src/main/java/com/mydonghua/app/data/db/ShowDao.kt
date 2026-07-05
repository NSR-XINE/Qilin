package com.mydonghua.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowDao {
    @Query("SELECT * FROM shows")
    fun getAllShows(): Flow<List<ShowEntity>>

    @Query("SELECT * FROM shows")
    suspend fun getAllShowsList(): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE status = :status")
    suspend fun getShowsByStatus(status: String): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE is_favorite = 1")
    suspend fun getFavoriteShows(): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE id = :id")
    suspend fun getShowById(id: String): ShowEntity?

    @Query("SELECT * FROM shows WHERE id = :id")
    fun getShowByIdFlow(id: String): Flow<ShowEntity?>

    @Query("SELECT * FROM shows WHERE title LIKE '%' || :query || '%' OR title_zh LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'")
    suspend fun searchShows(query: String): List<ShowEntity>

    @Query("SELECT * FROM shows ORDER BY last_updated DESC LIMIT :limit")
    suspend fun getRecentlyUpdatedShows(limit: Int): List<ShowEntity>

    @Query("SELECT COALESCE(MAX(alarm_request_code), 0) FROM shows")
    suspend fun getMaxAlarmRequestCode(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateShow(show: ShowEntity)

    @Query("DELETE FROM shows WHERE id = :showId")
    suspend fun deleteShow(showId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(shows: List<ShowEntity>)

    // Settings
    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): SettingEntity?

    @Query("SELECT * FROM settings WHERE `key` = :key")
    fun getSettingFlow(key: String): Flow<SettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)

    // Watch History
    @Query("SELECT * FROM watch_history ORDER BY watched_timestamp DESC")
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY watched_timestamp DESC")
    suspend fun getWatchHistoryList(): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history ORDER BY watched_timestamp DESC LIMIT :limit")
    suspend fun getWatchHistoryLimited(limit: Int): List<WatchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE show_id = :showId AND episode_num = :episodeNum")
    suspend fun deleteHistoryEntry(showId: String, episodeNum: Int)
}
