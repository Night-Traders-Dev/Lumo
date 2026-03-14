# Lumo Updates

This file tracks the major Android-port milestones and recent fixes in plain language.

## Version History

### v0.5.0 — 2026-03-13: Gesture engine, full settings, multitask redesign

This session overhauled the gesture system, added comprehensive settings, and fixed several UI issues.

#### Gesture engine and back gesture service

- Added `LumoBackGestureService` — an AccessibilityService that provides system-wide back gesture interception on the right screen edge
- Back gesture is automatically suppressed when the app drawer is open, so swiping out of the app drawer always works
- Runtime-configurable handle width and swipe threshold via static companion properties

#### Full settings control

- Added `LumoLauncherSettings` data class with 16 configurable properties
- New settings sections in Lumo System: Appearance, Gesture Toggles, Gesture Sensitivity
- Appearance: app icon size, app grid columns, dash rail width (all via sliders)
- Gesture toggles: back gesture, bottom edge, left edge, multitask, indicator swipe (on/off switches)
- Gesture sensitivity: 8 sliders for handle widths and swipe thresholds for every gesture zone
- Settings propagate reactively: DataStore → Repository → ViewModel → Activity → Composables

#### App drawer overhaul

- Replaced HorizontalPager with direction-aware AnimatedVisibility overlays (bottom slide-up and side slide-in)
- Fixed artifact where home screen content bled through the app drawer (opaque gradient background)
- App drawer button in the dock now toggles: opens when closed, closes when already open
- Horizontal swipe on home opens app drawer; swipe back from app drawer returns home

#### Notification dismissal fix

- Added 5-second cooldown suppression in `LauncherNotificationCenter` to prevent dismissed notifications from reappearing when the source app re-posts them
- Uses `ConcurrentHashMap` with timestamp-based expiry

#### Multitask view redesign

- Redesigned to match Ubuntu Touch card style: vertical `LazyColumn` with window preview cards
- Each card has a title bar (app icon + name + close button) and a preview area
- Swipe-to-dismiss with `graphicsLayer` translation and alpha fade, 200px threshold
- Local `dismissedKeys` state prevents dismissed cards from flickering back

#### Lock screen companion and security

- Added `LumoLockScreenCompanionService` for wake-on-screen-off behavior
- Added PIN and password security with SHA-256 hashing and random salt
- Dash lock: quick-settings dashboard can be locked behind PIN/password
- `LumoUnlockReceiver` listens for screen-off and user-present broadcasts

#### Screenshots and documentation

- Added `screenshots/` directory with 10 launcher screenshots
- Added `.gitignore` to exclude build artifacts and IDE files

### v0.4.0 — 2026-03-13: Android launcher full feature build

The initial Android launcher build with all core Ubuntu Touch features.

#### Launcher core

- Home-role launcher activity with `HOME` + `DEFAULT` intent filters
- App discovery via Android launcher activities
- Persisted favorites using DataStore
- Recent apps tracking
- Long-press app actions: favorite, app info, uninstall

#### Ubuntu Touch UI

- Home surface with time, date, and notification preview cards
- Ubuntu Touch-style dock with BFB (Big Friendly Button) and squircle app icons
- Edge-reveal launcher rail on Home
- Full-width indicator panel with status pills, quick actions, and "Clear all"

#### Notification system

- Real `NotificationListenerService` with notification center
- Deep linking: tapping a notification opens specific content (SMS thread, not just app)
- Heads-up notification alerts with auto-dismiss
- Swipe-to-dismiss and long-press action sheets (open, open app, snooze, dismiss)

#### Keyboard IME

- Full `InputMethodService` styled after Ubuntu Touch Maliit
- Charcoal flat keys with subtle rounding, character popover preview
- Swipe typing with `KeyboardWordEngine` (Damerau-Levenshtein scoring)
- Haptic feedback per key and during swipe transitions
- Space bar cursor control (hold + drag)
- Primary and alternate symbol layouts
- Suggestion strip with autocomplete and spell check
- Swipe trail visualization via `SwipeTrailView`

#### Lock screen

- Ubuntu Touch InfoGraphic ring with day-of-month dots
- Metric messages (notification count, day of year, month progress)
- Double-tap to cycle metrics with fade animation
- Past-day dots with orange tint for notification activity
- Swipe up to unlock into Android's real keyguard flow

#### Overlay services

- Gesture sidebar overlay for reaching Lumo from other apps
- Ubuntu-styled overlay dock matching the in-app rail design

#### Settings

- Lumo System settings activity
- Sections for default home, notifications, overlay permission, keyboard, lock screen
- Wi-Fi, display, wallpaper, and accessibility shortcuts

### v0.3.0 — 2025-09-03: Terminal and utilities

- Added embedded terminal component
- Added apps/util integration
- Updates to shell and app drawer

### v0.2.0 — 2025-08-29: GTK prototype expansion

- Added settings app to GTK shell
- Added app drawer with search
- Added terminal emulator
- Added Facebook Messenger integration
- Updated favorites and panel behavior
- Added LumoIDE prototype

### v0.1.0 — 2025-08-27: Keyboard and text engine

- Extensive keyboard development (20+ commits)
- Text engine (TE) development and refinements
- Initial keyboard layout and input handling

### v0.0.1 — 2025-08-19: Initial commit

- Project scaffolding
- GTK/Python shell prototype with panel, favorites rail, and app drawer
- Text engine foundation (Lumo TE)

## Known Limits

- The overlay sidebar is an Android overlay approximation, not a true system shell
- The lock screen is not a full Android lock-screen replacement
- The keyboard cannot embed Gboard's proprietary autocomplete engine
- Swipe typing accuracy depends on the bundled dictionary size
- Widgets and deeper launcher customization still need more work

## Recommended Next Work

- Long-press alternate characters and accent popups for the keyboard
- Keyboard emoji input panel and language switching
- Quick toggles grid (Wi-Fi, Bluetooth, Airplane mode, etc.) in indicators panel
- Media controls (MPRIS-style) in indicators panel
- Widget hosting
- Wallpaper support
- Persistent home layout customization
