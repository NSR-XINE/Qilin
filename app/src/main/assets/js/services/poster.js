const _posterCache = new Map();
const _pendingFetches = new Map();
const _posterCallbacks = new Set();

function _cleanupPosterCallback(name) {
    _posterCallbacks.delete(name);
    delete window[name];
}

function _registerPosterCallback(name, fn, timeoutMs = 30000) {
    _posterCallbacks.add(name);
    window[name] = function(...args) {
        _cleanupPosterCallback(name);
        return fn(...args);
    };
    setTimeout(() => {
        if (window[name]) _cleanupPosterCallback(name);
    }, timeoutMs);
}

function fetchShowBanner(showId, countdownUrl) {
    if (!DB._available) return;
    if (!countdownUrl || !countdownUrl.startsWith('http')) return;
    if (_pendingFetches.has(showId + '_banner')) return;

    _pendingFetches.set(showId + '_banner', true);
    const callbackName = 'cb_banner_' + showId.replace(/[^a-zA-Z0-9]/g, '') + '_' + Date.now();

    _registerPosterCallback(callbackName, function(html) {
        _pendingFetches.delete(showId + '_banner');
        if (!html) return;
        try {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            let imageUrl = null;
            const ogImage = doc.querySelector('meta[property="og:image"]');
            if (ogImage) imageUrl = ogImage.getAttribute('content');
            if (!imageUrl) {
                const twImage = doc.querySelector('meta[name="twitter:image"]');
                if (twImage) imageUrl = twImage.getAttribute('content');
            }
            if (!imageUrl) {
                const itemImg = doc.querySelector('img[src*="posters/"], img[src*="fanart/"], .poster-img');
                imageUrl = itemImg ? itemImg.src : null;
            }
            if (imageUrl) {
                if (imageUrl.startsWith('//')) imageUrl = 'https:' + imageUrl;
                const show = getShowById(showId);
                if (show && show.poster !== imageUrl) {
                    show.poster = imageUrl;
                    persistState();
                }
            }
        } catch (e) {
            console.error("Failed to parse banner HTML for ID: " + showId, e);
        }
    });
    DB.fetchUrl(countdownUrl, callbackName);
}

function fetchPosterFromJikan(showId, title) {
    if (!DB._available) return;
    if (_pendingFetches.has(showId + '_jikan')) return;

    const cacheKey = 'jikan_' + title;
    const cached = _posterCache.get(cacheKey);
    if (cached) {
        const show = getShowById(showId);
        if (show && show.poster !== cached) {
            show.poster = cached;
            persistState();
        }
        return;
    }

    _pendingFetches.set(showId + '_jikan', true);
    const query = encodeURIComponent(title);
    const url = 'https://api.jikan.moe/v4/anime?q=' + query + '&limit=1';
    const callbackName = 'cb_jikan_' + showId.replace(/[^a-zA-Z0-9]/g, '') + '_' + Date.now();

    _registerPosterCallback(callbackName, function(json) {
        _pendingFetches.delete(showId + '_jikan');
        if (!json) return;
        try {
            const data = JSON.parse(json);
            const entry = data && data.data && data.data[0];
            if (!entry) return;
            const imageUrl = entry.images && entry.images.jpg && entry.images.jpg.large_image_url;
            if (!imageUrl) return;
            _posterCache.set(cacheKey, imageUrl);
            const show = getShowById(showId);
            if (show && show.poster !== imageUrl) {
                show.poster = imageUrl;
                persistState();
            }
        } catch(e) {
            console.error('Jikan parse error', e);
        }
    });
    DB.fetchUrl(url, callbackName);
}

function fetchShowDetails(title, silent) {
    if (!title) {
        if (!silent) alert('Please enter an English title first.');
        return;
    }

    function fillFormFromEntry(entry, source) {
        const imgUrl = entry.images && entry.images.jpg && entry.images.jpg.large_image_url;
        if (imgUrl) {
            const posterInput = document.getElementById('show-poster');
            if (posterInput) posterInput.value = imgUrl;
        }
        if (entry.title_japanese) {
            const titleZhEl = document.getElementById('show-title-zh');
            if (titleZhEl && !titleZhEl.value.trim()) titleZhEl.value = entry.title_japanese;
        }
        if (entry.synopsis) {
            const notesEl = document.getElementById('show-notes');
            if (notesEl && !notesEl.value.trim()) notesEl.value = entry.synopsis;
        }
        if (entry.status) {
            const statusSelect = document.getElementById('show-status');
            if (statusSelect) {
                const s = entry.status.toLowerCase();
                if (s.includes('currently airing')) statusSelect.value = 'ongoing';
                else if (s.includes('finished airing')) statusSelect.value = 'completed';
                else if (s.includes('cancelled')) statusSelect.value = 'stopped';
            }
        }
        if (entry.episodes) {
            const totalEpEl = document.getElementById('show-total-ep');
            if (totalEpEl && (!totalEpEl.value || parseInt(totalEpEl.value) === 0)) {
                totalEpEl.value = entry.episodes;
            }
        }
        if (!silent) alert('Auto-filled from ' + source + '!');
    }

    function tryAnilistFallback() {
        const query = `query ($search: String) { Page(page: 1, perPage: 1) { media(search: $search, type: ANIME) { id title { romaji english native } coverImage { large extraLarge } description(asHtml: false) episodes status } } }`;
        const body = JSON.stringify({ query, variables: { search: title } });
        if (DB._available) {
            const cb = 'cb_anilist_' + Date.now();
            _registerPosterCallback(cb, function(json) {
                if (!json) { if (!silent) alert('No matching show found.'); return; }
                try {
                    const data = JSON.parse(json);
                    const media = data && data.data && data.data.Page && data.data.Page.media && data.data.Page.media[0];
                    if (!media) { if (!silent) alert('No matching show found.'); return; }
                    const mapped = {
                        title_japanese: media.title && (media.title.native || media.title.romaji),
                        synopsis: media.description,
                        status: media.status,
                        episodes: media.episodes,
                        images: { jpg: { large_image_url: media.coverImage && (media.coverImage.extraLarge || media.coverImage.large) } }
                    };
                    fillFormFromEntry(mapped, 'AniList');
                } catch(e) { if (!silent) alert('Error parsing AniList response.'); }
            });
            DB.fetchUrlPost('https://graphql.anilist.co/', body, cb);
        } else {
            if (!silent) alert('No matching show found.');
        }
    }

    const query = encodeURIComponent(title);
    const url = 'https://api.jikan.moe/v4/anime?q=' + query + '&limit=1';

    if (DB._available) {
        const cbName = 'cb_modal_poster_' + Date.now();
        _registerPosterCallback(cbName, function(json) {
            if (!json) { tryAnilistFallback(); return; }
            try {
                const data = JSON.parse(json);
                const entry = data && data.data && data.data[0];
                if (!entry) { tryAnilistFallback(); return; }
                fillFormFromEntry(entry, 'MyAnimeList');
            } catch(e) { tryAnilistFallback(); }
        });
        DB.fetchUrl(url, cbName);
    } else {
        fetch(url).then(r => r.json()).then(data => {
            const entry = data && data.data && data.data[0];
            if (entry) fillFormFromEntry(entry, 'MyAnimeList');
            else tryAnilistFallback();
        }).catch(() => tryAnilistFallback());
    }
}

