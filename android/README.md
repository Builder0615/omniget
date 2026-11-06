# 小黑下载 for Android

小黑下载 is a native Android client built from OmniGet's media download
workflow. Links are resolved and downloaded on the device, with no remote
resolver backend.

## Features

- Paste, share, or open an `http`/`https` media link.
- Preview title, uploader, thumbnail, duration, and available video heights.
- Download video or extract audio with bundled yt-dlp and FFmpeg.
- Select video/audio mode and quality in the bottom sheet opened by Download.
- Import a Netscape-format cookie file, and optionally allow playlist
  downloads and subtitles.
- Queue downloads in a foreground service with progress notifications and
  pause, resume, retry, cancel, open, and share actions.
- Persist queue state across app restarts.
- Save completed files to `Downloads/OmniGet` on Android 10 and newer. Android
  8 and 9 use app-private external storage with a `FileProvider`.
- Keep Home focused on the black cat and link input; show the media player
  after successful parsing and keep download history on Downloads.
- Use warm paper surfaces, recessed inputs, terracotta accents, light/dark
  themes, responsive navigation, and motion for interactive controls.

The desktop plugin marketplace, browser extension bridge, account and
cloud-sync features, and non-download desktop workflows are not part of this
Android client.

## Visual Design and Artwork

The paper palette and raised/recessed surface treatment are inspired by
[letterpress-companion](https://github.com/tianyupaipai-cmd/letterpress-companion).
The Android interface is implemented independently with Jetpack Compose;
no source code or artwork from that project is copied into this client.

The black cat has separate Lottie states for idle, analyzing, completed, and
error. These animations combine generated image illustrations with animated
transforms and vector effects; the character's limbs are not individually
rigged. The app respects the system's disabled-animation setting.

Artwork is stored in `app/src/main/res/drawable-nodpi/xiaohei_icon.png` and
`app/src/main/assets/pet/images/`. Lottie compositions are stored in
`app/src/main/assets/pet/`. See [ARTWORK.md](ARTWORK.md) for provenance,
generation prompts, and asset details.

After changing the source illustrations or animation specification, regenerate
the compositions from the repository root:

```bash
node android/tools/generate-pet-animations.mjs
```

## Project Layout

| Path | Purpose |
| --- | --- |
| `app/src/main/java/com/openselena/omniget/ui/` | Compose UI and screen state |
| `app/src/main/java/com/openselena/omniget/download/` | yt-dlp/FFmpeg engine, foreground service, storage delivery |
| `app/src/main/java/com/openselena/omniget/data/` | Settings and persisted download queue |
| `app/src/main/java/com/openselena/omniget/util/` | URL detection and progress parsing |
| `app/src/test/` | JVM unit tests |
| `app/src/main/assets/pet/` | Black-cat illustrations and Lottie state compositions |
| `tools/generate-pet-animations.mjs` | Reproducible Lottie composition generation and asset checks |
| `tools/build-webp-16k.sh` | Reproducible WebP compatibility build |

## Build

Requirements:

- JDK 17 or newer
- Android SDK Platform 35
- Android SDK Build Tools
- Android NDK 27 only when rebuilding the 16 KB WebP libraries

Create `android/local.properties` if the SDK is not already discovered:

```properties
sdk.dir=/path/to/Android/Sdk
```

Build and test:

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The debug APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 16 KB Page Compatibility

The app is compatible with Android's 16 KB page-size requirement. The
`youtubedl-android:ffmpeg:0.18.1` AAR aligns its top-level libraries, but its
embedded WebP shared libraries are still linked for 4 KB pages. This prevents
FFmpeg from loading on 16 KB devices.

The app includes rebuilt WebP 1.6.0 libraries for `arm64-v8a` and `x86_64`.
After the official FFmpeg package is extracted at startup,
`FFmpegWebpCompat` replaces only the following files:

- `libsharpyuv.so`
- `libwebp.so`
- `libwebpdecoder.so`
- `libwebpdemux.so`
- `libwebpmux.so`

The replacement keeps the original SONAMEs and exported symbol sets, and all
`PT_LOAD` segments use `0x4000` alignment. The application of the patch is
skipped on 4 KB devices.

To rebuild the assets, install Android NDK 27 and run:

```bash
./tools/build-webp-16k.sh
```

The compatibility workaround can be removed after a future upstream AAR ships
16 KB-aligned WebP libraries. Upstream tracking issue:
<https://github.com/yausername/youtubedl-android/issues/365>.

WebP is distributed under its BSD license. The license text is bundled at
`app/src/main/assets/ffmpeg16k/LICENSE.webp`.
