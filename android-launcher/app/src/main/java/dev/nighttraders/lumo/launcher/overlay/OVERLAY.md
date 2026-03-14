# Gesture Sidebar Overlay

System overlay service that provides a persistent edge-swipe sidebar accessible from any app.

## Files

- `LumoGestureSidebarService.kt` — `Service` with `TYPE_APPLICATION_OVERLAY` windows: thin edge handle (18dp) for swipe detection (26dp threshold), full-height rail with pinned apps, dismiss scrim, Ubuntu symbol button for app drawer

## Features

- Edge swipe from left side reveals the dash rail overlay
- Pinned/favorite apps displayed as squircle icons (52dp with 44dp inner icon)
- Ubuntu symbol button at bottom opens the app drawer (via intent to MainActivity)
- Tap outside rail to dismiss
- Respects overlay permission (`SYSTEM_ALERT_WINDOW`)
- Blocked when device is locked (`KeyguardManager.isKeyguardLocked`) or Lumo lock screen is active (`LumoLockState.isLocked`)
- Auto-refreshes app list when notified
- Restored on boot/package-replace by `LumoStartupReceiver` (when enabled in preferences)

## Known Issues / Bugs

- Overlay rail width is fixed (68dp) and doesn't dynamically match the in-launcher dash settings
- On some Android 14+ devices, overlay touches may be blocked by system security policies

## Plans

- Match overlay rail width/icon size with launcher dash settings
- Add quick-action buttons (e.g., flashlight, Wi-Fi toggle) to the overlay rail
