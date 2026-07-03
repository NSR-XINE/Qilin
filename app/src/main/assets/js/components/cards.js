function renderShowsGrid() {
    const containerEl = document.getElementById('shows-sections-container');
    const emptyStateEl = document.getElementById('empty-state');
    if (!containerEl) return;

    let statusFilters = [];
    if (activeTab === 'airing') statusFilters = ['ongoing'];
    else if (activeTab === 'stopped') statusFilters = ['stopped'];
    else if (activeTab === 'complete') statusFilters = ['completed'];
    else if (activeTab === 'favorites') { containerEl.innerHTML = ''; renderFavoritesGrid(); return; }
    else { containerEl.innerHTML = ''; return; }

    const genreFilter = filters.genre ? filters.genre.toLowerCase() : '';
    const studioFilter = filters.studio ? filters.studio.toLowerCase() : '';
    const yearFilter = filters.year ? parseInt(filters.year, 10) : 0;
    const minRating = filters.minRating ? parseInt(filters.minRating, 10) : 0;
    const maxEp = filters.maxEpisodes ? parseInt(filters.maxEpisodes, 10) : 0;

    let filteredShows = shows.filter(show => {
        const matchesSearch = (() => {
            if (!filters.search) return true;
            const q = filters.search.trim().toLowerCase();
            return fuzzyMatch(show.title, q) ||
                   fuzzyMatch(show.titleZh, q) ||
                   fuzzyMatch(show.notes, q);
        })();
        const matchesGenre = !genreFilter || (show.genres && show.genres.toLowerCase().includes(genreFilter));
        const matchesStudio = !studioFilter || (show.studio && show.studio.toLowerCase().includes(studioFilter));
        const matchesYear = !yearFilter || (show.releaseYear && show.releaseYear === yearFilter);
        const matchesRating = !minRating || (show.rating && show.rating >= minRating);
        const matchesMaxEp = !maxEp || (show.totalEp > 0 && show.totalEp <= maxEp);
        return matchesSearch && matchesGenre && matchesStudio && matchesYear && matchesRating && matchesMaxEp && statusFilters.includes(show.status);
    });

    filteredShows.sort((a, b) => {
        if (filters.sortBy === 'alphabetical') return a.title.localeCompare(b.title);
        if (filters.sortBy === 'progress') return (b.currentEp || 0) - (a.currentEp || 0);
        if (filters.sortBy === 'last-updated') return (b.lastUpdated || 0) - (a.lastUpdated || 0);
        if (activeTab === 'stopped') return a.title.localeCompare(b.title);
        if (activeTab === 'airing') {
            const schedA = getNextReleaseDate(a.releaseDay, a.releaseTime);
            const schedB = getNextReleaseDate(b.releaseDay, b.releaseTime);
            if (schedA.airingNow && !schedB.airingNow) return -1;
            if (!schedA.airingNow && schedB.airingNow) return 1;
            return schedA.targetDate - schedB.targetDate;
        }
        return a.title.localeCompare(b.title);
    });

    if (shows.length === 0) {
        containerEl.innerHTML = '';
        showEmptyState(emptyStateEl, 'Watchlist is Empty', 'Your watchlist is empty. Search for a show above to start tracking!', false);
        return;
    }
    if (filteredShows.length === 0) {
        containerEl.innerHTML = '';
        const tabLabels = { airing: 'Currently Airing', complete: 'Completed', stopped: 'Stopped / Hiatus' };
        const tabLabel = tabLabels[activeTab] || activeTab;
        const msgTitle = filters.search ? 'No Matches Found' : `No ${tabLabel} Donghuas`;
        const msgText = filters.search ? 'We couldn\'t find any shows matching your current search.'
            : activeTab === 'airing' ? 'You don\'t have any shows marked as Airing. Add a show or change its status to Airing!'
            : activeTab === 'stopped' ? 'No stopped shows tracked. Add a show and set its status to Stopped / Hiatus!'
            : `You don\'t have any shows marked as ${tabLabel} in your watchlist.`;
        showEmptyState(emptyStateEl, msgTitle, msgText, !!filters.search);
        return;
    }

    emptyStateEl.style.display = 'none';
    const todayName = getTodayName();
    const sectionTitles = { airing: 'Airing', stopped: 'Upcoming & On Hiatus', complete: 'Completed Series' };
    const sectionTitle = sectionTitles[activeTab] || 'Shows';

    let html = `<div class="shows-section">
        <div class="section-title-row">
            <h2>${sectionTitle}</h2>
            <span class="count-tag">${filteredShows.length} show${filteredShows.length === 1 ? '' : 's'}</span>
        </div>
        <div class="shows-grid">`;

    html += filteredShows.map(show => renderCard(show, todayName)).join('');
    html += '</div></div>';
    containerEl.innerHTML = html;
    rebuildCardElementMap();
}

