let activeTab = 'home';
Object.defineProperty(window, 'activeTab', { get() { return activeTab; }, set(v) { activeTab = v; } });

function switchTab(tabName) {
    if (tabName === 'add') return;
    activeTab = tabName;

    document.body.classList.remove('tab-home', 'tab-airing', 'tab-stopped', 'tab-complete');
    document.body.classList.add(`tab-${tabName}`);

    document.querySelectorAll('.bottom-nav .nav-item').forEach(btn => btn.classList.remove('active'));
    const activeBtn = document.getElementById(`nav-btn-${tabName}`);
    if (activeBtn) activeBtn.classList.add('active');

    const searchPanel = document.querySelector('.panel-search');
    const scheduleContainer = document.querySelector('.schedule-container');
    const heroBanner = document.getElementById('next-up-banner');
    const sectionsContainer = document.getElementById('shows-sections-container');
    const controlPanel = document.querySelector('.control-panel');
    const contentArea = document.querySelector('.content-area');
    const emptyStateEl = document.getElementById('empty-state');
    const continueWatching = document.getElementById('continue-watching-section');
    const recentlyUpdated = document.getElementById('recently-updated-section');

    const isMobile = window.innerWidth <= 1024;
    if (isMobile) {
        [searchPanel, scheduleContainer, heroBanner, sectionsContainer, emptyStateEl, continueWatching, recentlyUpdated].forEach(el => {
            if (el) el.style.setProperty('display', 'none', 'important');
        });
        if (controlPanel) controlPanel.style.setProperty('display', 'none', 'important');
        if (contentArea) contentArea.style.setProperty('display', 'none', 'important');

        if (tabName === 'home') {
            if (contentArea) contentArea.style.setProperty('display', 'block', 'important');
            if (scheduleContainer) scheduleContainer.style.setProperty('display', 'block', 'important');
            if (heroBanner) { heroBanner.style.removeProperty('display'); renderHeroBanner(); }
            if (continueWatching) { continueWatching.style.removeProperty('display'); renderContinueWatching(); }
            if (recentlyUpdated) { recentlyUpdated.style.removeProperty('display'); renderRecentlyUpdated(); }
            applyDashboardToggles();
            if (emptyStateEl && shows.length === 0) emptyStateEl.style.setProperty('display', 'flex', 'important');
        } else if (tabName === 'airing' || tabName === 'complete' || tabName === 'stopped') {
            if (contentArea) contentArea.style.setProperty('display', 'block', 'important');
            if (sectionsContainer) sectionsContainer.style.setProperty('display', 'block', 'important');
            renderShowsGrid();
        }
        const showFilterBars = tabName === 'airing' || tabName === 'complete' || tabName === 'stopped';
        const genreBar = document.getElementById('genre-filter-bar');
        const advFilterBar = document.getElementById('advanced-filter-bar');
        if (genreBar) {
            genreBar.style.setProperty('display', showFilterBars ? 'flex' : 'none', 'important');
            if (showFilterBars) populateGenreFilter();
        }
        if (advFilterBar) {
            advFilterBar.style.setProperty('display', showFilterBars ? 'flex' : 'none', 'important');
            if (showFilterBars) initAdvancedFilters();
        }
    } else {
        [searchPanel, scheduleContainer, sectionsContainer, controlPanel, contentArea].forEach(el => {
            if (el) el.style.display = '';
        });
        renderHeroBanner();
        renderContinueWatching();
        renderRecentlyUpdated();
        const showFilterBars = tabName === 'airing' || tabName === 'complete' || tabName === 'stopped';
        const genreBar = document.getElementById('genre-filter-bar');
        const advFilterBar = document.getElementById('advanced-filter-bar');
        if (genreBar) {
            genreBar.style.display = showFilterBars ? 'flex' : 'none';
            if (showFilterBars) populateGenreFilter();
        }
        if (advFilterBar) {
            advFilterBar.style.display = showFilterBars ? 'flex' : 'none';
            if (showFilterBars) initAdvancedFilters();
        }
    }

    window.scrollTo({ top: 0, behavior: 'instant' });
}

