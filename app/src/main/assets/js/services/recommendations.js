(function() {
    'use strict';

    const RECS_COUNT = 6;

    function parseGenres(genreStr) {
        return (genreStr || '').split(',').map(g => g.trim()).filter(Boolean);
    }

    function genreIntersection(a, b) {
        const setA = new Set(a.map(g => g.toLowerCase()));
        return b.filter(g => setA.has(g.toLowerCase())).length;
    }

    function getRecommendations() {
        const allShows = window.shows || [];
        if (allShows.length < 2) return [];

        const genreFreq = {};
        const watchedIds = new Set();

        for (const show of allShows) {
            if (show.genres) {
                parseGenres(show.genres).forEach(g => {
                    genreFreq[g] = (genreFreq[g] || 0) + 1;
                });
            }
            const havingWatched = show.currentEp > 0 || show.isFavorite;
            if (havingWatched) {
                watchedIds.add(show.id);
            }
        }

        const topGenres = Object.entries(genreFreq)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 5)
            .map(e => e[0]);

        if (topGenres.length === 0) return [];

        const candidates = allShows.filter(s => !watchedIds.has(s.id));
        const scored = candidates.map(show => {
            const showGenres = parseGenres(show.genres);
            const sharedGenres = genreIntersection(topGenres, showGenres);
            let score = sharedGenres * 10;
            if (showGenres.some(g => topGenres.includes(g))) {
                score += 5;
            }
            return { show, score };
        });

        const filtered = scored.filter(s => s.score > 0);
        filtered.sort((a, b) => b.score - a.score);

        return filtered.slice(0, RECS_COUNT).map(s => s.show);
    }

    window.getRecommendations = getRecommendations;
})();
