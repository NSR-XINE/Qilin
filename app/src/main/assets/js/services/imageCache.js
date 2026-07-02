const ImageCache = {
    _memoryCache: new Map(),
    _pendingLoads: new Map(),
    _maxMemoryItems: 100,
    _maxDiskItems: 200,
    _cachePrefix: 'img_cache_',
    _ttlMs: 86400000,

    init() {
        this._cleanup();
    },

    get(url) {
        if (!url) return null;
        const cached = this._memoryCache.get(url);
        if (cached) return cached;
        const disk = this._fromDisk(url);
        if (disk) {
            this._memoryCache.set(url, disk);
            return disk;
        }
        return null;
    },

    put(url, data) {
        if (!url) return;
        this._memoryCache.set(url, data);
        if (this._memoryCache.size > this._maxMemoryItems) {
            const firstKey = this._memoryCache.keys().next().value;
            this._memoryCache.delete(firstKey);
        }
        this._toDisk(url, data);
    },

    loadImage(url) {
        if (!url) return Promise.reject(new Error('No URL'));
        const cached = this.get(url);
        if (cached === 'loaded') return Promise.resolve();
        if (this._pendingLoads.has(url)) return this._pendingLoads.get(url);

        const promise = new Promise((resolve, reject) => {
            const img = new Image();
            img.onload = () => {
                this.put(url, 'loaded');
                this._pendingLoads.delete(url);
                resolve();
            };
            img.onerror = () => {
                this._pendingLoads.delete(url);
                reject(new Error('Failed to load: ' + url));
            };
            img.src = url;
        });
        this._pendingLoads.set(url, promise);
        return promise;
    },

    preloadImages(urls) {
        return Promise.allSettled(urls.filter(Boolean).map(url => this.loadImage(url)));
    },

    prefetch(url) {
        if (!url || this._memoryCache.has(url)) return;
        const link = document.createElement('link');
        link.rel = 'prefetch';
        link.href = url;
        document.head.appendChild(link);
        setTimeout(() => link.remove(), 5000);
    },

    _toDisk(url, data) {
        try {
            const key = this._cachePrefix + btoa(url).slice(0, 64);
            const item = JSON.stringify({ data, ts: Date.now() });
            localStorage.setItem(key, item);
        } catch (e) {}
    },

    _fromDisk(url) {
        try {
            const key = this._cachePrefix + btoa(url).slice(0, 64);
            const raw = localStorage.getItem(key);
            if (!raw) return null;
            const item = JSON.parse(raw);
            if (Date.now() - item.ts > this._ttlMs) {
                localStorage.removeItem(key);
                return null;
            }
            return item.data;
        } catch (e) { return null; }
    },

    _cleanup() {
        try {
            const keys = [];
            for (let i = 0; i < localStorage.length; i++) {
                const k = localStorage.key(i);
                if (k && k.startsWith(this._cachePrefix)) keys.push(k);
            }
            keys.sort((a, b) => {
                try {
                    const va = JSON.parse(localStorage.getItem(a));
                    const vb = JSON.parse(localStorage.getItem(b));
                    return (va.ts || 0) - (vb.ts || 0);
                } catch (e) { return 0; }
            });
            while (keys.length > this._maxDiskItems) {
                const old = keys.shift();
                localStorage.removeItem(old);
            }
            for (const k of keys) {
                try {
                    const v = JSON.parse(localStorage.getItem(k));
                    if (Date.now() - v.ts > this._ttlMs) localStorage.removeItem(k);
                } catch (e) { localStorage.removeItem(k); }
            }
        } catch (e) {}
    },

    has(url) {
        if (this._memoryCache.has(url)) return true;
        const disk = this._fromDisk(url);
        if (disk) {
            this._memoryCache.set(url, disk);
            return true;
        }
        return false;
    },

    clear() {
        this._memoryCache.clear();
        this._pendingLoads.clear();
        try {
            const keys = [];
            for (let i = 0; i < localStorage.length; i++) {
                const k = localStorage.key(i);
                if (k && k.startsWith(this._cachePrefix)) keys.push(k);
            }
            for (const k of keys) localStorage.removeItem(k);
        } catch (e) {}
    },

    invalidate(url) {
        this._memoryCache.delete(url);
        try {
            const key = this._cachePrefix + btoa(url).slice(0, 64);
            localStorage.removeItem(key);
        } catch (e) {}
    }
};
