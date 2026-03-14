# Lock Screen

Custom lock screen with Ubuntu Touch-style infographic ring, bokeh circles, PIN/password security, and notifications.

## Files

- `LockScreenActivity.kt` — Activity shown over the system keyguard; handles PIN/password verification, hashing (SHA-256 + salt), conditional `setTurnScreenOn` via intent extra
- `LumoLockScreenScreen.kt` — Composable UI: infographic ring (280dp) with day-of-month dots and progress arc, 18 orbiting bokeh circles in 3 orbital rings, time (64sp), date, auto-cycling metrics (6s interval, double-tap to cycle), PIN entry panel, notification previews
- `LumoLockScreenCompanionService.kt` — Foreground service that wakes the lock screen via full-screen intent when the device screen turns on (Android 13 and earlier only)
- `LumoLockState.kt` — Global lock/unlock state singleton (`StateFlow<Boolean>`) with 3-second delayed lock on screen off and immediate `lockNow()` for process start
- `LumoUnlockReceiver.kt` — BroadcastReceiver for `ACTION_SCREEN_OFF` that triggers `LumoLockState.lock()` (always registered, not gated on companion setting)

## Features

- PIN (max 10 digits) and password (max 32 chars) security with salted SHA-256 hashing
- Infographic ring on lock screen: day-of-month as dots, circular progress arc
- Ubuntu Touch bokeh circles: 18 translucent orange/red circles orbiting in 3 rings (inner/main/outer) behind the infographic ring
- Auto-cycling metrics: weather, humidity, notifications, messages, email, steps, battery, network, calendar facts
- Notification preview on lock screen
- Swipe-up to unlock (when no security set)
- PIN entry panel slides up from bottom with numeric keypad and visual PIN dots
- 3-second grace period: if screen turns back on within 3 seconds, lock is cancelled
- Security state starts as "loading" to prevent bypass during async DataStore load
- Status bar blocked when locked (overlay + FLAG_FULLSCREEN + BEHAVIOR_DEFAULT)
- Gesture sidebar blocked when locked (checks `LumoLockState.isLocked`)
- Companion service uses `createWakeIntent()` with `EXTRA_TURN_SCREEN_ON` to conditionally wake screen
- Security credentials persist across app updates (stored in DataStore)

## Known Issues / Bugs

- Companion service requires `USE_FULL_SCREEN_INTENT` permission which may not be granted on all devices
- Companion service only works on Android 13 and earlier (`isWakeCompanionSupported()` returns false on 14+)
- Device owner lock task mode (full nav button blocking) requires `dpm set-device-owner` which is blocked on devices with multiple user profiles (e.g., Samsung Knox Secure Folder)

## Plans

- Pattern unlock
- Fingerprint / biometric unlock integration
- Customizable lock screen widgets
