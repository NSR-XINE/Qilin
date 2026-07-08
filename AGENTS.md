# AGENTS.md

## Project
RadixUI — Compose music app with iOS-like liquid glass UI.

## Glass Effect Strategy
Kyant 1.0.6 source files are copied directly into the project under `app/src/main/java/com/kyant/backdrop/` to avoid the Java 21 build issue (Kyant AAR was compiled with Java 21, but the environment only has JDK 17).

13 files were copied (simplified — only `drawPlainBackdrop`, no highlight/shadow/lens effects):
- `Backdrop.kt`, `BackdropEffectScope.kt`, `DrawBackdropModifier.kt` (drawPlainBackdrop only)
- `InverseLayerScope.kt`, `LayerRecorder.kt`, `RuntimeShaderCache.kt`
- `Shaders.kt` (GammaAdjustmentShader only), `ShapeProvider.kt`
- `backdrops/LayerBackdrop.kt`, `backdrops/LayerBackdropModifier.kt`
- `effects/Blur.kt`, `effects/ColorFilter.kt`, `effects/RenderEffect.kt`

Dependency removed from `app/build.gradle` (no more `io.github.kyant0:backdrop:1.0.6`).

Effects chain: `vibrancy() + blur(24.dp.toPx())`.

## Modified Files
- `app/build.gradle` — removed Kyant dependency
- `app/src/main/java/com/mydonghua/app/ui/GlassEffect.kt` — `LocalBackdrop` + `drawPlainBackdrop` with vibrancy+blur
- `app/src/main/java/com/mydonghua/app/MainActivity.kt` — `rememberLayerBackdrop` + `layerBackdrop` for stars capture; glass surfaces via `drawPlainBackdrop`
- `app/src/main/java/com/mydonghua/app/ui/common/ShowCard.kt` — `drawPlainBackdrop`
- `app/src/main/java/com/mydonghua/app/ui/home/HomeScreen.kt` — `drawPlainBackdrop`
- `app/src/main/java/com/mydonghua/app/ui/screens/ShowDetailScreen.kt` — `GlassChip` via `drawPlainBackdrop`

## Build
- Java 17 JDK embedded in AndroidIDE (no shell-level `java` command).
- Project targets Java 21 (`compileOptions VERSION_21`, `kotlinOptions jvmTarget = '21'`).
- Build must be run from within AndroidIDE, not from shell.
