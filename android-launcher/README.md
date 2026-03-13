# Lumo Launcher for Android

This directory contains the native Android launcher port of Lumo. It keeps the Lomiri-inspired interaction model from the GTK prototype, but rebuilds it with Android launcher APIs and Jetpack Compose.

## Current Scope

- Home-screen activity with `HOME` and `DEFAULT` intent filters
- Request flow for Android's Home role
- App discovery via launcher activities, not Linux `.desktop` files
- Searchable app library
- Persistent favorites rail backed by DataStore
- Live header with time, battery, and network status

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
  Compose UI, system status handling, and screen state
- `app/src/main/java/dev/nighttraders/lumo/launcher/ui/theme/`
  Color, typography, and Material theme setup

## Intentional Omissions

These Linux-only prototype features were not copied into the launcher MVP:

- Embedded terminal
- `xdotool` keyboard injection
- Termux shell bridge
- Desktop file manager behavior
- Shell-command-based launchers

