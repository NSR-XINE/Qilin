package com.mydonghua.app.domain.model

data class Show(
    val id: String,
    val title: String,
    val titleZh: String? = null,
    val status: String = "ongoing",
    val releaseDay: String? = "Sunday",
    val releaseTime: String? = "10:00",
    val currentEp: Int = 0,
    val totalEp: Int = 0,
    val poster: String? = null,
    val watchUrl: String? = null,
    val countdownUrl: String? = null,
    val notes: String? = null,
    val isFavorite: Int = 0,
    val rating: Int = 0,
    val lastUpdated: Long? = null,
    val dateAdded: Long? = null,
    val alarmRequestCode: Int? = null,
    val genres: String? = null,
    val studio: String? = null,
    val releaseYear: Int = 0,
    val language: String? = "Chinese",
    val collectionId: String? = null,
    val aniListId: Int? = null
)
