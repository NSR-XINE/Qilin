# AGENTS.md

## Project
MyDonghua — Anime tracking app with Haze glass backgrounds and Kyant liquid glass bottom nav.
- Package: `com.mydonghua.app`
- Branch: `staging`
- Remote: `https://github.com/NSR-XINE/my_donguha.git`

## Workflow
- Before pushing: `git pull --rebase origin staging`, then `git push origin staging`.
- Commit message style: concise imperative sentence summarizing the change.
- Build must run inside AndroidIDE (Java 17 JDK embedded there; no shell-level `java` command). AGP 8.6.0, Kotlin 2.0.0.
- Keep `README.md` in sync with the current state of the app (folder structure, features, DB schema, build steps).

## Glass Effect Strategy
- **Haze 0.7.3** for general glass backgrounds (stars background, drawer, etc.)
- **Kyant Backdrop (vendored source)** for the liquid glass bottom navigation bar
  - AAR at `com.github.Kyant0:AndroidLiquidGlass:1.0.0-alpha15` is **unusable** — compiled with Java 21, env has JDK 17
  - All 22 source files copied from tag `1.0.0-alpha15` to `app/src/main/java/com/kyant/backdrop/`
  - `@Language("AGSL")` annotations removed (not on classpath)
  - Vendored source adapted for Compose 1.7.x (no `roundRectPath`/`fastCoerceAtMost` dependency)
- Compose BOM upgraded to `2024.09.00` for `GraphicsLayer` API availability (requires Kotlin 2.0+)
- `ExperimentalGraphicsLayerApi` opt-in set globally via `-opt-in=` compiler arg

## Performance Rules (IMPORTANT)
The app previously lagged with many cards on screen, when opening search/settings, and during tab switches. Preserve these mitigations:
- **Glass background orbs are STATIC.** `GlassBackground` in `ui/GlassEffect.kt` used to run an `infiniteTransition` with 11 animators that re-rasterized 5 full-screen gradient layers every frame. It now uses fixed orb positions/scales. Do NOT reintroduce per-frame animation here.
- **Navbar luminance readback runs off the main thread every 2s.** In `ui/LiquidGlassNavBar.kt` the whole `layer.toImageBitmap()` + 5x5 thumbnail average runs inside `withContext(Dispatchers.Default)`, skipped while the user is dragging or the capsule is animating (`userInteracting`). Keep it off the UI thread and slow (>= 1s).
- **NavHost transitions are short pure cross-fades** (`MainActivity.kt`): `fadeIn(tween(140))` / `fadeOut(tween(110))` for enter/exit/pop. Avoid scale/slide transitions — they rendered two screens diverging around the seam during tab switches.
- **Liquid glass blur intensity MUST stay low.** Tab-switch lag was traced to per-frame shader cost: the capsule's blur had a 20dp constant floor (~22-36dp total, ~66-108px @3x) plus 10/14dp dispersion, re-run every frame during the slide. Fixed by cutting capsule blur to 7-14dp (adaptive 1-8dp + 6dp constant), halving dispersion to 5/7dp, and reducing search-button blur to 1-10dp with refraction halved to size/8 & size/4. Do NOT bump these values back up without re-measuring NavLag `maxFrameGapMs`.

## Key Dependencies
- Compose BOM: `2024.09.00` (provides `rememberGraphicsLayer` / `GraphicsLayer` via Compose UI 1.7.x)
- Haze: `0.7.3`
- Kyant Liquid Glass: **none (vendored)**

## Liquid Glass Nav Bar
- `LiquidGlassNavBar.kt` in `com.mydonghua.app.ui` package
- Uses `rememberLayerBackdrop()` + `Modifier.layerBackdrop(backdrop)` to capture screen content
- `Modifier.drawBackdropCustomShape()` applies: `colorControls(brightness/contrast/saturation) + blur(1-10px adaptive) + refraction(size/8)`
- Luminance animation: captures 5x5 thumbnail **every 2s on `Dispatchers.Default`**, calculates average luminance, animates `Animatable` — used for adaptive effect intensity; skipped while `userInteracting` (drag / capsule animation)
- 5 items: Home, Airing, Search (center circle), Complete, Stopped
- Applied as `Modifier` on the Row containing all nav items

