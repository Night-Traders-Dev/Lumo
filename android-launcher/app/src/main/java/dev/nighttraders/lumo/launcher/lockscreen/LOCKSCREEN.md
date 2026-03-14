# Lock Screen

Custom lock screen with PIN/password security, infographics, and notifications.

## Files

- `LockScreenActivity.kt` — Activity shown over the system keyguard; handles PIN/password verification, hashing (SHA-256 + salt)
- `LumoLockScreenScreen.kt` — Composable UI: clock, date, infographic circle (steps, battery, storage), swipe-to-unlock, PIN entry, notification previews
- `LumoLockScreenCompanionService.kt` — Foreground service that wakes the lock screen via full-screen intent when the device screen turns on
- `LumoLockState.kt` — Global lock/unlock state (`StateFlow<Boolean>`)
- `LumoUnlockReceiver.kt` — BroadcastReceiver for `SCREEN_OFF` (lock + launch lock screen) and `USER_PRESENT` (bring launcher home)

## Features

- PIN and password security with salted SHA-256 hashing
- Infographic circle on lock screen (step count, battery, storage)
- Notification preview on lock screen
- Swipe-up to unlock (when no security set)
- Companion service for showing lock screen over system keyguard
- Security credentials persist across app updates (stored in DataStore)

## Known Issues / Bugs

- Companion service requires `USE_FULL_SCREEN_INTENT` permission which may not be granted on all devices
- On some OEMs, the companion wake approach doesn't work reliably

## Plans

- Pattern unlock
- Fingerprint / biometric unlock integration
- Customizable lock screen widgets
