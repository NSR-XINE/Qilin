document.getElementById('next-up-banner')?.addEventListener('click', (e) => {
    const streamLink = e.target.closest('.banner-stream-link');
    if (streamLink) {
        const showId = streamLink.dataset.showId;
        const url = getWatchUrlById(showId);
        if (url) openWatchScreen(url);
        return;
    }
    const banner = e.currentTarget;
    const showId = banner.dataset.id;
    if (showId) {
        const show = getShowById(showId);
        if (show) openDetailsModal(show);
    }
});

function renderHeroBanner() {
    const bannerEl = document.getElementById('next-up-banner');
    if (!bannerEl) return;

    const isMobile = window.innerWidth <= 1024;
    if (isMobile && activeTab !== 'home') {
        bannerEl.style.setProperty('display', 'none', 'important');
        return;
    }

    const nextUp = getNextUpShow();
    if (!nextUp) {
        bannerEl.style.display = 'none';
        return;
    }

    bannerEl.style.display = 'flex';
    bannerEl.dataset.id = nextUp.show.id;
    const show = nextUp.show;
    const isAiring = nextUp.airingNow;

    let countdownHtml = '';
    if (isAiring) {
        countdownHtml = `
            <div class="banner-countdown" id="hero-countdown-box">
                <div class="countdown-box" style="border-color: var(--accent-cyan); min-width: 250px;">
                    <div class="num" style="font-size: 1.6rem; animation: pulse 1s infinite;">AIRING NOW</div>
                    <div class="label">Episode ${show.currentEp + 1} Released!</div>
                </div>
            </div>`;
    } else {
        const time = calculateTimeRemaining(nextUp.targetDate);
        countdownHtml = `
            <div class="banner-countdown" id="hero-countdown-box">
                <div class="countdown-box"><div class="num">${String(time.days).padStart(2, '0')}</div><div class="label">Days</div></div>
                <div class="countdown-box"><div class="num">${String(time.hours).padStart(2, '0')}</div><div class="label">Hrs</div></div>
                <div class="countdown-box"><div class="num">${String(time.minutes).padStart(2, '0')}</div><div class="label">Min</div></div>
                <div class="countdown-box"><div class="num">${String(time.seconds).padStart(2, '0')}</div><div class="label">Sec</div></div>
            </div>`;
    }

    const safePoster = safePosterUrl(show.poster);
    const isCached = safePoster && ImageCache.has(safePoster);

    let imageHtml = '';
    if (safePoster) {
        const shimmerCls = isCached ? '' : ' shimmer-loading';
        const loadedCls = isCached ? ' loaded' : '';
        imageHtml = `
            <div class="banner-image-wrap${shimmerCls}" style="background: ${getPosterGradient(show.title)};">
                <img class="banner-poster${loadedCls}"
                     src="${safePoster}"
                     alt="${escapeHtml(show.title)}">
            </div>`;
        if (!isCached) {
            ImageCache.loadImage(safePoster).then(() => {
                const wrap = bannerEl.querySelector('.banner-image-wrap');
                if (wrap) {
                    wrap.classList.remove('shimmer-loading');
                    const img = wrap.querySelector('img');
                    if (img) img.classList.add('loaded');
                }
            }).catch(() => {
                const wrap = bannerEl.querySelector('.banner-image-wrap');
                if (wrap) {
                    wrap.classList.remove('shimmer-loading');
                    const img = wrap.querySelector('img');
                    if (img) img.style.display = 'none';
                }
            });
        }
    } else {
        imageHtml = `<div class="banner-image-wrap" style="background: ${getPosterGradient(show.title)};"></div>`;
    }

    bannerEl.innerHTML = `
        ${imageHtml}
        <div class="banner-info-section">
            <div class="banner-details">
                <span class="banner-badge">
                    <i class="fa-solid ${isAiring ? 'fa-satellite-dish' : 'fa-clock'}"></i> 
                    ${isAiring ? 'Live Release' : 'Next Airing'}
                </span>
                <h2 class="banner-title">${escapeHtml(show.title)}</h2>
                <div class="banner-meta">
                    <span><i class="fa-solid fa-calendar"></i> ${show.releaseDay}s at ${show.releaseTime}</span>
                    <span><i class="fa-solid fa-play"></i> Episode ${show.currentEp + 1} next</span>
                    <span class="banner-stream-link" data-show-id="${show.id}" style="color: var(--accent-cyan); cursor: pointer;"><i class="fa-solid fa-up-right-from-square"></i> Stream Link</span>
                </div>
            </div>
            ${countdownHtml}
        </div>`;
}
