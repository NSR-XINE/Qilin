function getCollections() {
    const cols = new Set();
    shows.forEach(s => {
        if (s.collection && s.collection.trim()) cols.add(s.collection.trim());
    });
    return Array.from(cols).sort();
}

function getShowsByCollection(collection) {
    return shows.filter(s => s.collection === collection);
}

function renderCollectionsView() {
    const container = document.getElementById('collections-content');
    if (!container) return;

    const collections = getCollections();

    if (collections.length === 0) {
        container.innerHTML = `<div style="text-align:center;padding:2rem 1rem;color:var(--text-muted);">
            <i class="fa-solid fa-folder-open" style="font-size:2rem;margin-bottom:0.8rem;opacity:0.5;"></i>
            <p>No collections yet.</p>
            <p style="font-size:0.8rem;">Edit a show and add a collection name to group your shows.</p>
        </div>`;
        return;
    }

    let html = `<div style="display:flex;flex-direction:column;gap:1rem;max-height:55vh;overflow-y:auto;padding-right:0.3rem;">`;
    collections.forEach(col => {
        const colShows = getShowsByCollection(col);
        html += `<div style="background:rgba(255,255,255,0.02);border-radius:8px;padding:0.6rem 0.8rem;border:1px solid var(--border-color);">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:0.5rem;">
                <h4 style="font-size:0.85rem;font-weight:600;color:var(--text-primary);margin:0;display:flex;align-items:center;gap:0.4rem;">
                    <i class="fa-solid fa-layer-group" style="color:var(--accent-cyan);font-size:0.8rem;"></i> ${escapeHtml(col)}
                </h4>
                <span style="font-size:0.65rem;color:var(--text-muted);background:rgba(255,255,255,0.05);padding:0.15rem 0.4rem;border-radius:4px;">${colShows.length}</span>
            </div>
            <div style="display:flex;flex-direction:column;gap:0.2rem;">
                ${colShows.map(s => `
                    <div class="collection-item" data-show-id="${escapeHtml(String(s.id))}" style="display:flex;align-items:center;gap:0.5rem;padding:0.3rem 0.4rem;border-radius:4px;cursor:pointer;transition:background 0.2s;">
                        <div style="width:28px;height:28px;border-radius:4px;background:${getPosterGradient(s.title)};display:flex;align-items:center;justify-content:center;font-size:0.5rem;font-weight:700;color:#fff;overflow:hidden;flex-shrink:0;">
                            ${(() => { const p = safePosterUrl(s.poster); return p ? `<img src="${p}" style="width:100%;height:100%;object-fit:cover;" loading="lazy">` : getInitials(s.title); })()}
                        </div>
                        <span style="flex:1;font-size:0.78rem;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(s.title)}</span>
                        <span class="status-badge ${s.status}" style="font-size:0.55rem;padding:0.1rem 0.3rem;">${STATUS.getDisplayName(s.status)}</span>
                    </div>
                `).join('')}
            </div>
        </div>`;
    });
    html += `</div>`;
    container.innerHTML = html;
}

function openCollectionsModal() {
    closeDrawer();
    const modal = document.getElementById('collections-modal');
    if (!modal) return;
    renderCollectionsView();
    modal.style.display = 'flex';
    document.body.classList.add('modal-open');
}

function closeCollectionsModal() {
    const modal = document.getElementById('collections-modal');
    if (!modal) return;
    modal.style.display = 'none';
    document.body.classList.remove('modal-open');
}

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => {
        const btnCollections = document.getElementById('btn-open-collections');
        if (btnCollections) btnCollections.addEventListener('click', openCollectionsModal);
        const closeCollections = document.getElementById('btn-close-collections');
        if (closeCollections) closeCollections.addEventListener('click', closeCollectionsModal);
        const collectionsModal = document.getElementById('collections-modal');
        if (collectionsModal) {
            collectionsModal.addEventListener('click', (e) => {
                if (e.target.id === 'collections-modal') closeCollectionsModal();
            });
        }

        document.getElementById('collections-content')?.addEventListener('click', (e) => {
            const item = e.target.closest('.collection-item');
            if (item && item.dataset.showId) {
                openDetailsById(item.dataset.showId);
            }
        });
    });
}