const _autoFetchQueue = [];
let _autoFetchRunning = false;

function autoFetchPosters() {
    shows.forEach(show => {
        if (!show.poster) {
            if (show.watchUrl && show.watchUrl.startsWith('https://donghuastream.org/')) {
                _autoFetchQueue.push(() => fetchShowBanner(show.id, show.watchUrl));
            } else if (show.countdownUrl && show.countdownUrl.startsWith('https://animecountdown.com/')) {
                _autoFetchQueue.push(() => fetchShowBanner(show.id, show.countdownUrl));
            } else {
                _autoFetchQueue.push(() => fetchPosterFromJikan(show.id, show.title));
            }
        }
    });
    if (!_autoFetchRunning) {
        _autoFetchRunning = true;
        _processAutoFetchQueue();
    }
}

function _processAutoFetchQueue() {
    if (_autoFetchQueue.length === 0) {
        _autoFetchRunning = false;
        return;
    }
    const task = _autoFetchQueue.shift();
    try { task(); } catch(e) { console.error('Auto-fetch error', e); }
    setTimeout(_processAutoFetchQueue, 250);
}

function getWatchUrl(show) {
    if (!show) return '';
    if (show.watchUrl && show.watchUrl.trim() !== '') return show.watchUrl;
    let source = 'donghuastream';
    if (DB._available) {
        source = DB.getSetting('pref_streaming_source', '') || DB.getLocal('pref_streaming_source') || 'donghuastream';
    } else {
        source = DB.getLocal('pref_streaming_source') || 'donghuastream';
    }
    if (source === 'luciferdonghua') {
        return 'https://luciferdonghua.org/?s=' + encodeURIComponent(show.title);
    }
    return 'https://donghuastream.org/?s=' + encodeURIComponent(show.title);
}

function selectPreferredSource(source) {
    if (DB._available) DB.saveSetting('pref_streaming_source', source);
    DB.setLocal('pref_streaming_source', source);
    updateSourceUI(source);
    renderShowsGrid();
    renderHeroBanner();
}

function updateSourceUI(source) {
    const cardDonghua = document.getElementById('source-card-donghua');
    const cardLucifer = document.getElementById('source-card-lucifer');
    if (!cardDonghua || !cardLucifer) return;
    const badgeDonghua = cardDonghua.querySelector('.active-badge');
    const badgeLucifer = cardLucifer.querySelector('.active-badge');
    if (source === 'luciferdonghua') {
        cardDonghua.style.borderColor = 'var(--border-color)';
        cardDonghua.style.background = 'rgba(255,255,255,0.02)';
        cardDonghua.style.boxShadow = 'none';
        if (badgeDonghua) badgeDonghua.style.display = 'none';
        cardLucifer.style.borderColor = 'var(--accent-purple)';
        cardLucifer.style.background = 'rgba(157, 78, 221, 0.05)';
        cardLucifer.style.boxShadow = '0 0 15px rgba(157, 78, 221, 0.2)';
        if (badgeLucifer) badgeLucifer.style.display = 'block';
    } else {
        cardDonghua.style.borderColor = 'var(--accent-cyan)';
        cardDonghua.style.background = 'rgba(0, 242, 254, 0.03)';
        cardDonghua.style.boxShadow = '0 0 15px rgba(0, 242, 254, 0.15)';
        if (badgeDonghua) badgeDonghua.style.display = 'block';
        cardLucifer.style.borderColor = 'var(--border-color)';
        cardLucifer.style.background = 'rgba(255,255,255,0.02)';
        cardLucifer.style.boxShadow = 'none';
        if (badgeLucifer) badgeLucifer.style.display = 'none';
    }
}

function setupPosterFetch() {
    setTimeout(autoFetchPosters, 2000);
}

function _cleanupAllPosterCallbacks() {
    for (const name of _posterCallbacks) {
        delete window[name];
    }
    _posterCallbacks.clear();
    _autoFetchQueue.length = 0;
    _autoFetchRunning = false;
}