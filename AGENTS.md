# AGENTS.md

## Project
Qilin — Anime tracking app with Haze glass backgrounds, Kyant liquid glass bottom nav, shared-element poster transitions, and predictive back.
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
- **Keep navigation motion short and small.** Tab switches use direction-aware fades + tiny horizontal slides (`TabFade*`/`TabSlide*`, 140–280ms); detail/settings use 200ms cross-fades; drawer selections use a fade. Predictive back scales the Scaffold to 0.94 with a 10% x-translation. Keep full-screen scale/slide subtle and never animate the glass/blur surfaces — larger motion previously made two screens diverge around the seam during tab switches.
- **Liquid glass blur intensity MUST stay low.** Tab-switch lag was traced to per-frame shader cost: the capsule's blur had a 20dp constant floor (~22-36dp total, ~66-108px @3x) plus 10/14dp dispersion, re-run every frame during the slide. Fixed by cutting capsule blur to 7-14dp (adaptive 1-8dp + 6dp constant), halving dispersion to 5/7dp, and reducing search-button blur to 1-10dp with refraction halved to size/8 & size/4. Do NOT bump these values back up without re-measuring NavLag `maxFrameGapMs`.
- **First-time poster loads are deferred past the capsule slide.** A burst of full-width Coil decodes during a tab switch stalled the slide (141-166ms frame gap). `PosterStagger` in `ui/common/DonghuaPoster.kt` defers first-time loads that start within 800ms of a tab switch by ~250ms (staggered 30ms each), so decodes happen after the slide; scroll reveals and cached repeats load immediately. Do NOT remove the deferral or drop the switch-burst window.
- **Poster loads wait for the scroll to settle.** During a fling, Coil decodes of posters entering the viewport stalled frames (33-49ms `maxFrameGapMs` on Airing). `ScrollGate` in `ui/common/ScrollPerf.kt` is set `true` by `ScrollPerfMonitor` while `LazyListState.isScrollInProgress`, cleared in `finally` on cancellation; `DonghuaPoster` defers enabling a poster until `ScrollGate.scrolling` is `false` (50ms poll). Do NOT remove this gate or the `finally` cleanup — a leaked `true` would block every future poster load.

## Key Dependencies
- Compose BOM: `2026.09.00` (Compose UI 1.12.x, requires `compileSdk 37` + `compileSdkMinor 2`)
- Haze: `1.7.3`
- Coil: `2.7.0` (Coil 3.x requires Kotlin 2.4 metadata, blocked by Hilt `2.60.1`/Kotlin `2.3.21`)
- Kyant Liquid Glass: **none (vendored)**
- Compose shared-element APIs remain experimental — opt in with `@OptIn(ExperimentalSharedTransitionApi::class)`; Compose 1.12 renamed `sharedElement(state = ...)` to `sharedContentState = ...`

## Motion & Shared Elements
- `ui/SharedTransition.kt` provides `LocalSharedTransitionScope`, `LocalNavAnimatedVisibilityScope`, `SharedBoundsSpring`, `SharedPosterKey`, and `NavGraphBuilder.scopedComposable`.
- `MainActivity` wraps the NavHost in `SharedTransitionLayout`; detail-bearing destinations use `scopedComposable`.
- Poster keys are unique per source surface: `show-poster-card-*` (library grids), `show-poster-cw-*` (Continue Watching), `show-poster-ru-*` (Recently Updated). `openDetail(showId, posterKey)` stores the source key in `SharedPosterKey.current` before navigating; the detail poster reads it. Never place two shared posters with the same key on one screen.
- `DonghuaPoster` accepts `sharedElementKey` + `sharedOverlayShape`; pass the banner's exact `RoundedCornerShape` so overlay clipping matches during flight.
- Tab switches use direction-aware fade + slide; detail uses a cross-fade + bounds spring; drawer navigation uses a fade (`drawerNavigating`). Predictive back is enabled only off Home and while search, drawer, and loading are closed.

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
- Keys include theme/accent/UI preferences, stream provider, ad blocker, recent searches, search frequency, migration state, storage permission state, and AniList account metadata.
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
- `app/build.gradle` — Compose BOM `2026.09.00`, Kyant AAR dependency removed, Kotlin `2.3.21` + compose compiler plugin, Hilt `2.60.1`, Room `2.8.5`, Media3 `1.11.1`, Haze `1.7.3`
- `app/src/main/java/com/nsr/qilin/ui/LiquidGlassNavBar.kt` — NEW: liquid glass nav bar composable + `drawBackdropCustomShape` extension; off-main 2s luminance sampling
- `app/src/main/java/com/nsr/qilin/ui/GlassEffect.kt` — static glass background (no infinite animation); `GlassBackground`/`GlassSection` glass components
- `app/src/main/java/com/nsr/qilin/MainActivity.kt` — `rememberLayerBackdrop()` + `layerBackdrop(backdrop)`; `LiquidGlassNavBar`; `SharedTransitionLayout`/`scopedComposable` NavHost; direction-aware tab transitions; drawer scrim/panel split; predictive back; synchronous settings seed for first-frame theme; search bar keyboard focus handling
- `app/src/main/java/com/nsr/qilin/ui/SharedTransition.kt` — NEW: shared-element scope locals, bounds spring, `SharedPosterKey`, and `scopedComposable`
- `app/src/main/java/com/nsr/qilin/ui/common/AppLoadingScreen.kt` — minimal trimmed-emblem loading screen with a standard progress indicator
- `app/src/main/java/com/kyant/backdrop/` — NEW: 22 vendored Kyant Backdrop source files

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
- `DonghuaPoster`: Compose 1.12 shared-element argument rename `state -> sharedContentState`
- `app/proguard-rules.pro`: keep Commons Compress `ZipExtraField` implementations and no-arg constructors; `ExtraFieldUtils` instantiates them reflectively. Removing these rules reintroduces a release-only `class ... is not a concrete class` crash when opening ZIPs.
