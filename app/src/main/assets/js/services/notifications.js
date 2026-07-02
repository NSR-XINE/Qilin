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
    let json = null;
    if (DB._available) {
        json = DB.getAllShows();
    } else {
        json = DB.getLocal('donghua_shows');
    }
    if (!json) { alert('No data to export.'); return; }
    try {
        const parsed = JSON.parse(json);
        if (!Array.isArray(parsed) || parsed.length === 0) {
            alert('No data to export.');
            return;
        }
    } catch (e) {
        alert('Export failed: stored data is corrupted.');
        return;
    }
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
        if (!Array.isArray(parsed)) throw new Error('Invalid format: root must be a JSON array');
        for (const item of parsed) {
            if (!item.title || !item.id) {
                throw new Error("Missing 'title' or 'id' fields in backup item");
            }
        }
        if (DB._available) {
            const currentDbData = JSON.parse(DB.getAllShows() || '[]');
            const importedIds = new Set(parsed.map(s => s.id));
            currentDbData.forEach(s => { if (!importedIds.has(s.id)) DB.deleteShow(String(s.id)); });
        }
        shows = parsed;
        existingShowIds.clear();
        shows.forEach(s => existingShowIds.add(s.id));
        DB.setLocal('donghua_shows', JSON.stringify(shows));
        persistState();
        alert('Import successful! ' + shows.length + ' shows loaded.');
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
