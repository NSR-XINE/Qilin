function getNextReleaseDate(releaseDay, releaseTime) {
    const now = new Date();
    const [hours, minutes] = releaseTime.split(':').map(Number);
    const targetDayNum = DAYS_MAP[releaseDay];
    const currentDayNum = now.getDay();
    let daysUntil = (targetDayNum - currentDayNum + 7) % 7;
    let targetDate = new Date(
        now.getFullYear(), now.getMonth(), now.getDate() + daysUntil
    );
    targetDate.setHours(hours, minutes, 0, 0);
    if (daysUntil === 0 && now > targetDate) {
        const diffMs = now - targetDate;
        const airDurationMs = 90 * 60 * 1000;
        if (diffMs < airDurationMs) {
            return { targetDate, airingNow: true };
        } else {
            targetDate.setDate(targetDate.getDate() + 7);
        }
    }
    return { targetDate, airingNow: false };
}

function calculateTimeRemaining(targetDate) {
    const now = new Date();
    const diffMs = targetDate - now;
    if (diffMs <= 0) {
        return { days: 0, hours: 0, minutes: 0, seconds: 0, elapsed: true };
    }
    const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diffMs % (1000 * 60)) / 1000);
    return { days, hours, minutes, seconds, elapsed: false };
}

function getCountdownHtml(show) {
    if (STATUS.isCompleted(show)) {
        return `
            <div class="card-countdown completed-state">
                <i class="fa-solid fa-circle-check"></i> Series Completed
            </div>
        `;
    }
    const schedule = getNextReleaseDate(show.releaseDay, show.releaseTime);
    if (schedule.airingNow) {
        return `
            <div class="card-countdown" style="background: rgba(0, 242, 254, 0.1); border-color: rgba(0, 242, 254, 0.3)">
                <div class="c-item" style="width: 100%">
                    <div class="c-val" style="font-size: 0.95rem; animation: pulse 1s infinite">AIRING NOW / RELEASED</div>
                </div>
            </div>
        `;
    }
    const time = calculateTimeRemaining(schedule.targetDate);
    return `
        <div class="card-countdown">
            <div class="c-item">
                <div class="c-val">${String(time.days).padStart(2, '0')}</div>
                <div class="c-lbl">D</div>
            </div>
            <div class="c-item">
                <div class="c-val">${String(time.hours).padStart(2, '0')}</div>
                <div class="c-lbl">H</div>
            </div>
            <div class="c-item">
                <div class="c-val">${String(time.minutes).padStart(2, '0')}</div>
                <div class="c-lbl">M</div>
            </div>
            <div class="c-item">
                <div class="c-val">${String(time.seconds).padStart(2, '0')}</div>
                <div class="c-lbl">S</div>
            </div>
        </div>
    `;
}

function getNextUpShow() {
    const ongoing = getOngoingShows();
    if (ongoing.length === 0) return null;
    const computed = ongoing.map(show => {
        const schedule = getNextReleaseDate(show.releaseDay, show.releaseTime);
        return { show, targetDate: schedule.targetDate, airingNow: schedule.airingNow, timeDiff: schedule.targetDate - new Date() };
    });
    computed.sort((a, b) => {
        if (a.airingNow && !b.airingNow) return -1;
        if (!a.airingNow && b.airingNow) return 1;
        if (a.airingNow && b.airingNow) return b.timeDiff - a.timeDiff;
        return a.timeDiff - b.timeDiff;
    });
    return computed[0] || null;
}


