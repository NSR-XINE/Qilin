function renderWatchHistory() {
    const container = document.getElementById('history-content');
    if (!container) return;

    let historyData = [];
    if (DB._available) {
        const raw = DB.getWatchHistory();
        if (raw) {
            try { historyData = JSON.parse(raw); } catch(e) { historyData = []; }
        }
    }
    if (!Array.isArray(historyData)) historyData = [];

    if (historyData.length === 0) {
        container.innerHTML = `<div style="text-align:center;padding:2rem 1rem;color:var(--text-muted);">
            <i class="fa-solid fa-clock-rotate-left" style="font-size:2rem;margin-bottom:0.8rem;opacity:0.5;"></i>
            <p>No watch history yet.</p>
            <p style="font-size:0.8rem;">Episode progress changes are recorded here.</p>
        </div>`;
        return;
    }

    const grouped = {};
    historyData.forEach(entry => {
        const date = new Date(entry.timestamp).toLocaleDateString();
        if (!grouped[date]) grouped[date] = [];
        grouped[date].push(entry);
    });

    let html = `<div style="max-height:50vh;overflow-y:auto;padding-right:0.3rem;">`;
    Object.keys(grouped).forEach(date => {
        html += `<div style="margin-bottom:0.8rem;">
            <div style="font-size:0.7rem;font-weight:600;color:var(--text-muted);text-transform:uppercase;margin-bottom:0.4rem;letter-spacing:0.5px;">${date}</div>`;
        grouped[date].forEach(entry => {
            const time = new Date(entry.timestamp).toLocaleTimeString([], { hour:'2-digit', minute:'2-digit' });
            html += `<div class="history-item" data-show-id="${escapeHtml(String(entry.showId))}" style="display:flex;align-items:center;gap:0.6rem;padding:0.4rem 0.5rem;border-radius:6px;background:rgba(255,255,255,0.02);margin-bottom:0.2rem;cursor:pointer;">
                <div style="width:32px;height:32px;border-radius:4px;background:var(--bg-card);overflow:hidden;flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:0.6rem;font-weight:700;color:var(--text-secondary);">
                    ${(() => { const p = safePosterUrl(entry.poster); return p ? `<img src="${p}" style="width:100%;height:100%;object-fit:cover;" loading="lazy">` : `<i class="fa-solid fa-film"></i>`; })()}
                </div>
                <div style="flex:1;min-width:0;">
                    <div style="font-size:0.8rem;font-weight:500;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(entry.title || 'Unknown')}</div>
                    <div style="font-size:0.65rem;color:var(--text-muted);">Episode ${entry.episodeNum} · ${time}</div>
                </div>
                <i class="fa-solid fa-chevron-right" style="color:var(--text-muted);font-size:0.7rem;"></i>
            </div>`;
        });
        html += `</div>`;
    });
    html += `</div>`;
    container.innerHTML = html;
}

function openHistoryModal() {
    closeDrawer();
    const modal = document.getElementById('history-modal');
    if (!modal) return;
    renderWatchHistory();
    modal.style.display = 'flex';
    document.body.classList.add('modal-open');
}

function closeHistoryModal() {
    const modal = document.getElementById('history-modal');
    if (!modal) return;
    modal.style.display = 'none';
    document.body.classList.remove('modal-open');
}

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => {
        const btnHistory = document.getElementById('btn-open-history');
        if (btnHistory) btnHistory.addEventListener('click', openHistoryModal);
        const closeHistory = document.getElementById('btn-close-history');
        if (closeHistory) closeHistory.addEventListener('click', closeHistoryModal);
        const historyModal = document.getElementById('history-modal');
        if (historyModal) {
            historyModal.addEventListener('click', (e) => {
                if (e.target.id === 'history-modal') closeHistoryModal();
            });
        }

        document.getElementById('history-content')?.addEventListener('click', (e) => {
            const item = e.target.closest('.history-item');
            if (item && item.dataset.showId) {
                openDetailsById(item.dataset.showId);
            }
        });
    });
}
