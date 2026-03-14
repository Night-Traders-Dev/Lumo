# Lumo Launcher for Android

This directory contains the native Android launcher port of Lumo. It keeps the Lomiri-inspired interaction model from the GTK prototype, but rebuilds it with Android launcher APIs and Jetpack Compose.

## Current Scope

- Home-screen activity with `HOME` and `DEFAULT` intent filters
- Request flow for Android's Home role
- App discovery via launcher activities, not Linux `.desktop` files
- Ubuntu Touch-style home surface with time, date, and notification cards
- Searchable app library with 4-column grid and long-press favorites
- Ubuntu Touch-style dock with BFB button, squircle app icons, and apps grid (both in-app rail and overlay sidebar)
- Full-width Ubuntu Touch indicator panel with status pills, swipe-to-dismiss notifications, and quick actions
- Notification deep linking (opens specific SMS thread, not just the app)
- Lock screen with InfoGraphic ring (day dots, metric messages, double-tap cycling)
- Keyboard IME styled after Ubuntu Touch Maliit (charcoal flat keys, character popover, swipe typing)
- Live header with time, battery, network, and notification count

## Requirements

- Android Studio or a full Android SDK installation
- A full JDK 17+ installation
- Android SDK path configured through `local.properties` or `ANDROID_HOME`

Example `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Running

```bash
cd android-launcher
./gradlew :app:assembleDebug
```

Then install the APK on a device or emulator and set Lumo Launcher as the default Home app.

## Project Layout

- `app/src/main/java/dev/nighttraders/lumo/launcher/MainActivity.kt`
  Home activity and Home-role request flow
- `app/src/main/java/dev/nighttraders/lumo/launcher/data/`
  Launcher discovery, favorites persistence, and app launching
- `app/src/main/java/dev/nighttraders/lumo/launcher/ui/`
  Compose UI: launcher app, lock screen, system status, view model
- `app/src/main/java/dev/nighttraders/lumo/launcher/ui/theme/`
  Color, typography, and Material theme setup
- `app/src/main/java/dev/nighttraders/lumo/launcher/input/`
  Keyboard IME service and word engine (swipe typing, suggestions, spell check)
- `app/src/main/java/dev/nighttraders/lumo/launcher/notifications/`
  Notification listener service, notification center, and deep linking
- `app/src/main/java/dev/nighttraders/lumo/launcher/overlay/`
  Gesture sidebar overlay service (system-wide dock)

## Intentional Omissions

These Linux-only prototype features were not copied into the launcher MVP:

- Embedded terminal
- `xdotool` keyboard injection
- Termux shell bridge
- Desktop file manager behavior
- Shell-command-based launchers
