function openDetailsModal(show) {
    const detailsModal = document.getElementById('details-modal');
    const detailsTitle = document.getElementById('details-modal-title');
    const detailsContent = document.getElementById('details-modal-content');
    if (!detailsModal || !detailsContent) return;

    show.lastOpened = Date.now();

    detailsTitle.innerText = show.title;
    const maxEps = show.totalEp > 0 ? show.totalEp : Math.max(12, show.currentEp + 10);

    let posterHtml = '';
    const safePoster = safePosterUrl(show.poster);
    if (safePoster) {
        if (!ImageCache.has(safePoster)) {
            ImageCache.loadImage(safePoster).catch(() => {});
        }
        posterHtml = `<img src="${safePoster}" style="width: 90px; height: 130px; object-fit: cover; border-radius: 8px; border: 1px solid var(--border-color); box-shadow: 0 4px 12px rgba(0,0,0,0.5);" alt="${show.title}" loading="lazy">`;
    } else {
        const gradient = getPosterGradient(show.title);
        const initials = getInitials(show.title);
        posterHtml = `<div style="width: 90px; height: 130px; background: ${gradient}; border-radius: 8px; border: 1px solid var(--border-color); display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.5);">
            <div style="font-size: 1.4rem; font-weight: 800; color: #fff; text-shadow: 0 2px 4px rgba(0,0,0,0.5); z-index: 2;">${initials}</div>
            ${show.titleZh ? `<div style="position: absolute; bottom: 4px; right: 4px; font-size: 1.6rem; font-weight: 800; color: rgba(255,255,255,0.06); pointer-events: none; z-index: 1;">${show.titleZh[0]}</div>` : ''}
        </div>`;
    }

    let epPills = '';
    for (let i = 1; i <= maxEps; i++) {
        const isWatched = i <= show.currentEp;
        const activeStyle = isWatched ? 'background: linear-gradient(135deg, var(--accent-cyan), var(--accent-purple)); border: none; color: #fff; font-weight: bold;' : 'background: var(--bg-input); border: 1px solid var(--border-color); color: var(--text-secondary);';
        epPills += `<button class="ep-pill-btn" data-ep="${i}" aria-label="Episode ${i}" style="padding: 0.5rem; border-radius: 6px; font-size: 0.75rem; cursor: pointer; transition: all 0.2s; ${activeStyle}">${i}</button>`;
    }

    const statusText = STATUS.getDisplayName(show.status).toUpperCase();

    detailsContent.innerHTML = `
        <div style="display: flex; gap: 1rem; align-items: flex-start;">
            ${posterHtml}
            <div style="flex: 1; display: flex; flex-direction: column; gap: 0.4rem;">
                <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: center;">
                    <span class="status-badge ${show.status}" style="padding: 0.2rem 0.5rem; font-size: 0.65rem; border-radius: 4px; text-transform: uppercase;">${statusText}</span>
                    ${show.isFavorite ? '<span style="font-size:0.65rem;color:var(--accent-rose);"><i class="fa-solid fa-heart"></i> Favorite</span>' : ''}
                    ${STATUS.isCompleted(show) ? '<span style="font-size: 0.75rem; color: var(--text-muted);"><i class="fa-solid fa-circle-check"></i> Series Complete</span>'
                    : `<span style="font-size: 0.75rem; color: var(--text-muted);"><i class="fa-regular fa-calendar"></i> ${show.releaseDay}s at ${show.releaseTime}</span>`}
                    ${show.totalEp ? `<span style="font-size: 0.75rem; color: var(--text-muted);"><i class="fa-solid fa-film"></i> ${show.totalEp} eps total</span>` : ''}
                </div>
                <h3 style="font-size: 1rem; color: #fff; margin: 0.2rem 0 0 0; font-family: var(--font-heading); font-weight: 700;">${escapeHtml(show.title)}</h3>
                ${show.titleZh ? `<div style="font-size: 0.75rem; color: var(--text-muted); font-style: italic;">${escapeHtml(show.titleZh)}</div>` : ''}
                <div style="font-size: 0.75rem; color: var(--text-secondary); margin-top: 0.4rem;">
                    Watched: <strong id="watched-count" style="color: var(--accent-cyan);">${show.currentEp}</strong> episodes
                </div>
            </div>
        </div>
        <div>
            <h4 style="font-size: 0.8rem; color: #fff; margin: 0 0 0.4rem 0; text-transform: uppercase; letter-spacing: 0.5px;">Synopsis</h4>
            <p style="font-size: 0.78rem; color: var(--text-secondary); line-height: 1.4; margin: 0;">
                ${show.notes ? escapeHtml(show.notes) : '<span style="color: var(--text-muted); font-style: italic;">No synopsis added.</span>'}
            </p>
        </div>
        <div>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                <h4 style="font-size: 0.8rem; color: #fff; margin: 0; text-transform: uppercase; letter-spacing: 0.5px;">Episodes ${show.totalEp ? `<span id="eps-progress" style="font-weight:400; color: var(--text-muted);">(${show.currentEp}/${show.totalEp})</span>` : ''}</h4>
                <span style="font-size: 0.7rem; color: var(--text-muted);">Tap to update progress</span>
            </div>
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(40px, 1fr)); gap: 0.4rem; max-height: 200px; overflow-y: auto; padding-right: 0.2rem;">
                ${epPills}
            </div>
        </div>`;

    detailsContent.querySelectorAll('.ep-pill-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const newEp = parseInt(btn.dataset.ep, 10);
            if (newEp === show.currentEp) {
                show.currentEp = Math.max(0, newEp - 1);
            } else {
                if (newEp > show.currentEp) {
                    for (let ep = show.currentEp + 1; ep <= newEp; ep++) {
                        if (DB._available) DB.addWatchHistory(show.id, ep);
                    }
                }
                show.currentEp = newEp;
            }
            show.lastUpdated = Date.now();
            persistState();

            detailsContent.querySelectorAll('.ep-pill-btn').forEach(b => {
                const ep = parseInt(b.dataset.ep, 10);
                const isWatched = ep <= show.currentEp;
                const style = isWatched ? 'background: linear-gradient(135deg, var(--accent-cyan), var(--accent-purple)); border: none; color: #fff; font-weight: bold;' : 'background: var(--bg-input); border: 1px solid var(--border-color); color: var(--text-secondary);';
                b.style.cssText = `padding: 0.5rem; border-radius: 6px; font-size: 0.75rem; cursor: pointer; transition: all 0.2s; ${style}`;
            });

            const wc = document.getElementById('watched-count');
            if (wc) wc.textContent = show.currentEp;

            const epProg = document.getElementById('eps-progress');
            if (epProg) epProg.textContent = `(${show.currentEp}/${show.totalEp})`;
        });
    });

    detailsModal.style.display = 'flex';
    document.body.classList.add('modal-open');
}

