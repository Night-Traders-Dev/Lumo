# Settings

Settings activity and related screens for configuring Lumo launcher.

## Files

- `SettingsActivity.kt` — Settings host activity; manages wallpaper picker, debug log, and settings screen navigation
- `LumoSettingsScreen.kt` — Main settings composable: appearance (icon size, grid columns, dash size), gestures, security, permissions, developer tools
- `LumoDebugScreen.kt` — Terminal-style debug log viewer with pinch-to-zoom, selectable text, auto-scroll, color-coded log levels
- `WallpaperPickerScreen.kt` — Wallpaper picker: stock Ubuntu Touch wallpapers (from assets), default gradient, custom image via SAF

## Features

- Appearance: app icon size, grid columns, dash rail width, dash icon size
- Wallpaper: 8 stock Ubuntu Touch wallpapers, custom image picker with persistent URI permission
- Gesture configuration: toggle bottom/left edge, multitask swipe, indicator swipe; adjust thresholds and dimensions
- Security: PIN (max 10 digits) / password (max 32 chars) setup with salted SHA-256 hashing
- Permissions: default home, notification access, overlay, usage stats, keyboard
- Gesture sidebar toggle (starts/stops `LumoGestureSidebarService`)
- Lock screen companion toggle (starts/stops `LumoLockScreenCompanionService`)
- Debug log terminal (survives rotation, pinch-to-zoom, selectable/copyable text, color-coded levels)
- Cache/data migration on launcher update

## Known Issues / Bugs

- Settings changes are instant but may flash briefly on lifecycle restart (mitigated by Eagerly sharing)
- Wallpaper picker custom image requires persistent URI permission which some file managers don't support

## Plans

- Export/import settings backup
- Per-app icon customization
- Theme picker (beyond wallpaper)
