package com.mydonghua.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mydonghua.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_SHOWS = "shows";
    public static final String TABLE_HISTORY = "watch_history";
    public static final String TABLE_SETTINGS = "settings";

    public static final String COL_SHOW_ID = "id";
    public static final String COL_SHOW_TITLE = "title";
    public static final String COL_SHOW_TITLE_ZH = "title_zh";
    public static final String COL_SHOW_STATUS = "status";
    public static final String COL_SHOW_RELEASE_DAY = "release_day";
    public static final String COL_SHOW_RELEASE_TIME = "release_time";
    public static final String COL_SHOW_CURRENT_EP = "current_ep";
    public static final String COL_SHOW_TOTAL_EP = "total_ep";
    public static final String COL_SHOW_POSTER = "poster";
    public static final String COL_SHOW_WATCH_URL = "watch_url";
    public static final String COL_SHOW_COUNTDOWN_URL = "countdown_url";
    public static final String COL_SHOW_NOTES = "notes";
    public static final String COL_SHOW_IS_FAVORITE = "is_favorite";
    public static final String COL_SHOW_RATING = "rating";
    public static final String COL_SHOW_LAST_UPDATED = "last_updated";
    public static final String COL_SHOW_DATE_ADDED = "date_added";
    public static final String COL_SHOW_ALARM_CODE = "alarm_request_code";
    public static final String COL_SHOW_GENRES = "genres";
    public static final String COL_SHOW_STUDIO = "studio";
    public static final String COL_SHOW_RELEASE_YEAR = "release_year";
    public static final String COL_SHOW_LANGUAGE = "language";
    public static final String COL_SHOW_COLLECTION = "collection_id";

    public static final String COL_HIST_ID = "id";
    public static final String COL_HIST_SHOW_ID = "show_id";
    public static final String COL_HIST_EPISODE = "episode_num";
    public static final String COL_HIST_TIMESTAMP = "watched_timestamp";

    public static final String COL_SET_KEY = "key";
    public static final String COL_SET_VAL = "value";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createShowsTable = "CREATE TABLE " + TABLE_SHOWS + " (" +
                COL_SHOW_ID + " TEXT PRIMARY KEY, " +
                COL_SHOW_TITLE + " TEXT NOT NULL, " +
                COL_SHOW_TITLE_ZH + " TEXT, " +
                COL_SHOW_STATUS + " TEXT NOT NULL DEFAULT 'ongoing', " +
                COL_SHOW_RELEASE_DAY + " TEXT, " +
                COL_SHOW_RELEASE_TIME + " TEXT, " +
                COL_SHOW_CURRENT_EP + " INTEGER DEFAULT 0, " +
                COL_SHOW_TOTAL_EP + " INTEGER DEFAULT 0, " +
                COL_SHOW_POSTER + " TEXT, " +
                COL_SHOW_WATCH_URL + " TEXT, " +
                COL_SHOW_COUNTDOWN_URL + " TEXT, " +
                COL_SHOW_NOTES + " TEXT, " +
                COL_SHOW_IS_FAVORITE + " INTEGER DEFAULT 0, " +
                COL_SHOW_RATING + " INTEGER DEFAULT 0, " +
                COL_SHOW_LAST_UPDATED + " INTEGER, " +
                COL_SHOW_DATE_ADDED + " INTEGER, " +
                COL_SHOW_ALARM_CODE + " INTEGER, " +
                COL_SHOW_GENRES + " TEXT, " +
                COL_SHOW_STUDIO + " TEXT, " +
                COL_SHOW_RELEASE_YEAR + " INTEGER DEFAULT 0, " +
                COL_SHOW_LANGUAGE + " TEXT DEFAULT 'Chinese', " +
                COL_SHOW_COLLECTION + " TEXT" +
                ")";
        db.execSQL(createShowsTable);

        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COL_HIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HIST_SHOW_ID + " TEXT NOT NULL, " +
                COL_HIST_EPISODE + " INTEGER, " +
                COL_HIST_TIMESTAMP + " INTEGER, " +
                "FOREIGN KEY (" + COL_HIST_SHOW_ID + ") REFERENCES " + TABLE_SHOWS + "(" + COL_SHOW_ID + ") ON DELETE CASCADE" +
                ")";
        db.execSQL(createHistoryTable);

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_show_id ON " + TABLE_HISTORY + " (" + COL_HIST_SHOW_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_timestamp ON " + TABLE_HISTORY + " (" + COL_HIST_TIMESTAMP + " DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_status ON " + TABLE_SHOWS + " (" + COL_SHOW_STATUS + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_favorite ON " + TABLE_SHOWS + " (" + COL_SHOW_IS_FAVORITE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_release_day ON " + TABLE_SHOWS + " (" + COL_SHOW_RELEASE_DAY + ")");

        String createSettingsTable = "CREATE TABLE " + TABLE_SETTINGS + " (" +
                COL_SET_KEY + " TEXT PRIMARY KEY, " +
                COL_SET_VAL + " TEXT" +
                ")";
        db.execSQL(createSettingsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int version = oldVersion; version < newVersion; version++) {
            switch (version) {
                case 1:
                    try {
                        db.execSQL("ALTER TABLE " + TABLE_SHOWS + " ADD COLUMN " + COL_SHOW_GENRES + " TEXT DEFAULT ''");
                        db.execSQL("ALTER TABLE " + TABLE_SHOWS + " ADD COLUMN " + COL_SHOW_STUDIO + " TEXT DEFAULT ''");
                        db.execSQL("ALTER TABLE " + TABLE_SHOWS + " ADD COLUMN " + COL_SHOW_RELEASE_YEAR + " INTEGER DEFAULT 0");
                        db.execSQL("ALTER TABLE " + TABLE_SHOWS + " ADD COLUMN " + COL_SHOW_LANGUAGE + " TEXT DEFAULT 'Chinese'");
                        db.execSQL("ALTER TABLE " + TABLE_SHOWS + " ADD COLUMN " + COL_SHOW_COLLECTION + " TEXT DEFAULT ''");
                    } catch (Exception ignored) {}
                    try {
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_timestamp ON " + TABLE_HISTORY + " (" + COL_HIST_TIMESTAMP + " DESC)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_status ON " + TABLE_SHOWS + " (" + COL_SHOW_STATUS + ")");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_favorite ON " + TABLE_SHOWS + " (" + COL_SHOW_IS_FAVORITE + ")");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_release_day ON " + TABLE_SHOWS + " (" + COL_SHOW_RELEASE_DAY + ")");
                    } catch (Exception ignored) {}
                    try {
                        db.execSQL("DELETE FROM " + TABLE_HISTORY + " WHERE " + COL_HIST_SHOW_ID + " NOT IN (SELECT " + COL_SHOW_ID + " FROM " + TABLE_SHOWS + ")");
                        db.execSQL("UPDATE " + TABLE_SHOWS + " SET " + COL_SHOW_STATUS + " = 'ongoing' WHERE " + COL_SHOW_STATUS + " = 'upcoming'");
                    } catch (Exception ignored) {}
                    break;
                case 2:
                    try {
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_genres ON " + TABLE_SHOWS + " (" + COL_SHOW_GENRES + ")");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_collection ON " + TABLE_SHOWS + " (" + COL_SHOW_COLLECTION + ")");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_last_updated ON " + TABLE_SHOWS + " (" + COL_SHOW_LAST_UPDATED + ")");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_release_day ON " + TABLE_SHOWS + " (" + COL_SHOW_RELEASE_DAY + ")");
                    } catch (Exception ignored) {}
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHOWS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    public synchronized boolean insertShow(JSONObject showJson) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        try {
            String showId = showJson.getString("id");
            values.put(COL_SHOW_ID, showId);
            values.put(COL_SHOW_TITLE, showJson.getString("title"));
            values.put(COL_SHOW_TITLE_ZH, showJson.optString("titleZh", ""));
            values.put(COL_SHOW_STATUS, showJson.optString("status", "ongoing"));
            values.put(COL_SHOW_RELEASE_DAY, showJson.optString("releaseDay", "Sunday"));
            values.put(COL_SHOW_RELEASE_TIME, showJson.optString("releaseTime", "10:00"));
            values.put(COL_SHOW_CURRENT_EP, showJson.optInt("currentEp", 0));
            values.put(COL_SHOW_TOTAL_EP, showJson.optInt("totalEp", 0));
            values.put(COL_SHOW_POSTER, showJson.optString("poster", ""));
            values.put(COL_SHOW_WATCH_URL, showJson.optString("watchUrl", ""));
            values.put(COL_SHOW_COUNTDOWN_URL, showJson.optString("countdownUrl", ""));
            values.put(COL_SHOW_NOTES, showJson.optString("notes", ""));
            values.put(COL_SHOW_IS_FAVORITE, showJson.optBoolean("isFavorite", false) ? 1 : 0);
            values.put(COL_SHOW_RATING, showJson.optInt("rating", 0));
            values.put(COL_SHOW_LAST_UPDATED, showJson.optLong("lastUpdated", System.currentTimeMillis()));
            values.put(COL_SHOW_DATE_ADDED, showJson.optLong("dateAdded", System.currentTimeMillis()));
            values.put(COL_SHOW_GENRES, showJson.optString("genres", ""));
            values.put(COL_SHOW_STUDIO, showJson.optString("studio", ""));
            values.put(COL_SHOW_RELEASE_YEAR, showJson.optInt("releaseYear", 0));
            values.put(COL_SHOW_LANGUAGE, showJson.optString("language", "Chinese"));
            values.put(COL_SHOW_COLLECTION, showJson.optString("collection", ""));

            int alarmCode = showJson.optInt("alarmRequestCode", -1);
            if (alarmCode == -1) {
                Cursor cursor = null;
                try {
                    cursor = db.rawQuery("SELECT COALESCE(MAX(" + COL_SHOW_ALARM_CODE + "), 0) FROM " + TABLE_SHOWS, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        alarmCode = cursor.getInt(0) + 1;
                    } else {
                        alarmCode = 1;
                    }
                } catch (Exception e) {
                    alarmCode = 1;
                } finally {
                    if (cursor != null) cursor.close();
                }
            }
            values.put(COL_SHOW_ALARM_CODE, alarmCode);

            long result = db.insertWithOnConflict(TABLE_SHOWS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (result == -1) {
                result = db.update(TABLE_SHOWS, values, COL_SHOW_ID + " = ?", new String[]{showId});
                return result > 0;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean updateShow(JSONObject showJson) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        try {
            String id = showJson.getString("id");
            values.put(COL_SHOW_TITLE, showJson.getString("title"));
            values.put(COL_SHOW_TITLE_ZH, showJson.optString("titleZh", ""));
            values.put(COL_SHOW_STATUS, showJson.optString("status", "ongoing"));
            values.put(COL_SHOW_RELEASE_DAY, showJson.optString("releaseDay", "Sunday"));
            values.put(COL_SHOW_RELEASE_TIME, showJson.optString("releaseTime", "10:00"));
            values.put(COL_SHOW_CURRENT_EP, showJson.optInt("currentEp", 0));
            values.put(COL_SHOW_TOTAL_EP, showJson.optInt("totalEp", 0));
            values.put(COL_SHOW_POSTER, showJson.optString("poster", ""));
            values.put(COL_SHOW_WATCH_URL, showJson.optString("watchUrl", ""));
            values.put(COL_SHOW_COUNTDOWN_URL, showJson.optString("countdownUrl", ""));
            values.put(COL_SHOW_NOTES, showJson.optString("notes", ""));
            values.put(COL_SHOW_IS_FAVORITE, showJson.optBoolean("isFavorite", false) ? 1 : 0);
            values.put(COL_SHOW_RATING, showJson.optInt("rating", 0));
            values.put(COL_SHOW_LAST_UPDATED, showJson.optLong("lastUpdated", System.currentTimeMillis()));
            values.put(COL_SHOW_GENRES, showJson.optString("genres", ""));
            values.put(COL_SHOW_STUDIO, showJson.optString("studio", ""));
            values.put(COL_SHOW_RELEASE_YEAR, showJson.optInt("releaseYear", 0));
            values.put(COL_SHOW_LANGUAGE, showJson.optString("language", "Chinese"));
            values.put(COL_SHOW_COLLECTION, showJson.optString("collection", ""));

            if (showJson.has("alarmRequestCode")) {
                values.put(COL_SHOW_ALARM_CODE, showJson.getInt("alarmRequestCode"));
            }

            int result = db.update(TABLE_SHOWS, values, COL_SHOW_ID + " = ?", new String[]{id});
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean deleteShow(String showId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_SHOWS, COL_SHOW_ID + " = ?", new String[]{showId});
        db.delete(TABLE_HISTORY, COL_HIST_SHOW_ID + " = ?", new String[]{showId});
        return result > 0;
    }

    public synchronized String getAllShows() {
        JSONArray arr = new JSONArray();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_SHOWS + " ORDER BY " + COL_SHOW_DATE_ADDED + " DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    arr.put(cursorToJson(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return arr.toString();
    }

    public synchronized String getShowsByStatus(String status) {
        JSONArray arr = new JSONArray();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SHOWS, null, COL_SHOW_STATUS + " = ?", new String[]{status}, null, null, COL_SHOW_DATE_ADDED + " DESC");
            if (cursor.moveToFirst()) {
                do {
                    arr.put(cursorToJson(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return arr.toString();
    }

    public synchronized String getFavoriteShows() {
        JSONArray arr = new JSONArray();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SHOWS, null, COL_SHOW_IS_FAVORITE + " = 1", null, null, null, COL_SHOW_DATE_ADDED + " DESC");
            if (cursor.moveToFirst()) {
                do {
                    arr.put(cursorToJson(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return arr.toString();
    }

    public synchronized String searchShows(String query) {
        JSONArray arr = new JSONArray();
        if (query == null || query.trim().isEmpty()) {
            return arr.toString();
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sanitized = query.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff\\s]", "");
            if (sanitized.isEmpty()) {
                return arr.toString();
            }
            if (sanitized.length() > 100) {
                sanitized = sanitized.substring(0, 100);
            }
            String like = "%" + sanitized + "%";
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_SHOWS +
                    " WHERE " + COL_SHOW_TITLE + " LIKE ? OR " + COL_SHOW_TITLE_ZH + " LIKE ? OR " + COL_SHOW_NOTES + " LIKE ?" +
                    " ORDER BY CASE WHEN " + COL_SHOW_TITLE + " = ? THEN 0 WHEN " + COL_SHOW_TITLE + " LIKE ? THEN 1 ELSE 2 END",
                    new String[]{like, like, like, query, query + "%"});
            if (cursor.moveToFirst()) {
                do {
                    arr.put(cursorToJson(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return arr.toString();
    }

    public synchronized void addWatchHistory(String showId, int episodeNum) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COL_HIST_SHOW_ID, showId);
            values.put(COL_HIST_EPISODE, episodeNum);
            values.put(COL_HIST_TIMESTAMP, System.currentTimeMillis());
            db.insertWithOnConflict(TABLE_HISTORY, null, values, SQLiteDatabase.CONFLICT_IGNORE);

            db.execSQL("DELETE FROM " + TABLE_HISTORY + " WHERE " + COL_HIST_ID + " NOT IN " +
                    "(SELECT " + COL_HIST_ID + " FROM " + TABLE_HISTORY +
                    " ORDER BY " + COL_HIST_TIMESTAMP + " DESC LIMIT 500)");

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public synchronized String getWatchHistory(int limit) {
        JSONArray arr = new JSONArray();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT h.*, s.title, s.poster FROM " + TABLE_HISTORY + " h " +
                    "LEFT JOIN " + TABLE_SHOWS + " s ON h." + COL_HIST_SHOW_ID + " = s." + COL_SHOW_ID +
                    " ORDER BY h." + COL_HIST_TIMESTAMP + " DESC LIMIT " + Math.max(1, Math.min(limit, 500)), null);
            if (cursor.moveToFirst()) {
                do {
                    JSONObject obj = new JSONObject();
                    obj.put("id", cursor.getInt(cursor.getColumnIndexOrThrow(COL_HIST_ID)));
                    obj.put("showId", cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_SHOW_ID)));
                    obj.put("title", getStringOrEmpty(cursor, COL_SHOW_TITLE));
                    obj.put("poster", getStringOrEmpty(cursor, COL_SHOW_POSTER));
                    obj.put("episodeNum", cursor.getInt(cursor.getColumnIndexOrThrow(COL_HIST_EPISODE)));
                    obj.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow(COL_HIST_TIMESTAMP)));
                    arr.put(obj);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return arr.toString();
    }

    public synchronized String getWatchHistory() {
        return getWatchHistory(200);
    }

    public synchronized String getRecentlyUpdatedShows(int limit) {
        JSONArray arr = new JSONArray();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SHOWS, null, null, null, null, null,
                    COL_SHOW_LAST_UPDATED + " DESC", String.valueOf(Math.max(1, Math.min(limit, 100))));
            if (cursor.moveToFirst()) {
                do {
                    arr.put(cursorToJson(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return arr.toString();
    }

    public synchronized String getStats() {
        SQLiteDatabase db = this.getReadableDatabase();
        JSONObject stats = new JSONObject();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SHOWS, null);
            if (cursor.moveToFirst()) stats.put("total", cursor.getInt(0));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SHOWS + " WHERE " + COL_SHOW_STATUS + " = 'ongoing'", null);
            if (cursor.moveToFirst()) stats.put("airing", cursor.getInt(0));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SHOWS + " WHERE " + COL_SHOW_STATUS + " = 'completed'", null);
            if (cursor.moveToFirst()) stats.put("completed", cursor.getInt(0));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SHOWS + " WHERE " + COL_SHOW_STATUS + " = 'stopped'", null);
            if (cursor.moveToFirst()) stats.put("stopped", cursor.getInt(0));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        try {
            cursor = db.rawQuery("SELECT COALESCE(SUM(" + COL_SHOW_CURRENT_EP + "), 0) FROM " + TABLE_SHOWS, null);
            if (cursor.moveToFirst()) stats.put("episodesWatched", cursor.getInt(0));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY, null);
            if (cursor.moveToFirst()) stats.put("watchHistoryEntries", cursor.getInt(0));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SHOWS + " WHERE " + COL_SHOW_IS_FAVORITE + " = 1", null);
            if (cursor.moveToFirst()) stats.put("favorites", cursor.getInt(0));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return stats.toString();
    }

    public synchronized void bulkInsert(JSONArray showsJson) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < showsJson.length(); i++) {
                insertShow(showsJson.getJSONObject(i));
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public synchronized void saveSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SET_KEY, key);
        values.put(COL_SET_VAL, value);
        db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized String getSetting(String key, String defaultVal) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String val = defaultVal;
        try {
            cursor = db.query(TABLE_SETTINGS, new String[]{COL_SET_VAL}, COL_SET_KEY + " = ?", new String[]{key}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                val = cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return val;
    }

    private JSONObject cursorToJson(Cursor cursor) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("id", cursor.getString(cursor.getColumnIndexOrThrow(COL_SHOW_ID)));
        obj.put("title", cursor.getString(cursor.getColumnIndexOrThrow(COL_SHOW_TITLE)));
        obj.put("titleZh", getStringOrEmpty(cursor, COL_SHOW_TITLE_ZH));
        obj.put("status", getStringOrEmpty(cursor, COL_SHOW_STATUS));
        obj.put("releaseDay", getStringOrEmpty(cursor, COL_SHOW_RELEASE_DAY));
        obj.put("releaseTime", getStringOrEmpty(cursor, COL_SHOW_RELEASE_TIME));
        obj.put("currentEp", cursor.getInt(cursor.getColumnIndexOrThrow(COL_SHOW_CURRENT_EP)));
        obj.put("totalEp", cursor.getInt(cursor.getColumnIndexOrThrow(COL_SHOW_TOTAL_EP)));
        obj.put("poster", getStringOrEmpty(cursor, COL_SHOW_POSTER));
        obj.put("watchUrl", getStringOrEmpty(cursor, COL_SHOW_WATCH_URL));
        obj.put("countdownUrl", getStringOrEmpty(cursor, COL_SHOW_COUNTDOWN_URL));
        obj.put("notes", getStringOrEmpty(cursor, COL_SHOW_NOTES));
        obj.put("isFavorite", cursor.getInt(cursor.getColumnIndexOrThrow(COL_SHOW_IS_FAVORITE)) == 1);
        obj.put("rating", cursor.getInt(cursor.getColumnIndexOrThrow(COL_SHOW_RATING)));
        obj.put("lastUpdated", cursor.getLong(cursor.getColumnIndexOrThrow(COL_SHOW_LAST_UPDATED)));
        obj.put("dateAdded", cursor.getLong(cursor.getColumnIndexOrThrow(COL_SHOW_DATE_ADDED)));
        obj.put("alarmRequestCode", cursor.getInt(cursor.getColumnIndexOrThrow(COL_SHOW_ALARM_CODE)));
        obj.put("genres", getStringOrEmpty(cursor, COL_SHOW_GENRES));
        obj.put("studio", getStringOrEmpty(cursor, COL_SHOW_STUDIO));
        obj.put("releaseYear", cursor.getInt(cursor.getColumnIndexOrThrow(COL_SHOW_RELEASE_YEAR)));
        obj.put("language", getStringOrEmpty(cursor, COL_SHOW_LANGUAGE));
        obj.put("collection", getStringOrEmpty(cursor, COL_SHOW_COLLECTION));
        return obj;
    }

    private String getStringOrEmpty(Cursor cursor, String column) {
        try {
            int idx = cursor.getColumnIndexOrThrow(column);
            return cursor.getString(idx) != null ? cursor.getString(idx) : "";
        } catch (Exception e) {
            return "";
        }
    }
}
