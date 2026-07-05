package com.mydonghua.app.domain.repository

import com.mydonghua.app.data.db.ShowEntity
import kotlinx.coroutines.flow.Flow

interface ShowRepository {
    fun getAllShows(): Flow<List<ShowEntity>>
    suspend fun getAllShowsList(): List<ShowEntity>
    suspend fun getShowsByStatus(status: String): List<ShowEntity>
    suspend fun getFavoriteShows(): List<ShowEntity>
    suspend fun getShowById(id: String): ShowEntity?
    fun getShowByIdFlow(id: String): Flow<ShowEntity?>
    suspend fun searchShows(query: String): List<ShowEntity>
    suspend fun getRecentlyUpdatedShows(limit: Int): List<ShowEntity>
    suspend fun getMaxAlarmRequestCode(): Int
    suspend fun insertOrUpdateShow(show: ShowEntity)
    suspend fun deleteShow(showId: String): Boolean
    suspend fun bulkInsert(shows: List<ShowEntity>)
}
