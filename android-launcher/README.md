# Lumo Launcher for Android

This directory contains the native Android launcher port of Lumo. It keeps the Lomiri-inspired interaction model from the GTK prototype, but rebuilds it with Android launcher APIs and Jetpack Compose.

## Current Scope

- Home-screen activity with `HOME` and `DEFAULT` intent filters
- Request flow for Android's Home role
- App discovery via launcher activities, not Linux `.desktop` files
- Ubuntu Touch-style home surface with time, date, and notification cards
- Direction-aware app drawer with bottom swipe-up and side slide-in overlays
- Searchable app library with configurable grid columns and icon size
- Ubuntu Touch-style dock with BFB button, squircle app icons, and apps grid (both in-app rail and overlay sidebar)
- Full-width Ubuntu Touch indicator panel with status pills, swipe-to-dismiss notifications, and quick actions
- Notification deep linking (opens specific SMS thread, not just the app)
- Notification dismissal cooldown to suppress re-posting flicker
- Ubuntu Touch-style multitask view with swipe-to-dismiss app cards
- Lock screen with InfoGraphic ring (day dots, metric messages, double-tap cycling)
- Lock screen security with PIN and password (SHA-256 hashed with salt)
- Lock screen companion service for wake-on-screen-off behavior
- Keyboard IME styled after Ubuntu Touch Maliit (charcoal flat keys, character popover, swipe typing, swipe trail)
- AccessibilityService-based back gesture with automatic suppression during app drawer
- Full settings control: appearance (icon size, grid columns, rail width), gesture toggles (5 gestures), gesture sensitivity (8 threshold/width sliders)
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
- `app/src/main/java/dev/nighttraders/lumo/launcher/SettingsActivity.kt`
  Lumo System settings activity
- `app/src/main/java/dev/nighttraders/lumo/launcher/LockScreenActivity.kt`
  Lock screen with PIN/password security
- `app/src/main/java/dev/nighttraders/lumo/launcher/data/`
  Launcher discovery, favorites persistence, settings (preferences, repository, settings data class)
- `app/src/main/java/dev/nighttraders/lumo/launcher/ui/`
  Compose UI: launcher app, settings screen, lock screen, system status, view model
- `app/src/main/java/dev/nighttraders/lumo/launcher/ui/theme/`
  Color, typography, and Material theme setup
- `app/src/main/java/dev/nighttraders/lumo/launcher/input/`
  Keyboard IME service, word engine (swipe typing, suggestions, spell check), swipe trail view
- `app/src/main/java/dev/nighttraders/lumo/launcher/notifications/`
  Notification listener service, notification center with dismissal cooldown, and deep linking
- `app/src/main/java/dev/nighttraders/lumo/launcher/overlay/`
  Gesture sidebar overlay service (system-wide dock), back gesture accessibility service
- `app/src/main/java/dev/nighttraders/lumo/launcher/lockscreen/`
  Lock state management and lock screen companion service

## Intentional Omissions

These Linux-only prototype features were not copied into the launcher MVP:

- Embedded terminal
- `xdotool` keyboard injection
- Termux shell bridge
- Desktop file manager behavior
- Shell-command-based launchers
