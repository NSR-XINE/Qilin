function openDetailsModal(show) {
    const detailsModal = document.getElementById('details-modal');
    const detailsTitle = document.getElementById('details-modal-title');
    const detailsContent = document.getElementById('details-modal-content');
    if (!detailsModal || !detailsContent) return;

    detailsTitle.innerText = show.title;
    const maxEps = show.totalEp > 0 ? show.totalEp : Math.max(12, show.currentEp + 10);

    let posterHtml = '';
    const safePoster = safePosterUrl(show.poster);
    if (safePoster) {
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
        epPills += `<button class="ep-pill-btn" data-ep="${i}" style="padding: 0.5rem; border-radius: 6px; font-size: 0.75rem; cursor: pointer; transition: all 0.2s; ${activeStyle}">${i}</button>`;
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

function addSwipeToDismiss(modalOverlayId, closeFn) {
    const overlay = document.getElementById(modalOverlayId);
    if (!overlay) return;
    const panel = overlay.querySelector('.modal-content');
    if (!panel) return;

    let startX = 0, startY = 0, isDragging = false;
    const THRESHOLD = 80;

    panel.addEventListener('touchstart', (e) => {
        startX = e.touches[0].clientX;
        startY = e.touches[0].clientY;
        isDragging = true;
        panel.style.transition = 'none';
    }, { passive: true });

    panel.addEventListener('touchmove', (e) => {
        if (!isDragging) return;
        const dx = e.touches[0].clientX - startX;
        const dy = e.touches[0].clientY - startY;
        if (Math.abs(dy) > Math.abs(dx)) return;
        panel.style.transform = `translateX(${dx}px)`;
        panel.style.opacity = `${1 - Math.min(Math.abs(dx) / 250, 0.5)}`;
    }, { passive: true });

    panel.addEventListener('touchend', (e) => {
        if (!isDragging) return;
        isDragging = false;
        const dx = e.changedTouches[0].clientX - startX;
        panel.style.transition = 'transform 0.25s ease, opacity 0.25s ease';
        if (Math.abs(dx) >= THRESHOLD) {
            panel.style.transform = `translateX(${dx > 0 ? '110%' : '-110%'})`;
            panel.style.opacity = '0';
            setTimeout(() => {
                closeFn();
                panel.style.transition = 'none';
                panel.style.transform = '';
                panel.style.opacity = '';
            }, 220);
        } else {
            panel.style.transform = '';
            panel.style.opacity = '';
        }
    }, { passive: true });
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
}

function setupFormSubmission() {
    const titleInput = document.getElementById('show-title');

    if (titleInput) {
        const container = document.createElement('div');
        container.id = 'title-suggestions';
        container.style.cssText = 'position:absolute;top:100%;left:0;right:0;background:var(--bg-card);border:1px solid var(--border-color);border-radius:8px;z-index:2000;max-height:180px;overflow-y:auto;margin-top:2px;display:none;';
        titleInput.parentElement.style.position = 'relative';
        titleInput.parentElement.appendChild(container);

        const debouncedSuggest = debounce(() => {
            const q = titleInput.value.trim().toLowerCase();
            if (!q || q.length < 2) { container.style.display = 'none'; return; }
            const matches = shows.filter(s => s.title.toLowerCase().includes(q)).slice(0, 6);
            if (matches.length === 0) { container.style.display = 'none'; return; }
            container.innerHTML = matches.map(s =>
                `<div style="padding:0.4rem 0.6rem;cursor:pointer;font-size:0.8rem;color:var(--text-secondary);border-bottom:1px solid var(--border-color);transition:background 0.15s" onmouseover="this.style.background='rgba(255,255,255,0.05)'" onmouseout="this.style.background='transparent'">${escapeHtml(s.title)}${s.titleZh ? ' <span style="color:var(--text-muted)">· ' + escapeHtml(s.titleZh) + '</span>' : ''}</div>`
            ).join('');
            container.style.display = 'block';
            Array.from(container.children).forEach((el, i) => {
                el.addEventListener('click', () => {
                    const show = matches[i];
                    if (!show) return;
                    openModal(show);
                    container.style.display = 'none';
                });
            });
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
