# OmniGet for Android

This directory contains a native Android client for OmniGet's media download
workflow. It uses the same local-first model as the desktop app: links are
resolved and downloaded on the device, with no OmniGet resolver backend.

## Features

- Paste, share, or open an `http`/`https` media link.
- Preview title, uploader, thumbnail, duration, and available video heights.
- Download video or extract audio with bundled yt-dlp and FFmpeg.
- Select quality, import a Netscape-format cookie file, and optionally allow
  playlist downloads and subtitles.
- Queue downloads in a foreground service with progress notifications and
  pause, resume, retry, cancel, open, and share actions.
- Persist queue state across app restarts.
- Save completed files to `Downloads/OmniGet` on Android 10 and newer. Android
  8 and 9 use app-private external storage with a `FileProvider`.
- Follow the desktop visual language with the orange OmniGet accent, rounded
  surfaces, mascot artwork, light/dark themes, and responsive navigation.

The desktop plugin marketplace, browser extension bridge, account and
cloud-sync features, and non-download desktop workflows are not part of this
Android client.

## Project Layout

| Path | Purpose |
| --- | --- |
| `app/src/main/java/com/openselena/omniget/ui/` | Compose UI and screen state |
| `app/src/main/java/com/openselena/omniget/download/` | yt-dlp/FFmpeg engine, foreground service, storage delivery |
| `app/src/main/java/com/openselena/omniget/data/` | Settings and persisted download queue |
| `app/src/main/java/com/openselena/omniget/util/` | URL detection and progress parsing |
| `app/src/test/` | JVM unit tests |
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
