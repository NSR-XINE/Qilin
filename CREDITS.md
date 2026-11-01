# Credits & Acknowledgments

Qilin (formerly My Donghua) is built on the work of many open-source projects and community services.
Thank you to everyone who maintains them.

## Project

- **Qilin** (formerly My Donghua) — created by [NSR-XINE](https://github.com/NSR-XINE) and
  [Koustubh Gaikwad](https://github.com/Koustubh12345).
- **Maintainers** — NSR-XINE and Koustubh Gaikwad.

## Design & Inspiration

- **ColorOS 17 / OxygenOS design language** by OPPO and OnePlus — inspiration for the
  app's visual style, motion curves, and liquid glass surfaces.
- **Kyant** — the vendored Liquid Glass backdrop and AGSL shader system under
  `app/src/main/java/com/kyant/backdrop` (Apache-2.0).
- **Chris Banes** — [Haze](https://github.com/chrisbanes/haze), real-time backdrop blur
  and glassmorphism.

## Libraries

| Component | Version | License |
|---|---|---|
| [Gradle](https://gradle.org) | 9.8.2 | Apache-2.0 |
| [Android Gradle Plugin](https://developer.android.com/build) | 9.5.0 | Apache-2.0 |
| [Kotlin](https://kotlinlang.org) | 2.3.21 | Apache-2.0 |
| [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) | 1.11.0 | Apache-2.0 |
| [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) | 1.12.0 | Apache-2.0 |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) (BOM) | 2026.09.00 | Apache-2.0 |
| [Material 3](https://m3.material.io) / Material Icons Extended | Compose BOM | Apache-2.0 |
| [AndroidX Activity](https://developer.android.com/jetpack/androidx/releases/activity) | 1.13.0 | Apache-2.0 |
| [AndroidX AppCompat](https://developer.android.com/jetpack/androidx/releases/appcompat) | 1.8.0 | Apache-2.0 |
| [Material Components](https://github.com/material-components/material-components-android) | 1.14.0 | Apache-2.0 |
| [AndroidX Navigation Compose](https://developer.android.com/jetpack/androidx/releases/navigation) | 2.10.2 | Apache-2.0 |
| [AndroidX Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle) | 2.11.0 | Apache-2.0 |
| [AndroidX DataStore](https://developer.android.com/jetpack/androidx/releases/datastore) | 1.3.0 | Apache-2.0 |
| [Room](https://developer.android.com/jetpack/androidx/releases/room) | 2.8.5 | Apache-2.0 |
| [Hilt](https://dagger.dev/hilt/) / [Dagger](https://dagger.dev) | 2.60.1 | Apache-2.0 |
| [Hilt Navigation Compose](https://developer.android.com/jetpack/androidx/releases/hilt) | 1.4.0 | Apache-2.0 |
| [Media3 / ExoPlayer](https://developer.android.com/media/media3) | 1.11.1 | Apache-2.0 |
| [Coil](https://github.com/coil-kt/coil) | 2.7.0 | Apache-2.0 |
| [Retrofit](https://square.github.io/retrofit/) | 3.0.0 | Apache-2.0 |
| [OkHttp](https://square.github.io/okhttp/) | 5.5.1 | Apache-2.0 |
| [Gson](https://github.com/google/gson) | 2.15.0 | Apache-2.0 |
| [Haze](https://github.com/chrisbanes/haze) | 1.7.3 | Apache-2.0 |
| Kyant Backdrop (vendored) | — | Apache-2.0 |
| [youtubedl-android](https://github.com/JunkFood02/youtubedl-android) | 0.18.1 | GPL-3.0 |
| [yt-dlp](https://github.com/yt-dlp/yt-dlp) | bundled | Unlicense |
| [aria2](https://aria2.github.io) | bundled | GPL-2.0-or-later |
| [FFmpeg](https://ffmpeg.org) | bundled | LGPL-2.1+ / GPL-2.0+ (build dependent) |

### Test-only

| Component | Version | License |
|---|---|---|
| [JUnit 4](https://junit.org/junit4/) | 4.13.2 | EPL-1.0 |
| [Robolectric](https://github.com/robolectric/robolectric) | 4.17 | MIT |
| [AndroidX Test](https://developer.android.com/training/testing) | 1.7.0 / 1.3.0 | Apache-2.0 |
| [Kotlinx Coroutines Test](https://github.com/Kotlin/kotlinx.coroutines) | 1.11.0 | Apache-2.0 |
| [Room Testing](https://developer.android.com/jetpack/androidx/releases/room) | 2.8.5 | Apache-2.0 |

## Data, APIs & Services

- [AniList](https://anilist.co) — GraphQL API, metadata, scores, and cover art.
  Data and images belong to AniList and its contributors.
- [MyAnimeList](https://myanimelist.net) and [Jikan](https://jikan.moe) — search and
  metadata lookups. Anime data belongs to MyAnimeList; Jikan is a community API.
- Streaming providers and video hosts — links open third-party services. All video,
  artwork, and metadata remain the property of their respective owners.

Qilin is an unofficial client. It is not affiliated with AniList, MyAnimeList,
OPPO, OnePlus, or any streaming provider.

## Fonts & Artwork

- **OnePlus Sans** (Regular, Medium, SemiBold, Bold, ExtraBold) — © OnePlus,
  bundled for the app's typography.
- **Material Icons** — © Google, Apache-2.0.
- Dragon emblem, app icon, and other original artwork — Qilin project assets.

## License Notes

Every third-party component is used under its own license; refer to the linked
upstream projects for the full terms. This document provides attribution only and
does not relicense any dependency. Components licensed under GPL-3.0 or GPL-2.0
(including youtubedl-android and bundled aria2/FFmpeg binaries) retain their
copyleft terms.

If something is missing or misattributed, please open an issue at
https://github.com/NSR-XINE/Qilin/issues.
