# 东画 My Donghua (Android App)

A sleek, high-aesthetic dark-themed native Android application designed for Chinese anime (Donghua) fans to track watched episodes, view release calendars, follow live countdown clocks, install home screen widgets, and receive native push notifications.

---

## 🌟 Tech Stack & Key Technologies

- **Language & Runtime**: 100% Native Kotlin 2.0.0 (JVM 17)
- **UI Framework**: Jetpack Compose (BOM `2024.09.00`), Material 3, Navigation-Compose
- **Visual Design**:
  - **Haze 0.7.3** — Real-time backdrop blur & glassmorphism
  - **Kyant Liquid Glass** — Custom vendored AGSL shader-based liquid glass bottom navigation bar
  - **Performance-tuned**: static glass background (no per-frame gradient animation), off-main-thread GPU luminance sampling, short pure cross-fade route transitions — tuned to keep scrolling, search, settings, and tab switches smooth
- **Architecture**: Clean Architecture + MVVM + Dagger Hilt Dependency Injection
- **Database & Storage**: Room Database (v3 Schema) with `StateFlow` reactive queries, Jetpack DataStore Preferences
- **Network & Parsing**: Retrofit 2.11 + OkHttp 4.12 + Kotlinx Serialization, AniList GraphQL & MAL scrapers
- **Media Playback**: ExoPlayer / Media3 1.3.1 (Supports `.mp4`, `.m3u8`, `.mkv` direct streaming & custom web player fallback)
- **Background & Scheduling**: Android `AlarmManager` (DST-aware exact alarms), `PowerManager.WakeLock` boot receivers, App Widgets

---

## 📁 Folder Structure

```
my_donghua/
├── app/
│   ├── build.gradle                          # Dependencies, Kotlin 2.0 Compose compiler, Hilt config
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml               # Permissions, receivers, activities, widget providers
│       └── java/
│           ├── com/kyant/backdrop/           # Vendored Kyant Liquid Glass shader system (22 files)
│           └── com/mydonghua/app/
│               ├── DonghuaApp.kt             # Application class (@HiltAndroidApp)
│               ├── MainActivity.kt           # Entry activity, NavHost, Haze glass layer, LiquidGlass nav, search keyboard focus
│               ├── WatchActivity.kt          # ExoPlayer / Media3 full-screen video player
│               ├── AlarmScheduler.kt         # DST-aware exact alarm scheduler
│               ├── BootReceiver.kt           # Re-schedules alarms on boot or app update (WakeLock)
│               ├── NotificationReceiver.kt   # High-priority Heads-up Notification creator
│               ├── CountdownWidget.kt        # App Widget provider for Home Screen release countdowns
│               ├── di/                       # Dagger Hilt Modules
│               │   ├── DatabaseModule.kt     # Room AppDatabase & DAO injection
│               │   └── RepositoryModule.kt   # Repository interface bindings
│               ├── data/
│               │   ├── db/                   # Room v3 (shows, settings, watch_history) + DAOs
│               │   ├── pref/
│               │   │   └── ThemePreference.kt# DataStore preferences for theme & UI visibility
│               │   ├── repository/           # Show / Settings / WatchHistory repository impls (Room-backed)
│               │   └── service/
│               │       └── PosterFetchService.kt # Jikan, AniList GraphQL & HTML scrapers
│               ├── domain/
│               │   ├── model/                # Show, CountdownCalculator (DST-safe), StatsCalculator
│               │   └── repository/           # Domain repository interfaces
│               └── ui/
│                   ├── GlassEffect.kt        # Static glass background, GlassSection glass components
│                   ├── GlassHeader.kt        # Top glass header
│                   ├── LiquidGlassButton.kt  # Liquid glass button
│                   ├── LiquidGlassNavBar.kt  # Custom liquid glass bottom navigation bar
│                   ├── common/               # ShowCard, DonghuaPoster, EmptyState, ErrorState, ShimmerEffect
│                   ├── home/                 # HomeScreen layout, hero banner, continue watching, recent searches
│                   ├── theme/                # Color palettes, Material 3 theme, animations & dynamic wallpaper matching
│                   └── screens/              # Airing, Completed, Stopped, Favorites, Detail, Search, Settings, Stats, Calendar, Collections, History, Add/Edit
├── AGENTS.md
└── README.md
```

---

## ✨ Features

