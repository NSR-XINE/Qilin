package com.mydonghua.app.domain.repository

import com.mydonghua.app.domain.model.Show
import kotlinx.coroutines.flow.Flow

interface ShowRepository {
    fun getAllShows(): Flow<List<Show>>
    suspend fun getAllShowsList(): List<Show>
    suspend fun getShowsByStatus(status: String): List<Show>
    suspend fun getFavoriteShows(): List<Show>
    suspend fun getShowById(id: String): Show?
    suspend fun getShowByAniListId(aniListId: Int): Show?
    fun getShowByIdFlow(id: String): Flow<Show?>
    suspend fun searchShows(query: String): List<Show>
    suspend fun getRecentlyUpdatedShows(limit: Int): List<Show>
    suspend fun getMaxAlarmRequestCode(): Int
    suspend fun insertOrUpdateShow(show: Show)
    suspend fun deleteShow(showId: String): Boolean
    suspend fun clearAllShows()
    suspend fun bulkInsert(shows: List<Show>)
}
