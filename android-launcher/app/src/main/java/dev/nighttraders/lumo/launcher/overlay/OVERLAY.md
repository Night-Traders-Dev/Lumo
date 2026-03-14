# Gesture Sidebar Overlay

Thin edge-detection overlay service that triggers the Compose dash rail from any app.

## Files

- `LumoGestureSidebarService.kt` — `Service` with a single `TYPE_APPLICATION_OVERLAY` edge handle (18dp, left side). Detects swipe gestures and brings the launcher to the foreground with the dash rail visible.

## Architecture

There is only **one dash implementation** — the Compose `UbuntuTouchLauncherRail` in `LumoLauncherApp.kt`. This overlay service is purely an edge gesture detector. When a swipe is detected:

1. Service calls `MainActivity.createDashIntent()` (START_PAGE_DASH = 2)
2. MainActivity brings to foreground via `onNewIntent`
3. `LumoLauncherApp` toggles `railVisible` in response to the navigation request

This ensures the dash always has the same size, features (drag-and-drop reorder), and settings regardless of whether it was opened from within the launcher or from another app.

## Features

- Edge swipe from left side (26dp threshold) opens the dash rail
- Blocked when device is keyguard-locked or Lumo lock screen is active (`LumoLockState.isLocked`)
- Respects overlay permission (`SYSTEM_ALERT_WINDOW`)
- Restored on boot/package-replace by `LumoStartupReceiver` (when enabled in preferences)

## Known Issues / Bugs

- On some Android 14+ devices, overlay touches may be blocked by system security policies

## Plans

- Configurable edge handle width from settings
