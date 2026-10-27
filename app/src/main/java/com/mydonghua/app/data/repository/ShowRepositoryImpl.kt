package com.mydonghua.app.data.repository

import com.mydonghua.app.data.db.ShowDao
import com.mydonghua.app.data.db.ShowEntity
import com.mydonghua.app.domain.model.Show
import com.mydonghua.app.domain.repository.ShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ShowRepositoryImpl @Inject constructor(
    private val showDao: ShowDao
) : ShowRepository {
    override fun getAllShows(): Flow<List<Show>> = showDao.getAllShows().map { list -> list.map { it.toDomain() } }
    override suspend fun getAllShowsList(): List<Show> = showDao.getAllShowsList().map { it.toDomain() }
    override suspend fun getShowsByStatus(status: String): List<Show> = showDao.getShowsByStatus(status).map { it.toDomain() }
    override suspend fun getFavoriteShows(): List<Show> = showDao.getFavoriteShows().map { it.toDomain() }
    override suspend fun getShowById(id: String): Show? = showDao.getShowById(id)?.toDomain()
    override fun getShowByIdFlow(id: String): Flow<Show?> = showDao.getShowByIdFlow(id).map { it?.toDomain() }
    override suspend fun searchShows(query: String): List<Show> = showDao.searchShows(query).map { it.toDomain() }
    override suspend fun getRecentlyUpdatedShows(limit: Int): List<Show> = showDao.getRecentlyUpdatedShows(limit).map { it.toDomain() }
    override suspend fun getMaxAlarmRequestCode(): Int = showDao.getMaxAlarmRequestCode()

    override suspend fun insertOrUpdateShow(show: Show) {
        val entity = ShowEntity.fromDomain(show)
        var status = entity.status
        if (status == "upcoming") {
            status = "stopped"
        }
        if (status !in listOf("ongoing", "completed", "stopped")) {
            status = "ongoing"
        }

        if (status == "ongoing" && entity.totalEp > 0 && entity.currentEp >= entity.totalEp) {
            status = "completed"
        }

        val migratedEntity = entity.copy(status = status, lastUpdated = System.currentTimeMillis())
        showDao.insertOrUpdateShow(migratedEntity)
    }

    override suspend fun deleteShow(showId: String): Boolean = showDao.deleteShow(showId) > 0

    override suspend fun clearAllShows() = showDao.deleteAllShows()

    override suspend fun bulkInsert(shows: List<Show>) {
        val entities = shows.map { ShowEntity.fromDomain(it) }
        val migratedList = entities.map { entity ->
            var status = entity.status
            if (status == "upcoming") {
                status = "stopped"
            }
            if (status !in listOf("ongoing", "completed", "stopped")) {
                status = "ongoing"
            }

            if (status == "ongoing" && entity.totalEp > 0 && entity.currentEp >= entity.totalEp) {
                status = "completed"
            }
            entity.copy(status = status, lastUpdated = System.currentTimeMillis())
        }
        showDao.bulkInsert(migratedList)
    }
}
