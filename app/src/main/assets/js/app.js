document.addEventListener('DOMContentLoaded', () => {
    loadState();
    initTheme();

    let source = 'donghuastream';
    if (DB._available) source = DB.getSetting('pref_streaming_source', '') || DB.getLocal('pref_streaming_source') || 'donghuastream';
    else source = DB.getLocal('pref_streaming_source') || 'donghuastream';
    updateSourceUI(source);

    setupResizeObservers();
    updateStats();
    renderWeeklySchedule();
    renderHeroBanner();
    renderShowsGrid();
    resumeTimers();

    document.getElementById('btn-export-data')?.addEventListener('click', exportData);

    setupImportModal();
    setupDetailsModal();
    setupSettingsModal();
    setupFormSubmission();
    setupSearch();
    setupScheduleTabs();
    setupCardActions();
    setupPosterFetch();

    LOADING_MESSAGES.forEach((msg, idx) => {
        setTimeout(() => {
            const el = document.getElementById('loading-status-text');
            if (el) el.innerText = msg;
        }, idx * 300);
    });

    setTimeout(() => {
        const loader = document.getElementById('app-loading-screen');
        if (loader) {
            loader.classList.add('fade-out');
            document.body.classList.remove('loading-active');
            setTimeout(() => loader.remove(), 600);
        }
    }, 1600);

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

    switchTab('home');

    document.getElementById('btn-close-modal')?.addEventListener('click', closeModal);
    document.getElementById('btn-cancel-modal')?.addEventListener('click', closeModal);
    document.getElementById('donghua-modal')?.addEventListener('click', (e) => {
        if (e.target.id === 'donghua-modal') closeModal();
    });

    document.addEventListener('contextmenu', e => e.preventDefault());

    window.addEventListener('resize', () => switchTab(activeTab));
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
