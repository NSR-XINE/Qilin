# 东画 My Donghua (Android App)

A sleek, high-aesthetic dark-themed native Android application designed for Chinese anime (Donghua) fans to track watched episodes, view release schedules, follow live countdown clocks, and receive native push notifications.

---

## Folder Structure

```
donghua-tracker-repo/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mydonghua/app/
│       │   ├── MainActivity.java         # WebView settings, lifecycle, AndroidBridge JS interface
│       │   ├── WatchActivity.java        # YouTube-style player with fullscreen
│       │   ├── AlarmScheduler.java       # DST-aware alarm scheduling and permission handling
│       │   ├── BootReceiver.java         # Re-schedules alarms after device reboot / upgrade
│       │   ├── DatabaseHelper.java       # SQLite database CRUD operations (v2 schema)
│       │   └── NotificationReceiver.java # Push notification channel and handler
│       └── assets/
│           ├── index.html                # Main markup with all modal sheets
│           ├── css/
│           │   ├── base.css              # Reset, CSS custom properties, scrollbar, stars bg, safe-area
│           │   ├── layout.css            # App container, header, main grid, glass-card
│           │   ├── components.css        # Search, stats, cards, buttons, schedule, empty-state
│           │   ├── themes.css            # Light & AMOLED theme overrides (no hardcoded dark colors)
│           │   ├── navigation.css        # Bottom nav bar (fixed glass, nav items, add icon)
│           │   ├── banner.css            # Hero countdown banner with full-cover poster
│           │   ├── modals.css            # Modal overlay, header/body/footer, form inputs
│           │   └── animations.css        # Float, pulse, scaleUp, loading overlay keyframes
│           ├── js/
│           │   ├── app.js                # Entry point: DOM init, timer, event wiring
│           │   ├── utils/
│           │   │   ├── constants.js      # DAYS_MAP, STATUS_MAP, GRADIENTS, VALID_STATUSES
│           │   │   └── helpers.js        # hashCode, debounce, throttle, fuzzyMatch, escapeHtml
│           │   ├── database/
│           │   │   └── bridge.js         # DB bridge wrapping AndroidApp with localStorage fallback
│           │   ├── services/
│           │   │   ├── state.js          # Global shows array, CRUD, status migrations, persist
│           │   │   ├── countdown.js      # Next release date, time remaining, countdown HTML
│           │   │   ├── theme.js          # setThemeMode, initTheme, updateThemeMeta
│           │   │   ├── search.js         # performSearch with fuzzy scoring, suggestions
│           │   │   ├── poster.js         # Auto-fetch posters from Jikan, AniList, AnimeCountdown
│           │   │   ├── notifications.js  # syncAlarm, export/import data, openWatchScreen
│           │   │   └── stats.js          # computeStats, updateStats (total/airing/completed/favs)
│           │   ├── components/
│           │   │   ├── cards.js          # renderShowsGrid, renderCard, renderFavoritesGrid
│           │   │   ├── banner.js         # renderHeroBanner with countdown & full-cover poster
│           │   │   ├── schedule.js       # renderWeeklySchedule with day grouping & today highlight
│           │   │   ├── modals.js         # All modal open/close, swipe-to-dismiss, back-press
│           │   │   └── nav.js            # switchTab (home/airing/complete/stopped/favorites)
│           │   └── pages/
│           │       ├── history.js        # Watch History modal with date-grouped entries
│           │       ├── stats.js          # Statistics dashboard (8-card grid + week preview)
│           │       ├── calendar.js       # Monthly calendar view with release dots
│           │       └── collections.js    # Collection grouping UI
│           ├── app.js                    # (legacy monolithic — replaced by modular files)
│           └── styles.css                # (legacy monolithic — replaced by modular files)
└── README.md
```

---

## Features

- **Track shows** by status: Airing, Completed, Hiatus
- **Hero countdown banner** for the next releasing show (full-cover poster)
- **Live countdown clocks** on every show card (D/H/M/S tick every second)
- **Weekly schedule calendar** with day tabs, today highlighting, time-sorted items
- **Calendar view** — monthly grid with release-day dots, week preview
- **Favorites** — heart-toggle on any show, dedicated Favorites tab
- **Watch History** — chronological log of episode progress changes
- **Statistics dashboard** — totals, airing/completed/stopped counts, episodes, hours watched, completion rate, day streak, weekly/monthly activity, top genres & studios, average rating, longest series
- **Continue Watching** — horizontal scroll section showing in-progress shows with progress bars and "Continue" button; sorted by most recently opened
- **Recently Updated** — horizontal scroll section showing the 10 most recently modified shows with time-ago labels
- **Collections** — group related shows by collection name
- **Native push notifications** via Android AlarmManager (DST-aware); configurable quiet hours
- **Poster auto-fetch** from Jikan (MyAnimeList), AniList GraphQL, DonghuaStream, AnimeCountdown
- **Auto-fill** Chinese title, synopsis, poster, total episodes from API
- **Export/Import** watchlist as JSON backup (native share intent or file picker)
- **Streaming links** — configurable default source (DonghuaStream / LuciferDonghua)
- **Auto-rotate** toggle with manual rotation button fallback
- **Multiple themes**: Dark, AMOLED (true black), Light; dynamic accent colors
- **Advanced filtering** — filter by genre, studio, year, rating, max episodes, sort by countdown/alphabetical/progress/last-updated
- **Advanced search** — recent searches, trending suggestions, fuzzy matching, keyboard navigation, highlighted matches
- **Smart Home Dashboard** — toggle visibility of countdown banner, continue watching, recently updated, and weekly schedule sections
- **Cache management** — image cache stored in localStorage with automatic TTL cleanup; clear from settings
- **Material You dynamic colors** — accent color auto-selected from system wallpaper on Android 12+
- **Image optimization** — memory + disk cache with TTL, preloading, lazy loading
- **Bottom navigation**: Home, Airing, Add, Complete, Stopped, Favorites
- **Swipe-to-dismiss** modals (settings, add/edit, details, history, etc.)
- **Safe area insets** — notch/punch-hole and navigation bar aware (env + JS-injected)
- **SQLite storage** with LocalStorage fallback; localStorage is authoritative on startup

