# AGENTS.md

## Project
Qilin — Anime tracking app with Haze glass backgrounds, Kyant liquid glass bottom nav, and predictive back.
- Application ID: `com.nsr.qilin`
- Package / Namespace: `com.nsr.qilin`
- Branch: `staging`
- Remote: `https://github.com/NSR-XINE/my_donguha.git`

## Workflow
- Before pushing: `git pull --rebase origin staging`, then `git push origin staging`.
- Commit message style: concise imperative sentence summarizing the change.
- JDK 21 required. AndroidIDE bundles it; for terminal builds set `JAVA_HOME` to a Java 21 JDK. Gradle 9.8.2, AGP 9.5.0, Kotlin 2.3.21.
- Keep `README.md` in sync with the current state of the app (folder structure, features, DB schema, build steps). Update `CREDITS.md` when dependencies or bundled assets change.

## Glass Effect Strategy
- **Haze 1.7.3** for general glass backgrounds (stars background, drawer, etc.). Use `hazeSource` on the captured content, `LocalHazeStyle` for the shared default style, and `hazeEffect` on glass surfaces; pass rounded clipping via `BlurredEdgeTreatment(shape)`.
- **Kyant Backdrop (vendored source)** for the liquid glass bottom navigation bar
  - AAR at `com.github.Kyant0:AndroidLiquidGlass:1.0.0-alpha15` is **not used** — the vendored source keeps the custom low-blur/AGSL tuning and avoids AAR API drift
  - All 22 source files copied from tag `1.0.0-alpha15` to `app/src/main/java/com/kyant/backdrop/`
  - `@Language("AGSL")` annotations removed (not on classpath)
  - Vendored source adapted for Compose 1.7.x (no `roundRectPath`/`fastCoerceAtMost` dependency)
- Compose BOM `2026.09.00` (Compose UI 1.12.x, `compileSdk 37` + `compileSdkMinor 2`)
- Compose Compiler Gradle Plugin (`org.jetbrains.kotlin.plugin.compose`) handles Compose compiler integration

## Performance Rules (IMPORTANT)
The app previously lagged with many cards on screen, when opening search/settings, and during tab switches. Preserve these mitigations:
- **Glass background orbs are STATIC.** `GlassBackground` in `ui/GlassEffect.kt` used to run an `infiniteTransition` with 11 animators that re-rasterized 5 full-screen gradient layers every frame. It now uses fixed orb positions/scales. Do NOT reintroduce per-frame animation here.
- **Navbar luminance is reactively animated via theme without software readbacks.** In `ui/LiquidGlassNavBar.kt`, luminance adapts smoothly with spring physics on theme/route changes without calling `layer.toImageBitmap()`, completely eliminating GPU-to-CPU pipeline stalls and micro-stutters.
- **Keep navigation motion short and small.** Tab switches use direction-aware fades + tiny horizontal slides (`TabFade*`/`TabSlide*`, 140–280ms); detail/settings use 200ms cross-fades; drawer selections use a fade. Predictive back scales the content container to 0.94 with edge-aware translation, Animatable progress tracking, and smooth corner curvature while keeping header and liquid glass navbar chrome stable. Keep full-screen scale/slide subtle and never animate the glass/blur surfaces — larger motion previously made two screens diverge around the seam during tab switches.
- **Liquid glass blur intensity MUST stay low.** Tab-switch lag was traced to per-frame shader cost: the capsule's blur had a 20dp constant floor (~22-36dp total, ~66-108px @3x) plus 10/14dp dispersion, re-run every frame during the slide. Fixed by cutting capsule blur to 7-14dp (adaptive 1-8dp + 6dp constant), halving dispersion to 5/7dp, and reducing search-button blur to 1-10dp with refraction halved to size/8 & size/4. Do NOT bump these values back up without re-measuring NavLag `maxFrameGapMs`.
- **First-time poster loads are deferred past the capsule slide.** A burst of full-width Coil decodes during a tab switch stalled the slide (141-166ms frame gap). `PosterStagger` in `ui/common/DonghuaPoster.kt` defers first-time loads that start within 800ms of a tab switch by ~250ms (staggered 30ms each), so decodes happen after the slide; scroll reveals and cached repeats load immediately. Do NOT remove the deferral or drop the switch-burst window.
- **Poster loads wait for the scroll to settle.** During a fling, Coil decodes of posters entering the viewport stalled frames (33-49ms `maxFrameGapMs` on Airing). `ScrollGate` in `ui/common/ScrollPerf.kt` is set `true` by `ScrollPerfMonitor` while `LazyListState.isScrollInProgress`, cleared in `finally` on cancellation; `DonghuaPoster` defers enabling a poster until `ScrollGate.scrolling` is `false` (50ms poll). Do NOT remove this gate or the `finally` cleanup — a leaked `true` would block every future poster load.

