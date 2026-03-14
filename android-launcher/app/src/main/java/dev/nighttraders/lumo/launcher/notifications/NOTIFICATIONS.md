# Notifications

System notification listener and in-launcher notification center.

## Files

- `LumoNotificationListenerService.kt` — `NotificationListenerService` that captures active notifications and exposes them via `LauncherNotificationCenter`
- `LauncherNotificationCenter.kt` — Singleton managing notification list (`StateFlow`), heads-up display, dismiss/snooze
- `LauncherNotification.kt` — Data class for notifications (key, title, message, app label, package, icon, messaging flag, timestamp)

## Features

- Real-time notification tracking from the system notification drawer
- Heads-up notification display with auto-dismiss (6 seconds)
- Swipe-to-dismiss individual notifications
- Notification snooze support
- Open notification content intent or fall back to app launch
- Notification count badge in top bar

## Known Issues / Bugs

- Notification access must be granted manually in system settings
- Some notifications (e.g., ongoing/foreground service) may not be dismissible

## Plans

- Notification grouping by app
- Quick reply from notification
- Notification history / log
