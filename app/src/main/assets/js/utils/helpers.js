function hashCode(str) {
    let hash = 5381;
    for (let i = 0; i < str.length; i++) {
        hash = ((hash << 5) + hash) ^ str.charCodeAt(i);
    }
    return hash >>> 0;
}

function safePosterUrl(url) {
    if (!url || typeof url !== 'string') return null;
    const trimmed = url.trim();
    if (!/^https?:\/\//i.test(trimmed)) return null;
    if (/['"()\\<>\s]/.test(trimmed)) return null;
    return trimmed;
}

function getPosterGradient(title) {
    let hash = 0;
    for (let i = 0; i < title.length; i++) {
        hash = title.charCodeAt(i) + ((hash << 5) - hash);
    }
    const index = Math.abs(hash) % GRADIENTS.length;
    return GRADIENTS[index];
}

function getInitials(title) {
    if (!title) return '';
    return title.split(' ').map(w => w[0]).filter(Boolean).slice(0, 3).join('').toUpperCase();
}

function debounce(fn, delay) {
    let timer = null;
    return function(...args) {
        if (timer) clearTimeout(timer);
        timer = setTimeout(() => fn.apply(this, args), delay);
    };
}

function fuzzyMatch(text, query) {
    if (!text || !query) return false;
    const t = text.toLowerCase().trim();
    const q = query.toLowerCase().trim();
    if (q.length === 0) return true;
    if (t.includes(q)) return true;
    let qi = 0;
    for (let i = 0; i < t.length && qi < q.length; i++) {
        if (t[i] === q[qi]) qi++;
    }
    return qi === q.length;
}

function getTodayName() {
    return DAYS_ARRAY[new Date().getDay()];
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
