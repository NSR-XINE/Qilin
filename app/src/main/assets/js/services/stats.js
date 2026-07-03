function computeStats() {
    const total = shows.length;
    const counts = STATUS.countByStatus();
    const airing = counts.airing;
    const completed = counts.completed;
    const stopped = counts.stopped;
    const totalEpisodes = shows.reduce((sum, s) => sum + (s.currentEp || 0), 0);
    const favorites = shows.filter(s => s.isFavorite).length;
    const totalEpsPossible = shows.reduce((sum, s) => sum + (s.totalEp || 0), 0);
    const hoursWatched = Math.round((totalEpisodes * 24) / 60);
    const completionRate = totalEpsPossible > 0 ? Math.round((totalEpisodes / totalEpsPossible) * 100) : 0;

    const ratings = shows.filter(s => s.rating && s.rating > 0);
    const avgRating = ratings.length > 0 ? (ratings.reduce((sum, s) => sum + s.rating, 0) / ratings.length).toFixed(1) : '—';

    const genreCounts = {};
    const studioCounts = {};
    shows.forEach(s => {
        if (s.genres) {
            s.genres.split(',').map(g => g.trim()).filter(Boolean).forEach(g => {
                genreCounts[g] = (genreCounts[g] || 0) + 1;
            });
        }
        if (s.studio) {
            studioCounts[s.studio] = (studioCounts[s.studio] || 0) + 1;
        }
    });
    const topGenres = Object.entries(genreCounts).sort((a, b) => b[1] - a[1]).slice(0, 5);
    const topStudios = Object.entries(studioCounts).sort((a, b) => b[1] - a[1]).slice(0, 5);

    let longestSeries = null;
    let longestEpCount = 0;
    shows.forEach(s => {
        const eps = s.totalEp || s.currentEp || 0;
        if (eps > longestEpCount) {
            longestEpCount = eps;
            longestSeries = s;
        }
    });

    const now = Date.now();
    const weekMs = 7 * 24 * 60 * 60 * 1000;
    const monthMs = 30 * 24 * 60 * 60 * 1000;
    const weeklyActivity = shows.filter(s => s.lastUpdated && (now - s.lastUpdated) < weekMs).length;
    const monthlyActivity = shows.filter(s => s.lastUpdated && (now - s.lastUpdated) < monthMs).length;

    const sortedByLastUpdated = [...shows].filter(s => s.lastUpdated).sort((a, b) => (b.lastUpdated || 0) - (a.lastUpdated || 0));
    let streak = 0;
    if (sortedByLastUpdated.length > 0) {
        let checkDate = new Date();
        for (let i = 0; i < 365; i++) {
            const dayStart = new Date(checkDate.getFullYear(), checkDate.getMonth(), checkDate.getDate()).getTime();
            const dayEnd = dayStart + 86400000;
            const hasActivity = sortedByLastUpdated.some(s => s.lastUpdated >= dayStart && s.lastUpdated < dayEnd);
            if (hasActivity) {
                streak++;
                checkDate.setDate(checkDate.getDate() - 1);
            } else {
                break;
            }
        }
    }

    return {
        total, airing, completed, stopped,
        totalEpisodes, totalEpsPossible, favorites, hoursWatched,
        completionRate, avgRating,
        topGenres, topStudios,
        longestSeries, longestEpCount,
        weeklyActivity, monthlyActivity, streak
    };
}

function updateStats() {
    return computeStats();
}
