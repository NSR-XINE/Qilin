# 麒麟 Qilin (formerly My Donghua) - Android App

A sleek, high-aesthetic, themeable native Android application designed for Chinese anime (Donghua) fans to track watched episodes, view release calendars, follow live countdown clocks, download episodes with 16-stream accelerated downloading, install home screen widgets, and receive native push notifications.

**Created by** [NSR-XINE](https://github.com/NSR-XINE) and [Koustubh Gaikwad](https://github.com/Koustubh12345).

---

## 🌟 Tech Stack & Key Technologies

- **Language & Runtime**: 100% Native Kotlin 2.3.21 (JVM 21)
- **UI Framework**: Jetpack Compose (BOM `2026.09.00`), Material 3, Navigation-Compose
- **Visual Design & Motion**:
  - **ColorOS 17 Fluid Motion** — Natural spring physics, fluid press bounce responses (`colorOSPressBounce`), and smooth easing curves across all interactive cards and controls
  - **Predictive Back** — Back gestures shrink and round the current screen in real time before the navigation commits
  - **Haze 1.7.3** — Real-time backdrop blur & glassmorphism
  - **Kyant Liquid Glass** — Custom vendored AGSL shader-based liquid glass bottom navigation bar
  - **Performance-tuned**: Static glass background (zero per-frame gradient overhead), off-main-thread GPU luminance sampling, short direction-aware tab transitions, and cross-fade detail transitions
- **Accelerated Download Engine**:
  - **YoutubeDL-Android 0.18.1** (embedded Python runtime + `yt-dlp` core)
  - **Aria2c 16-Connection Acceleration** — Multi-stream split downloading for ultra-fast transfers (`-j 16 -x 16 -s 16 -k 1M -c`)
  - **Multi-Fragment HLS Downloader** — Concurrent 16-fragment downloading for `.m3u8` streams with on-the-fly AES-128 segment decryption
  - **Embedded FFmpeg & FFprobe** — Video processing, stream extraction, and format muxing
  - **Foreground Service Architecture** — `DownloadService` with ongoing notification progress bar, cancel intent receiver (`DownloadReceiver`), and `PowerManager.WakeLock`
- **Ad Blocker & Stream Defusal Engine**:
  - Built-in multi-layered Ad Blocker with dynamic anti-adblock defusal (P21 probe spoofing, transparent pixel interception, anti-anti-adblock bypass)
  - User-configurable toggle switch in Settings
- **Architecture**: Clean Architecture + MVVM + Dagger Hilt Dependency Injection
- **Database & Storage**: Room Database (v4 Schema) with `StateFlow` reactive queries — unified Room-backed app settings store, with dedicated local persistence for downloads and AniList session state
- **Network & Parsing**: Retrofit 3.0 + OkHttp 5.5 + Kotlinx Serialization, AniList GraphQL & MAL scrapers
- **Media Playback**: ExoPlayer / Media3 1.11.1 (Supports direct `.mp4`, `.m3u8`, `.mkv` playback, hardware acceleration, and in-app web player fallback with stream sniffer)
- **Background & Scheduling**: Android `AlarmManager` (DST-aware exact alarms), `PowerManager.WakeLock` boot receivers, App Widgets

---

## 📁 Folder Structure

```
my_donghua/
├── app/
│   ├── build.gradle                          # Dependencies, NDK (arm64-v8a, armeabi-v7a), ABI splits, timestamped naming
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml               # Foreground service dataSync, receivers, activities, widget providers
│       ├── res/
│       │   ├── drawable/                     # Vector assets, ic_download, liquid glass graphics
│       │   └── raw/
│       │       └── ytdlp                     # Bundled yt-dlp core executable archive
│       └── java/
│           ├── com/kyant/backdrop/           # Vendored Kyant Liquid Glass shader system
│           └── com/nsr/qilin/
│               ├── QilinApp.kt               # Application class (@HiltAndroidApp), engine bootstrap
│               ├── MainActivity.kt           # Entry activity, NavHost, Haze glass layer, LiquidGlass nav
│               ├── WatchActivity.kt          # Media3 ExoPlayer & In-App Web Player with stream sniffer
│               ├── AlarmScheduler.kt         # DST-aware exact alarm scheduler
│               ├── BootReceiver.kt           # Re-schedules alarms on boot or app update (WakeLock)
│               ├── NotificationReceiver.kt   # High-priority Heads-up Notification creator
│               ├── CountdownWidget.kt        # App Widget provider for Home Screen release countdowns
│               ├── di/                       # Dagger Hilt Modules
│               │   ├── DatabaseModule.kt     # Room AppDatabase & DAO injection
│               │   └── RepositoryModule.kt   # Repository interface bindings
│               ├── data/
│               │   ├── db/                   # Room v4 (shows, settings, watch_history) + DAOs and migrations
│               │   ├── pref/
│               │   │   └── LegacySettingsMigrator.kt # Settings migration
│               │   ├── repository/           # Show / Settings / WatchHistory repository impls
│               │   └── service/
│               │       ├── AdBlocker.kt      # Multi-tier ad blocking & anti-adblock defusal engine
│               │       ├── DownloadEngine.kt # Unified reactive download facade (StateFlow)
│               │       ├── DownloadModel.kt  # Download metadata & progress model
│               │       ├── DownloadReceiver.kt# Broadcast receiver for notification cancel actions
│               │       ├── DownloadService.kt# Foreground service with Aria2c & YoutubeDL
│               │       └── PosterFetchService.kt # Jikan, AniList GraphQL & HTML scrapers
│               ├── domain/
│               │   ├── model/                # Show, CountdownCalculator (DST-safe), StatsCalculator
│               │   └── repository/           # Domain repository interfaces
│               └── ui/
│                   ├── theme/                # Color palettes, Material 3, ColorOS 17 motion specs, Haptics
│                   ├── common/               # ShowCard, DonghuaPoster, loading screen, shimmer, scroll perf
│                   ├── home/                 # HomeScreen, hero banner, continue watching, recently updated
│                   └── screens/              # Airing, Completed, Downloads, Favorites, Detail, Search, Settings, AniList, Calendar, History, Stats, Collections
├── scripts/
│   └── setup_private_keystore.sh             # Interactive zero-knowledge private release keystore generator
├── AGENTS.md
├── CREDITS.md
└── README.md
```

---

## ✨ Features

- ⚡ **16-Stream Accelerated Downloads**: Download episodes with Aria2c multi-connection parallel acceleration. Full support for direct streams (`.mp4`, `.mkv`), HLS multi-fragment streams (`.m3u8`), and popular video hosting extractors with pause, resume, and real-time progress notifications.
- 🛡️ **Intelligent Ad Blocker**: Blocks intrusive popups, banners, redirects, and defuses anti-adblock probes (P21/Anti-Adblocker trap evasion) with an on/off switch in Settings.
- 📦 **Dual 32-bit & 64-bit Split APKs**: Generates optimized APKs for both 64-bit (`arm64-v8a`) and 32-bit (`armeabi-v7a`) Android devices, plus a universal fallback APK, all labeled with dynamic build timestamps.
- 🔐 **Zero-Knowledge Release Signing**: Private keystore setup script (`scripts/setup_private_keystore.sh`) with strict gitignore isolation and GitHub Actions CI secret integration, safely falling back to debug signing when building without secrets.
- 📳 **Global Haptic Feedback Toggle**: Toggleable tactile feedback in Settings (`SettingsKeys.HAPTICS_ENABLED`) strictly confined to purposeful interactions (switches, seek scrubbing, player gestures, drawer selections).
- 📺 **Watchlist Tracking**: Categorize shows into Airing (`ongoing`), Completed (`completed`), and Stopped/Hiatus (`stopped`).
- ⏱️ **Live Countdown Clocks**: Ticking coroutine timers (Days, Hours, Minutes, Seconds) on every show card and hero banner.
- 📱 **Home Screen Widget**: App Widget support (`CountdownWidget.kt`) to keep track of upcoming episode countdowns directly from your Android home screen.
- 🎨 **ColorOS 17 Fluid Motion & Liquid Glass**: Bouncy physics-driven tap interactions, fluid card press effects, elevated pinned drawer header, and Kyant liquid glass shader navigation bar.
- 🔍 **Smart Search**: Auto-focus search bar, recent search history with individual delete or clear-all, and debounced online search across AniList and MyAnimeList.
- ☁️ **Redesigned AniList Bento Screen**: Account login, auto-sync toggle, 2-way sync, cloud import/push, profile stats, and disconnect — accessible directly from the navigation drawer.
- 📅 **Weekly Release Calendar & Monthly Grid**: View release schedules grouped by day of the week or browse full monthly calendars with episode release indicators.
- 🔔 **Native Push Notifications**: Automatic DST-aware heads-up notifications when a new episode releases.
- 📊 **Statistics Dashboard**: Analytics panel tracking episode counts, total watch hours, day streaks, completion rates, top genres, and favorite studios.
- 🎬 **Dual Video Player**: Switch between high-performance hardware-accelerated ExoPlayer / Media3 and in-app web player with automatic video stream sniffing.
- 💾 **Offline Storage Management**: Manage saved downloads with device storage gauges, direct playback from local disk, and one-tap file deletion.
- 🌓 **Multiple Themes**: Dark Mode, AMOLED (True Black), Light Mode, and Material You dynamic color extraction on Android 12+.

---

## 🗄️ Database Schema (Room v4)

All records are stored locally in native SQLite (`qilin.db`):

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
| `anilist_id` | INTEGER | Optional AniList media ID for matching and synchronization |

### 2. `settings`
| Column | Type | Description |
|---|---|---|
| `key` | TEXT (PK) | Setting key |
| `value` | TEXT | Setting value (theme mode, accent color, ad_blocker_enabled, etc.) |

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
- **JDK**: Java 21+ (embedded in AndroidIDE)
- **Android SDK**: `compileSdk 37`, `targetSdk 37`, `minSdk 30`
- **Gradle**: 9.8.2 with AGP 9.5.0 and Kotlin 2.3.21

### Local Build Commands

Run these Gradle tasks from AndroidIDE or a terminal where `JAVA_HOME` points to Java 21:

```bash
# Build Debug APKs (arm64, armv7a, and universal)
./gradlew assembleDebug

# Build Signed Release APKs (arm64, armv7a, and universal)
./gradlew assembleRelease

# Run Domain Unit Tests
./gradlew testDebugUnitTest
```

### Output APK Architecture & Naming
Builds output separate optimized APKs into `app/build/outputs/apk/release/` with dynamic timestamped filenames:

| Target | Architecture | Filename Format |
|---|---|---|
| **64-bit ARM** | `arm64-v8a` | `Qilin-v<version>-arm64-64bit-<variant>-<timestamp>.apk` |
| **32-bit ARM** | `armeabi-v7a` | `Qilin-v<version>-armv7a-32bit-<variant>-<timestamp>.apk` |
| **Universal** | All supported | `Qilin-v<version>-universal-<variant>-<timestamp>.apk` |

---

### 🔑 Private Release Signing

To sign release builds with your own private key without committing credentials to git:

1. **Generate your private keystore** on your device:
   ```bash
   bash scripts/setup_private_keystore.sh
   ```
   * Prompts silently for your password (hidden input, nothing printed to screen or shell history).
   * Generates `release.keystore` and `keystore.properties` locally with `chmod 600`.
   * Both files are protected by `.gitignore` and can never be committed.

2. **Automated Cloud Builds (GitHub Actions)**:
   * To build and sign on GitHub Actions, encode your local keystore:
     ```bash
     base64 -w 0 release.keystore
     ```
   * Add the repository secrets in GitHub (**Settings → Secrets and variables → Actions**):
     * `KEYSTORE_BASE64`
     * `KEYSTORE_PASSWORD`
     * `KEY_ALIAS`
     * `KEY_PASSWORD`
   * *Fallback*: If no secrets or local properties are configured, Gradle safely falls back to `debug.keystore`, ensuring output APKs are always properly signed and installable.

---

### 🚀 Automated GitHub Release Publishing

The GitHub Actions workflow [`.github/workflows/build.yml`](.github/workflows/build.yml) builds and publishes release posts with APKs attached strictly on demand (never automatically on push):

* **Manual Trigger**: Under the **Actions** tab → **Build Android APK** → **Run workflow**, choose your branch and click **Run workflow** (with *Publish as GitHub Release post* checked). The workflow will build, sign, attach the split APKs, and generate the changelog.

---

> `RoomMigrationTest` requires the Robolectric native runtime, which supports Linux `x86_64` only. On Termux/`aarch64`, run the full test suite on CI or an x86_64 machine; the non-Robolectric domain tests pass locally.

---

## Credits

See [CREDITS.md](CREDITS.md) for third-party libraries, data sources, fonts, and acknowledgments.
