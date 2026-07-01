const DB = {
    _available: typeof AndroidApp !== 'undefined' && AndroidApp !== null,
    _localStorageAvailable: (() => { try { return typeof localStorage !== 'undefined'; } catch(e) { return false; } })(),

    _call(method, ...args) {
        if (this._available && typeof AndroidApp[method] === 'function') {
            return AndroidApp[method](...args);
        }
        return undefined;
    },

    getAllShows() {
        return this._call('dbGetAllShows');
    },

    getShowsByStatus(status) {
        return this._call('dbGetShowsByStatus', status);
    },

    getFavoriteShows() {
        return this._call('dbGetFavoriteShows');
    },

    searchShows(query) {
        return this._call('dbSearchShows', query);
    },

    insertShow(json) {
        return this._call('dbInsertShow', json);
    },

    updateShow(json) {
        return this._call('dbUpdateShow', json);
    },

    deleteShow(id) {
        return this._call('dbDeleteShow', id);
    },

    addWatchHistory(showId, episodeNum) {
        return this._call('dbAddWatchHistory', showId, episodeNum);
    },

    getWatchHistory() {
        return this._call('dbGetWatchHistory');
    },

    getWatchHistoryLimited(limit) {
        return this._call('dbGetWatchHistoryLimited', limit);
    },

    getRecentlyUpdated(limit) {
        return this._call('dbGetRecentlyUpdated', limit);
    },

    getStats() {
        return this._call('dbGetStats');
    },

    saveSetting(key, value) {
        return this._call('dbSaveSetting', key, value);
    },

    getSetting(key, defaultValue) {
        return this._call('dbGetSetting', key, defaultValue);
    },

    fetchUrl(url, callback) {
        return this._call('fetchUrl', url, callback);
    },

    fetchUrlPost(url, body, callback) {
        return this._call('fetchUrlPost', url, body, callback);
    },

    openWatchScreen(url) {
        return this._call('openWatchScreen', url);
    },

    shareJsonFile(json) {
        return this._call('shareJsonFile', json);
    },

    exitApp() {
        return this._call('exitApp');
    },

    setSystemThemeMode(mode) {
        return this._call('setSystemThemeMode', mode);
    },

    scheduleReminder(id, title, day, time, code) {
        return this._call('scheduleReminder', id, title, day, time, code);
    },

    cancelReminder(id, code) {
        return this._call('cancelReminder', id, code);
    },

    syncAllAlarms() {
        return this._call('syncAllAlarms');
    },

    getLocal(key) {
        try { return localStorage.getItem(key); } catch(e) { return null; }
    },

    setLocal(key, val) {
        try { localStorage.setItem(key, val); } catch(e) {}
    },

    removeLocal(key) {
        try { localStorage.removeItem(key); } catch(e) {}
    }
};

const LS_KEYS = {
    SHOWS: 'donghua_shows',
    THEME: 'app_theme',
    SOURCE: 'pref_streaming_source'
};
