package com.mydonghua.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shows",
    indices = [
        Index(value = ["status"], name = "idx_shows_status"),
        Index(value = ["is_favorite"], name = "idx_shows_favorite"),
        Index(value = ["release_day"], name = "idx_shows_release_day"),
        Index(value = ["genres"], name = "idx_genres"),
        Index(value = ["collection_id"], name = "idx_collection"),
        Index(value = ["last_updated"], name = "idx_last_updated")
    ]
)
data class ShowEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    @ColumnInfo(name = "title_zh")
    val titleZh: String?,
    val status: String = "ongoing",
    @ColumnInfo(name = "release_day")
    val releaseDay: String?,
    @ColumnInfo(name = "release_time")
    val releaseTime: String?,
    @ColumnInfo(name = "current_ep")
    val currentEp: Int = 0,
    @ColumnInfo(name = "total_ep")
    val totalEp: Int = 0,
    val poster: String?,
    @ColumnInfo(name = "watch_url")
    val watchUrl: String?,
    @ColumnInfo(name = "countdown_url")
    val countdownUrl: String?,
    val notes: String?,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Int = 0,
    val rating: Int = 0,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long?,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long?,
    @ColumnInfo(name = "alarm_request_code")
    val alarmRequestCode: Int?,
    val genres: String?,
    val studio: String?,
    @ColumnInfo(name = "release_year")
    val releaseYear: Int = 0,
    val language: String? = "Chinese",
    @ColumnInfo(name = "collection_id")
    val collectionId: String?
) {
    fun toDomain(): com.mydonghua.app.domain.model.Show = com.mydonghua.app.domain.model.Show(
        id = id,
        title = title,
        titleZh = titleZh,
        status = status,
        releaseDay = releaseDay,
        releaseTime = releaseTime,
        currentEp = currentEp,
        totalEp = totalEp,
        poster = poster,
        watchUrl = watchUrl,
        countdownUrl = countdownUrl,
        notes = notes,
        isFavorite = isFavorite,
        rating = rating,
        lastUpdated = lastUpdated,
        dateAdded = dateAdded,
        alarmRequestCode = alarmRequestCode,
        genres = genres,
        studio = studio,
        releaseYear = releaseYear,
        language = language,
        collectionId = collectionId
    )

    companion object {
        fun fromDomain(show: com.mydonghua.app.domain.model.Show): ShowEntity = ShowEntity(
            id = show.id,
            title = show.title,
            titleZh = show.titleZh,
            status = show.status,
            releaseDay = show.releaseDay,
            releaseTime = show.releaseTime,
            currentEp = show.currentEp,
            totalEp = show.totalEp,
            poster = show.poster,
            watchUrl = show.watchUrl,
            countdownUrl = show.countdownUrl,
            notes = show.notes,
            isFavorite = show.isFavorite,
            rating = show.rating,
            lastUpdated = show.lastUpdated,
            dateAdded = show.dateAdded,
            alarmRequestCode = show.alarmRequestCode,
            genres = show.genres,
            studio = show.studio,
            releaseYear = show.releaseYear,
            language = show.language,
            collectionId = show.collectionId
        )
    }
}
