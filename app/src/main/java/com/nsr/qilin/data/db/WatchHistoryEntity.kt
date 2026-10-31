package com.nsr.qilin.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_history",
    foreignKeys = [
        ForeignKey(
            entity = ShowEntity::class,
            parentColumns = ["id"],
            childColumns = ["show_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["show_id"], name = "idx_history_show_id"),
        Index(value = ["watched_timestamp"], name = "idx_history_timestamp")
    ]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "show_id")
    val showId: String,
    @ColumnInfo(name = "episode_num")
    val episodeNum: Int,
    @ColumnInfo(name = "watched_timestamp")
    val watchedTimestamp: Long
)