function renderCard(show, todayName) {
    const isReleasingToday = STATUS.isAiring(show) && show.releaseDay === todayName;
    const safePoster = safePosterUrl(show.poster);
    const gradientBg = getPosterGradient(show.title);
    const initials = getInitials(show.title);
    const firstChineseChar = show.titleZh ? show.titleZh.slice(0, 2) : '';
    const countdownHtml = getCountdownHtml(show);
    const watchUrl = getWatchUrl(show);

    let posterHtml = '';
    if (safePoster) {
        const cached = ImageCache.has(safePoster);
        posterHtml = `<div class="card-poster-wrapper${cached ? '' : ' poster-loading'}"><img class="card-poster" src="${safePoster}" alt="${show.title}" loading="lazy"></div>`;
        if (!cached) {
            ImageCache.loadImage(safePoster).catch(() => {}); // cache for future
        }
    }

    const placeholderHtml = `<div class="poster-placeholder" style="background: ${gradientBg}">
        <div class="placeholder-symbol"><i class="fa-solid fa-dragon"></i></div>
        <div class="placeholder-text">${initials}</div>
        ${firstChineseChar ? `<div class="placeholder-zh">${firstChineseChar}</div>` : ''}
    </div>`;

    const genreTags = show.genre ? show.genre.split(',').map(g => `<span class="genre-tag">${escapeHtml(g.trim())}</span>`).join('') : '';

    return `<article class="show-card ${isReleasingToday ? 'releasing-today' : ''}" data-id="${show.id}">
        <div class="card-header">
            <div class="card-badges">
                <span class="status-badge ${show.status}">${STATUS.getDisplayName(show.status)}</span>
                ${show.isFavorite ? '<span class="status-badge favorite"><i class="fa-solid fa-heart"></i></span>' : ''}
                ${isReleasingToday ? '<span class="status-badge releasing-today-badge">Airs Today</span>' : ''}
            </div>
            ${genreTags ? `<div class="card-genres">${genreTags}</div>` : ''}
            ${safePoster ? posterHtml : ''}
            ${placeholderHtml}
            <div class="card-overlay"></div>
        </div>
        <div class="card-body">
            <div class="card-title-block">
                <h3 class="card-title" title="${escapeHtml(show.title)}">${escapeHtml(show.title)}</h3>
                ${show.titleZh ? `<div class="card-title-zh">${escapeHtml(show.titleZh)}</div>` : ''}
            </div>
            ${countdownHtml}
            <div class="card-schedule-info">
                <i class="fa-regular fa-calendar"></i>
                ${STATUS.isCompleted(show) ? '<span>Series Complete</span>' : `<span>${show.releaseDay}s at ${show.releaseTime}</span>`}
            </div>
            <div class="card-progress-section">
                <div class="progress-header">
                    <div class="ep-counter">Watched: <span>${show.currentEp} episodes</span></div>
                    <div class="ep-buttons">
                        <button class="ep-btn btn-minus" title="Decrease episode"><i class="fa-solid fa-minus"></i></button>
                        <button class="ep-btn btn-plus" title="Increase episode"><i class="fa-solid fa-plus"></i></button>
                    </div>
                </div>
                ${show.totalEp > 0 ? `<div class="progress-bar-container"><div class="progress-bar-fill" style="width:${Math.min(100, (show.currentEp / show.totalEp) * 100)}%"></div></div>` : ''}
            </div>
            <p class="card-notes" title="${escapeHtml(show.notes || '')}">
                ${show.notes ? escapeHtml(show.notes) : '<span style="color: var(--text-muted); font-style: italic;">No synopsis added.</span>'}
            </p>
            <div class="card-actions">
                <button class="watch-link watch-btn" data-watch-url="${watchUrl}">
                    <i class="fa-solid fa-circle-play"></i> Stream
                </button>
                <a class="countdown-link" href="${/^https?:\/\//i.test(show.countdownUrl || '') ? show.countdownUrl : 'https://www.google.com/search?q=site:animecountdown.com+' + encodeURIComponent(show.title)}" target="_blank" rel="noopener noreferrer">
                    <i class="fa-solid fa-hourglass-half"></i> Countdown
                </a>
                <div class="card-ctrls">
                    <button class="ctrl-btn notify-btn" data-notify="${getNotifyPref(show.id)}" title="${getNotifyPref(show.id) ? 'Notifications On' : 'Notifications Off'}"><i class="${getNotifyPref(show.id) ? 'fa-solid fa-bell' : 'fa-regular fa-bell'}"></i></button>
                    <button class="ctrl-btn edit-btn" title="Edit show"><i class="fa-solid fa-pen-to-square"></i></button>
                    <button class="ctrl-btn favorite-btn" data-fav="${show.isFavorite}" title="${show.isFavorite ? 'Unfavorite' : 'Favorite'}"><i class="${show.isFavorite ? 'fa-solid' : 'fa-regular'} fa-heart"></i></button>
                    <button class="ctrl-btn delete-btn" title="Delete show"><i class="fa-solid fa-trash"></i></button>
                </div>
            </div>
        </div>
    </article>`;
}

