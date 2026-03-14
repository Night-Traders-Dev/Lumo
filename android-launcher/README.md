# Lumo Launcher for Android

Native Android launcher port of Lumo. Keeps the Lomiri/Ubuntu Touch-inspired interaction model but rebuilds it with Android launcher APIs and Jetpack Compose.

## Current Scope

- Home-screen activity with `HOME` and `DEFAULT` intent filters
- Request flow for Android's Home role
- App discovery via launcher activities, not Linux `.desktop` files
- Ubuntu Touch-style home surface with time, date, and notification cards
- Direction-aware app drawer with bottom swipe-up and side slide-in overlays
- Searchable app library with configurable grid columns and icon size
- Ubuntu Touch-style dash rail with BFB button, squircle app icons, drag-and-drop reordering
- System-wide gesture sidebar overlay (accessible from any app, blocked when locked)
- Full-width Ubuntu Touch indicator panel with status pills, swipe-to-dismiss notifications, and quick actions (Wi-Fi, Bluetooth, airplane, flashlight, location)
- Notification deep linking (opens specific SMS thread, not just the app)
- Notification dismissal cooldown to suppress re-posting flicker
- Heads-up notification display with auto-dismiss
- Ubuntu Touch-style multitask view with swipe-to-dismiss app cards
- Lock screen with InfoGraphic ring (day dots, metric cycling) and Ubuntu Touch bokeh circles
- Lock screen security with PIN and password (SHA-256 hashed with salt)
- Delayed lock (3-second grace period after screen off)
- Status bar blocker (overlay + FLAG_FULLSCREEN + BEHAVIOR_DEFAULT) when lock screen is active
- Lock screen companion service for wake-on-screen-off behavior (Android 13 and earlier)
- Weather display via Open-Meteo API (no API key required)
- Device Admin receiver for enhanced device management
- Keyboard IME styled after Ubuntu Touch Maliit (charcoal flat keys, character popover, swipe typing, swipe trail)
- AccessibilityService-based back gesture with automatic suppression during app drawer
- Full settings control: appearance (icon size, grid columns, rail width), gesture toggles (5 gestures), gesture sensitivity (8 threshold/width sliders)
- Live top bar with time, battery, network, and notification count
- Boot receiver for restoring services on device restart

## Build

```bash
cd android-launcher

# Quick build (assembleDebug + adb install)
./build.sh --build

# Clean build (clean + build + assembleDebug + adb install)
./build.sh --clean-build
```

Manual build:

```bash
./gradlew :app:assembleDebug
```

Then install the APK on a device or emulator and set Lumo Launcher as the default Home app.

## Requirements

- Android Studio or a full Android SDK installation
- A full JDK 17+ installation
- Android SDK path configured through `local.properties` or `ANDROID_HOME`

Example `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Project Layout

- `app/src/main/java/dev/nighttraders/lumo/launcher/MainActivity.kt`
  Home activity, Home-role request, lock state observation, status bar blocker
- `app/src/main/java/dev/nighttraders/lumo/launcher/LumoStartupReceiver.kt`
  Boot/package-replace receiver — restores gesture sidebar and lock screen companion services
- `app/src/main/java/dev/nighttraders/lumo/launcher/admin/`
  Device Admin receiver for enhanced device management
- `app/src/main/java/dev/nighttraders/lumo/launcher/data/`
  Launcher discovery, ordered favorites, recent apps, settings (preferences, repository, settings data class)
- `app/src/main/java/dev/nighttraders/lumo/launcher/ui/`
  Compose UI: launcher app (home, dash rail, app drawer, multitask), system status, infographic metrics, view model
- `app/src/main/java/dev/nighttraders/lumo/launcher/ui/theme/`
  Color, typography, and Material theme setup
- `app/src/main/java/dev/nighttraders/lumo/launcher/keyboard/`
  Keyboard IME service, word engine (swipe typing, suggestions, spell check), swipe trail view
- `app/src/main/java/dev/nighttraders/lumo/launcher/notifications/`
  Notification listener service, notification center with dismissal cooldown, heads-up, and deep linking
- `app/src/main/java/dev/nighttraders/lumo/launcher/overlay/`
  Gesture sidebar overlay service (system-wide dock), back gesture accessibility service
- `app/src/main/java/dev/nighttraders/lumo/launcher/lockscreen/`
  Lock state management (delayed lock), lock screen UI (infographic ring + bokeh), companion service, unlock receiver
- `app/src/main/java/dev/nighttraders/lumo/launcher/settings/`
  Settings activity, settings screen, debug log viewer, wallpaper picker
- `app/src/main/java/dev/nighttraders/lumo/launcher/weather/`
  Open-Meteo weather provider (free API, no key required)

## Intentional Omissions

These Linux-only prototype features were not copied into the launcher MVP:

- Embedded terminal
- `xdotool` keyboard injection
- Termux shell bridge
- Desktop file manager behavior
- Shell-command-based launchers
