function getContinueWatchingShow() {
    const ongoing = getOngoingShows();
    if (ongoing.length === 0) return null;
    ongoing.sort((a, b) => (b.lastUpdated || 0) - (a.lastUpdated || 0));
    return ongoing[0] || null;
}

const NOTIFY_PREFS_KEY = 'notification_prefs';

function getNotifyPref(showId) {
    try {
        const raw = localStorage.getItem(NOTIFY_PREFS_KEY);
        const prefs = raw ? JSON.parse(raw) : {};
        return prefs[showId] !== false;
    } catch (e) { return true; }
}

function setNotifyPref(showId, enabled) {
    try {
        const raw = localStorage.getItem(NOTIFY_PREFS_KEY);
        const prefs = raw ? JSON.parse(raw) : {};
        prefs[showId] = enabled;
        localStorage.setItem(NOTIFY_PREFS_KEY, JSON.stringify(prefs));
    } catch (e) {}
}

function toggleNotifyForShow(showId) {
    const current = getNotifyPref(showId);
    setNotifyPref(showId, !current);
    return !current;
}

function scheduleReminderForShow(show) {
    if (!show || !STATUS.isAiring(show)) return;
    if (!getNotifyPref(show.id)) return;
    try {
        DB.scheduleReminder(show.id, show.title, show.releaseDay, show.releaseTime,
            show.alarmRequestCode || hashCode(show.id));
    } catch (e) {
        console.error('Failed to schedule reminder for', show.id, e);
    }
}

function exportData() {
    let showsJson = null;
    if (DB._available) {
        showsJson = DB.getAllShows();
    } else {
        showsJson = DB.getLocal('donghua_shows');
    }
    if (!showsJson) { alert('No data to export.'); return; }

    let showsArr;
    try {
        showsArr = JSON.parse(showsJson);
        if (!Array.isArray(showsArr) || showsArr.length === 0) {
            alert('No data to export.');
            return;
        }
    } catch (e) {
        alert('Export failed: stored data is corrupted.');
        return;
    }

    let watchHistory = [];
    if (DB._available) {
        try { watchHistory = JSON.parse(DB.getWatchHistory() || '[]'); } catch (_) {}
    } else {
        try { watchHistory = JSON.parse(DB.getLocal('donghua_watch_history') || '[]'); } catch (_) {}
    }

    let settings = {};
    try {
        if (DB._available) {
            const keys = ['app_theme', 'pref_streaming_source', 'sqlite_migrated', 'donghua_last_sync'];
            for (const key of keys) settings[key] = DB.getSetting(key, '');
        } else {
            settings = { app_theme: localStorage.getItem('app_theme') || '' };
        }
    } catch (_) {}

    const backup = {
        version: 2,
        exportedAt: new Date().toISOString(),
        appVersion: 'v6.8',
        shows: showsArr,
        watchHistory: watchHistory,
        settings: settings,
        notifyPrefs: localStorage.getItem('notification_prefs') || '{}'
    };

    const json = JSON.stringify(backup, null, 2);

    if (DB._available && DB.shareJsonFile) {
        try {
            DB.shareJsonFile(json);
            return;
        } catch (e) {
            // fall through to fallback
        }
    }
    try {
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'mydonghua_backup_' + new Date().toISOString().slice(0, 10) + '.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) {
        navigator.clipboard.writeText(json).then(() => {
            alert('Backup JSON copied to clipboard.');
        }).catch(() => {
            alert('Export failed. Please try again manually.');
        });
    }
}

function importData(jsonString) {
    let json = jsonString;
    if (!json) {
        json = prompt('Paste your backup JSON here:');
    }
    if (!json) return false;
    try {
        const parsed = JSON.parse(json);

        let showsArr, watchHistory, settings, notifyPrefs;

        if (parsed.version === 2 || (parsed.shows && Array.isArray(parsed.shows))) {
            showsArr = parsed.shows || [];
            watchHistory = parsed.watchHistory || [];
            settings = parsed.settings || {};
            notifyPrefs = parsed.notifyPrefs;
        } else if (Array.isArray(parsed)) {
            showsArr = parsed;
            watchHistory = [];
            settings = {};
            notifyPrefs = null;
        } else {
            throw new Error('Invalid format: expected a JSON array or version-2 backup object');
        }

        for (const item of showsArr) {
            if (!item.title || !item.id) {
                throw new Error("Missing 'title' or 'id' fields in backup item");
            }
        }

        if (DB._available) {
            const currentDbData = JSON.parse(DB.getAllShows() || '[]');
            const importedIds = new Set(showsArr.map(s => s.id));
            currentDbData.forEach(s => { if (!importedIds.has(s.id)) DB.deleteShow(String(s.id)); });
        }

        shows = showsArr;
        existingShowIds.clear();
        shows.forEach(s => existingShowIds.add(s.id));
        DB.setLocal('donghua_shows', JSON.stringify(shows));

        if (watchHistory.length > 0 && DB._available) {
            for (const entry of watchHistory) {
                try { DB.addWatchHistory(entry.showId, entry.episodeNum); } catch (_) {}
            }
        }

        if (notifyPrefs) {
            try { localStorage.setItem('notification_prefs', notifyPrefs); } catch (_) {}
        }

        if (settings) {
            if (DB._available) {
                for (const [key, val] of Object.entries(settings)) {
                    if (val) try { DB.saveSetting(key, String(val)); } catch (_) {}
                }
            }
            if (settings.app_theme) {
                localStorage.setItem('app_theme', settings.app_theme);
            }
        }

        persistState();
        alert('Import successful! ' + showsArr.length + ' shows' +
            (watchHistory.length > 0 ? ', ' + watchHistory.length + ' history entries' : '') +
            ' loaded.');
        return true;
    } catch(e) {
        alert('Failed to import backup: ' + e.message);
        return false;
    }
}

function openWatchScreen(url) {
    if (!url) return;
    if (!/^https?:\/\//i.test(url)) return;
    if (DB._available && typeof AndroidApp.openWatchScreen === 'function') {
        AndroidApp.openWatchScreen(url);
    } else {
        window.location.href = url;
    }
}

function getWatchUrlById(id) {
    const show = getShowById(id);
    if (!show) return '';
    return getWatchUrl(show);
}
