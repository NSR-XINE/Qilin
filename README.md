# 麒麟 Qilin (formerly My Donghua) - Android App

A sleek, high-aesthetic native Android application for Chinese anime (Donghua) fans to track watched episodes, follow countdown clocks, stream and download episodes with 16-stream acceleration, and receive native push notifications. Designed with the ColorOS 17 / OxygenOS fluid design language and AGSL liquid glass shaders.

**Created by** [NSR-XINE](https://github.com/NSR-XINE) and [Koustubh Gaikwad](https://github.com/Koustubh12345).

---

## 🌟 Tech Stack

- **Core & Runtime**: 100% Kotlin 2.3.21 · JVM 21 · Gradle 9.8.2 · AGP 9.5.0
- **UI & Motion**: Jetpack Compose (BOM `2026.09.00`) · Material 3 · ColorOS 17 Fluid Springs & Press Bounce · Parallax Slide Transitions
- **Glassmorphism**: Haze 1.7.3 (static backdrops) + Kyant Backdrop (custom vendored AGSL liquid glass bottom nav)
- **Download & Video**: YoutubeDL-Android 0.18.1 (`yt-dlp` core) · Aria2c (16-stream split acceleration) · Multi-Fragment HLS · ExoPlayer / Media3 1.11.1
- **Architecture & Storage**: Clean MVVM + Hilt DI · Room Database (v4 Schema) · Retrofit 3.0 · OkHttp 5.5 · Kotlinx Serialization

---

## ✨ Features

- ⚡ **16-Stream Accelerated Downloads**: Parallel multi-connection downloading via Aria2c and concurrent fragment HLS engine for `.mp4`, `.m3u8`, and video hosts.
- 🛡️ **Intelligent Ad Blocker**: Blocks intrusive popups and defuses anti-adblock traps (P21 probe spoofing) with an in-app Settings toggle.
- 📦 **Dual 32-bit & 64-bit Split APKs**: Generates optimized APKs for 64-bit (`arm64-v8a`) and 32-bit (`armeabi-v7a`) devices, plus a universal fallback APK, all labeled with build timestamps.
- 🔐 **Zero-Knowledge Signing**: Private keystore generator (`scripts/setup_private_keystore.sh`) with strict gitignore isolation and automatic debug fallback for test builds.
- 📳 **Refined Haptic Feedback**: Tactile responses confined to intentional interactions (switches, seek scrubbing, player gestures) with a master mute switch (`SettingsKeys.HAPTICS_ENABLED`).
- 🎨 **ColorOS 17 Fluid Motion & Liquid Glass**: Bouncy physics-driven tap interactions, fluid card press effects, elevated pinned drawer header, and Kyant liquid glass shader navigation bar.
- ⏱️ **Watchlist & Live Countdowns**: Airing, Completed, and Stopped lists with ticking coroutine timers, weekly calendar, and home screen widgets.
- ☁️ **AniList Bento Cloud Sync**: Direct AniList OAuth login, 2-way cloud synchronization, profile stats, and library backup.
- 🎬 **Dual Video Player**: Hardware-accelerated ExoPlayer / Media3 alongside an in-app web player with automatic video stream sniffing.

---

## 📁 Repository Structure

```
my_donghua/
├── app/
│   ├── src/main/java/com/kyant/backdrop/    # Vendored Kyant liquid glass shader engine
│   ├── src/main/java/com/nsr/qilin/         # Activities, DI, Room DB, download engine, viewmodels, UI
│   ├── src/main/res/                        # Vector drawables, ColorOS motion tokens, fonts, widgets
│   └── build.gradle                         # AGP 9.5, ABI splits, ProGuard rules, timestamped naming
├── scripts/
│   └── setup_private_keystore.sh            # Zero-knowledge private release keystore generator
├── .github/workflows/build.yml              # On-demand CI build & GitHub Release publisher
└── README.md / AGENTS.md / CREDITS.md
```

---

## 🗄️ Database Schema (Room v4)

Stored locally in native SQLite (`qilin.db`):
- **`shows`**: Show metadata, tracking status (`ongoing`, `completed`, `stopped`), episode progress, broadcast schedules, poster URLs, and AniList IDs.
- **`settings`**: Single source of truth for application preferences (theme mode, haptics, ad blocker, custom providers, search history).
- **`watch_history`**: Granular episode watch logs and timestamps.

---

## 🛠️ Building & Compilation

### Requirements
- **JDK**: Java 21+
- **Android SDK**: `compileSdk 37`, `targetSdk 37`, `minSdk 30`
- **Gradle**: 9.8.2 with AGP 9.5.0 and Kotlin 2.3.21

### Local Build Commands
```bash
# Build Debug APKs (arm64, armv7a, and universal)
./gradlew assembleDebug

# Build Signed Release APKs (arm64, armv7a, and universal)
./gradlew assembleRelease

# Run Domain Unit Tests
./gradlew testDebugUnitTest
```

### Output APK Architecture & Naming
Builds output separate optimized APKs into `app/build/outputs/apk/release/`:

| Target | Architecture | Filename Format |
|---|---|---|
| **64-bit ARM** | `arm64-v8a` | `Qilin-v<version>-arm64-64bit-<variant>-<timestamp>.apk` |
| **32-bit ARM** | `armeabi-v7a` | `Qilin-v<version>-armv7a-32bit-<variant>-<timestamp>.apk` |
| **Universal** | All supported | `Qilin-v<version>-universal-<variant>-<timestamp>.apk` |

---

### 🔑 Private Release Signing

1. **Generate your private keystore locally**:
   ```bash
   bash scripts/setup_private_keystore.sh
   ```
   Prompts silently for your password and generates `release.keystore` and `keystore.properties` (`chmod 600`), both protected by `.gitignore`.

2. **Automated Cloud Builds (GitHub Actions)**:
   Add repository secrets in GitHub (**Settings → Secrets and variables → Actions**):
   - `KEYSTORE_BASE64` (`base64 -w 0 release.keystore`)
   - `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
   
   *Fallback*: If no secrets are configured, Gradle safely signs using `debug.keystore`, ensuring output APKs are always installable.

3. **Automated Releases**:
   [`.github/workflows/build.yml`](.github/workflows/build.yml) builds, signs, and attaches all split APKs to a GitHub Release post strictly on manual trigger (**Actions → Build Android APK → Run workflow**), never on `push`.

---

## 📜 Credits

See [CREDITS.md](CREDITS.md) for third-party libraries, licenses, typography, and community data sources.