function triggerAddModal() {
    openModal();
}

function openDetailsById(showId) {
    const show = getShowById(showId);
    if (show) {
        if (STATUS.isAiring(show)) switchTab('airing');
        else if (STATUS.isCompleted(show)) switchTab('complete');
        else if (STATUS.isStopped(show)) switchTab('stopped');
        else switchTab('home');
        openDetailsModal(show);
    }
}

function openDrawer() {
    const drawer = document.getElementById('right-drawer');
    const overlay = document.getElementById('drawer-overlay');
    if (drawer) drawer.classList.add('open');
    if (overlay) { overlay.style.display = 'block'; setTimeout(() => overlay.classList.add('open'), 10); }
    document.body.classList.add('modal-open');
    setTimeout(setupDrawerSwipe, 50);
}

function setupDrawerSwipe() {
    const drawer = document.getElementById('right-drawer');
    if (!drawer || drawer.dataset.swipeAttached) return;
    drawer.dataset.swipeAttached = '1';
    let startX = 0, isDragging = false;
    drawer.addEventListener('touchstart', (e) => {
        startX = e.touches[0].clientX;
        isDragging = true;
        drawer.style.transition = 'none';
    }, { passive: true });
    drawer.addEventListener('touchmove', (e) => {
        if (!isDragging) return;
        const dx = e.touches[0].clientX - startX;
        if (dx < 0) { isDragging = false; drawer.style.transform = ''; drawer.style.transition = ''; return; }
        drawer.style.transform = `translateX(${Math.min(dx, drawer.offsetWidth)}px)`;
    }, { passive: true });
    drawer.addEventListener('touchend', (e) => {
        if (!isDragging) return;
        isDragging = false;
        const dx = e.changedTouches[0].clientX - startX;
        drawer.style.transition = 'transform 0.25s ease';
        if (dx > 50) {
            drawer.style.transform = 'translateX(100%)';
            setTimeout(closeDrawer, 220);
        } else {
            drawer.style.transform = '';
            setTimeout(() => { drawer.style.transition = ''; }, 250);
        }
    }, { passive: true });
}

function closeDrawer() {
    const drawer = document.getElementById('right-drawer');
    const overlay = document.getElementById('drawer-overlay');
    if (drawer) {
        drawer.classList.remove('open');
        setTimeout(() => { drawer.style.transform = ''; drawer.style.transition = ''; }, 300);
    }
    if (overlay) {
        overlay.classList.remove('open');
        setTimeout(() => overlay.style.display = 'none', 300);
    }
    document.body.classList.remove('modal-open');
}

let _genreList = [];

function populateGenreFilter() {
    const bar = document.getElementById('genre-filter-bar');
    if (!bar) return;
    const allGenres = new Set();
    shows.forEach(s => {
        if (s.genres) s.genres.split(',').map(g => g.trim().toLowerCase()).filter(Boolean).forEach(g => allGenres.add(g));
    });
    const sorted = Array.from(allGenres).sort();
    if (JSON.stringify(sorted) === JSON.stringify(_genreList) && bar.children.length > 0) return;
    _genreList = sorted;
    let html = `<button class="genre-filter-btn ${!filters.genre ? 'active' : ''}" data-genre="">All</button>`;
    sorted.forEach(g => {
        html += `<button class="genre-filter-btn ${filters.genre === g ? 'active' : ''}" data-genre="${g}">${escapeHtml(g.charAt(0).toUpperCase() + g.slice(1))}</button>`;
    });
    bar.innerHTML = html;
    Array.from(bar.children).forEach(btn => {
        btn.addEventListener('click', () => {
            filters.genre = btn.dataset.genre;
            Array.from(bar.children).forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            if (activeTab === 'seasonal') renderSeasonalView();
            else renderShowsGrid();
        });
    });
}