## Key Dependencies
- Compose BOM: `2026.09.00` (Compose UI 1.12.x, requires `compileSdk 37` + `compileSdkMinor 2`)
- Haze: `1.7.3`
- Coil: `2.7.0` (Coil 3.x requires Kotlin 2.4 metadata, blocked by Hilt `2.60.1`/Kotlin `2.3.21`)
- Kyant Liquid Glass: **none (vendored)**

## Motion & Navigation
- Tab switches use direction-aware fade + slide; detail/sub-pages use native edge-to-edge parallax horizontal slide transitions (`slideInHorizontally` + `slideOutHorizontally`); drawer navigation uses a fade (`drawerNavigating`).
- Predictive back feedback is disabled system-wide (`android:enableOnBackInvokedCallback="false"` in `AndroidManifest.xml`). Avoid custom `PredictiveBackHandler` scale wrappers around `NavHost` as they cause floating truncated cards and expose incomplete background screens on Android 14+.
- Navigation drawer menu pins the header with an elevated scaffold, and the drawer scroll state resets on open.

## Haptic Feedback Strategy
- **NEVER attach `.hapticOnTap()` to root or full-screen layouts.** Root-level interception caused the device to vibrate on every screen touch.
- **`Modifier.colorOSPressBounce` defaults to `hapticFeedback = false`.** General UI tapping, card bouncing, and list flings must remain silent and smooth.
- **Haptic vibrations are reserved exclusively for intentional tactile events**:
  - Settings switches (`ColorOSSwitch` / `Haptics.toggle`)
  - Video player scrubbing & double-tap 10s seek (`Haptics.tick` / `Haptics.confirm`)
  - Screen lock/unlock toggle (`Haptics.toggle`)
  - Bottom navigation bar tab drag transitions (`Haptics.soft`)
  - Explicit cloud actions (AniList login, 2-way sync)
- **Process-wide mute**: `Haptics.enabled` is backed by `SettingsKeys.HAPTICS_ENABLED` in Room and controlled via a user toggle in `SettingsScreen.kt`.

## Build, Splits & Release Signing
- **Architectures**: ABI splits configured for `arm64-v8a` (64-bit) and `armeabi-v7a` (32-bit) with `universalApk true`.
- **Timestamped Filenames**: Handled via `androidComponents.onVariants`, generating `Qilin-v<version>-<abi>-<buildType>-<timestamp>.apk`.
- **Release Signing**: Configured in `app/build.gradle` to check `KEYSTORE_FILE`, `keystore.properties`, and `KEYSTORE_BASE64`, with automatic fallback to `debug.keystore` for test/CI builds without secrets (preventing unsigned uninstallable APKs).
- **Private Keystore Setup**: `scripts/setup_private_keystore.sh` generates local keystore with `chmod 600` and verifies gitignore isolation.
- **Automated GitHub Release Publishing**: `.github/workflows/build.yml` compiles 32-bit, 64-bit, and Universal APKs and creates/updates GitHub Release posts on manual `workflow_dispatch` (never triggers on `push`).