function openModal(showData) {
    const modalEl = document.getElementById('donghua-modal');
    const formEl = document.getElementById('donghua-form');
    const statusSelect = document.getElementById('show-status');
    if (!modalEl || !formEl) return;

    if (statusSelect) {
        statusSelect.innerHTML = `<option value="ongoing">Airing</option><option value="completed">Completed</option><option value="stopped">Stopped / Hiatus</option>`;
    }

    formEl.reset();
    document.getElementById('show-id').value = '';

    if (showData) {
        document.getElementById('modal-title').innerHTML = '<i class="fa-solid fa-pen-to-square"></i> Edit Donghua';
        setFormValue('show-id', showData.id);
        setFormValue('show-title', showData.title);
        setFormValue('show-title-zh', showData.titleZh || '');
        setFormValue('show-status', showData.status);
        setFormValue('show-watch-url', showData.watchUrl || '');
        setFormValue('show-countdown-url', showData.countdownUrl || '');
        setFormValue('show-release-day', showData.releaseDay);
        setFormValue('show-release-time', showData.releaseTime);
        setFormValue('show-current-ep', showData.currentEp);
        setFormValue('show-total-ep', showData.totalEp || 0);
        setFormValue('show-poster', showData.poster || '');
        setFormValue('show-collection', showData.collection || '');
        setFormValue('show-notes', showData.notes || '');
    } else {
        document.getElementById('modal-title').innerHTML = '<i class="fa-solid fa-circle-plus"></i> Add New Donghua';
        const todayDay = getTodayName();
        setFormValue('show-release-day', todayDay);
        setFormValue('show-release-time', '10:00');
        setFormValue('show-current-ep', 0);
        setFormValue('show-total-ep', 0);
    }

    modalEl.style.display = 'flex';
    document.body.classList.add('modal-open');
}

