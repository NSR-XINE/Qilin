package com.nsr.qilin.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowDao {
    @Query("SELECT * FROM shows ORDER BY date_added DESC")
    fun getAllShows(): Flow<List<ShowEntity>>

    @Query("SELECT * FROM shows ORDER BY date_added DESC")
    suspend fun getAllShowsList(): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE status = :status ORDER BY date_added DESC")
    suspend fun getShowsByStatus(status: String): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE is_favorite = 1 ORDER BY date_added DESC")
    suspend fun getFavoriteShows(): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE id = :id")
    suspend fun getShowById(id: String): ShowEntity?

    @Query("SELECT * FROM shows WHERE id = :id")
    fun getShowByIdFlow(id: String): Flow<ShowEntity?>

    @Query("SELECT * FROM shows WHERE anilist_id = :aniListId LIMIT 1")
    suspend fun getShowByAniListId(aniListId: Int): ShowEntity?

    @Query("SELECT * FROM shows WHERE title LIKE '%' || :query || '%' OR title_zh LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY CASE WHEN title = :query THEN 0 WHEN title LIKE :query || '%' THEN 1 ELSE 2 END")
    suspend fun searchShows(query: String): List<ShowEntity>

    @Query("SELECT * FROM shows ORDER BY last_updated DESC LIMIT :limit")
    suspend fun getRecentlyUpdatedShows(limit: Int): List<ShowEntity>

    @Query("SELECT COALESCE(MAX(alarm_request_code), 0) FROM shows")
    suspend fun getMaxAlarmRequestCode(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateShow(show: ShowEntity)

    @Query("DELETE FROM shows WHERE id = :showId")
    suspend fun deleteShow(showId: String): Int

    @Query("DELETE FROM shows")
    suspend fun deleteAllShows()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(shows: List<ShowEntity>)
}
