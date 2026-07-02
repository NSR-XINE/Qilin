// --- Injected styles ---
(function injectSearchStyles() {
    if (document.getElementById('search-styles')) return;
    const style = document.createElement('style');
    style.id = 'search-styles';
    style.textContent = `
        .search-highlight {
            color: var(--accent-cyan);
            font-weight: 600;
        }
        .search-suggestion-item.selected {
            background: rgba(255,255,255,0.08) !important;
        }
    `;
    document.head.appendChild(style);
})();

// --- Recent Searches (localStorage) ---
const RECENT_SEARCHES_KEY = 'recent_searches';
const SEARCH_FREQ_KEY = 'search_frequency';
const MAX_RECENT = 10;

function loadRecentSearches() {
    try {
        const raw = localStorage.getItem(RECENT_SEARCHES_KEY);
        return raw ? JSON.parse(raw) : [];
    } catch(e) { return []; }
}

function saveRecentSearches(searches) {
    try { localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(searches)); } catch(e) {}
}

function addRecentSearch(query) {
    const q = query.trim();
    if (!q) return;
    let recent = loadRecentSearches();
    recent = recent.filter(s => s.toLowerCase() !== q.toLowerCase());
    recent.unshift(q);
    if (recent.length > MAX_RECENT) recent = recent.slice(0, MAX_RECENT);
    saveRecentSearches(recent);
}

function loadSearchFrequency() {
    try {
        const raw = localStorage.getItem(SEARCH_FREQ_KEY);
        return raw ? JSON.parse(raw) : {};
    } catch(e) { return {}; }
}

function saveSearchFrequency(freq) {
    try { localStorage.setItem(SEARCH_FREQ_KEY, JSON.stringify(freq)); } catch(e) {}
}

function incrementSearchFrequency(query) {
    const q = query.trim().toLowerCase();
    if (!q) return;
    const freq = loadSearchFrequency();
    freq[q] = (freq[q] || 0) + 1;
    saveSearchFrequency(freq);
}

function getPopularSearches(limit) {
    if (limit === undefined) limit = 3;
    const freq = loadSearchFrequency();
    return Object.entries(freq)
        .sort((a, b) => b[1] - a[1])
        .slice(0, limit)
        .map(e => e[0]);
}

// --- Highlight helper ---
function highlightMatch(text, query) {
    if (!text || !query) return escapeHtml(text);
    const lower = text.toLowerCase();
    const q = query.toLowerCase();
    const idx = lower.indexOf(q);
    if (idx === -1) return escapeHtml(text);
    const before = escapeHtml(text.slice(0, idx));
    const match = escapeHtml(text.slice(idx, idx + q.length));
    const after = escapeHtml(text.slice(idx + q.length));
    return before + '<span class="search-highlight">' + match + '</span>' + after;
}

// --- Ranking ---
function scoreShow(show, query) {
    const q = query.toLowerCase().trim();
    const title = (show.title || '').toLowerCase().trim();
    const titleZh = (show.titleZh || '').toLowerCase().trim();

    if (title === q) return 100;
    if (title.startsWith(q)) return 80;
    if (title.includes(q)) return 60;
    if (titleZh.includes(q)) return 50;
    if (fuzzyMatch(titleZh, q)) return 40;
    if (show.notes && fuzzyMatch(show.notes.toLowerCase(), q)) return 20;
    return 0;
}

function getSearchSuggestions(query) {
    const q = query.trim();
    if (!q) {
        const recent = loadRecentSearches();
        const popular = getPopularSearches(3);
        const suggestions = [];
        const seen = new Set();

        popular.forEach(p => {
            if (!seen.has(p)) {
                suggestions.push({ type: 'popular', text: p });
                seen.add(p);
            }
        });
        recent.forEach(r => {
            if (!seen.has(r)) {
                suggestions.push({ type: 'recent', text: r });
                seen.add(r);
            }
        });
        return suggestions;
    }

    const qLower = q.toLowerCase();
    const scored = [];
    const seenIds = new Set();

    shows.forEach(show => {
        if (seenIds.has(show.id)) return;
        const score = scoreShow(show, q);
        if (score > 0) {
            seenIds.add(show.id);
            const titleLower = (show.title || '').toLowerCase();
            const titleZhLower = (show.titleZh || '').toLowerCase();
            let displayText = show.title;
            if (titleLower === qLower || titleLower.startsWith(qLower) || titleLower.includes(qLower)) {
                displayText = show.title;
            } else if (titleZhLower.includes(qLower) || fuzzyMatch(titleZhLower, qLower)) {
                displayText = show.titleZh;
            } else if (show.notes && fuzzyMatch(show.notes.toLowerCase(), qLower)) {
                displayText = show.title;
            }
            scored.push({ show, score, displayText });
        }
    });

    scored.sort((a, b) => b.score - a.score);
    return scored.slice(0, 8).map(item => ({
        type: 'show',
        show: item.show,
        displayText: item.displayText
    }));
}