function renderFavoritesGrid() {
    const containerEl = document.getElementById('shows-sections-container');
    const emptyStateEl = document.getElementById('empty-state');
    if (!containerEl) return;

    const genreFilter = filters.genre ? filters.genre.toLowerCase() : '';
    const filteredShows = shows.filter(s => {
        if (!s.isFavorite) return false;
        if (filters.search && !fuzzyMatch(s.title, filters.search) && !fuzzyMatch(s.titleZh, filters.search)) return false;
        if (genreFilter && !(s.genre && s.genre.toLowerCase().includes(genreFilter))) return false;
        return true;
    });
    filteredShows.sort((a, b) => (b.lastUpdated || 0) - (a.lastUpdated || 0));

    if (filteredShows.length === 0) {
        containerEl.innerHTML = '';
        showEmptyState(emptyStateEl, 'No Favorites', 'Tap the heart icon on any show card to add it to your favorites!', false);
        return;
    }

    emptyStateEl.style.display = 'none';
    const todayName = getTodayName();
    let html = `<div class="shows-section">
        <div class="section-title-row">
            <h2><i class="fa-solid fa-heart" style="color: var(--accent-rose);"></i> Favorites</h2>
            <span class="count-tag">${filteredShows.length} show${filteredShows.length === 1 ? '' : 's'}</span>
        </div>
        <div class="shows-grid">`;
    html += filteredShows.map(show => renderCard(show, todayName)).join('');
    html += '</div></div>';
    containerEl.innerHTML = html;
    rebuildCardElementMap();
}

function showEmptyState(el, title, text, showReset) {
    if (!el) return;
    el.style.display = 'flex';
    el.className = 'empty-state';
    el.innerHTML = `
        <i class="fa-solid fa-seedling"></i>
        <h3>${title}</h3>
        <p>${text}</p>
        ${showReset ? '<button class="btn btn-secondary" id="btn-reset-filters" style="margin-top: 0.5rem;">Reset Filters</button>' : ''}
    `;
    const resetBtn = el.querySelector('#btn-reset-filters');
    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            filters.search = '';
            filters.genre = '';
            const searchInput = document.getElementById('search-input');
            if (searchInput) searchInput.value = '';
            renderShowsGrid();
        });
    }
}

let _plusLongPress = false;

