# AGENTS.md

## Project
MyDonghua — Anime tracking app with Haze glass backgrounds and Kyant liquid glass bottom nav.

## Glass Effect Strategy
- **Haze 0.7.3** for general glass backgrounds (stars background, drawer, etc.)
- **Kyant Backdrop `com.github.Kyant0:AndroidLiquidGlass:1.0.0-alpha15` from JitPack** for the liquid glass bottom navigation bar
- Compose BOM bumped to `2024.06.00` for `GraphicsLayer` API availability
- `ExperimentalGraphicsLayerApi` opt-in used where needed

## Key Dependencies
- Compose BOM: `2024.06.00` (provides `rememberGraphicsLayer` / `GraphicsLayer`)
- Haze: `0.7.3`
- Kyant Liquid Glass: `com.github.Kyant0:AndroidLiquidGlass:1.0.0-alpha15`

## Liquid Glass Nav Bar
- `LiquidGlassNavBar.kt` in `com.mydonghua.app.ui` package
- Uses `rememberLayerBackdrop()` + `Modifier.layerBackdrop(backdrop)` to capture screen content
- `Modifier.drawBackdropCustomShape()` applies: `colorControls(brightness/contrast/saturation) + blur(2-16px adaptive) + refraction(24px)`
- Luminance animation: captures 5x5 thumbnail every 1s, calculates average luminance, animates `Animatable` — used for adaptive effect intensity
- 5 items: Home, Airing, Search (center circle), Complete, Stopped
- Applied as `Modifier` on the Row containing all nav items

## Modified Files
- `settings.gradle` — added JitPack repository
- `app/build.gradle` — Bump BOM to `2024.06.00`, add Kyant dependency
- `app/src/main/java/com/mydonghua/app/ui/LiquidGlassNavBar.kt` — NEW: liquid glass nav bar composable + `drawBackdropCustomShape` extension
- `app/src/main/java/com/mydonghua/app/MainActivity.kt` — `rememberLayerBackdrop()` + `layerBackdrop(backdrop)` on NavHost content; replaced old nav with `LiquidGlassNavBar`

## Build
- Java 17 JDK embedded in AndroidIDE (no shell-level `java` command).
- Build must be run from within AndroidIDE, not from shell.
- `compileSdk 36`, `targetSdk 35`, `minSdk 29`
- Kotlin 1.9.24, AGP 8.6.0
