let timerInterval = null;
const cardElements = new Map();

function pauseTimers() {
    if (timerInterval) { clearInterval(timerInterval); timerInterval = null; }
}

function resumeTimers() {
    if (!timerInterval) {
        buildCardElementMap();
        timerInterval = setInterval(updateTimers, 1000);
    }
}

document.addEventListener('visibilitychange', () => {
    if (document.hidden) pauseTimers();
    else resumeTimers();
});

function buildCardElementMap() {
    cardElements.clear();
    document.querySelectorAll('.show-card').forEach(card => {
        const id = card.dataset.id;
        if (id) {
            cardElements.set(id, {
                card,
                countdown: card.querySelector('.card-countdown')
            });
        }
    });
}

function rebuildCardElementMap() {
    buildCardElementMap();
}

function updateTimers() {
    const clockEl = document.getElementById('clock-display');
    if (clockEl) {
        const now = new Date();
        clockEl.innerText = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) + ' (' + getTodayName() + ')';
    }

    const heroCD = document.getElementById('hero-countdown-box');
    if (heroCD) {
        const heroBanner = document.getElementById('next-up-banner');
        const heroShowId = heroBanner ? heroBanner.dataset.id : null;
        if (heroShowId) {
            const show = getShowById(heroShowId);
            if (show && STATUS.isAiring(show)) {
                const sched = getNextReleaseDate(show.releaseDay, show.releaseTime);
                if (!sched.airingNow) {
                    const time = calculateTimeRemaining(sched.targetDate);
                    if (time.elapsed) { renderHeroBanner(); return; }
                    const nums = heroCD.querySelectorAll('.num');
                    if (nums.length >= 4) {
                        nums[0].innerText = String(time.days).padStart(2, '0');
                        nums[1].innerText = String(time.hours).padStart(2, '0');
                        nums[2].innerText = String(time.minutes).padStart(2, '0');
                        nums[3].innerText = String(time.seconds).padStart(2, '0');
                    }
                }
            }
        }
    }

    for (const [id, el] of cardElements) {
        if (!el.countdown) continue;
        const show = getShowById(id);
        if (!show || !STATUS.isAiring(show)) continue;

        const schedule = getNextReleaseDate(show.releaseDay, show.releaseTime);
        const valEls = el.countdown.querySelectorAll('.c-val');

        if (schedule.airingNow) {
            if (valEls.length === 4) {
                el.countdown.innerHTML = `
                    <div class="c-item" style="width: 100%">
                        <div class="c-val" style="font-size: 0.95rem; animation: pulse 1s infinite">AIRING NOW / RELEASED</div>
                    </div>
                `;
            }
        } else {
            const time = calculateTimeRemaining(schedule.targetDate);
            if (valEls.length === 4) {
                valEls[0].innerText = String(time.days).padStart(2, '0');
                valEls[1].innerText = String(time.hours).padStart(2, '0');
                valEls[2].innerText = String(time.minutes).padStart(2, '0');
                valEls[3].innerText = String(time.seconds).padStart(2, '0');
            } else {
                el.countdown.innerHTML = `
                    <div class="c-item"><div class="c-val">${String(time.days).padStart(2, '0')}</div><div class="c-lbl">D</div></div>
                    <div class="c-item"><div class="c-val">${String(time.hours).padStart(2, '0')}</div><div class="c-lbl">H</div></div>
                    <div class="c-item"><div class="c-val">${String(time.minutes).padStart(2, '0')}</div><div class="c-lbl">M</div></div>
                    <div class="c-item"><div class="c-val">${String(time.seconds).padStart(2, '0')}</div><div class="c-lbl">S</div></div>
                `;
            }
        }
    }
}