function setFormValue(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = val != null ? val : '';
}

function closeModal() {
    const el = document.getElementById('donghua-modal');
    if (el) el.style.display = 'none';
    document.body.classList.remove('modal-open');
}

function openSettingsModal() {
    closeDrawer();
    const modal = document.getElementById('settings-modal');
    if (modal) {
        modal.style.setProperty('display', 'flex', 'important');
        document.body.classList.add('modal-open');
    }
}

function closeSettingsModal() {
    const modal = document.getElementById('settings-modal');
    if (modal) {
        modal.style.setProperty('display', 'none', 'important');
        document.body.classList.remove('modal-open');
    }
}

function showExitModal() {
    const modal = document.getElementById('exit-modal');
    if (modal) {
        modal.style.setProperty('display', 'flex', 'important');
        document.body.classList.add('modal-open');
    }
}

function closeExitModal() {
    const modal = document.getElementById('exit-modal');
    if (modal) {
        modal.style.setProperty('display', 'none', 'important');
        document.body.classList.remove('modal-open');
    }
}

function confirmExitApp() {
    pauseTimers();
    if (DB._available && typeof AndroidApp.exitApp === 'function') {
        AndroidApp.exitApp();
    } else {
        closeExitModal();
    }
}

function closeTopModal() {
    const drawer = document.getElementById('right-drawer');
    if (drawer && drawer.classList.contains('open')) { closeDrawer(); return; }
    const modalIds = ['history-modal', 'stats-modal', 'calendar-modal', 'collections-modal', 'seasonal-modal', 'settings-modal', 'donghua-modal', 'import-modal', 'details-modal', 'exit-modal'];
    for (let i = modalIds.length - 1; i >= 0; i--) {
        const el = document.getElementById(modalIds[i]);
        if (el && el.style.display === 'flex') {
            if (modalIds[i] === 'history-modal') closeHistoryModal();
            else if (modalIds[i] === 'stats-modal') closeStatsModal();
            else if (modalIds[i] === 'calendar-modal') closeCalendarModal();
            else if (modalIds[i] === 'collections-modal') closeCollectionsModal();
            else if (modalIds[i] === 'seasonal-modal') closeSeasonalModal();
            else if (modalIds[i] === 'settings-modal') closeSettingsModal();
            else if (modalIds[i] === 'donghua-modal') closeModal();
            else if (modalIds[i] === 'import-modal') closeImportModal();
            else if (modalIds[i] === 'details-modal') closeDetailsModal();
            else if (modalIds[i] === 'exit-modal') closeExitModal();
            return;
        }
    }
}

function closeDetailsModal() {
    const modal = document.getElementById('details-modal');
    if (modal) {
        modal.style.display = 'none';
        document.body.classList.remove('modal-open');
    }
}

function closeImportModal() {
    const importModal = document.getElementById('import-modal');
    if (importModal) {
        importModal.style.display = 'none';
        document.body.classList.remove('modal-open');
        const settingsModal = document.getElementById('settings-modal');
        if (settingsModal && settingsModal.style.display === 'flex') {
            document.body.classList.add('modal-open');
        }
    }
}

function handleBackPress() {
    const modalIds = ['history-modal', 'stats-modal', 'calendar-modal', 'collections-modal', 'seasonal-modal', 'settings-modal', 'donghua-modal', 'import-modal', 'details-modal', 'exit-modal'];
    for (let i = modalIds.length - 1; i >= 0; i--) {
        const el = document.getElementById(modalIds[i]);
        if (el && el.style.display === 'flex') {
            if (modalIds[i] === 'exit-modal') {
                confirmExitApp();
            } else {
                closeTopModal();
            }
            return;
        }
    }
    showExitModal();
}

