# 东画 My Donghua (Android App)

A sleek, high-aesthetic dark-themed native Android application designed for Chinese anime (Donghua) fans to track watched episodes, view release calendars, follow live countdown clocks, and receive native push notifications.

---

## Folder Structure

```
my_donghua/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/mydonghua/app/
│           ├── MainActivity.kt           # NavHost, entry activity, permission requester
│           ├── WatchActivity.kt          # ExoPlayer/Media3 media playback player with browser fallback
│           ├── AlarmScheduler.kt         # DST-aware exact alarm scheduler
│           ├── BootReceiver.kt           # Re-schedules alarms on boot or package replace (30s WakeLock)
│           ├── NotificationReceiver.kt   # High-priority Heads-up Notification creator & de-duplicator
│           ├── data/
│           │   ├── db/
│           │   │   ├── AppDatabase.kt    # Room database (v3 Schema) with singleton provider
│           │   │   ├── ShowDao.kt        # Show CRUD queries
│           │   │   ├── SettingsDao.kt    # Setting CRUD queries
│           │   │   ├── WatchHistoryDao.kt# Watch history CRUD queries
│           │   │   ├── ShowEntity.kt     # Room entity matching legacy v2 SQLite shows table
│           │   │   ├── SettingEntity.kt  # Room entity matching legacy v2 SQLite settings table
│           │   │   ├── WatchHistoryEntity.kt # Room entity matching watch_history table
│           │   │   └── RoomSyncHelper.kt # Blocking bridge for legacy SQLite imports/conversions
│           │   ├── pref/
│           │   │   └── ThemePreference.kt# Jetpack DataStore preferences for theme/accent/elements toggles
│           │   └── service/
│           │       └── PosterFetchService.kt # Retrofit MAL, AniList GraphQL, and scrapers
│           ├── domain/
│           │   └── model/
│           │       ├── CountdownCalculator.kt # DST-safe countdown ticks calculations
│           │       └── StatsCalculator.kt # Analytics builder for total episodes, streak, hours watched
│           └── ui/
│               ├── common/
│               │   └── ShowCard.kt       # Card grid element with ticking coroutine countdown timer
│               ├── home/
│               │   └── HomeScreen.kt     # Branded header, Hero banner, scroll rows, weekly schedule
│               ├── theme/
│               │   ├── Color.kt          # Preset light, dark, and AMOLED color tokens
│               │   └── Theme.kt          # MaterialTheme with dynamic wallpaper matching on API 31+
│               └── screens/
│                   ├── AiringScreen.kt   # Filtered list of currently airing shows
│                   ├── CompletedScreen.kt# Filtered list of completed shows
│                   ├── StoppedScreen.kt  # Filtered list of hiatus/stopped shows
│                   ├── FavoritesScreen.kt# Filtered list of user-favorited shows
│                   ├── AddEditShowScreen.kt # Form editing / creation screen with poster auto-fill
│                   ├── ShowDetailScreen.kt # Full-bleed hero banner, episode controllers, notes
│                   ├── SearchScreen.kt   # Subsequence fuzzy search bar with dropdown suggestions
│                   ├── SettingsScreen.kt # Theme mode, accent selectors, visibility toggles, SAF sharing
│                   ├── StatsScreen.kt    # 8-card statistics panel + genre/studio tag lists
│                   ├── CalendarScreen.kt # Monthly release calendar grid with indicator dots
│                   ├── CollectionsScreen.kt # Show group folders backed by collection_id field
│                   ├── HistoryScreen.kt  # Date-grouped watch log listing thumbnails
│                   └── ShowsViewModel.kt # Unified view state flow manager
└── README.md
```

---

## Features

- **Track shows** by status: Airing, Completed, Stopped
- **Hero countdown banner** for the next releasing show (full-cover poster)
- **Live countdown clocks** on every show card (D/H/M/S tick every second via coroutines)
- **Weekly schedule calendar** with day tabs, today highlighting, time-sorted items
- **Calendar view** — monthly grid with release-day dots, week preview
- **Favorites** — heart-toggle on any show, dedicated Favorites tab
- **Watch History** — chronological date-grouped log of episode progress changes
- **Statistics dashboard** — totals, airing/completed/stopped counts, episodes, hours watched, completion rate, day streak, weekly/monthly activity, top genres & studios, average rating, longest series
- **Continue Watching** — horizontal scroll section showing in-progress shows with progress bars; sorted by most recently opened
- **Recently Updated** — horizontal scroll section showing the 10 most recently modified shows
- **Collections** — group related shows by collection folders
- **Native push notifications** via Android AlarmManager (DST-aware); configurable quiet hours
- **Poster auto-fill** from Jikan (MyAnimeList), AniList GraphQL, DonghuaStream, AnimeCountdown
- **Export/Import** watchlist as JSON backup via Storage Access Framework (SAF) and native share intents
- **Streaming media** — ExoPlayer / Media3 direct video playback (`.mp4`, `.m3u8`, `.mkv`) with safe system browser fallback for external streaming sites
- **Multiple themes**: Dark, AMOLED (true black), Light; dynamic accent colors
- **Advanced filtering** — filter by genre, studio, year, rating, max episodes, sort by countdown/alphabetical/progress/last-updated
- **Advanced search** — recent searches, trending suggestions, subsequence fuzzy matching, highlighted matches
- **Smart Home Dashboard** — toggle visibility of countdown banner, continue watching, recently updated, and weekly schedule sections
- **Material You dynamic colors** — accent color auto-selected from system wallpaper on Android 12+
- **Bottom navigation**: Home, Airing, Add, Complete, Stopped, Favorites

---

## Room Database Schema (v3)

All watchlist records are stored in a native Room SQLite database (`mydonghua.db`) with three tables:

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

Indexes: `idx_shows_status`, `idx_shows_release_day`, `idx_shows_favorite`, `idx_genres`, `idx_collection`, `idx_last_updated`

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

Capped at 500 entries (oldest removed).

---

## Notification Alarm Flow

1. Show details or episode progress adjustments trigger alarm updates.
2. Calls `AlarmScheduler.schedule` → registers `AlarmManager` intent with `FLAG_IMMUTABLE` (API 23+).
3. `AlarmScheduler` calculates next release time with DST-aware calendar calendar logic.
4. `BootReceiver` uses `PowerManager.WakeLock` (30s) to query Room database and reschedule all alarms after reboot or package upgrade.
5. `NotificationReceiver` de-duplicates by alarm code hash and auto-reschedules after firing.
6. Alarm fires at release time → high-priority heads-up notification.
7. Tapping notification opens the app and navigates to the show's detail screen.

---

## Compilation

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

---

## Architecture Notes

- **Pure Native Stack** — 100% written in Kotlin using Jetpack Compose, Material 3, and Navigation-Compose.
- **Data Persistence** — Room Database handles shows, history logs, and settings. Jetpack DataStore manages theme, color presets, and dashboard UI toggles.
- **Network & Parsing** — Retrofit + OkHttp handles API calls to Jikan (MyAnimeList) and AniList GraphQL endpoints.
- **Media Playback** — ExoPlayer/Media3 plays direct streams (`.mp4`, `.m3u8`, `.mkv`) natively with full screen controls.
- **Auto-Sync** — WorkManager schedules background synchronization workers periodically.
