# Android Launcher Migration

Lumo started as a GTK shell prototype for Linux. This repository now includes a native Android launcher implementation in [`android-launcher/`](../android-launcher/) so the Lomiri-inspired experience can move onto Android without carrying over Linux-only platform glue.

## What Ports Cleanly

These concepts map well from the Python prototype into Android:

| GTK prototype | Android launcher equivalent |
| --- | --- |
| `TopPanel` in `src/windows/panel.py` | Compose status/header bar in `MainActivity` |
| `LeftFavorites` in `src/windows/favorites.py` | Persistent favorites rail |
| `AppDrawer` in `src/windows/app_drawer.py` | Searchable all-apps grid |
| `FAVORITES` in `src/config/config.py` | Persisted pinned-app state in DataStore |
| `TouchShell` orchestration in `src/shell/shell.py` | `LauncherViewModel` + Compose scene |

## What Does Not Port Directly

These GTK/Linux features were deliberately not copied into the Android launcher MVP:

- `xdotool` virtual keyboard injection
- `.desktop` file discovery under `/usr/share/applications`
- Termux TCP shell bridge in `src/termux/listener.sh`
- VTE terminal embedding
- `xdg-open` file launching
- Direct shell-command launch actions like `firefox` and `am start`

## New Android Architecture

- `android-launcher/app/src/main/java/.../MainActivity.kt`
  Owns the Home role request flow and renders the launcher UI.
- `android-launcher/app/src/main/java/.../data/LauncherRepository.kt`
  Discovers launchable activities through Android launcher APIs, launches apps, and persists favorites.
- `android-launcher/app/src/main/java/.../ui/LauncherViewModel.kt`
  Bridges repository data into UI state.
- `android-launcher/app/src/main/java/.../ui/LumoLauncherApp.kt`
  Implements the Lomiri-inspired launcher surface in Jetpack Compose.

## Immediate Next Steps

1. Install an Android SDK and open `android-launcher/` in Android Studio.
2. Set the app as the default Home app on a device or emulator.
3. Tune gestures, wallpaper handling, and widgets.
4. Decide which GTK prototype features should become separate Android apps versus launcher features.