- 📺 **Watchlist Tracking**: Categorize shows into Airing (`ongoing`), Completed (`completed`), and Stopped/Hiatus (`stopped`).
- ⏱️ **Live Countdown Clocks**: Ticking coroutine timers (Days, Hours, Minutes, Seconds) on every show card and hero banner.
- 📱 **Home Screen Widget**: App Widget support (`CountdownWidget.kt`) to keep track of upcoming episode countdowns directly from your Android home screen.
- 🎨 **Kyant Liquid Glass & Haze Backgrounds**: Ultra-sleek glassmorphism UI with liquid refraction, adaptive brightness/saturation, and real-time backdrop blur.
- 🔍 **Smart Search**: Search bar auto-focuses and opens the keyboard on tap, with a recent-search history that supports per-item delete and "Clear all".
- 📅 **Weekly Release Calendar & Monthly Grid**: View release schedules grouped by day of the week or browse full monthly calendars with episode release indicators.
- 🔔 **Native Push Notifications**: Automatic DST-aware heads-up notifications when a new episode releases.
- 📊 **Statistics Dashboard**: 8-card analytics panel tracking episode counts, total watch hours, day streaks, completion rates, top genres, and favorite studios.
- 🎬 **Native Media Playback**: ExoPlayer / Media3 video player supporting direct streams (`.mp4`, `.m3u8`, `.mkv`) and external web stream browser fallbacks.
- 🖼️ **Automated Poster Fetching**: Auto-fills high-res cover art from Jikan (MyAnimeList), AniList GraphQL, DonghuaStream, and AnimeCountdown.
- 📁 **Collections & Favorites**: Group related donghua series into collection folders and bookmark favorites with one tap.
- 💾 **JSON Backup & Restore**: Export and import full watchlists via the Android document picker, plus FileProvider sharing.
- 🌓 **Multiple Themes**: Dark Mode, AMOLED (True Black), Light Mode, and Material You dynamic color extraction on Android 12+.

---

## 🗄️ Database Schema (Room v3)

All records are stored locally in native SQLite (`mydonghua.db`):

### 1. `shows`
| Column | Type | Description |
|---|---|---|
| `id` | TEXT (PK) | Unique show identifier |
| `title` | TEXT (NOT NULL) | English / Romaji title |
| `title_zh` | TEXT | Chinese title |
| `status` | TEXT | `ongoing`, `completed`, `stopped` |
| `release_day` | TEXT | `Monday` – `Sunday` |
| `release_time` | TEXT | `HH:MM` release time |
| `current_ep` | INTEGER | Watched episode count |
| `total_ep` | INTEGER | Total episodes (0 = unknown) |
| `poster` | TEXT | Poster image URL |
| `watch_url` | TEXT | Streaming video URL |
| `countdown_url` | TEXT | AnimeCountdown page URL |
| `notes` | TEXT | Synopsis & personal notes |
| `is_favorite` | INTEGER | Favorite flag (`0` or `1`) |
| `rating` | INTEGER | User rating (1–10) |
| `last_updated` | INTEGER | Unix ms timestamp |
| `date_added` | INTEGER | Unix ms timestamp |
| `alarm_request_code` | INTEGER | Unique notification request code |
| `genres` | TEXT | Comma-separated genres |
| `studio` | TEXT | Animation studio name |
| `release_year` | INTEGER | Year of release |
| `language` | TEXT | Language |
| `collection_id` | TEXT | Collection folder name |

### 2. `settings`
| Column | Type | Description |
|---|---|---|
| `key` | TEXT (PK) | Setting key |
| `value` | TEXT | Setting value (e.g. `recent_searches` stores a JSON array) |

### 3. `watch_history`
| Column | Type | Description |
|---|---|---|
| `id` | INTEGER (PK AUTO) | History entry ID |
| `show_id` | TEXT (NOT NULL) | Show ID reference |
| `episode_num` | INTEGER (NOT NULL) | Watched episode number |
| `watched_timestamp` | INTEGER (NOT NULL) | Log timestamp |

---

## 🛠️ Building & Compilation

### Requirements
- **JDK**: Java 17+ (embedded in AndroidIDE)
- **Android SDK**: `compileSdk 36`, `targetSdk 35`, `minSdk 29`
- **Gradle**: 8.6+ with Kotlin 2.0.0

### Build Commands

```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease

# Run Unit Tests
./gradlew test
```

> **Note**: This project is developed and built from within AndroidIDE (the Java 17 JDK is embedded there; there is no shell-level `java` command).
