function openSeasonalModal() {
    closeDrawer();
    const modal = document.getElementById('seasonal-modal');
    if (!modal) return;
    renderSeasonalView();
    modal.style.display = 'flex';
    document.body.classList.add('modal-open');
}

function closeSeasonalModal() {
    const modal = document.getElementById('seasonal-modal');
    if (modal) modal.style.display = 'none';
    document.body.classList.remove('modal-open');
}

function renderSeasonalView() {
    const container = document.getElementById('seasonal-content');
    if (!container) return;

    if (shows.length === 0) {
        container.innerHTML = '<div class="empty-state" style="display:flex;"><i class="fa-solid fa-seedling"></i><h3>No Shows</h3><p>Add shows to see them grouped by season.</p></div>';
        return;
    }

    const genreFilter = filters.genre ? filters.genre.toLowerCase() : '';
    const filteredShows = genreFilter ? shows.filter(s => s.genre && s.genre.toLowerCase().includes(genreFilter)) : shows;

    if (filteredShows.length === 0) {
        container.innerHTML = '<div class="empty-state" style="display:flex;"><i class="fa-solid fa-filter"></i><h3>No Matches</h3><p>No shows match the selected genre filter.</p></div>';
        return;
    }

    const seasons = {};
    const now = new Date();
    const todayIndex = now.getDay();

    filteredShows.forEach(show => {
        const season = getSeasonLabel(show);
        if (!seasons[season]) seasons[season] = [];
        seasons[season].push(show);
    });

    if (Object.keys(seasons).length === 0) {
        container.innerHTML = '<div class="empty-state" style="display:flex;"><i class="fa-solid fa-calendar"></i><h3>No Matches</h3><p>No shows found.</p></div>';
        return;
    }

    const seasonOrder = Object.keys(seasons).sort((a, b) => {
        const aVal = a === 'Unknown' ? -1 : parseSeasonVal(a);
        const bVal = b === 'Unknown' ? -1 : parseSeasonVal(b);
        return bVal - aVal;
    });

    let html = '';
    seasonOrder.forEach(season => {
        const seasonShows = seasons[season];
        const todayName = DAYS_ARRAY[todayIndex];
        html += `<div class="shows-section">
            <div class="section-title-row">
                <h2>${getSeasonIcon(season)} ${escapeHtml(season)}</h2>
                <span class="count-tag">${seasonShows.length} show${seasonShows.length === 1 ? '' : 's'}</span>
            </div>
            <div class="shows-grid">`;
        html += seasonShows.map(show => renderCard(show, todayName)).join('');
        html += '</div></div>';
    });

    container.innerHTML = html;
}

function getSeasonLabel(show) {
    const ts = show.dateAdded || show.lastUpdated || Date.now();
    const d = new Date(ts);
    const m = d.getMonth();
    const y = d.getFullYear();
    if (m <= 1) return 'Winter ' + y;
    if (m <= 4) return 'Spring ' + y;
    if (m <= 7) return 'Summer ' + y;
    if (m <= 10) return 'Fall ' + y;
    return 'Winter ' + (y + 1);
}

function parseSeasonVal(label) {
    const parts = label.split(' ');
    const year = parseInt(parts[1], 10) || 0;
    const seasonMap = { Winter: 0, Spring: 1, Summer: 2, Fall: 3 };
    return year * 4 + (seasonMap[parts[0]] || 0);
}

function getSeasonIcon(label) {
    const season = label.split(' ')[0];
    const icons = { Winter: 'fa-snowflake', Spring: 'fa-seedling', Summer: 'fa-sun', Fall: 'fa-leaf' };
    return `<i class="fa-regular ${icons[season] || 'fa-calendar'}" style="color:var(--accent-cyan)"></i>`;
}

document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('btn-open-seasonal');
    if (btn) btn.addEventListener('click', openSeasonalModal);
    const closeBtn = document.getElementById('btn-close-seasonal');
    if (closeBtn) closeBtn.addEventListener('click', closeSeasonalModal);
    const modal = document.getElementById('seasonal-modal');
    if (modal) modal.addEventListener('click', (e) => { if (e.target.id === 'seasonal-modal') closeSeasonalModal(); });
});