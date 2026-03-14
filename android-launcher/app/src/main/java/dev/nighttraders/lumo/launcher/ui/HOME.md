# Home / Launcher Shell

The core launcher UI — home screen, app drawer, multitask overlay, dash rail, indicators panel, lock screen, and gesture handling.

## Files

- `LumoLauncherApp.kt` — Main composable: home screen, app drawer (bottom/side), multitask overlay, dash rail with drag-and-drop reorder, gesture zones, indicators, lock screen overlay
- `LauncherUiState.kt` — UI state data class (apps, ordered favorites, recent apps, notifications)
- `LauncherViewModel.kt` — ViewModel: app loading with debounced refresh, settings flow, periodic refresh, notification management, favorite reordering
- `SystemStatus.kt` — System status snapshot (time, date, network, battery) + `rememberSystemStatus()`
- `InfoGraphicMetrics.kt` — Lock screen / home infographic data (weather, steps, battery, storage, calendar)
- `theme/` — Material 3 theme (Color, Type, Theme)

## Features

- Ubuntu Touch-style home screen with clock, date, network label
- Dash rail (left side, flush against screen edge) with pinned/favorite apps + Ubuntu symbol button
- Drag-and-drop reorder of dash rail apps via long-press drag gesture
- `RailAppIcon` (tap-only, no long-click) for dash rail; `DockAppIcon` (tap + long-press toggle) for other contexts
- Rail positioned below the launcher's top bar, not Android's system status bar
- App drawer: slide from bottom or side, with search, grid layout, Gaussian blur backdrop
- App long-press action sheet: Open, Pin/Unpin from Launcher, App Info, Uninstall
- Multitask overlay: horizontal card spread of recent apps with accent-tinted backgrounds
- Indicator panel: Wi-Fi, Bluetooth, airplane mode, flashlight, location, notifications
- Long-press homescreen opens wallpaper picker
- Configurable icon sizes, grid columns, gesture sensitivity
- Lock screen integration: when `isDashLocked=true`, entire UI replaced with `LumoLockScreenScreen`

## Known Issues / Bugs

- Dash rail could flash default size on cold start before DataStore emits saved values (mitigated by `SharingStarted.Eagerly`)
- Content offset animation when rail opens/closes may lag on low-end devices

## Plans

- Screenshot thumbnails for multitask cards (blocked by Android security — third-party launchers can't capture other apps)
- Animated transitions between home and app drawer states
