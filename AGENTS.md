# AGENTS.md

## Project
MyDonghua — Anime tracking app with Haze glass backgrounds and Kyant liquid glass bottom nav.

## Glass Effect Strategy
- **Haze 0.7.3** for general glass backgrounds (stars background, drawer, etc.)
- **Kyant Backdrop (vendored source)** for the liquid glass bottom navigation bar
  - AAR at `com.github.Kyant0:AndroidLiquidGlass:1.0.0-alpha15` is **unusable** — compiled with Java 21, env has JDK 17
  - All 22 source files copied from tag `1.0.0-alpha15` to `app/src/main/java/com/kyant/backdrop/`
  - `@Language("AGSL")` annotations removed (not on classpath)
  - Vendored source adapted for Compose 1.7.x (no `roundRectPath`/`fastCoerceAtMost` dependency)
- Compose BOM upgraded to `2024.09.00` for `GraphicsLayer` API availability (requires Kotlin 2.0+)
- `ExperimentalGraphicsLayerApi` opt-in set globally via `-opt-in=` compiler arg

## Key Dependencies
- Compose BOM: `2024.09.00` (provides `rememberGraphicsLayer` / `GraphicsLayer` via Compose UI 1.7.x)
- Haze: `0.7.3`
- Kyant Liquid Glass: **none (vendored)**

## Liquid Glass Nav Bar
- `LiquidGlassNavBar.kt` in `com.mydonghua.app.ui` package
- Uses `rememberLayerBackdrop()` + `Modifier.layerBackdrop(backdrop)` to capture screen content
- `Modifier.drawBackdropCustomShape()` applies: `colorControls(brightness/contrast/saturation) + blur(2-16px adaptive) + refraction(24px)`
- Luminance animation: captures 5x5 thumbnail every 1s, calculates average luminance, animates `Animatable` — used for adaptive effect intensity
- 5 items: Home, Airing, Search (center circle), Complete, Stopped
- Applied as `Modifier` on the Row containing all nav items

## Vendor Directory
- `app/src/main/java/com/kyant/backdrop/` — 22 Kyant Backdrop source files (no external dependency)
  - Core: `Backdrop.kt`, `BackdropEffectScope.kt`, `DrawBackdropModifier.kt`, `InverseLayerScope.kt`, `RuntimeShaderCacheScope.kt`, `Shaders.kt`, `ShapeProvider.kt`
  - `backdrops/`: `CanvasBackdrop.kt`, `CombinedBackdrop.kt`, `EmptyBackdrop.kt`, `LayerBackdrop.kt`, `LayerBackdropModifier.kt`
  - `effects/`: `Blur.kt`, `ColorFilter.kt`, `Lens.kt`, `RenderEffect.kt`
  - `highlight/`: `Highlight.kt`, `HighlightModifier.kt`, `HighlightStyle.kt`
  - `shadow/`: `InnerShadow.kt`, `InnerShadowModifier.kt`, `Shadow.kt`, `ShadowModifier.kt`

## Modified Files
- `settings.gradle` — added JitPack repository (kept for other potential JitPack deps)
- `app/build.gradle` — bumped BOM to `2024.06.00`, Kyant AAR dependency removed
- `app/src/main/java/com/mydonghua/app/ui/LiquidGlassNavBar.kt` — NEW: liquid glass nav bar composable + `drawBackdropCustomShape` extension
- `app/src/main/java/com/mydonghua/app/MainActivity.kt` — `rememberLayerBackdrop()` + `layerBackdrop(backdrop)` on NavHost content; replaced old nav with `LiquidGlassNavBar`
- `app/src/main/java/com/kyant/backdrop/` — NEW: 22 vendored Kyant Backdrop source files

## Build
- Java 17 JDK embedded in AndroidIDE (no shell-level `java` command).
- Build must be run from within AndroidIDE, not from shell.
- `compileSdk 36`, `targetSdk 35`, `minSdk 29`
- Kotlin 2.0.0, AGP 8.6.0
- Compose Compiler Gradle Plugin (`org.jetbrains.kotlin.plugin.compose`) replaces `composeOptions { kotlinCompilerExtensionVersion }`
- `composeOptions` block removed — compose plugin handles compilation

## Build File Changes (Kotlin 2.0 Upgrade)
- `build.gradle` (project): `kotlin 1.9.24 → 2.0.0`, added `org.jetbrains.kotlin.plugin.compose 2.0.0`
- `app/build.gradle`:
  - Added `id 'org.jetbrains.kotlin.plugin.compose'` to plugins
  - Removed `composeOptions { kotlinCompilerExtensionVersion '1.5.14' }`
  - Added `-opt-in=androidx.compose.ui.graphics.ExperimentalGraphicsLayerApi` to freeCompilerArgs
  - Compose BOM: `2024.06.00 → 2024.09.00`
- No changes needed to settings.gradle or gradle-wrapper
