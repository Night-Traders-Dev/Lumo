# Lumo Updates

This file tracks the major Android-port milestones and recent fixes in plain language.

## Current Snapshot

- The repo now includes a native Android launcher in `android-launcher/`
- The launcher installs and runs on modern Android devices
- Lumo can act as a Home app, not just a standalone demo
- The original GTK shell still exists under `src/` as a historical reference

## Major Android Work Completed

### Keyboard: Ubuntu Touch Maliit styling and swipe typing

- Restyled keyboard to match Ubuntu Touch's Maliit keyboard: charcoal grey background (`#2B2B2B`), flat keys with subtle 6dp rounding (`#3C3C3C`), darker special keys (`#494949`)
- Added character popover (key preview popup) that shows a magnified letter above the pressed key, matching Ubuntu Touch and iOS behavior
- Added swipe typing (gesture typing) with haptic feedback per key transition during swipe
- Key preview follows finger during swipe mode, showing the current key being touched
- Swipe word resolution uses `KeyboardWordEngine` with Damerau-Levenshtein distance scoring
- Space bar cursor control: press-and-hold spacebar then drag left/right to move the text cursor

### Notification drawer: Ubuntu Touch indicator panel redesign

- Redesigned indicators panel from a narrow right-side dropdown to a full-width Ubuntu Touch-style panel
- Status indicators shown as rounded pills (Wi-Fi, Battery, Notifications) across the top
- Date header with thin separator lines between sections
- Notifications use swipe-to-dismiss cards with long-press for action sheet
- "Clear all" text link in orange replaces the old Material button
- Bottom action row with icon+label buttons: Settings, Lock, Set Home

### Lock screen: InfoGraphic improvements

- Added Ubuntu Touch-style metric messages inside the InfoGraphic circle (notification count, messaging count, day of year, days remaining, month progress)
- Double-tap the circle to cycle through metric messages with fade animation
- Past days now show larger dots (3.5dp) with orange tint when notifications exist, matching Ubuntu Touch's activity visualization
- Accent arc opacity increased for better visibility

### Notification deep linking

- Clicking a notification now fires `contentIntent` first, which deep links to the specific content (e.g., tapping an SMS notification opens that conversation thread, not just the Messages app)
- Fallback to launching the app's main activity only when no `contentIntent` is available
- Notification is automatically dismissed from the system after opening
- Removed redundant dismiss calls in the ViewModel chain

### Dock: Ubuntu Touch BFB and squircle design

- Both the Compose launcher rail and the overlay gesture sidebar now match Ubuntu Touch's dock
- Ubuntu BFB (Big Friendly Button) at top with Circle of Friends vector icon
- Squircle (superellipse) shaped app icons with `|x|^n + |y|^n = 1` (n=4)
- Dot-grid apps button at bottom
- Thin separator lines between dock sections
- 68dp width, dark semi-transparent background

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
- Swipe typing accuracy depends on the bundled dictionary size
- Widgets and deeper launcher customization still need more work

## Recommended Next Work

- Long-press alternate characters and accent popups for the keyboard
- Keyboard emoji input panel and language switching
- Quick toggles grid (Wi-Fi, Bluetooth, Airplane mode, etc.) in indicators panel
- Right-edge task switcher gesture
- Passcode/passphrase entry on lock screen
- Media controls (MPRIS-style) in indicators panel
- Widget hosting
- Wallpaper support
- Persistent home layout customization