## Search Bar / Keyboard
- Search bar tap path (`MainActivity.kt`): Row `pointerInput` `awaitFirstDown` -> `focusAndOpenKeyboard()` (`focusRequester.requestFocus()` + 3x/150ms retry + `keyboardController?.show()` + `WindowCompat...show(ime())`)
- **NEVER call `requestFocus()` inside `onFocusChanged`** — StackOverflowError in Compose 1.7.x. `onFocusChanged` only sets state.
- While `isSearchOpen`, the Scaffold `topBar` is emptied (the GlassHeader top region previously overlapped and ate search-bar taps).
- Recent searches: stored in the Room `settings` table under key `"recent_searches"` (JSON array) via `SearchViewModel`. `SearchScreen` offers per-row delete (x) and "Clear all".

## Settings Storage
- **Single source of truth: the Room `settings` table** (`SettingsDao` / `SettingsRepositoryImpl`), accessed through `SettingsRepository` + typed helpers (`getBoolFlow`/`putBool`) and key constants in `domain/repository/SettingsKeys.kt`.
- Keys: `app_theme`, `pref_accent_color`, `show_banner`, `show_continue`, `show_recent`, `auto_open_keyboard`, `default_stream_provider`, `recent_searches` (JSON), `search_frequency` (JSON), `prefs_migrated`.
- Legacy stores were consolidated away:
  - `ThemePreference` (DataStore "settings") was **deleted** — values migrated once into Room by `LegacySettingsMigrator` (`data/pref/LegacySettingsMigrator.kt`), run from `DonghuaApp.onCreate`, gated by `prefs_migrated`.
  - SharedPreferences `notification_prefs` was **removed**; its keys are migrated into Room too.
  - Backup/restore (`SettingsViewModel`) now round-trips all Room settings under the `notifyPrefs` JSON field (kept for backward compatibility with old backups).
- Do NOT reintroduce DataStore/SharedPreferences for app settings; add new keys to `SettingsKeys`.

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
- `app/build.gradle` — bumped Compose BOM to `2024.09.00`, Kyant AAR dependency removed, Kotlin 2.0.0 + compose compiler plugin
- `app/src/main/java/com/mydonghua/app/ui/LiquidGlassNavBar.kt` — NEW: liquid glass nav bar composable + `drawBackdropCustomShape` extension; off-main 2s luminance sampling
- `app/src/main/java/com/mydonghua/app/ui/GlassEffect.kt` — static glass background (no infinite animation); `GlassBackground`/`GlassSection` glass components
- `app/src/main/java/com/mydonghua/app/MainActivity.kt` — `rememberLayerBackdrop()` + `layerBackdrop(backdrop)` on NavHost content; replaced old nav with `LiquidGlassNavBar`; short cross-fade transitions; search bar keyboard focus handling
- `app/src/main/java/com/kyant/backdrop/` — NEW: 22 vendored Kyant Backdrop source files

## Build
- Java 17 JDK embedded in AndroidIDE (no shell-level `java` command).
- Build must be run from within AndroidIDE, not from shell.
- `compileSdk 36`, `targetSdk 35`, `minSdk 29`
- Kotlin 2.0.0, AGP 8.6.0
- Compose Compiler Gradle Plugin (`org.jetbrains.kotlin.plugin.compose`) replaces `composeOptions { kotlinCompilerExtensionVersion }`
- `composeOptions` block removed — compose plugin handles compilation

## Build File Changes (Kotlin 2.0 Upgrade)
- `build.gradle` (project): `kotlin 1.9.24 -> 2.0.0`, added `org.jetbrains.kotlin.plugin.compose 2.0.0`
- `app/build.gradle`:
  - Added `id 'org.jetbrains.kotlin.plugin.compose'` to plugins
  - Removed `composeOptions { kotlinCompilerExtensionVersion '1.5.14' }`
  - Added `-opt-in=androidx.compose.ui.graphics.ExperimentalGraphicsLayerApi` to freeCompilerArgs
  - Compose BOM: `2024.06.00 -> 2024.09.00`
- No changes needed to settings.gradle or gradle-wrapper
