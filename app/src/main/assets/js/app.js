let lastSyncTime = Date.now();
let syncInterval = null;
let _renderRafId = null;

function _debouncedRender(fn) {
    if (_renderRafId) cancelAnimationFrame(_renderRafId);
    _renderRafId = requestAnimationFrame(() => {
        _renderRafId = null;
        try { fn(); } catch (e) { console.error('Render error', e); }
    });
}

const _debouncedRenderSchedule = () => _debouncedRender(renderWeeklySchedule);
const _debouncedRenderBanner = () => _debouncedRender(renderHeroBanner);
const _debouncedRenderGrid = () => _debouncedRender(renderShowsGrid);
const _debouncedRenderContinueWatching = () => _debouncedRender(renderContinueWatching);

window.addEventListener('error', (e) => {
    console.error('Uncaught error:', e.message, e.filename, e.lineno);
    try {
        const toast = document.querySelector('.toast-notification');
        if (!toast) showToast('An error occurred. Check logs for details.', 4000);
    } catch (_) {}
    return true;
});

window.addEventListener('unhandledrejection', (e) => {
    console.error('Unhandled promise rejection:', e.reason);
    e.preventDefault();
});

function showToast(message, duration = 2000) {
    const existing = document.querySelector('.toast-notification');
    if (existing) existing.remove();
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.textContent = message;
    document.body.appendChild(toast);
    requestAnimationFrame(() => toast.classList.add('show'));
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

function updateLastSyncDisplay() {
    const el = document.getElementById('last-sync-time');
    if (!el) return;
    const diff = Date.now() - lastSyncTime;
    const seconds = Math.floor(diff / 1000);
    if (seconds < 60) {
        el.textContent = 'Last sync: ' + seconds + 's ago';
    } else {
        const minutes = Math.floor(seconds / 60);
        el.textContent = 'Last sync: ' + minutes + ' min ago';
    }
}

function refreshAllData() {
    autoFetchPosters();
    persistState();
    resumeTimers();
    lastSyncTime = Date.now();
    updateLastSyncDisplay();
    pushWidgetUpdate();
}

function pushWidgetUpdate() {
    if (!DB._available || typeof AndroidApp.updateWidget !== 'function') return;
    try {
        const nextShow = getContinueWatchingShow() || getNextUpShow();
        if (!nextShow) {
            AndroidApp.updateWidget('No shows', '--d --h --m', 'Add a show to get started');
            return;
        }
        const countdownInfo = getNextReleaseDate(nextShow.releaseDay, nextShow.releaseTime);
        let countdownStr = '--d --h --m';
        if (countdownInfo.airingNow) {
            countdownStr = 'AIRING NOW';
        } else if (countdownInfo.targetDate) {
            const diff = countdownInfo.targetDate - Date.now();
            if (diff > 0) {
                const d = Math.floor(diff / 86400000);
                const h = Math.floor((diff % 86400000) / 3600000);
                const m = Math.floor((diff % 3600000) / 60000);
                countdownStr = d + 'd ' + h + 'h ' + m + 'm';
            }
        }
        const epStr = nextShow.totalEp > 0
            ? 'Ep ' + (nextShow.currentEp + 1) + ' / ' + nextShow.totalEp
            : 'Ep ' + (nextShow.currentEp + 1);
        AndroidApp.updateWidget(nextShow.title, countdownStr, epStr);
    } catch (e) {
        console.error('Widget push failed', e);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    ImageCache.init();
    SyncManager.init();
    loadState();
    initTheme();

    let source = 'donghuastream';
    if (DB._available) source = DB.getSetting('pref_streaming_source', '') || DB.getLocal('pref_streaming_source') || 'donghuastream';
    else source = DB.getLocal('pref_streaming_source') || 'donghuastream';
    updateSourceUI(source);

    setupResizeObservers();
    updateStats();
    _debouncedRenderSchedule();
    _debouncedRenderBanner();
    _debouncedRenderContinueWatching();
    _debouncedRenderGrid();
    resumeTimers();
    pushWidgetUpdate();

    document.getElementById('btn-export-data')?.addEventListener('click', exportData);

    setupImportModal();
    setupDetailsModal();
    setupSettingsModal();
    setupFormSubmission();
    setupSearch();
    setupScheduleTabs();
    setupCardActions();
    setupContinueWatching();
    setupPosterFetch();

    LOADING_MESSAGES.forEach((msg, idx) => {
        setTimeout(() => {
            const el = document.getElementById('loading-status-text');
            if (el) el.innerText = msg;
        }, idx * 500);
    });

    setTimeout(() => {
        const loader = document.getElementById('app-loading-screen');
        if (loader) {
            loader.classList.add('fade-out');
            document.body.classList.remove('loading-active');
            setTimeout(() => loader.remove(), 600);
        }
    }, 3500);

    function showLoadingOffline() {
        const msg = document.getElementById('loading-offline-msg');
        if (msg) msg.style.display = 'flex';
    }

    function setOfflineState(offline) {
        const banner = document.getElementById('offline-banner');
        if (banner) banner.style.display = offline ? 'flex' : 'none';
        if (offline) showLoadingOffline();
    }

    function checkConnectivity() {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 5000);
        fetch('https://httpbin.org/anything', { signal: controller.signal, mode: 'no-cors' })
            .then(() => { clearTimeout(timeout); setOfflineState(false); })
            .catch(() => {
                fetch('https://www.google.com/generate_204', { signal: controller.signal, mode: 'no-cors' })
                    .then(() => { clearTimeout(timeout); setOfflineState(false); })
                    .catch(() => { clearTimeout(timeout); setOfflineState(true); });
            });
    }

    if (!navigator.onLine) setOfflineState(true);
    checkConnectivity();

    window.addEventListener('online', () => setOfflineState(false));
    window.addEventListener('offline', () => setOfflineState(true));

    addSwipeToDismiss('settings-modal', closeSettingsModal);
    addSwipeToDismiss('donghua-modal', closeModal);
    addSwipeToDismiss('import-modal', closeImportModal);
    addSwipeToDismiss('details-modal', closeDetailsModal);
    addSwipeToDismiss('exit-modal', closeExitModal);
    addSwipeToDismiss('history-modal', closeHistoryModal);
    addSwipeToDismiss('stats-modal', closeStatsModal);
    addSwipeToDismiss('calendar-modal', closeCalendarModal);
    addSwipeToDismiss('collections-modal', closeCollectionsModal);
    addSwipeToDismiss('seasonal-modal', closeSeasonalModal);

    document.getElementById('btn-open-drawer')?.addEventListener('click', openDrawer);
    document.getElementById('btn-close-drawer')?.addEventListener('click', closeDrawer);
    document.getElementById('drawer-overlay')?.addEventListener('click', closeDrawer);

    document.getElementById('nav-btn-home')?.addEventListener('click', () => switchTab('home'));
    document.getElementById('nav-btn-airing')?.addEventListener('click', () => switchTab('airing'));
    document.getElementById('nav-btn-add')?.addEventListener('click', triggerAddModal);
    document.getElementById('nav-btn-complete')?.addEventListener('click', () => switchTab('complete'));
    document.getElementById('nav-btn-stopped')?.addEventListener('click', () => switchTab('stopped'));
    document.getElementById('btn-exit-cancel')?.addEventListener('click', closeExitModal);
    document.getElementById('btn-exit-confirm')?.addEventListener('click', confirmExitApp);

    switchTab('home');

    document.getElementById('btn-close-modal')?.addEventListener('click', closeModal);
    document.getElementById('btn-cancel-modal')?.addEventListener('click', closeModal);
    document.getElementById('donghua-modal')?.addEventListener('click', (e) => {
        if (e.target.id === 'donghua-modal') closeModal();
    });

    document.addEventListener('contextmenu', e => e.preventDefault());

    window.addEventListener('resize', () => switchTab(activeTab));

    let touchStartY = 0;
    let touchStartX = 0;
    let touchMoveY = 0;
    let isPulling = false;
    let isRefreshing = false;

    function isDrawerOpen() {
        return document.getElementById('right-drawer')?.classList.contains('open') ?? false;
    }

    function updatePtrTop() {
        const header = document.querySelector('.app-header');
        const top = header ? header.getBoundingClientRect().bottom : 0;
        ptrIndicator.style.setProperty('--ptr-top', top + 'px');
    }

    const ptrIndicator = document.createElement('div');
    ptrIndicator.className = 'pull-to-refresh-indicator';
    ptrIndicator.innerHTML = '<i class="fa-solid fa-spinner"></i><span>Pull to refresh</span>';
    document.body.appendChild(ptrIndicator);
    updatePtrTop();
    window.addEventListener('resize', updatePtrTop);
    try {
        if (screen.orientation) {
            screen.orientation.addEventListener('change', () => {
                switchTab(activeTab);
                _debouncedRenderBanner();
                _debouncedRenderGrid();
            });
        }
    } catch (e) {}

    function resetPtr() {
        isPulling = false;
        ptrIndicator.classList.remove('visible');
        const icon = ptrIndicator.querySelector('i');
        if (icon) icon.className = 'fa-solid fa-spinner';
        const span = ptrIndicator.querySelector('span');
        if (span) span.textContent = 'Pull to refresh';
        touchStartY = 0;
        touchMoveY = 0;
    }

    document.body.addEventListener('touchstart', (e) => {
        if (isRefreshing || isDrawerOpen() || !navigator.onLine) return;
        if (document.documentElement.scrollTop <= 0) {
            touchStartY = e.touches[0].clientY;
            touchStartX = e.touches[0].clientX;
            touchMoveY = touchStartY;
            isPulling = true;
        }
    }, { passive: true });

    document.body.addEventListener('touchmove', (e) => {
        if (!isPulling || isRefreshing) return;
        touchMoveY = e.touches[0].clientY;
        const pullDist = touchMoveY - touchStartY;
        const moveX = e.touches[0].clientX - touchStartX;
        if (Math.abs(moveX) > Math.abs(pullDist)) { resetPtr(); return; }
        if (pullDist > 0) {
            ptrIndicator.classList.add('visible');
            const icon = ptrIndicator.querySelector('i');
            if (icon) icon.className = 'fa-solid fa-spinner fa-spin';
            const span = ptrIndicator.querySelector('span');
            if (span) span.textContent = pullDist > 60 ? 'Release to refresh' : 'Pull to refresh';
        }
    }, { passive: true });

    function onTouchEnd() {
        if (!isPulling || !navigator.onLine) { resetPtr(); return; }
        const pullDist = touchMoveY - touchStartY;
        if (isRefreshing) return;
        if (pullDist > 60) {
            isRefreshing = true;
            const icon = ptrIndicator.querySelector('i');
            if (icon) icon.className = 'fa-solid fa-spinner fa-spin';
            const span = ptrIndicator.querySelector('span');
            if (span) span.textContent = 'Refreshing...';
            refreshAllData();
            setTimeout(() => {
                isRefreshing = false;
                resetPtr();
            }, 1200);
            return;
        }
        resetPtr();
    }

    document.body.addEventListener('touchend', onTouchEnd, { passive: true });
    document.body.addEventListener('touchcancel', resetPtr, { passive: true });

    syncInterval = setInterval(() => {
        autoFetchPosters();
        pushWidgetUpdate();
        updateLastSyncDisplay();
    }, 300000);

    setInterval(updateLastSyncDisplay, 10000);

    window.addEventListener('beforeunload', () => {
        pauseTimers();
        _cleanupAllPosterCallbacks();
        if (syncInterval) clearInterval(syncInterval);
    });

    resetPtr();
});