let suggestionsVisible = false;
let selectedIndex = -1;

function updateSearchSuggestions(suggestions) {
    let container = document.getElementById('search-suggestions');
    if (!container) {
        container = document.createElement('div');
        container.id = 'search-suggestions';
        container.style.cssText = 'position:absolute;top:100%;left:0;right:0;background:var(--bg-card);border:1px solid var(--border-color);border-radius:8px;z-index:1000;max-height:300px;overflow-y:auto;margin-top:4px;';
        document.querySelector('.search-box')?.appendChild(container);
    }
    if (!suggestions || suggestions.length === 0) {
        container.style.display = 'none';
        suggestionsVisible = false;
        selectedIndex = -1;
        return;
    }
    container.style.display = 'block';
    suggestionsVisible = true;
    selectedIndex = -1;

    const searchInput = document.getElementById('search-input');
    const q = searchInput ? searchInput.value.trim() : '';

    container.innerHTML = suggestions.map((item, index) => {
        const isShow = item.type === 'show';
        const icon = isShow ? '' : (item.type === 'popular' ? '\uD83D\uDD25 ' : '\uD83D\uDD50 ');
        let displayHtml;
        if (isShow && q) {
            displayHtml = highlightMatch(item.displayText, q);
        } else {
            displayHtml = escapeHtml(item.displayText || item.text);
        }
        return '<div class="search-suggestion-item' + (index === selectedIndex ? ' selected' : '') + '" data-index="' + index + '"' + (isShow ? ' data-show-id="' + item.show.id + '"' : '') + ' style="padding:0.5rem 0.8rem;cursor:pointer;font-size:0.85rem;color:var(--text-secondary);border-bottom:1px solid var(--border-color);transition:background 0.2s" onmouseover="this.style.background=\'rgba(255,255,255,0.05)\'" onmouseout="this.style.background=\'transparent\'" onclick="selectSuggestion(' + index + ')">' + icon + displayHtml + '</div>';
    }).join('');
}

function selectSuggestion(index) {
    const container = document.getElementById('search-suggestions');
    if (!container) return;
    const items = container.querySelectorAll('.search-suggestion-item');
    if (index < 0 || index >= items.length) return;
    const item = items[index];
    const showId = item.dataset.showId;
    const text = (item.textContent || '').replace(/[\uD83D\uDD25\uD83D\uDD50]/g, '').trim();

    const searchInput = document.getElementById('search-input');
    searchInput.value = text;

    if (showId) {
        filters.search = text;
        renderShowsGrid();
        renderHeroBanner();
        addRecentSearch(text);
        incrementSearchFrequency(text);
    } else {
        searchInput.dispatchEvent(new Event('input'));
    }
    container.style.display = 'none';
    suggestionsVisible = false;
    selectedIndex = -1;
    searchInput.focus();
}

function setupSearch() {
    const searchInput = document.getElementById('search-input');
    if (!searchInput) return;

    const debouncedSearch = debounce(function() {
        const q = searchInput.value.trim();
        if (q) {
            addRecentSearch(q);
            incrementSearchFrequency(q);
        }
        filters.search = q;
        renderShowsGrid();
        renderHeroBanner();
    }, 200);

    const debouncedSuggestions = debounce(function() {
        const suggestions = getSearchSuggestions(searchInput.value);
        updateSearchSuggestions(suggestions);
    }, 150);

    searchInput.addEventListener('input', function() {
        debouncedSearch();
        debouncedSuggestions();
    });

    searchInput.addEventListener('focus', function() {
        const suggestions = getSearchSuggestions(searchInput.value);
        updateSearchSuggestions(suggestions);
    });

    searchInput.addEventListener('blur', function() {
        setTimeout(function() {
            const container = document.getElementById('search-suggestions');
            if (container) container.style.display = 'none';
            suggestionsVisible = false;
            selectedIndex = -1;
        }, 200);
    });

    searchInput.addEventListener('keydown', function(e) {
        const container = document.getElementById('search-suggestions');
        if (!container || container.style.display === 'none') return;
        const items = container.querySelectorAll('.search-suggestion-item');
        if (items.length === 0) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            selectedIndex = Math.min(selectedIndex + 1, items.length - 1);
            updateSelection(container, items);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            selectedIndex = Math.max(selectedIndex - 1, -1);
            updateSelection(container, items);
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (selectedIndex >= 0) selectSuggestion(selectedIndex);
        } else if (e.key === 'Escape') {
            container.style.display = 'none';
            suggestionsVisible = false;
            selectedIndex = -1;
        }
    });
}

function updateSelection(container, items) {
    items.forEach(function(item, i) {
        if (i === selectedIndex) {
            item.classList.add('selected');
        } else {
            item.classList.remove('selected');
        }
    });
    if (selectedIndex >= 0 && items[selectedIndex]) {
        items[selectedIndex].scrollIntoView({ block: 'nearest' });
    }
}
