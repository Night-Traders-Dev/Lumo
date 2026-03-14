# Lumo Updates

This file tracks the major Android-port milestones and recent fixes in plain language.

## Current Snapshot

- The repo now includes a native Android launcher in `android-launcher/`
- The launcher installs and runs on modern Android devices
- Lumo can act as a Home app, not just a standalone demo
- The original GTK shell still exists under `src/` as a historical reference

## Major Android Work Completed

### Launcher foundation

- Added a proper `HOME` + `DEFAULT` launcher activity
- Added Home-role request flow
- Replaced Linux `.desktop` discovery with Android launcher activity discovery
- Added persisted favorites using DataStore
- Added Lomiri-inspired home surface and app scope

### Gestures and shell feel

- Added hidden edge-reveal launcher rail on Home
- Added bottom-edge gesture between Home and Apps
- Added indicators sheet and top-swipe behavior
- Added optional overlay-based gesture sidebar so Lumo can be reached from other apps

### Notifications

- Added a real notification listener service
- Added recent notifications on Home
- Added heads-up notifications
- Added swipe-to-dismiss notification cards
- Added long-press notification actions for open, open app, snooze, and dismiss
- Fixed a Compose state issue where the dismiss background could linger after swiping away a notification

### Settings and system surfaces

- Added `Lumo System` settings activity
- Added links for default Home role, notification access, overlay permission, keyboard settings, and wallpaper/display/Wi-Fi settings
- Added a Lumo lock screen surface that works with Android's real keyguard dismissal flow

### Keyboard

- Added a real `InputMethodService` for Lumo Keyboard
- Added haptic feedback on keypress
- Added primary and alternate symbol layouts
- Added suggestion strip support using Android completions and spell-check APIs
- Fixed Android 16 / targetSdk 35 compatibility by removing blocked reads from `Settings.Secure`

## Known Limits

- The overlay sidebar is an Android overlay approximation, not a true system shell
- The lock screen is not a full Android lock-screen replacement
- The keyboard cannot embed Gboard's proprietary autocomplete engine
- Widgets and deeper launcher customization still need more work

## Recommended Next Work

- Long-press alternate characters and accent popups for the keyboard
- Widget hosting
- Better Ubuntu Touch indicator styling and animation polish
- Persistent home layout customization
- Stronger app drawer gesture behavior and scope transitions
