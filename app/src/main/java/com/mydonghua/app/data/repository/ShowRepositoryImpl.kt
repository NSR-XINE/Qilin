package com.mydonghua.app.data.repository

import com.mydonghua.app.data.db.ShowDao
import com.mydonghua.app.data.db.ShowEntity
import com.mydonghua.app.domain.repository.ShowRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ShowRepositoryImpl @Inject constructor(
    private val showDao: ShowDao
) : ShowRepository {
    override fun getAllShows(): Flow<List<ShowEntity>> = showDao.getAllShows()
    override suspend fun getAllShowsList(): List<ShowEntity> = showDao.getAllShowsList()
    override suspend fun getShowsByStatus(status: String): List<ShowEntity> = showDao.getShowsByStatus(status)
    override suspend fun getFavoriteShows(): List<ShowEntity> = showDao.getFavoriteShows()
    override suspend fun getShowById(id: String): ShowEntity? = showDao.getShowById(id)
    override fun getShowByIdFlow(id: String): Flow<ShowEntity?> = showDao.getShowByIdFlow(id)
    override suspend fun searchShows(query: String): List<ShowEntity> = showDao.searchShows(query)
    override suspend fun getRecentlyUpdatedShows(limit: Int): List<ShowEntity> = showDao.getRecentlyUpdatedShows(limit)
    override suspend fun getMaxAlarmRequestCode(): Int = showDao.getMaxAlarmRequestCode()
    override suspend fun insertOrUpdateShow(show: ShowEntity) {
        var status = show.status
        if (status == "upcoming") {
            status = "stopped"
        }
        if (status !in listOf("ongoing", "completed", "stopped")) {
            status = "ongoing"
        }

        // Auto-complete rules
        if (status == "ongoing" && show.totalEp > 0 && show.currentEp >= show.totalEp) {
            status = "completed"
        } else if (status == "completed" && (show.totalEp == 0 || show.currentEp < show.totalEp)) {
            status = "ongoing"
        }

        val migratedShow = show.copy(status = status, lastUpdated = System.currentTimeMillis())
        showDao.insertOrUpdateShow(migratedShow)
    }

    override suspend fun deleteShow(showId: String): Boolean = showDao.deleteShow(showId) > 0

    override suspend fun bulkInsert(shows: List<ShowEntity>) {
        val migratedList = shows.map { show ->
            var status = show.status
            if (status == "upcoming") {
                status = "stopped"
            }
            if (status !in listOf("ongoing", "completed", "stopped")) {
                status = "ongoing"
            }

            // Auto-complete rules
            if (status == "ongoing" && show.totalEp > 0 && show.currentEp >= show.totalEp) {
                status = "completed"
            } else if (status == "completed" && (show.totalEp == 0 || show.currentEp < show.totalEp)) {
                status = "ongoing"
            }
            show.copy(status = status, lastUpdated = System.currentTimeMillis())
        }
        showDao.bulkInsert(migratedList)
    }
}
