function computeStats() {
    const total = shows.length;
    const counts = STATUS.countByStatus();
    const airing = counts.airing;
    const completed = counts.completed;
    const stopped = counts.stopped;
    const totalEpisodes = shows.reduce((sum, s) => sum + (s.currentEp || 0), 0);
    const favorites = shows.filter(s => s.isFavorite).length;
    const hoursWatched = Math.round((totalEpisodes * 24) / 60);
    return { total, airing, completed, stopped, totalEpisodes, favorites, hoursWatched };
}

function updateStats() {
    return computeStats();
}