function setupResizeObservers() {
    const header = document.querySelector('.app-header');
    if (header && typeof ResizeObserver !== 'undefined') {
        new ResizeObserver(() => {
            document.documentElement.style.setProperty('--header-height', header.getBoundingClientRect().height + 'px');
        }).observe(header);
    }
    const bottomNav = document.querySelector('.bottom-nav');
    if (bottomNav && typeof ResizeObserver !== 'undefined') {
        new ResizeObserver(() => {
            const rect = bottomNav.getBoundingClientRect();
            document.documentElement.style.setProperty('--bottom-nav-height', (window.innerHeight - rect.top) + 'px');
        }).observe(bottomNav);
    }
}

function setupImportModal() {
    const importModal = document.getElementById('import-modal');
    const btnTriggerImport = document.getElementById('btn-trigger-import');
    const btnCloseImport = document.getElementById('btn-close-import');
    const btnCancelImport = document.getElementById('btn-cancel-import');
    const btnSubmitImport = document.getElementById('btn-submit-import');
    const importFileInput = document.getElementById('import-file-input');
    const importFileName = document.getElementById('import-file-name');

    importFileInput?.addEventListener('change', () => {
        const file = importFileInput.files[0];
        if (!file) return;
        if (importFileName) importFileName.textContent = file.name;
        if (btnSubmitImport) {
            btnSubmitImport.disabled = false;
            btnSubmitImport.style.opacity = '1';
            btnSubmitImport.style.cursor = 'pointer';
        }
    });

    btnTriggerImport?.addEventListener('click', () => {
        if (importFileInput) importFileInput.value = '';
        if (importFileName) importFileName.textContent = 'No file selected';
        if (btnSubmitImport) {
            btnSubmitImport.disabled = true;
            btnSubmitImport.style.opacity = '0.5';
            btnSubmitImport.style.cursor = 'not-allowed';
        }
        if (importModal) { importModal.style.display = 'flex'; document.body.classList.add('modal-open'); }
    });

    if (btnCloseImport) btnCloseImport.addEventListener('click', closeImportModal);
    if (btnCancelImport) btnCancelImport.addEventListener('click', closeImportModal);
    if (importModal) importModal.addEventListener('click', (e) => { if (e.target.id === 'import-modal') closeImportModal(); });

    btnSubmitImport?.addEventListener('click', () => {
        const file = importFileInput?.files[0];
        if (!file) { alert('Please select a .json backup file first.'); return; }
        const reader = new FileReader();
        reader.onload = (e) => { if (importData(e.target.result)) closeImportModal(); };
        reader.onerror = () => alert('Failed to read file. Please try again.');
        reader.readAsText(file);
    });
}

function setupDetailsModal() {
    const detailsModal = document.getElementById('details-modal');
    const btnCloseDetails = document.getElementById('btn-close-details');

    btnCloseDetails?.addEventListener('click', closeDetailsModal);
    if (detailsModal) detailsModal.addEventListener('click', (e) => { if (e.target.id === 'details-modal') closeDetailsModal(); });
}

