function renderDetailedStats() {
    const container = document.getElementById('detailed-stats-content');
    if (!container) return;

    const stats = computeStats();
    const now = new Date();
    const weekDays = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
    const todayIndex = now.getDay();

    const ongoing = getOngoingShows();
    const todayShows = ongoing.filter(s => s.releaseDay === weekDays[todayIndex]);
    const thisWeekShows = ongoing.filter(s => {
        const dayIndex = weekDays.indexOf(s.releaseDay);
        return dayIndex >= todayIndex;
    });

    let html = `
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:0.6rem;margin-bottom:1rem;">
        <div class="stat-card-mini"><div class="stat-val">${stats.total}</div><div class="stat-lbl">Total Tracked</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.airing}</div><div class="stat-lbl">Currently Airing</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.completed}</div><div class="stat-lbl">Completed</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.stopped}</div><div class="stat-lbl">Hiatus / Stopped</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.totalEpisodes}</div><div class="stat-lbl">Total Episodes</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.hoursWatched}</div><div class="stat-lbl">Hours Watched</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.favorites}</div><div class="stat-lbl">Favorites</div></div>
        <div class="stat-card-mini"><div class="stat-val">${todayShows.length}</div><div class="stat-lbl">Airing Today</div></div>
    </div>
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:0.6rem;margin-bottom:1rem;">
        <div class="stat-card-mini"><div class="stat-val">${stats.completionRate}%</div><div class="stat-lbl">Completion Rate</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.streak}</div><div class="stat-lbl">Day Streak</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.weeklyActivity}</div><div class="stat-lbl">Active This Week</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.monthlyActivity}</div><div class="stat-lbl">Active This Month</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.avgRating}</div><div class="stat-lbl">Avg Rating</div></div>
        <div class="stat-card-mini"><div class="stat-val">${stats.longestSeries ? escapeHtml(stats.longestSeries.title) : '—'}</div><div class="stat-lbl">Longest Series (${stats.longestEpCount} eps)</div></div>
    </div>`;

    if (stats.topGenres.length > 0) {
        const maxGenreCount = stats.topGenres[0][1];
        html += `<div style="border-top:1px solid var(--border-color);padding-top:0.8rem;margin-top:0.4rem;">
            <h4 style="font-size:0.8rem;color:var(--text-primary);margin:0 0 0.5rem 0;font-weight:600;">
                <i class="fa-solid fa-tags" style="color:var(--accent-purple);margin-right:0.4rem;"></i>Top Genres
            </h4>
            <div style="display:flex;flex-wrap:wrap;gap:0.4rem;">
                ${stats.topGenres.map(([genre, count]) => `
                    <span style="padding:0.2rem 0.5rem;font-size:0.65rem;color:var(--accent-cyan);background:rgba(var(--accent-cyan-rgb),0.1);border-radius:4px;">
                        ${escapeHtml(genre)} <span style="color:var(--text-muted);">${count}</span>
                    </span>
                `).join('')}
            </div>
        </div>`;
    }

    if (stats.topStudios.length > 0) {
        html += `<div style="border-top:1px solid var(--border-color);padding-top:0.8rem;margin-top:0.4rem;">
            <h4 style="font-size:0.8rem;color:var(--text-primary);margin:0 0 0.5rem 0;font-weight:600;">
                <i class="fa-solid fa-building" style="color:var(--accent-rose);margin-right:0.4rem;"></i>Top Studios
            </h4>
            <div style="display:flex;flex-wrap:wrap;gap:0.4rem;">
                ${stats.topStudios.map(([studio, count]) => `
                    <span style="padding:0.2rem 0.5rem;font-size:0.65rem;color:var(--accent-rose);background:rgba(var(--accent-rose-rgb),0.1);border-radius:4px;">
                        ${escapeHtml(studio)} <span style="color:var(--text-muted);">${count}</span>
                    </span>
                `).join('')}
            </div>
        </div>`;
    }

    html += `<div style="border-top:1px solid var(--border-color);padding-top:0.8rem;margin-top:0.4rem;">
        <h4 style="font-size:0.8rem;color:var(--text-primary);margin:0 0 0.5rem 0;font-weight:600;">
            <i class="fa-regular fa-calendar" style="color:var(--accent-cyan);margin-right:0.4rem;"></i>This Week's Releases
        </h4>
        ${thisWeekShows.length === 0
            ? '<p style="font-size:0.75rem;color:var(--text-muted);">No ongoing shows this week.</p>'
            : `<div style="display:flex;flex-direction:column;gap:0.3rem;max-height:150px;overflow-y:auto;">
                ${thisWeekShows.map(s => {
                    const schedule = getNextReleaseDate(s.releaseDay, s.releaseTime);
                    const isToday = s.releaseDay === weekDays[todayIndex];
                    return `<div style="display:flex;align-items:center;gap:0.5rem;padding:0.3rem 0.4rem;border-radius:4px;background:${isToday ? 'rgba(157,78,221,0.08)' : 'transparent'};font-size:0.75rem;">
                        <span style="width:60px;font-size:0.65rem;color:var(--text-muted);font-weight:600;">${s.releaseDay.slice(0,3)}</span>
                        <span style="flex:1;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(s.title)}</span>
                        <span style="color:var(--accent-cyan);font-weight:600;font-size:0.7rem;">${s.releaseTime}</span>
                    </div>`;
                }).join('')}
            </div>`
        }
    </div>`;

    container.innerHTML = html;
}

function openStatsModal() {
    closeDrawer();
    const modal = document.getElementById('stats-modal');
    if (!modal) return;
    renderDetailedStats();
    modal.style.display = 'flex';
    document.body.classList.add('modal-open');
}

function closeStatsModal() {
    const modal = document.getElementById('stats-modal');
    if (!modal) return;
    modal.style.display = 'none';
    document.body.classList.remove('modal-open');
}

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => {
        const btnStats = document.getElementById('btn-open-stats');
        if (btnStats) btnStats.addEventListener('click', openStatsModal);
        const closeStats = document.getElementById('btn-close-stats');
        if (closeStats) closeStats.addEventListener('click', closeStatsModal);
        const statsModal = document.getElementById('stats-modal');
        if (statsModal) {
            statsModal.addEventListener('click', (e) => {
                if (e.target.id === 'stats-modal') closeStatsModal();
            });
        }
    });
}