function setupCardActions() {
    const container = document.getElementById('shows-sections-container');

    let longPressTimer = null;
    container?.addEventListener('touchstart', (e) => {
        if (!e.target.closest('.btn-plus')) return;
        const card = e.target.closest('.show-card');
        if (!card) return;
        longPressTimer = setTimeout(() => {
            _plusLongPress = true;
            incrementEpisode(card.dataset.id, 5);
            const btn = e.target.closest('.btn-plus');
            if (btn) {
                btn.style.transition = 'transform 0.1s';
                btn.style.transform = 'scale(1.4)';
                setTimeout(() => btn.style.transform = '', 200);
            }
        }, 500);
    }, { passive: true });

    container?.addEventListener('touchend', () => {
        if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null; }
    }, { passive: true });

    container?.addEventListener('touchcancel', () => {
        if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null; }
    }, { passive: true });

    container?.addEventListener('click', (e) => {
        const card = e.target.closest('.show-card');
        if (!card) return;

        const isPlus = e.target.closest('.btn-plus');
        const isMinus = e.target.closest('.btn-minus');
        const isEdit = e.target.closest('.edit-btn');
        const isDelete = e.target.closest('.delete-btn');
        const isFavorite = e.target.closest('.favorite-btn');
        const isNotify = e.target.closest('.notify-btn');
        const isStream = e.target.closest('.watch-btn');
        const isCountdown = e.target.closest('.countdown-link');

        const showId = card.dataset.id;
        const show = getShowById(showId);
        if (!show) return;

        if (isPlus && !_plusLongPress) { incrementEpisode(showId); }
        else if (isMinus) { decrementEpisode(showId); }
        else if (isEdit) { openModal(show); }
        else if (isFavorite) {
            toggleFavorite(showId);
            renderShowsGrid();
            const btn = document.querySelector(`.show-card[data-id="${showId}"] .favorite-btn`);
            if (btn) { btn.classList.add('animate'); setTimeout(() => btn.classList.remove('animate'), 500); }
        }
        else if (isNotify) {
            const enabled = toggleNotifyForShow(showId);
            const btn = card.querySelector('.notify-btn');
            if (btn) {
                btn.dataset.notify = enabled;
                btn.title = enabled ? 'Notifications On' : 'Notifications Off';
                btn.innerHTML = `<i class="${enabled ? 'fa-solid' : 'fa-regular'} fa-bell"></i>`;
            }
            if (enabled && STATUS.isAiring(show)) {
                scheduleReminderForShow(show);
            } else if (!enabled) {
                try { DB.cancelReminder(showId, show.alarmRequestCode || hashCode(showId)); } catch (_) {}
            }
        }
        else if (isDelete) {
            if (confirm(`Are you sure you want to remove "${show.title}" from your list?`)) {
                deleteShow(showId);
            }
        }
        else if (isStream) { _plusLongPress = false; openWatchScreen(isStream.dataset.watchUrl); return; }
        else if (isCountdown) { _plusLongPress = false; return; }
        else { openDetailsModal(show); }
        _plusLongPress = false;
    });
}

function renderContinueWatching() {
    const section = document.getElementById('continue-watching-section');
    const scroll = document.getElementById('continue-watching-scroll');
    const count = document.getElementById('continue-count');
    if (!section || !scroll) return;

    const shows = getContinueWatchingShows();
    if (shows.length === 0) {
        section.style.display = 'none';
        return;
    }

    section.style.display = '';
    if (count) count.textContent = `${shows.length} show${shows.length === 1 ? '' : 's'}`;

    let html = '';
    for (const show of shows) {
        const safePoster = safePosterUrl(show.poster);
        const watchUrl = getWatchUrl(show);
        const progress = show.totalEp > 0 ? Math.min(100, Math.round((show.currentEp / show.totalEp) * 100)) : 0;
        const progressLabel = show.totalEp > 0 ? `${show.currentEp}/${show.totalEp}` : `${show.currentEp} ep`;

        let posterHtml = '';
        if (safePoster) {
            const cached = ImageCache.has(safePoster);
            posterHtml = `<div class="cw-poster${cached ? '' : ' poster-loading'}"><img class="cw-poster-img" src="${safePoster}" alt="${show.title}" loading="lazy"></div>`;
            if (!cached) ImageCache.loadImage(safePoster).catch(() => {});
        } else {
            posterHtml = `<div class="cw-poster cw-poster-placeholder" style="background:${getPosterGradient(show.title)}"><span>${getInitials(show.title)}</span></div>`;
        }

        html += `<div class="cw-item" data-id="${show.id}">
            ${posterHtml}
            <div class="cw-body">
                <div class="cw-title" title="${escapeHtml(show.title)}">${escapeHtml(show.title)}</div>
                <div class="cw-progress-row">
                    <span class="cw-progress-label">${progressLabel}</span>
                    ${show.totalEp > 0 ? `<div class="cw-progress-bar"><div class="cw-progress-fill" style="width:${progress}%"></div></div>` : ''}
                </div>
                <button class="cw-watch-btn" data-watch-url="${watchUrl}" title="Watch ${escapeHtml(show.title)}">
                    <i class="fa-solid fa-circle-play"></i> <span>Continue</span>
                </button>
            </div>
        </div>`;
    }
    scroll.innerHTML = html;
}

