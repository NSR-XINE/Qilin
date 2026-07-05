package com.mydonghua.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.mydonghua.app.data.db.RoomSyncHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mydonghua.db";
    private static final int DATABASE_VERSION = 3;

    private final Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Ignored since Room controls creation and upgrade schemas
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Ignored since Room controls creation and upgrade schemas
    }

    public synchronized boolean insertShow(JSONObject showJson) {
        return RoomSyncHelper.insertShow(mContext, showJson);
    }

    public synchronized boolean updateShow(JSONObject showJson) {
        return RoomSyncHelper.updateShow(mContext, showJson);
    }

    public synchronized boolean deleteShow(String showId) {
        return RoomSyncHelper.deleteShow(mContext, showId);
    }

    public synchronized String getAllShows() {
        return RoomSyncHelper.getAllShows(mContext);
    }

    public synchronized String getShowsByStatus(String status) {
        return RoomSyncHelper.getShowsByStatus(mContext, status);
    }

    public synchronized String getFavoriteShows() {
        return RoomSyncHelper.getFavoriteShows(mContext);
    }

    public synchronized String searchShows(String query) {
        return RoomSyncHelper.searchShows(mContext, query);
    }

    public synchronized void addWatchHistory(String showId, int episodeNum) {
        RoomSyncHelper.addWatchHistory(mContext, showId, episodeNum);
    }

    public synchronized String getWatchHistory(int limit) {
        return RoomSyncHelper.getWatchHistory(mContext, limit);
    }

    public synchronized String getWatchHistory() {
        return RoomSyncHelper.getWatchHistory(mContext, 200);
    }

    public synchronized String getRecentlyUpdatedShows(int limit) {
        return RoomSyncHelper.getRecentlyUpdatedShows(mContext, limit);
    }

    public synchronized String getStats() {
        return RoomSyncHelper.getStats(mContext);
    }

    public synchronized void bulkInsert(JSONArray showsJson) {
        RoomSyncHelper.bulkInsert(mContext, showsJson);
    }

    public synchronized void saveSetting(String key, String value) {
        RoomSyncHelper.saveSetting(mContext, key, value);
    }

    public synchronized String getSetting(String key, String defaultVal) {
        return RoomSyncHelper.getSetting(mContext, key, defaultVal);
    }
}
