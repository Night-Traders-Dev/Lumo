# Home / Launcher Shell

The core launcher UI — home screen, app drawer, multitask overlay, dash rail, indicators panel, and gesture handling.

## Files

- `LumoLauncherApp.kt` — Main composable: home screen, app drawer (bottom/side), multitask overlay, dash rail, gesture zones, indicators
- `LauncherUiState.kt` — UI state data class (apps, favorites, notifications, recents)
- `LauncherViewModel.kt` — ViewModel: app loading, settings flow, periodic refresh, notification management
- `SystemStatus.kt` — System status snapshot (time, date, network, battery) + `rememberSystemStatus()`
- `InfoGraphicMetrics.kt` — Lock screen / home infographic data (steps, battery, storage, etc.)
- `theme/` — Material 3 theme (Color, Type, Theme)

## Features

- Ubuntu Touch–style home screen with clock, date, network label
- Dash rail (left side) with pinned/favorite apps + Ubuntu symbol button
- App drawer: slide from bottom or side, with search, grid layout, Gaussian blur backdrop
- Multitask overlay: horizontal card spread of recent apps with accent-tinted backgrounds
- Indicator panel: Wi-Fi, Bluetooth, airplane mode, flashlight, location, notifications
- Long-press homescreen opens wallpaper picker
- Configurable icon sizes, grid columns, gesture sensitivity

## Known Issues / Bugs

- Dash rail could flash default size on cold start before DataStore emits saved values (mitigated by `SharingStarted.Eagerly`)
- Content offset animation when rail opens/closes may lag on low-end devices

## Plans

- Screenshot thumbnails for multitask cards (blocked by Android security — third-party launchers can't capture other apps)
- Animated transitions between home and app drawer states