---

## SQLite Database Schema (v2)

All show data is stored in a native SQLite database (`mydonghua.db`) with three core tables and full index support.

### 1. `shows`

| Column | Type | Description |
|---|---|---|
| `id` | TEXT PK | Show identifier |
| `title` | TEXT NOT NULL | English/Romaji title |
| `title_zh` | TEXT | Chinese title |
| `status` | TEXT | `ongoing`, `completed`, `stopped` |
| `release_day` | TEXT | `Monday` – `Sunday` |
| `release_time` | TEXT | `HH:MM` |
| `current_ep` | INTEGER | Watched episodes |
| `total_ep` | INTEGER | Total episodes (0 = unknown) |
| `poster` | TEXT | Poster image URL |
| `watch_url` | TEXT | DonghuaStream or custom URL |
| `countdown_url` | TEXT | AnimeCountdown page URL |
| `notes` | TEXT | Synopsis and personal notes |
| `is_favorite` | INTEGER | 0 or 1 |
| `rating` | INTEGER | User rating 1–10 |
| `last_updated` | INTEGER | Unix ms timestamp |
| `date_added` | INTEGER | Unix ms timestamp |
| `alarm_request_code` | INTEGER | Unique alarm identifier |
| `genres` | TEXT | Comma-separated genres |
| `studio` | TEXT | Animation studio |
| `release_year` | INTEGER | Year of release |
| `language` | TEXT | Language (e.g. Chinese, Japanese) |
| `collection_id` | TEXT | Collection/group name |

Indexes: `idx_status`, `idx_release_day`, `idx_favorite`, `idx_last_updated`, `idx_collection`

### 2. `settings`

| Column | Type |
|---|---|
| `key` | TEXT PK |
| `value` | TEXT |

### 3. `watch_history`

| Column | Type |
|---|---|
| `id` | INTEGER PK AUTOINCREMENT |
| `show_id` | TEXT NOT NULL |
| `episode_num` | INTEGER NOT NULL |
| `watched_timestamp` | INTEGER NOT NULL |

Capped at 500 entries (oldest removed). Foreign key cascades on show delete.

---

## Notification Alarm Flow

1. Episode progress changes trigger alarm scheduling via `state.js`.
2. Calls `window.AndroidApp.scheduleReminder` → registers `AlarmManager` intent with `FLAG_IMMUTABLE` (API 23+).
3. `AlarmScheduler.java` calculates next release time with DST-aware logic.
4. `BootReceiver.java` uses `PowerManager.WakeLock` (30s) to reschedule all alarms after reboot or package upgrade.
5. `NotificationReceiver.java` de-duplicates by alarm code hash and auto-reschedules after firing.
6. Alarm fires at release time → high-priority heads-up notification.
7. Tapping notification opens the app and opens the show's detail modal.

---

## Compilation

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Output at `app/build/outputs/apk/debug/app-debug.apk` or `release/app-release.apk` (signed with debug keystore).

---

## Architecture Notes

- **Java** — Thin native layer: WebView host, SQLite via `SQLiteOpenHelper`, alarms via `AlarmManager` + `BroadcastReceiver`, notifications via `NotificationCompat`.
- **JavaScript** — Entire UI is a single-page application rendered in a WebView. Modularized into `utils/`, `services/`, `components/`, `pages/` for maintainability.
- **CSS** — Modular files organized by concern. Uses CSS custom properties for theming. `themes.css` contains only override variables — no hardcoded dark colors in light/amoled overrides.
- **Backward compatibility** — All `@JavascriptInterface` method signatures preserved. LocalStorage fallback when native bridge is unavailable. v1→v2 schema migration on first launch.
- **LocalStorage-first startup** — `loadState()` prefers the in-memory/localStorage shows array over SQLite, ensuring data survives even if the native database insert fails or the WebView URL origin changes.
- **Status migration** — `upcoming` → `stopped` migrated automatically. Shows auto-complete when `currentEp >= totalEp > 0`.
