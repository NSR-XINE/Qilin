const STATUS = {
  AIRING: 'ongoing',
  COMPLETED: 'completed',
  STOPPED: 'stopped',

  VALID: new Set(['ongoing', 'completed', 'stopped']),

  isValid(status) {
    return this.VALID.has(status);
  },

  getDisplayName(status) {
    return STATUS_MAP[status] || status || 'Airing';
  },

  getCategory(show) {
    if (!show || !this.isValid(show.status)) return this.AIRING;
    return show.status;
  },

  isAiring(show) {
    return this.getCategory(show) === this.AIRING;
  },

  isCompleted(show) {
    return this.getCategory(show) === this.COMPLETED;
  },

  isStopped(show) {
    return this.getCategory(show) === this.STOPPED;
  },

  allAiring() {
    return shows.filter(s => this.isAiring(s));
  },

  allCompleted() {
    return shows.filter(s => this.isCompleted(s));
  },

  allStopped() {
    return shows.filter(s => this.isStopped(s));
  },

  countByStatus() {
    let airing = 0, completed = 0, stopped = 0;
    for (const s of shows) {
      if (this.isAiring(s)) airing++;
      else if (this.isCompleted(s)) completed++;
      else if (this.isStopped(s)) stopped++;
    }
    return { airing, completed, stopped };
  },

  migrate(show) {
    let changed = false;
    if (show.status === 'upcoming') {
      show.status = this.STOPPED;
      changed = true;
    }
    if (!this.isValid(show.status)) {
      show.status = this.AIRING;
      changed = true;
    }
    if (show.status === this.AIRING && show.totalEp > 0 && show.currentEp >= show.totalEp) {
      show.status = this.COMPLETED;
      changed = true;
    }
    return changed;
  },

  shouldAutoComplete(show) {
    return show.status === this.AIRING && show.totalEp > 0 && show.currentEp >= show.totalEp;
  },

  getNextEpisode(show) {
    if (!this.isAiring(show)) return null;
    return (show.currentEp || 0) + 1;
  },

  getProgress(show) {
    const current = show.currentEp || 0;
    const total = show.totalEp || 0;
    return { current, total, ratio: total > 0 ? current / total : 0 };
  }
};