function setupSettingsModal() {
    const settingsModal = document.getElementById('settings-modal');
    if (settingsModal) settingsModal.addEventListener('click', (e) => { if (e.target.id === 'settings-modal') closeSettingsModal(); });
    document.getElementById('btn-open-settings')?.addEventListener('click', openSettingsModal);
    document.getElementById('btn-close-settings')?.addEventListener('click', closeSettingsModal);

    document.querySelectorAll('[data-source]')?.forEach(el => {
        el.addEventListener('click', () => selectPreferredSource(el.dataset.source));
    });
    document.querySelectorAll('[data-theme]')?.forEach(el => {
        el.addEventListener('click', () => setThemeMode(el.dataset.theme));
    });

    const accentPicker = document.getElementById('accent-picker');
    if (accentPicker) {
        const defaultBtn = accentPicker.querySelector('.accent-btn[data-accent="default"]');
        const presets = Object.entries(ACCENT_PRESETS);
        for (const [key, preset] of presets) {
            const hex = currentTheme === 'light' ? preset.light : preset.dark;
            const btn = document.createElement('button');
            btn.className = 'accent-btn';
            btn.dataset.accent = hex;
            btn.title = preset.name;
            btn.style.background = hex;
            accentPicker.appendChild(btn);
        }
        if (DB._available && typeof AndroidApp.getDynamicColors === 'function') {
            const dynBtn = document.createElement('button');
            dynBtn.className = 'accent-btn';
            dynBtn.dataset.accent = 'dynamic';
            dynBtn.title = 'Material You (auto)';
            dynBtn.style.background = 'none';
            dynBtn.innerHTML = '<svg viewBox="0 0 384 384" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg"><circle cx="192" cy="192" r="182" fill="#D3E1FA"/><g transform="translate(0,384) scale(0.1,-0.1)" fill="#17171F" stroke="none"><path d="M1317 2958 c-49 -213 -216 -380 -432 -434 l-67 -17 70 -18 c111 -28 188 -73 273 -158 85 -85 137 -179 160 -286 l12 -60 13 61 c45 217 219 393 438 444 l69 16 -69 17 c-114 28 -190 71 -276 157 -87 86 -139 180 -161 288 l-13 62 -17 -72z"/><path d="M2182 2877 l-102 -102 108 -108 107 -107 105 105 104 104 -70 73 c-39 40 -83 88 -99 106 -15 17 -33 32 -39 32 -6 0 -57 -46 -114 -103z"/><path d="M2585 2451 c-16 -4 -45 -16 -64 -26 -19 -9 -259 -241 -533 -515 l-498 -498 0 -226 0 -226 228 0 227 0 502 503 c275 276 509 518 519 537 23 46 23 134 -1 180 -24 47 -201 227 -245 250 -44 22 -96 30 -135 21z m-420 -971 l-310 -310 -72 0 -73 0 0 78 0 78 308 307 307 307 75 -75 75 -75 -310 -310z"/><path d="M960 1655 l-104 -104 24 -27 c66 -71 181 -184 188 -184 4 0 53 46 110 103 l102 102 -108 108 -107 107 -105 -105z"/></g></svg>';
            accentPicker.appendChild(dynBtn);
        }
        accentPicker.addEventListener('click', (e) => {
            const btn = e.target.closest('.accent-btn');
            if (!btn) return;
            const val = btn.dataset.accent;
            if (val === 'dynamic') {
                setAccentColor('dynamic');
            } else if (val) {
                setAccentColor(val);
            } else {
                setAccentColor('default');
            }
        });
    }

    document.getElementById('btn-clear-image-cache')?.addEventListener('click', () => {
        if (typeof ImageCache !== 'undefined' && ImageCache.clear) {
            ImageCache.clear();
        } else {
            try { localStorage.removeItem('image_cache'); } catch (_) {}
        }
        showToast('Image cache cleared');
    });

    document.getElementById('btn-clear-local-data')?.addEventListener('click', () => {
        if (confirm('This will clear all local preferences (theme, source, notification settings). Watchlist data will NOT be affected. Continue?')) {
            try {
                const keep = ['shows', 'watch_history', 'favorites'];
                const keys = Object.keys(localStorage);
                for (const key of keys) {
                    if (!keep.includes(key) && !key.startsWith('show:')) {
                        localStorage.removeItem(key);
                    }
                }
                initTheme();
                showToast('Local preferences cleared');
            } catch (e) {
                console.error('Failed to clear local data', e);
            }
        }
    });

    initDashboardToggles();
    initCacheInfo();
    initQuietHours();
}

const DASHBOARD_TOGGLES_KEY = 'dashboard_toggles';

function getDashboardToggles() {
    try {
        return JSON.parse(localStorage.getItem(DASHBOARD_TOGGLES_KEY)) || {};
    } catch (e) { return {}; }
}

function saveDashboardToggles(toggles) {
    try { localStorage.setItem(DASHBOARD_TOGGLES_KEY, JSON.stringify(toggles)); } catch (e) {}
}

function applyDashboardToggles() {
    const toggles = getDashboardToggles();
    const banner = document.getElementById('hero-banner');
    const continueWatching = document.getElementById('continue-watching-section');
    const recentlyUpdated = document.getElementById('recently-updated-section');
    const schedule = document.getElementById('schedule-container');
    const recommendations = document.getElementById('recommendations-section');
    if (banner && toggles.countdown === false) banner.style.display = 'none';
    if (continueWatching && toggles.continue === false) continueWatching.style.display = 'none';
    if (recentlyUpdated && toggles.recently === false) recentlyUpdated.style.display = 'none';
    if (schedule && toggles.schedule === false) schedule.style.display = 'none';
    if (recommendations && toggles.recommendations === false) recommendations.style.display = 'none';
}

