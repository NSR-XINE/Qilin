window.imageCache = new Map();

window.onBannerImageLoad = function(img, url) {
    if (img.classList.contains('loaded')) return;
    img.classList.add('loaded');
    const wrap = img.closest('.banner-image-wrap');
    if (wrap) wrap.classList.remove('shimmer-loading');
    if (url) window.imageCache.set(url, 'loaded');
};

window.onBannerImageError = function(img) {
    img.style.display = 'none';
    const wrap = img.closest('.banner-image-wrap');
    if (wrap) wrap.classList.remove('shimmer-loading');
};

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

    const bannerGradient = getPosterGradient(show.title);
    const safePoster = safePosterUrl(show.poster);
    const isCached = safePoster && window.imageCache.get(safePoster) === 'loaded';

    let imageHtml = '';
    if (safePoster) {
        const shimmerCls = isCached ? '' : ' shimmer-loading';
        const loadedCls = isCached ? ' loaded' : '';
        const escapedUrl = safePoster.replace(/'/g, "\\'");
        imageHtml = `
            <div class="banner-image-wrap${shimmerCls}" style="background: ${bannerGradient};">
                <img class="banner-poster${loadedCls}"
                     src="${safePoster}"
                     alt="${escapeHtml(show.title)}"
                     onload="onBannerImageLoad(this, '${escapedUrl}')"
                     onerror="onBannerImageError(this)">
            </div>`;
    } else {
        imageHtml = `<div class="banner-image-wrap" style="background: ${bannerGradient};"></div>`;
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
                    <span><span onclick="openWatchScreen(getWatchUrlById('${String(show.id).replace(/'/g, "\\'")}'))" style="color: var(--accent-cyan); cursor: pointer;"><i class="fa-solid fa-up-right-from-square"></i> Stream Link</span></span>
                </div>
            </div>
            ${countdownHtml}
        </div>`;
}
