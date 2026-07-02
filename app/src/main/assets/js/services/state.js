let shows = [];
const existingShowIds = new Set();
const filters = { search: '', genre: '', status: 'all', scheduleDay: 'all', sortBy: 'countdown' };
Object.defineProperty(window, 'shows', { get() { return shows; }, set(v) { shows = v; } });
Object.defineProperty(window, 'existingShowIds', { get() { return existingShowIds; } });
Object.defineProperty(window, 'filters', { get() { return filters; } });

const _QUEUE_KEY = 'donghua_operation_queue';
const _alarmHashes = new Map();
let _alarmsInitialized = false;

function _getAlarmHash(show) {
    return `${show.status}|${show.releaseDay}|${show.releaseTime}|${show.alarmRequestCode}`;
}

function retryOperation(fn, retries = 3, delay = 200) {
    for (let i = 0; i < retries; i++) {
        try {
            const result = fn();
            if (result !== false && result !== undefined) return result;
        } catch (e) {
            if (i === retries - 1) throw e;
        }
        if (i < retries - 1) {
            const start = Date.now();
            while (Date.now() - start < delay) {}
        }
    }
    return false;
}

function _queueOperation(op) {
    try {
        const queue = JSON.parse(DB.getLocal(_QUEUE_KEY) || '[]');
        queue.push(op);
        DB.setLocal(_QUEUE_KEY, JSON.stringify(queue));
    } catch (e) {
        console.error('Failed to queue operation', e);
    }
}

function _flushQueue() {
    try {
        const raw = DB.getLocal(_QUEUE_KEY);
        if (!raw) return;
        const queue = JSON.parse(raw);
        if (!Array.isArray(queue) || queue.length === 0) return;
        if (!DB._available) return;
        const remaining = [];
        for (const op of queue) {
            try {
                if (op.type === 'updateShow' && op.data) {
                    retryOperation(() => DB.updateShow(JSON.stringify(op.data)));
                } else if (op.type === 'insertShow' && op.data) {
                    retryOperation(() => DB.insertShow(JSON.stringify(op.data)));
                } else if (op.type === 'deleteShow' && op.id != null) {
                    retryOperation(() => DB.deleteShow(op.id));
                } else if (op.type === 'addWatchHistory' && op.showId != null) {
                    retryOperation(() => DB.addWatchHistory(op.showId, op.episodeNum));
                }
            } catch (e) {
                remaining.push(op);
            }
        }
        DB.setLocal(_QUEUE_KEY, remaining.length > 0 ? JSON.stringify(remaining) : '[]');
    } catch (e) {
        console.error('Failed to flush operation queue', e);
    }
}

function loadState() {
    try {
        _flushQueue();
        if (DB._available) {
            const dbData = DB.getAllShows();
            shows = JSON.parse(dbData) || [];
            const hasMigrated = DB.getSetting('sqlite_migrated', 'false');
            if (hasMigrated === 'false') {
                const legacyData = DB.getLocal(LS_KEYS.SHOWS);
                if (legacyData) {
                    const legacyShows = JSON.parse(legacyData);
                    if (Array.isArray(legacyShows) && legacyShows.length > 0) {
                        legacyShows.forEach(s => {
                            DB.insertShow(JSON.stringify(s));
                        });
                        shows = JSON.parse(DB.getAllShows()) || [];
                    }
                }
                DB.saveSetting('sqlite_migrated', 'true');
            }
        } else {
            const saved = DB.getLocal(LS_KEYS.SHOWS);
            shows = saved ? JSON.parse(saved) : [];
        }
        if (!Array.isArray(shows)) shows = [];
    } catch (e) {
        console.error("Database load error", e);
        shows = [];
    }
    existingShowIds.clear();
    shows.forEach(s => existingShowIds.add(s.id));
    _alarmHashes.clear();
    shows.forEach(s => _alarmHashes.set(s.id, _getAlarmHash(s)));
    _alarmsInitialized = false;
    applyStatusMigrations();
}

function applyStatusMigrations() {
    let changed = false;
    shows.forEach(show => {
        if (STATUS.migrate(show)) {
            show.lastUpdated = Date.now();
            changed = true;
        }
    });
    if (changed) {
        persistState();
    }
}