function initDashboardToggles() {
    const toggles = getDashboardToggles();
    const ids = ['countdown', 'continue', 'recently', 'schedule', 'recommendations'];
    ids.forEach(id => {
        const el = document.getElementById('dash-toggle-' + id);
        if (el) {
            el.checked = toggles[id] !== false;
            el.addEventListener('change', () => {
                toggles[id] = el.checked;
                saveDashboardToggles(toggles);
                if (el.checked) location.reload();
                else applyDashboardToggles();
            });
        }
    });
}

function initCacheInfo() {
    const info = document.getElementById('cache-info');
    if (!info) return;
    let size = 0;
    let count = 0;
    try {
        for (let i = 0; i < localStorage.length; i++) {
            const k = localStorage.key(i);
            if (k && k.startsWith('img_cache_')) {
                count++;
                size += (localStorage.getItem(k) || '').length;
            }
        }
    } catch (e) {}
    const sizeKB = Math.round(size / 1024);
    info.textContent = `Image cache: ${count} items, ${sizeKB} KB`;
}

const QUIET_HOURS_KEY = 'quiet_hours';

function getQuietHours() {
    try {
        return JSON.parse(localStorage.getItem(QUIET_HOURS_KEY)) || {};
    } catch (e) { return {}; }
}

function saveQuietHours(config) {
    try { localStorage.setItem(QUIET_HOURS_KEY, JSON.stringify(config)); } catch (e) {}
}

function isQuietHours() {
    const qh = getQuietHours();
    if (!qh.enabled) return false;
    const now = new Date();
    const hm = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');
    const start = qh.start || '22:00';
    const end = qh.end || '08:00';
    if (start <= end) return hm >= start && hm <= end;
    return hm >= start || hm <= end;
}

function initQuietHours() {
    const toggle = document.getElementById('quiet-hours-toggle');
    const config = document.getElementById('quiet-hours-config');
    const start = document.getElementById('quiet-start');
    const end = document.getElementById('quiet-end');
    if (!toggle) return;
    const qh = getQuietHours();
    toggle.checked = qh.enabled || false;
    if (start) start.value = qh.start || '22:00';
    if (end) end.value = qh.end || '08:00';
    if (config) config.style.display = toggle.checked ? 'flex' : 'none';

    toggle.addEventListener('change', () => {
        if (config) config.style.display = toggle.checked ? 'flex' : 'none';
        const qh = getQuietHours();
        qh.enabled = toggle.checked;
        qh.start = start ? start.value : '22:00';
        qh.end = end ? end.value : '08:00';
        saveQuietHours(qh);
    });

    if (start) start.addEventListener('change', () => {
        const qh = getQuietHours();
        qh.start = start.value;
        saveQuietHours(qh);
    });

    if (end) end.addEventListener('change', () => {
        const qh = getQuietHours();
        qh.end = end.value;
        saveQuietHours(qh);
    });
}

