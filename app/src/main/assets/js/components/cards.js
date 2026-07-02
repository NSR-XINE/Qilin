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

    let filteredShows = shows.filter(show => {
        const matchesSearch = (() => {
            if (!filters.search) return true;
            const q = filters.search.trim().toLowerCase();
            return fuzzyMatch(show.title, q) ||
                   fuzzyMatch(show.titleZh, q) ||
                   fuzzyMatch(show.notes, q);
        })();
        const matchesGenre = !genreFilter || (show.genres && show.genres.toLowerCase().includes(genreFilter));
        return matchesSearch && matchesGenre && statusFilters.includes(show.status);
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
        posterHtml = `<div class="card-poster-wrapper"><img class="card-poster" src="${safePoster}" alt="${show.title}" loading="lazy" onerror="var w=this.closest('.card-poster-wrapper');var h=this.closest('.card-header');if(w&&h){w.style.display='none';h.querySelector('.poster-placeholder').style.display='flex'}"></div>`;
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
                <button class="watch-link watch-btn" data-watch-url="${watchUrl}" onclick="openWatchScreen(this.getAttribute('data-watch-url'))">
                    <i class="fa-solid fa-circle-play"></i> Stream
                </button>
                <a class="countdown-link" href="${/^https?:\/\//i.test(show.countdownUrl || '') ? show.countdownUrl : 'https://www.google.com/search?q=site:animecountdown.com+' + encodeURIComponent(show.title)}" target="_blank" rel="noopener noreferrer">
                    <i class="fa-solid fa-hourglass-half"></i> Countdown
                </a>
                <div class="card-ctrls">
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
        else if (isDelete) {
            if (confirm(`Are you sure you want to remove "${show.title}" from your list?`)) {
                deleteShow(showId);
            }
        }
        else if (isStream || isCountdown) { return; }
        else { openDetailsModal(show); }
        _plusLongPress = false;
    });
}