function persistState(fullRender = true) {
    let sqliteOk = true;
    let alarmFieldsChanged = !_alarmsInitialized;
    if (DB._available) {
        shows.forEach(show => {
            const json = JSON.stringify(show);
            if (existingShowIds.has(show.id)) {
                try {
                    const ok = retryOperation(() => DB.updateShow(json));
                    if (!ok) {
                        sqliteOk = false;
                        _queueOperation({ type: 'updateShow', data: JSON.parse(json) });
                    }
                } catch (e) {
                    console.error('Failed to update show', show.id, e);
                    sqliteOk = false;
                    _queueOperation({ type: 'updateShow', data: JSON.parse(json) });
                }
            } else {
                try {
                    const ok = retryOperation(() => DB.insertShow(json));
                    if (ok) {
                        existingShowIds.add(show.id);
                    } else {
                        sqliteOk = false;
                        _queueOperation({ type: 'insertShow', data: JSON.parse(json) });
                    }
                } catch (e) {
                    console.error('Failed to insert show', show.id, e);
                    sqliteOk = false;
                    _queueOperation({ type: 'insertShow', data: JSON.parse(json) });
                }
            }
            const newHash = _getAlarmHash(show);
            const oldHash = _alarmHashes.get(show.id);
            if (oldHash === undefined || newHash !== oldHash) alarmFieldsChanged = true;
            _alarmHashes.set(show.id, newHash);
        });
        if (alarmFieldsChanged) {
            try {
                DB.syncAllAlarms();
                _alarmsInitialized = true;
            } catch (e) {
                console.error('Alarm sync failed', e);
            }
        }
    }
    if (!sqliteOk || !DB._available) {
        DB.setLocal(LS_KEYS.SHOWS, JSON.stringify(shows));
    }
    if (fullRender) {
        try {
            updateStats();
            renderWeeklySchedule();
            renderHeroBanner();
            renderShowsGrid();
        } catch (e) {
            console.error('Render failed in persistState', e);
        }
    }
}

function getShowById(id) {
    return shows.find(s => s.id === id || String(s.id) === String(id));
}

function getOngoingShows() {
    return STATUS.allAiring();
}

function deleteShow(showId) {
    try {
        const idx = shows.findIndex(s => s.id === showId);
        if (idx === -1) return false;
        const show = shows[idx];
        shows.splice(idx, 1);
        existingShowIds.delete(showId);
        if (DB._available) {
            try {
                retryOperation(() => DB.deleteShow(showId));
            } catch (e) {
                _queueOperation({ type: 'deleteShow', id: showId });
                console.error('Failed to delete show from DB', showId, e);
            }
            try {
                DB.cancelReminder(showId, show.alarmRequestCode || hashCode(showId));
            } catch (e) {
                console.error('Failed to cancel reminder', showId, e);
            }
        }
        persistState();
        return true;
    } catch (e) {
        console.error('Failed to delete show', showId, e);
        return false;
    }
}

function incrementEpisode(showId, count = 1) {
    try {
        const show = getShowById(showId);
        if (!show) return null;
        show.currentEp += count;
        show.lastUpdated = Date.now();
        if (DB._available) {
            try {
                retryOperation(() => DB.addWatchHistory(showId, show.currentEp));
            } catch (e) {
                _queueOperation({ type: 'addWatchHistory', showId, episodeNum: show.currentEp });
                console.error('Failed to add watch history', showId, e);
            }
        }
        if (STATUS.shouldAutoComplete(show)) {
            show.status = STATUS.COMPLETED;
        }
        persistState();
        return show;
    } catch (e) {
        console.error('Failed to increment episode', showId, e);
        return null;
    }
}

function decrementEpisode(showId) {
    try {
        const show = getShowById(showId);
        if (!show || show.currentEp <= 0) return null;
        show.currentEp--;
        show.lastUpdated = Date.now();
        if (STATUS.isCompleted(show) && show.currentEp < (show.totalEp || Infinity)) {
            show.status = STATUS.AIRING;
        }
        persistState();
        return show;
    } catch (e) {
        console.error('Failed to decrement episode', showId, e);
        return null;
    }
}

function toggleFavorite(showId) {
    try {
        const show = getShowById(showId);
        if (!show) return false;
        show.isFavorite = !show.isFavorite;
        show.lastUpdated = Date.now();
        persistState();
        return show.isFavorite;
    } catch (e) {
        console.error('Failed to toggle favorite', showId, e);
        return false;
    }
}

function updateShowField(showId, field, value) {
    try {
        const show = getShowById(showId);
        if (!show) return false;
        show[field] = value;
        show.lastUpdated = Date.now();
        persistState();
        return true;
    } catch (e) {
        console.error('Failed to update show field', showId, field, e);
        return false;
    }
}

function getRecentlyUpdated(limit = 10) {
    return [...shows]
        .filter(s => s.lastUpdated)
        .sort((a, b) => (b.lastUpdated || 0) - (a.lastUpdated || 0))
        .slice(0, limit);
}

function getAiringToday() {
    const todayName = DAYS_ARRAY[new Date().getDay()];
    return STATUS.allAiring().filter(s => s.releaseDay === todayName);
}