function setupFormSubmission() {
    const titleInput = document.getElementById('show-title');

    if (titleInput) {
        let container = document.getElementById('title-suggestions');
        if (!container) {
            container = document.createElement('div');
            container.id = 'title-suggestions';
            container.style.cssText = 'position:absolute;top:100%;left:0;right:0;background:var(--bg-card);border:1px solid var(--border-color);border-radius:8px;z-index:2000;max-height:180px;overflow-y:auto;margin-top:2px;display:none;';
            container.addEventListener('mouseover', (ev) => {
                const div = ev.target.closest('[data-suggestion-idx]');
                if (div) div.style.background = 'rgba(255,255,255,0.05)';
            });
            container.addEventListener('mouseout', (ev) => {
                const div = ev.target.closest('[data-suggestion-idx]');
                if (div) div.style.background = 'transparent';
            });
            container.addEventListener('click', (ev) => {
                const div = ev.target.closest('[data-suggestion-idx]');
                if (div) {
                    const idx = parseInt(div.dataset.suggestionIdx, 10);
                    if (!isNaN(idx) && matches && matches[idx]) {
                        openModal(matches[idx]);
                        container.style.display = 'none';
                    }
                }
            });
            titleInput.parentElement.style.position = 'relative';
            titleInput.parentElement.appendChild(container);
        }

        let matches = [];
        const debouncedSuggest = debounce(() => {
            const q = titleInput.value.trim().toLowerCase();
            if (!q || q.length < 2) { container.style.display = 'none'; return; }
            matches = shows.filter(s => s.title.toLowerCase().includes(q)).slice(0, 6);
            if (matches.length === 0) { container.style.display = 'none'; return; }
            container.innerHTML = matches.map((s, i) =>
                `<div data-suggestion-idx="${i}" style="padding:0.4rem 0.6rem;cursor:pointer;font-size:0.8rem;color:var(--text-secondary);border-bottom:1px solid var(--border-color);transition:background 0.15s">${escapeHtml(s.title)}${s.titleZh ? ' <span style="color:var(--text-muted)">· ' + escapeHtml(s.titleZh) + '</span>' : ''}</div>`
            ).join('');
            container.style.display = 'block';
        }, 200);

        titleInput.addEventListener('input', debouncedSuggest);
        titleInput.addEventListener('blur', () => setTimeout(() => container.style.display = 'none', 200));
        titleInput.addEventListener('focus', () => { if (container.children.length > 0 && titleInput.value.trim().length >= 2) container.style.display = 'block'; });
    }

    document.getElementById('btn-fetch-poster')?.addEventListener('click', () => {
        const title = titleInput?.value.trim();
        fetchShowDetails(title, false);
    });

    if (titleInput) {
        titleInput.addEventListener('change', (e) => {
            const title = e.target.value.trim();
            fetchShowDetails(title, true);
        });
    }

    document.getElementById('donghua-form')?.addEventListener('submit', (e) => {
        e.preventDefault();
        const showId = document.getElementById('show-id')?.value;
        const newShowData = {
            title: document.getElementById('show-title')?.value?.trim() || '',
            titleZh: document.getElementById('show-title-zh')?.value?.trim() || '',
            status: document.getElementById('show-status')?.value || 'ongoing',
            watchUrl: document.getElementById('show-watch-url')?.value?.trim() || '',
            countdownUrl: document.getElementById('show-countdown-url')?.value?.trim() || '',
            releaseDay: document.getElementById('show-release-day')?.value || 'Sunday',
            releaseTime: document.getElementById('show-release-time')?.value || '10:00',
            currentEp: parseInt(document.getElementById('show-current-ep')?.value) || 0,
            totalEp: parseInt(document.getElementById('show-total-ep')?.value) || 0,
            poster: document.getElementById('show-poster')?.value?.trim() || '',
            collection: document.getElementById('show-collection')?.value?.trim() || '',
            genres: document.getElementById('show-genre')?.value?.trim() || '',
            notes: document.getElementById('show-notes')?.value?.trim() || '',
            lastUpdated: Date.now()
        };

        if (showId) {
            const idx = shows.findIndex(s => s.id === showId);
            if (idx !== -1) {
                newShowData.id = showId;
                newShowData.dateAdded = shows[idx].dateAdded || Date.now();
                Object.assign(shows[idx], newShowData);
            }
        } else {
            newShowData.id = 'dh-' + Date.now();
            newShowData.dateAdded = Date.now();
            shows.push(newShowData);
        }

        const savedShow = showId ? shows.find(s => s.id === showId) : shows[shows.length - 1];
        if (savedShow && getNotifyPref(savedShow.id)) {
            scheduleReminderForShow(savedShow);
        }

        persistState();
        closeModal();

        if (!newShowData.poster) {
            if (newShowData.watchUrl && newShowData.watchUrl.startsWith('https://donghuastream.org/')) {
                setTimeout(() => fetchShowBanner(newShowData.id, newShowData.watchUrl), 200);
            } else if (newShowData.countdownUrl && newShowData.countdownUrl.startsWith('https://animecountdown.com/')) {
                setTimeout(() => fetchShowBanner(newShowData.id, newShowData.countdownUrl), 200);
            } else {
                setTimeout(() => fetchPosterFromJikan(newShowData.id, newShowData.title), 200);
            }
        }
    });
}
