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

function _queueOperation(op) {
    if (typeof SyncManager !== 'undefined' && SyncManager.enqueue) {
        SyncManager.enqueue(op.type, op.data || op);
    } else {
        try {
            const queue = JSON.parse(DB.getLocal(_QUEUE_KEY) || '[]');
            queue.push(op);
            DB.setLocal(_QUEUE_KEY, JSON.stringify(queue));
        } catch (e) {
            console.error('Failed to queue operation', e);
        }
    }
}

function _flushQueue() {
    if (typeof SyncManager !== 'undefined' && SyncManager.processQueue) {
        SyncManager.processQueue();
        return;
    }
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
                    DB.updateShow(JSON.stringify(op.data));
                } else if (op.type === 'insertShow' && op.data) {
                    DB.insertShow(JSON.stringify(op.data));
                } else if (op.type === 'deleteShow' && op.id != null) {
                    DB.deleteShow(op.id);
                } else if (op.type === 'addWatchHistory' && op.showId != null) {
                    DB.addWatchHistory(op.showId, op.episodeNum);
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

function _migrateLegacyQueue() {
    if (typeof SyncManager === 'undefined' || !SyncManager.enqueue) return;
    try {
        const raw = DB.getLocal(_QUEUE_KEY);
        if (!raw) return;
        const queue = JSON.parse(raw);
        if (!Array.isArray(queue) || queue.length === 0) return;
        for (const op of queue) {
            SyncManager.enqueue(op.type, op.data || op);
        }
        DB.setLocal(_QUEUE_KEY, '[]');
    } catch (e) {
        console.error('Legacy queue migration failed', e);
    }
}

function loadState() {
    try {
        _migrateLegacyQueue();
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

let _persistCount = 0;
function persistState(fullRender = true) {
    _persistCount++;
    const seq = _persistCount;

    try {
        if (DB._available) {
            const toInsert = [];
            const toUpdate = [];
            let alarmFieldsChanged = false;
            shows.forEach(show => {
                if (existingShowIds.has(show.id)) {
                    toUpdate.push(show);
                } else {
                    toInsert.push(show);
                }
                if (!alarmFieldsChanged) {
                    const newHash = _getAlarmHash(show);
                    const oldHash = _alarmHashes.get(show.id);
                    if (oldHash === undefined || newHash !== oldHash) {
                        alarmFieldsChanged = true;
                    }
                    _alarmHashes.set(show.id, newHash);
                }
            });
            if (toInsert.length > 0) {
                try {
                    const ok = DB.bulkInsert(JSON.stringify(toInsert));
                    if (ok) toInsert.forEach(s => existingShowIds.add(s.id));
                    else toInsert.forEach(s => _queueOperation({ type: 'insertShow', data: s }));
                } catch (e) {
                    toInsert.forEach(s => _queueOperation({ type: 'insertShow', data: s }));
                }
            }
            for (const show of toUpdate) {
                try {
                    const ok = DB.updateShow(JSON.stringify(show));
                    if (!ok) _queueOperation({ type: 'updateShow', data: show });
                } catch (e) {
                    _queueOperation({ type: 'updateShow', data: show });
                }
            }
            if (!_alarmsInitialized || alarmFieldsChanged) {
                try {
                    DB.syncAllAlarms();
                    _alarmsInitialized = true;
                } catch (e) {
                    console.error('Alarm sync failed', e);
                }
            }
        }

        DB.setLocal(LS_KEYS.SHOWS, JSON.stringify(shows));
    } catch (e) {
        console.error('persistState error', e);
    }

    if (seq !== _persistCount) return;

    if (fullRender) {
        _debouncedRender(() => {
            updateStats();
            _debouncedRenderSchedule();
            _debouncedRenderBanner();
            _debouncedRenderGrid();
        });
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
        setNotifyPref(showId, false);
        if (DB._available) {
            try {
                DB.deleteShow(showId);
            } catch (e) {
                _queueOperation({ type: 'deleteShow', id: showId });
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
                DB.addWatchHistory(showId, show.currentEp);
            } catch (e) {
                _queueOperation({ type: 'addWatchHistory', showId, episodeNum: show.currentEp });
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
