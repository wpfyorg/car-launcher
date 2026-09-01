# Car Launcher

Car Launcher is a landscape-first Android home screen for aftermarket car head units. It is designed for Android 9 (API 28) and newer, with a large-touch-target Compose UI suitable for dashboard use.

## Current features

- Home dashboard with status, navigation-preview, and media surfaces
- App library with search, list/grid views, pinning, and quick launch
- Multiple active media sessions with sticky source selection
- Swipeable media sources, artwork backgrounds, progress, and playback controls
- Expanded player with supported shuffle, repeat, and favorite controls
- Driving-oriented notification list
- Dark/light appearance, text sizing, startup-screen, and launcher settings
- Optional launch-at-boot behavior

## Build

Requirements:

- JDK 17
- Android SDK with API 36 installed

Build the public debug APK:

```bash
./gradlew assembleDebug
```

The APK is generated under `app/build/outputs/apk/debug/`. Build artifacts, signing keys, research APKs, private planning, and paid-edition work are intentionally excluded from this repository.

## Status

The app is under active development. The standard public build does not include privileged system-app embedding or paid navigation code. Hardware-specific work for YT5760D/AC8257 head units is tracked privately until it is ready for public release.

## Package compatibility

The product and Gradle project are named **Car Launcher**. The Android application ID remains `com.openlauncher.app` so existing development installs and launcher preferences continue to work.

## Attribution and licensing

This codebase began as a redesign of [vickoc911/openlauncher](https://github.com/vickoc911/openlauncher). That repository does not currently include a license file, so this public repository does not assert a new license over the upstream code. All third-party contributions remain subject to their respective authors' rights.
