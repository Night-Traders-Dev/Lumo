# Lumo

Lumo is now an Android-first, Lomiri-inspired launcher project.

This repository contains two codebases:

- `android-launcher/`: the active native Android launcher built with Kotlin and Jetpack Compose
- `src/`: the original GTK/Linux prototype that started the project and still acts as reference behavior

If you are here to build or test the current launcher, start with [`android-launcher/README.md`](android-launcher/README.md), [`UPDATES.md`](UPDATES.md), and [`Lumo_Guide.md`](Lumo_Guide.md).

## Current Android Scope

- Android Home-role launcher
- Ubuntu Touch-style home surface and app scope
- Ubuntu Touch-style dock with BFB button, squircle app icons, and separators
- Edge-reveal launcher rail matching the dock design
- Full-width Ubuntu Touch-style indicator panel with status pills, swipe-to-dismiss notifications, and quick actions
- Notification listener with deep linking (opens specific SMS thread, not just the app), heads-up alerts, swipe-to-dismiss, and long-press actions
- Lumo System settings surface
- Lumo lock screen with Ubuntu Touch InfoGraphic ring (day-of-month dots, metric messages, double-tap to cycle)
- Lumo Keyboard IME styled after Ubuntu Touch Maliit (charcoal flat keys, character popover, swipe typing, suggestion strip)
- Optional overlay-based gesture sidebar for reaching Lumo from other apps

## Repository Layout

- `android-launcher/`
  Native Android app, Gradle project, Compose UI, IME with swipe typing, notification listener with deep linking, overlay service
- `src/`
  Legacy Python + GTK prototype
- `docs/`
  Migration notes and supporting documentation

## Build

```bash
cd android-launcher
./gradlew :app:assembleRelease
```

The release APK is written to:

```text
android-launcher/app/build/outputs/apk/release/app-release.apk
```

## Install

```bash
adb install -r /home/kraken/Devel/Lumo/android-launcher/app/build/outputs/apk/release/app-release.apk
```

## Platform Notes

- A normal Android launcher can own Home, app discovery, favorites, widgets, and launcher-local gestures.
- It cannot fully replace SystemUI, the real lock screen, or the stock keyboard pipeline across the entire OS without deeper platform privileges.
- The cross-app sidebar in this repo is an overlay approximation, not full Lomiri shell ownership.
- The Lumo lock screen is a themed surface on top of Android's keyguard dismissal flow, not a full lock-screen replacement.
- The Lumo keyboard is a real IME. It can use Android's public completion and spell-check APIs, but it cannot embed Gboard's private autocomplete engine.

## Documents

- [`UPDATES.md`](UPDATES.md)
  Recent Android-port progress and notable fixes
- [`Lumo_Guide.md`](Lumo_Guide.md)
  Practical setup and usage guide for the launcher, notifications, sidebar, keyboard, and lock screen
- [`docs/android-launcher-migration.md`](docs/android-launcher-migration.md)
  Mapping from the GTK prototype to the Android architecture

## Legacy GTK Prototype

The original Linux shell is still available under `src/`. It includes the old panel, favorites rail, app drawer, file manager, and keyboard prototype. That code is still useful for product reference, but the Android launcher is the current implementation path.

## License

MIT License. See [`LICENSE`](LICENSE).