function renderRecentlyUpdated() {
    const section = document.getElementById('recently-updated-section');
    const scroll = document.getElementById('recently-updated-scroll');
    const count = document.getElementById('recently-updated-count');
    if (!section || !scroll) return;

    const showsList = getRecentlyUpdatedShows();
    if (showsList.length === 0) {
        section.style.display = 'none';
        return;
    }

    section.style.display = '';
    if (count) count.textContent = `${showsList.length} show${showsList.length === 1 ? '' : 's'}`;

    let html = '';
    for (const show of showsList) {
        const safePoster = safePosterUrl(show.poster);
        const timeAgo = getTimeAgo(show.lastUpdated);

        let posterHtml = '';
        if (safePoster) {
            const cached = ImageCache.has(safePoster);
            posterHtml = `<div class="ru-poster${cached ? '' : ' poster-loading'}"><img class="ru-poster-img" src="${safePoster}" alt="${show.title}" loading="lazy"></div>`;
            if (!cached) ImageCache.loadImage(safePoster).catch(() => {});
        } else {
            posterHtml = `<div class="ru-poster ru-poster-placeholder" style="background:${getPosterGradient(show.title)}"><span>${getInitials(show.title)}</span></div>`;
        }

        const statusIcon = STATUS.isCompleted(show) ? 'circle-check' : (STATUS.isStopped(show) ? 'pause' : 'tv');
        const statusColor = STATUS.isCompleted(show) ? 'var(--accent-green)' : (STATUS.isStopped(show) ? 'var(--text-muted)' : 'var(--accent-cyan)');

        html += `<div class="ru-item" data-id="${show.id}">
            ${posterHtml}
            <div class="ru-body">
                <div class="ru-title" title="${escapeHtml(show.title)}">${escapeHtml(show.title)}</div>
                <div class="ru-meta">
                    <span><i class="fa-solid fa-${statusIcon}" style="color:${statusColor}"></i> ${show.currentEp}/${show.totalEp || '?'} eps</span>
                    <span class="ru-ep-badge"><i class="fa-solid fa-clock"></i> ${timeAgo}</span>
                </div>
            </div>
        </div>`;
    }
    scroll.innerHTML = html;
}

function getTimeAgo(timestamp) {
    if (!timestamp) return '';
    const diff = Date.now() - timestamp;
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d ago`;
    const weeks = Math.floor(days / 7);
    if (weeks < 5) return `${weeks}w ago`;
    return `${Math.floor(days / 30)}mo ago`;
}

function setupContinueWatching() {
    const scroll = document.getElementById('continue-watching-scroll');
    if (!scroll) return;

    scroll.addEventListener('click', (e) => {
        const item = e.target.closest('.cw-item');
        if (!item) return;
        const showId = item.dataset.id;
        const watchBtn = e.target.closest('.cw-watch-btn');
        if (watchBtn) {
            const url = watchBtn.dataset.watchUrl;
            if (url) { openWatchScreen(url); return; }
        }
        const show = getShowById(showId);
        if (show) openDetailsModal(show);
    });
}

function initAdvancedFilters() {
    const studioEl = document.getElementById('filter-studio');
    const yearEl = document.getElementById('filter-year');
    if (!studioEl || studioEl.dataset.initialized) return;
    studioEl.dataset.initialized = 'true';

    const studios = new Set();
    const years = new Set();
    shows.forEach(s => {
        if (s.studio) studios.add(s.studio);
        if (s.releaseYear) years.add(String(s.releaseYear));
    });
    [...studios].sort().forEach(s => {
        studioEl.innerHTML += `<option value="${escapeHtml(s)}">${escapeHtml(s)}</option>`;
    });
    [...years].sort().reverse().forEach(y => {
        yearEl.innerHTML += `<option value="${y}">${y}</option>`;
    });

    const applyFilter = () => {
        filters.studio = studioEl.value;
        filters.year = yearEl.value;
        filters.minRating = document.getElementById('filter-min-rating')?.value || '';
        filters.maxEpisodes = document.getElementById('filter-max-episodes')?.value || '';
        filters.sortBy = document.getElementById('filter-sort')?.value || 'countdown';
        renderShowsGrid();
    };

    [studioEl, yearEl,
      document.getElementById('filter-min-rating'),
      document.getElementById('filter-max-episodes'),
      document.getElementById('filter-sort')
    ].forEach(el => {
        if (el) el.addEventListener('change', applyFilter);
    });

    const clearBtn = document.getElementById('btn-clear-filters');
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            filters.studio = '';
            filters.year = '';
            filters.minRating = '';
            filters.maxEpisodes = '';
            filters.sortBy = 'countdown';
            filters.genre = '';
            filters.search = '';
            document.getElementById('search-input').value = '';
            document.getElementById('genre-filter-bar').querySelectorAll('.genre-chip').forEach(c => c.classList.remove('active'));
            studioEl.value = '';
            yearEl.value = '';
            if (document.getElementById('filter-min-rating')) document.getElementById('filter-min-rating').value = '';
            if (document.getElementById('filter-max-episodes')) document.getElementById('filter-max-episodes').value = '';
            if (document.getElementById('filter-sort')) document.getElementById('filter-sort').value = 'countdown';
            renderShowsGrid();
        });
    }
}
