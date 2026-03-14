# Gesture Sidebar Overlay

System overlay service that provides a persistent edge-swipe sidebar accessible from any app.

## Files

- `LumoGestureSidebarService.kt` — `Service` with `TYPE_APPLICATION_OVERLAY` windows: thin edge handle for swipe detection, full-height rail with pinned apps, dismiss scrim, Ubuntu symbol button for app drawer

## Features

- Edge swipe from left side reveals the dash rail overlay
- Pinned/favorite apps displayed as squircle icons
- Ubuntu symbol button at bottom opens the app drawer (via intent to MainActivity)
- Tap outside rail to dismiss
- Respects overlay permission (`SYSTEM_ALERT_WINDOW`)
- Disabled when device is locked (`KeyguardManager.isKeyguardLocked`)
- Auto-refreshes app list when notified

## Known Issues / Bugs

- Overlay rail width is fixed and doesn't dynamically match the in-launcher dash settings
- On some Android 14+ devices, overlay touches may be blocked by system security policies

## Plans

- Match overlay rail width/icon size with launcher dash settings
- Add quick-action buttons (e.g., flashlight, Wi-Fi toggle) to the overlay rail
