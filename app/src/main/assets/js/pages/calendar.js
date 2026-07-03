function renderCalendarView() {
    const container = document.getElementById('calendar-content');
    if (!container) return;

    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth();
    const today = now.getDate();

    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const ongoing = getOngoingShows();
    const weekDays = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];

    const monthName = now.toLocaleString('default', { month: 'long', year: 'numeric' });

    let html = `
    <div style="margin-bottom:0.8rem;">
        <h3 style="font-family:var(--font-heading);font-size:1rem;font-weight:700;color:var(--text-primary);margin:0 0 0.6rem 0;display:flex;align-items:center;gap:0.5rem;">
            <i class="fa-regular fa-calendar" style="color:var(--accent-cyan);"></i> ${monthName}
        </h3>
    </div>
    <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:2px;margin-bottom:1rem;">
        ${['Su','Mo','Tu','We','Th','Fr','Sa'].map(d => `<div style="text-align:center;font-size:0.6rem;font-weight:700;color:var(--text-muted);padding:0.3rem 0;text-transform:uppercase;">${d}</div>`).join('')}
    </div>
    <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:2px;">`;

    for (let i = 0; i < firstDay; i++) {
        html += `<div></div>`;
    }

    for (let day = 1; day <= daysInMonth; day++) {
        const date = new Date(year, month, day);
        const dayName = weekDays[date.getDay()];
        const dayShows = ongoing.filter(s => s.releaseDay === dayName);
        const isToday = day === today;

        html += `<div data-cal-day="${dayName}" style="aspect-ratio:1;padding:0.2rem;border-radius:6px;background:${isToday ? 'rgba(157,78,221,0.15)' : 'rgba(255,255,255,0.02)'};border:1px solid ${isToday ? 'var(--accent-purple)' : 'transparent'};display:flex;flex-direction:column;align-items:center;overflow:hidden;cursor:pointer;">
            <span style="font-size:0.65rem;font-weight:${isToday ? '800' : '500'};color:${isToday ? 'var(--accent-purple)' : 'var(--text-secondary)'};">${day}</span>
            ${dayShows.length > 0 ? `<div style="display:flex;gap:2px;margin-top:auto;flex-wrap:wrap;justify-content:center;">
                ${dayShows.slice(0, 3).map(() => `<div style="width:4px;height:4px;border-radius:50%;background:var(--accent-cyan);"></div>`).join('')}
                ${dayShows.length > 3 ? `<span style="font-size:0.45rem;color:var(--text-muted);">+${dayShows.length - 3}</span>` : ''}
            </div>` : ''}
        </div>`;
    }

    html += `</div>`;

    const upcomingWeek = [];
    for (let i = 0; i < 7; i++) {
        const d = new Date(year, month, today + i);
        const dayName = weekDays[d.getDay()];
        const dayShows = ongoing.filter(s => s.releaseDay === dayName);
        if (dayShows.length > 0) {
            upcomingWeek.push({ dayName, date: d, shows: dayShows });
        }
    }

    if (upcomingWeek.length > 0) {
        html += `<div style="border-top:1px solid var(--border-color);padding-top:0.8rem;margin-top:0.8rem;">
            <h4 style="font-size:0.75rem;font-weight:600;color:var(--text-muted);margin:0 0 0.5rem 0;text-transform:uppercase;letter-spacing:0.5px;">Upcoming This Week</h4>
            ${upcomingWeek.map(({ dayName, date, shows }) => `
                <div style="margin-bottom:0.4rem;">
                    <div style="font-size:0.7rem;font-weight:600;color:${date.toDateString() === now.toDateString() ? 'var(--accent-purple)' : 'var(--text-secondary)'};margin-bottom:0.2rem;">${dayName} ${date.toDateString() === now.toDateString() ? '(Today)' : ''}</div>
                    ${shows.map(s => `<div style="font-size:0.75rem;color:var(--text-primary);padding:0.15rem 0.4rem;display:flex;align-items:center;gap:0.4rem;">
                        <span style="width:45px;font-size:0.65rem;color:var(--accent-cyan);font-weight:600;">${s.releaseTime}</span>
                        <span style="flex:1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(s.title)}</span>
                    </div>`).join('')}
                </div>
            `).join('')}
        </div>`;
    }

    container.innerHTML = html;
}

function filterScheduleByDay(dayName) {
    const scheduleTabs = document.querySelectorAll('#schedule-tabs .tab-btn');
    scheduleTabs.forEach(btn => {
        if (btn.dataset.day === dayName) {
            btn.click();
            switchTab('home');
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
    });
}

function openCalendarModal() {
    closeDrawer();
    const modal = document.getElementById('calendar-modal');
    if (!modal) return;
    renderCalendarView();
    modal.style.display = 'flex';
    document.body.classList.add('modal-open');
}

function closeCalendarModal() {
    const modal = document.getElementById('calendar-modal');
    if (!modal) return;
    modal.style.display = 'none';
    document.body.classList.remove('modal-open');
}

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => {
        const btnCalendar = document.getElementById('btn-open-calendar');
        if (btnCalendar) btnCalendar.addEventListener('click', openCalendarModal);
        const closeCalendar = document.getElementById('btn-close-calendar');
        if (closeCalendar) closeCalendar.addEventListener('click', closeCalendarModal);
        const calendarModal = document.getElementById('calendar-modal');
        if (calendarModal) {
            calendarModal.addEventListener('click', (e) => {
                if (e.target.id === 'calendar-modal') closeCalendarModal();
            });
        }

        document.getElementById('calendar-content')?.addEventListener('click', (e) => {
            const dayCell = e.target.closest('[data-cal-day]');
            if (dayCell) {
                filterScheduleByDay(dayCell.dataset.calDay);
            }
        });
    });
}