window.pauseTimers = pauseTimers;
window.resumeTimers = resumeTimers;
window.getWatchUrl = getWatchUrl;
window.getWatchUrlById = getWatchUrlById;
window.selectPreferredSource = selectPreferredSource;
window.updateSourceUI = updateSourceUI;
window.openWatchScreen = openWatchScreen;
window.exportData = exportData;
window.importData = importData;
window.openDrawer = openDrawer;
window.closeDrawer = closeDrawer;
window.hashCode = hashCode;
window.openDetailsModal = openDetailsModal;
window.openModal = openModal;
window.closeModal = closeModal;
window.openSettingsModal = openSettingsModal;
window.closeSettingsModal = closeSettingsModal;
window.showExitModal = showExitModal;
window.closeExitModal = closeExitModal;
window.confirmExitApp = confirmExitApp;
window.openDetailsById = openDetailsById;
window.setThemeMode = setThemeMode;
window.handleBackPress = handleBackPress;
window.closeTopModal = closeTopModal;
window.switchTab = switchTab;
window.triggerAddModal = triggerAddModal;
window.renderShowsGrid = renderShowsGrid;
window.renderHeroBanner = renderHeroBanner;
window.renderWeeklySchedule = renderWeeklySchedule;
window.renderContinueWatching = renderContinueWatching;
window.refreshAllData = refreshAllData;
window.getContinueWatchingShow = getContinueWatchingShow;
window.getRecentlyUpdated = getRecentlyUpdated;
window.getAiringToday = getAiringToday;
window.updateShowField = updateShowField;
window.setAccentColor = setAccentColor;
window.applyAccentColor = applyAccentColor;
window.pushWidgetUpdate = pushWidgetUpdate;