## Liquid Glass Nav Bar
- `LiquidGlassNavBar.kt` in `com.nsr.qilin.ui` package
- Uses `rememberLayerBackdrop()` + `Modifier.layerBackdrop(backdrop)` to capture screen content
- `Modifier.drawBackdropCustomShape()` applies: `colorControls(brightness/contrast/saturation) + blur(1-10px adaptive) + refraction(size/8)`
- Luminance animation: smoothly animates `Animatable` via spring physics on theme changes for adaptive effect intensity; zero frame drops or GPU readbacks
- 5 items: Home, Airing, Search (center circle), Complete, Stopped
- Applied as `Modifier` on the Row containing all nav items

## Search Bar / Keyboard
- Search bar tap path (`MainActivity.kt`): Row `pointerInput` `awaitFirstDown` -> `focusAndOpenKeyboard()` (`focusRequester.requestFocus()` + 3x/150ms retry + `keyboardController?.show()` + `WindowCompat...show(ime())`)
- **NEVER call `requestFocus()` inside `onFocusChanged`** — StackOverflowError in Compose 1.7.x. `onFocusChanged` only sets state.
- While `isSearchOpen`, the Scaffold `topBar` is emptied (the GlassHeader top region previously overlapped and ate search-bar taps).
- Recent searches: stored in the Room `settings` table under key `"recent_searches"` (JSON array) via `SearchViewModel`. `SearchScreen` offers per-row delete (x) and "Clear all".

## Settings Storage
- **Single source of truth for app settings: the Room `settings` table** (`SettingsDao` / `SettingsRepositoryImpl`), accessed through `SettingsRepository` + typed helpers (`getBoolFlow`/`putBool`) and key constants in `domain/repository/SettingsKeys.kt`.
- Keys include theme/accent/UI preferences, haptics_enabled, stream provider, ad blocker, recent searches, search frequency, migration state, storage permission state, and AniList account metadata.
- Legacy stores are retained only for one-time migration or dedicated subsystems: `LegacySettingsMigrator` reads the old DataStore/notification preferences; `AniListService` persists its session in `anilist_tracking_prefs`; downloads use the `DownloadService` preference store; widgets and the ad blocker have their own small preference stores.
- Do NOT reintroduce DataStore/SharedPreferences for ordinary app settings; add new keys to `SettingsKeys`.

## Runtime Permissions
- Storage permission is requested **once on first launch** (`MainActivity.requestStoragePermissionOnce`, gated by the Room key `storage_permission_prompted`). On API 33+ this is `READ_MEDIA_IMAGES`; below it is `READ_EXTERNAL_STORAGE`. It is NOT re-requested on later launches.
- Backup export/import buttons in `SettingsScreen` (Data Backups section) are **disabled until storage permission is granted**; a "Grant Storage Permission" button re-requests it.

## Vendor Directory
- `app/src/main/java/com/kyant/backdrop/` — 22 Kyant Backdrop source files (no external dependency)
  - Core: `Backdrop.kt`, `BackdropEffectScope.kt`, `DrawBackdropModifier.kt`, `InverseLayerScope.kt`, `RuntimeShaderCacheScope.kt`, `Shaders.kt`, `ShapeProvider.kt`
  - `backdrops/`: `CanvasBackdrop.kt`, `CombinedBackdrop.kt`, `EmptyBackdrop.kt`, `LayerBackdrop.kt`, `LayerBackdropModifier.kt`
  - `effects/`: `Blur.kt`, `ColorFilter.kt`, `Lens.kt`, `RenderEffect.kt`
  - `highlight/`: `Highlight.kt`, `HighlightModifier.kt`, `HighlightStyle.kt`
  - `shadow/`: `InnerShadow.kt`, `InnerShadowModifier.kt`, `Shadow.kt`, `ShadowModifier.kt`

