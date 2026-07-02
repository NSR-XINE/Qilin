const SyncManager = {
    _queueKey: 'sync_offline_queue',
    _isSyncing: false,
    _maxRetries: 5,
    _listeners: [],

    init() {
        window.addEventListener('online', () => this.processQueue());
        window.addEventListener('offline', () => this._paused = true);
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden && navigator.onLine) this.processQueue();
        });
        if (navigator.onLine) this.processQueue();
    },

    enqueue(type, payload) {
        const queue = this._getQueue();
        queue.push({
            type,
            payload,
            retries: 0,
            createdAt: Date.now()
        });
        this._saveQueue(queue);
        if (navigator.onLine) this.processQueue();
    },

    processQueue() {
        if (this._isSyncing || !navigator.onLine || !DB._available) return;
        const queue = this._getQueue();
        if (queue.length === 0) return;
        this._isSyncing = true;

        const remaining = [];
        let processed = 0;

        for (const item of queue) {
            try {
                let ok = false;
                switch (item.type) {
                    case 'insertShow':
                        ok = DB.insertShow(JSON.stringify(item.payload));
                        break;
                    case 'updateShow':
                        ok = DB.updateShow(JSON.stringify(item.payload));
                        break;
                    case 'deleteShow':
                        ok = DB.deleteShow(item.payload.id);
                        break;
                    case 'addWatchHistory':
                        DB.addWatchHistory(item.payload.showId, item.payload.episodeNum);
                        ok = true;
                        break;
                }
                if (ok) {
                    processed++;
                } else {
                    item.retries++;
                    if (item.retries < this._maxRetries) {
                        const backoff = Math.min(1000 * Math.pow(2, item.retries), 60000);
                        item.nextRetry = Date.now() + backoff;
                        remaining.push(item);
                    }
                }
            } catch (e) {
                item.retries++;
                if (item.retries < this._maxRetries) {
                    const backoff = Math.min(1000 * Math.pow(2, item.retries), 60000);
                    item.nextRetry = Date.now() + backoff;
                    remaining.push(item);
                }
            }
        }

        const oldQueue = this._getQueue();
        const pendingFromOld = oldQueue.slice(queue.length);
        this._saveQueue([...remaining, ...pendingFromOld]);
        this._isSyncing = false;

        if (processed > 0) {
            this._notify(processed);
        }

        if (remaining.length > 0) {
            setTimeout(() => this.processQueue(), 5000);
        }
    },

    _getQueue() {
        try {
            return JSON.parse(localStorage.getItem(this._queueKey) || '[]');
        } catch (e) { return []; }
    },

    _saveQueue(queue) {
        try { localStorage.setItem(this._queueKey, JSON.stringify(queue)); } catch (e) {}
    },

    getQueueSize() {
        return this._getQueue().length;
    },

    onProcessed(fn) {
        this._listeners.push(fn);
    },

    _notify(count) {
        for (const fn of this._listeners) {
            try { fn(count); } catch (e) {}
        }
    }
};
