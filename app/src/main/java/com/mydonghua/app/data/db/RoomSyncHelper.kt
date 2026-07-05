package com.mydonghua.app.data.db

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

object RoomSyncHelper {
    private var dbInstance: AppDatabase? = null

    @Synchronized
    private fun getDb(context: Context): AppDatabase {
        if (dbInstance == null) {
            dbInstance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "mydonghua.db"
            )
            .fallbackToDestructiveMigration()
            .build()
        }
        return dbInstance!!
    }

    @JvmStatic
    fun insertShow(context: Context, json: JSONObject): Boolean = runBlocking {
        try {
            val show = jsonToShowEntity(json)
            getDb(context).showDao().insertOrUpdateShow(show)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @JvmStatic
    fun updateShow(context: Context, json: JSONObject): Boolean = runBlocking {
        insertShow(context, json)
    }

    @JvmStatic
    fun deleteShow(context: Context, id: String): Boolean = runBlocking {
        getDb(context).showDao().deleteShow(id) > 0
    }

    @JvmStatic
    fun getAllShows(context: Context): String = runBlocking {
        val list = getDb(context).showDao().getAllShowsList()
        val arr = JSONArray()
        list.forEach { arr.put(showEntityToJson(it)) }
        arr.toString()
    }

    @JvmStatic
    fun getShowsByStatus(context: Context, status: String): String = runBlocking {
        val list = getDb(context).showDao().getShowsByStatus(status)
        val arr = JSONArray()
        list.forEach { arr.put(showEntityToJson(it)) }
        arr.toString()
    }

    @JvmStatic
    fun getFavoriteShows(context: Context): String = runBlocking {
        val list = getDb(context).showDao().getFavoriteShows()
        val arr = JSONArray()
        list.forEach { arr.put(showEntityToJson(it)) }
        arr.toString()
    }

    @JvmStatic
    fun searchShows(context: Context, query: String): String = runBlocking {
        val list = getDb(context).showDao().searchShows(query)
        val arr = JSONArray()
        list.forEach { arr.put(showEntityToJson(it)) }
        arr.toString()
    }

    @JvmStatic
    fun getRecentlyUpdatedShows(context: Context, limit: Int): String = runBlocking {
        val list = getDb(context).showDao().getRecentlyUpdatedShows(limit)
        val arr = JSONArray()
        list.forEach { arr.put(showEntityToJson(it)) }
        arr.toString()
    }

    @JvmStatic
    fun addWatchHistory(context: Context, showId: String, episodeNum: Int) = runBlocking {
        getDb(context).watchHistoryDao().insertHistory(
            WatchHistoryEntity(
                showId = showId,
                episodeNum = episodeNum,
                watchedTimestamp = System.currentTimeMillis()
            )
        )
    }

    @JvmStatic
    fun getWatchHistory(context: Context, limit: Int): String = runBlocking {
        val list = getDb(context).watchHistoryDao().getWatchHistoryLimited(limit)
        val shows = getDb(context).showDao().getAllShowsList()
        val arr = JSONArray()
        list.forEach { hist ->
            val show = shows.find { it.id == hist.showId }
            val obj = JSONObject().apply {
                put("id", hist.id)
                put("showId", hist.showId)
                put("title", show?.title ?: "")
                put("poster", show?.poster ?: "")
                put("episodeNum", hist.episodeNum)
                put("timestamp", hist.watchedTimestamp)
            }
            arr.put(obj)
        }
        arr.toString()
    }

    @JvmStatic
    fun getStats(context: Context): String = runBlocking {
        val db = getDb(context)
        val shows = db.showDao().getAllShowsList()
        val history = db.watchHistoryDao().getWatchHistoryList()

        val total = shows.size
        val airing = shows.count { it.status == "ongoing" }
        val completed = shows.count { it.status == "completed" }
        val stopped = shows.count { it.status == "stopped" }
        val favorites = shows.count { it.isFavorite == 1 }
        val episodesWatched = shows.sumOf { it.currentEp }
        val historyCount = history.size

        JSONObject().apply {
            put("totalShows", total)
            put("ongoingCount", airing)
            put("completedCount", completed)
            put("stoppedCount", stopped)
            put("totalEpisodes", episodesWatched)
            put("watchHistoryEntries", historyCount)
            put("favorites", favorites)
            put("hoursWatched", (episodesWatched * 20.0) / 60.0) // Estimate hours watched
        }.toString()
    }

    @JvmStatic
    fun bulkInsert(context: Context, jsonArr: JSONArray) = runBlocking {
        val list = mutableListOf<ShowEntity>()
        for (i in 0 until jsonArr.length()) {
            list.add(jsonToShowEntity(jsonArr.getJSONObject(i)))
        }
        getDb(context).showDao().bulkInsert(list)
    }

    @JvmStatic
    fun saveSetting(context: Context, key: String, value: String) = runBlocking {
        getDb(context).settingsDao().insertSetting(SettingEntity(key, value))
    }

    @JvmStatic
    fun getSetting(context: Context, key: String, defaultValue: String): String = runBlocking {
        getDb(context).settingsDao().getSetting(key)?.value ?: defaultValue
    }

    private fun jsonToShowEntity(json: JSONObject): ShowEntity {
        return ShowEntity(
            id = json.getString("id"),
            title = json.getString("title"),
            titleZh = json.optString("titleZh", null).takeIf { it != "null" && it.isNotEmpty() },
            status = json.optString("status", "ongoing"),
            releaseDay = json.optString("releaseDay", "Sunday"),
            releaseTime = json.optString("releaseTime", "10:00"),
            currentEp = json.optInt("currentEp", 0),
            totalEp = json.optInt("totalEp", 0),
            poster = json.optString("poster", null).takeIf { it != "null" && it.isNotEmpty() },
            watchUrl = json.optString("watchUrl", null).takeIf { it != "null" && it.isNotEmpty() },
            countdownUrl = json.optString("countdownUrl", null).takeIf { it != "null" && it.isNotEmpty() },
            notes = json.optString("notes", null).takeIf { it != "null" && it.isNotEmpty() },
            isFavorite = if (json.optBoolean("isFavorite", false)) 1 else json.optInt("isFavorite", 0),
            rating = json.optInt("rating", 0),
            lastUpdated = json.optLong("lastUpdated", System.currentTimeMillis()),
            dateAdded = json.optLong("dateAdded", System.currentTimeMillis()),
            alarmRequestCode = json.optInt("alarmRequestCode", 0),
            genres = json.optString("genres", null).takeIf { it != "null" && it.isNotEmpty() },
            studio = json.optString("studio", null).takeIf { it != "null" && it.isNotEmpty() },
            releaseYear = json.optInt("releaseYear", 0),
            language = json.optString("language", "Chinese"),
            collectionId = json.optString("collection", null).takeIf { it != "null" && it.isNotEmpty() }
        )
    }

    private fun showEntityToJson(show: ShowEntity): JSONObject {
        return JSONObject().apply {
            put("id", show.id)
            put("title", show.title)
            put("titleZh", show.titleZh ?: "")
            put("status", show.status)
            put("releaseDay", show.releaseDay ?: "Sunday")
            put("releaseTime", show.releaseTime ?: "10:00")
            put("currentEp", show.currentEp)
            put("totalEp", show.totalEp)
            put("poster", show.poster ?: "")
            put("watchUrl", show.watchUrl ?: "")
            put("countdownUrl", show.countdownUrl ?: "")
            put("notes", show.notes ?: "")
            put("isFavorite", show.isFavorite == 1)
            put("rating", show.rating)
            put("lastUpdated", show.lastUpdated ?: System.currentTimeMillis())
            put("dateAdded", show.dateAdded ?: System.currentTimeMillis())
            put("alarmRequestCode", show.alarmRequestCode ?: 0)
            put("genres", show.genres ?: "")
            put("studio", show.studio ?: "")
            put("releaseYear", show.releaseYear)
            put("language", show.language ?: "Chinese")
            put("collection", show.collectionId ?: "")
        }
    }
}