## Modified Files
- `settings.gradle` — added JitPack repository (kept for other potential JitPack deps)
- `app/build.gradle` — Compose BOM `2026.09.00`, Kyant AAR dependency removed, Kotlin `2.3.21` + compose compiler plugin, Hilt `2.60.1`, Room `2.8.5`, Media3 `1.11.1`, Haze `1.7.3`, ABI splits (`arm64-v8a`, `armeabi-v7a`), release signing & timestamped naming
- `.github/workflows/build.yml` — Automated CI build and GitHub Release publisher with APK asset uploads
- `scripts/setup_private_keystore.sh` — Interactive zero-knowledge private release keystore generator
- `app/src/main/java/com/nsr/qilin/ui/LiquidGlassNavBar.kt` — liquid glass nav bar composable + `drawBackdropCustomShape` extension; off-main 2s luminance sampling
- `app/src/main/java/com/nsr/qilin/ui/GlassEffect.kt` — static glass background (no infinite animation); `GlassBackground`/`GlassSection` glass components
- `app/src/main/java/com/nsr/qilin/MainActivity.kt` — `rememberLayerBackdrop()` + `layerBackdrop(backdrop)`; `LiquidGlassNavBar`; parallax horizontal slide sub-page transitions; elevated drawer header
- `app/src/main/java/com/nsr/qilin/ui/common/AppLoadingScreen.kt` — minimal ColorOS 17 loading screen with monotonic progress indicator
- `app/src/main/java/com/nsr/qilin/ui/screens/SettingsScreen.kt` & `SettingsViewModel.kt` — Haptics & Vibration toggle switch, custom stream URLs
- `app/src/main/java/com/nsr/qilin/ui/screens/AniListScreen.kt` — Redesigned ColorOS 17 liquid glass bento layout
- `app/src/main/java/com/kyant/backdrop/` — 22 vendored Kyant Backdrop source files

## Build
- JDK 21 required. AndroidIDE bundles it; for terminal builds, set `JAVA_HOME` to a Java 21 JDK before `./gradlew`.
- `compileSdk 37` (`compileSdkMinor 2`), `targetSdk 37`, `minSdk 30`
- Kotlin 2.3.21, AGP 9.5.0, Gradle 9.8.2, KSP 2.3.12
- Compose Compiler Gradle Plugin (`org.jetbrains.kotlin.plugin.compose`) replaces `composeOptions { kotlinCompilerExtensionVersion }`
- `composeOptions` block removed — compose plugin handles compilation
- Robolectric `4.17` native runtime only supports Linux `x86_64`; the four `RoomMigrationTest` cases cannot run on Termux/aarch64. Run `./gradlew test` on CI/x86_64. Domain unit tests pass locally.

## Current Toolchain Changes
- `gradle/wrapper/gradle-wrapper.properties`: Gradle `8.8 -> 9.8.2`
- `build.gradle` (project): AGP `8.6.0 -> 9.5.0`; removed `org.jetbrains.kotlin.android` (AGP 9 built-in Kotlin); Compose/serialization plugin `2.3.21`; KSP `2.3.12`; Hilt `2.60.1`
- `app/build.gradle`: `compileSdk 36 -> 37` + `compileSdkMinor 2`, Java `17 -> 21`, Hilt `2.51.1 -> 2.60.1`, hilt-navigation-compose `1.2.0 -> 1.4.0`, Room `2.6.1 -> 2.8.5`, Compose BOM `2024.09.00 -> 2026.09.00`, Media3 `1.3.1 -> 1.11.1`, Haze `0.7.3 -> 1.7.3`, Coil `2.6.0 -> 2.7.0`, Retrofit `2.11.0 -> 3.0.0`, OkHttp logging `4.12.0 -> 5.5.1`, coroutines `1.7.3 -> 1.11.0`, serialization `1.6.3 -> 1.12.0`, Gson `2.10.1 -> 2.15.0`
- `gradle.properties`: removed `android.enableJetifier=true` and `android.suppressUnsupportedCompileSdk=36`
- Haze call sites: `haze -> hazeSource`, `hazeChild(state, shape)` -> `hazeEffect(state) { blurredEdgeTreatment = BlurredEdgeTreatment(shape) }`; `LocalHazeStyle provides HazeMaterials.ultraThin()` preserves the former source-level default style
- `app/proguard-rules.pro`: keep Commons Compress `ZipExtraField` implementations and no-arg constructors; `ExtraFieldUtils` instantiates them reflectively. Removing these rules reintroduces a release-only `class ... is not a concrete class` crash when opening ZIPs.
