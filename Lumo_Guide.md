# Lumo Guide

This guide explains how to build, install, enable, and use the current Android version of Lumo.

## 1. Build the APK

Requirements:

- Android SDK configured through `ANDROID_HOME` or `android-launcher/local.properties`
- JDK 17 or newer
- `adb` installed if you want to sideload and debug on-device

Build command:

```bash
cd android-launcher
./gradlew :app:assembleRelease
```

APK output:

```text
android-launcher/app/build/outputs/apk/release/app-release.apk
```

## 2. Install on a Device

```bash
adb install -r android-launcher/app/build/outputs/apk/release/app-release.apk
```

If the package installer UI crashes on-device, `adb install -r` is the preferred path.

## 3. Set Lumo as the Launcher

You can do this in either of two ways:

- Open Lumo and accept the Home-role request
- Open `Lumo System` and use the launcher section

If Android does not prompt automatically, open system Home app settings and select Lumo manually.

## 4. Enable Notifications

Lumo needs notification listener access to show texts and alerts.

From Lumo:

- Open the indicators sheet
- Open `Settings`
- Use the Notifications section to open notification access settings
- Enable `Lumo Notifications`

What you get after enabling it:

- Recent notifications on Home and in the indicator panel
- Heads-up alerts with swipe-to-dismiss
- Swipe-to-dismiss notification cards in the indicator panel
- Long-press actions for open, open app, snooze, and dismiss
- Tap a notification to open the specific content (e.g., tapping an SMS opens that conversation thread, not just the Messages app)
- "Clear all" button to dismiss all notifications at once
- Dismissed notifications are suppressed for 5 seconds to prevent re-posting flicker

## 5. Enable the Gesture Sidebar

The cross-app sidebar uses Android's overlay permission.

From `Lumo System`:

1. Open the `Gesture Sidebar` section
2. Tap `Allow display over other apps`
3. Grant overlay permission for Lumo
4. Return to `Lumo System`
5. Tap `Enable gesture sidebar`

What it does:

- Adds a left-edge overlay handle
- Lets you reveal a Lumo dock from other apps
- Ubuntu BFB (Big Friendly Button) at top for Home
- Squircle-shaped pinned app icons
- Apps grid button at bottom

Important note:

- This is an overlay-based approximation of Ubuntu Touch behavior, not full system-shell ownership

## 6. Enable the Back Gesture Service

The back gesture service uses Android's AccessibilityService to provide a right-edge back swipe.

From `Lumo System`:

1. Open `Accessibility Settings`
2. Find and enable `Lumo Back Gesture`
3. Grant the accessibility permission

Behavior:

- Right-edge swipe triggers system back action
- Automatically suppressed when the app drawer is open (so you can swipe the drawer closed)
- Handle width and swipe threshold are configurable in Settings → Gesture Sensitivity

## 7. Enable the Keyboard

Lumo Keyboard is a real Android IME.

From `Lumo System`:

1. Open `Keyboard`
2. Tap `Open keyboard settings`
3. Enable `Lumo Keyboard`
4. Return and tap `Show input method picker`
5. Select `Lumo Keyboard`

Current keyboard features:

- Ubuntu Touch Maliit-inspired styling (charcoal flat keys with subtle rounding)
- Character popover preview on key press
- Swipe typing (gesture typing) across letter keys with word suggestions
- Swipe trail visualization during gesture typing
- Haptic key feedback (including per-key haptic during swipe)
- Shift and auto-capitalization
- Primary symbol page (`?123`) and alternate symbol page (`#+=`)
- Suggestion strip with autocorrect, spell check, and local dictionary
- Space bar cursor control: hold and drag to move the text cursor
- Android completion and spell-check integration

Important note:

- Lumo Keyboard can use Android's public suggestion pipeline
- It cannot directly embed Google's private Gboard autocomplete engine

## 8. Use the Lock Screen Surface

Lumo includes a themed lock-screen surface with optional security.

From `Lumo System`:

- Tap `Open Lumo lock screen`

Behavior:

- Ubuntu Touch InfoGraphic ring with day-of-month dots around the circle
- Shows time, date, and metric messages inside the ring
- Double-tap the circle to cycle through metrics (notification count, messages, day of year, etc.)
- Past days shown with larger dots and orange tint when notifications exist
- Accent arc tracks progress through the month
- Up to 3 notification cards shown below the ring
- Swipe up to dismiss into Android's real unlock flow

Lock screen security:

- Set a PIN or password from `Lumo System` → Lock Screen section
- Security is hashed with SHA-256 + random salt (never stored in plaintext)
- When security is set, the dash/quick-settings panel requires authentication to access
- Lock screen companion service can wake the lock screen when the screen turns off

Important note:

- This is a themed lock surface layered over Android's keyguard flow
- It is not a full replacement for the system lock screen

## 9. Customize the Launcher

All launcher behavior can be tuned from `Lumo System`.

### Appearance

- **App icon size**: Adjust the size of app icons in the app drawer (default: 56dp)
- **App grid columns**: Number of columns in the app drawer grid (default: 4)
- **Dash rail width**: Width of the Ubuntu Touch dock/rail (default: 68dp)

### Gesture Toggles

Each gesture can be individually enabled or disabled:

- **Back gesture**: Right-edge back swipe (requires accessibility service)
- **Bottom edge gesture**: Swipe up from bottom to open app drawer
- **Left edge gesture**: Swipe from left edge to reveal dock rail
- **Multitask gesture**: Access the multitask view
- **Indicator swipe**: Swipe down from top to open indicator panel

### Gesture Sensitivity

Fine-tune trigger zones and thresholds for each gesture:

- **Back gesture width**: How far from the right edge the back gesture activates (default: 20dp)
- **Back gesture threshold**: Minimum swipe distance to trigger back (default: 40dp)
- **Bottom edge height**: Height of the bottom-edge trigger zone (default: 70dp)
- **Bottom edge threshold**: Minimum swipe distance to open app drawer from bottom (default: 42dp)
- **Left edge width**: Width of the left-edge dock reveal zone (default: 20dp)
- **Left edge threshold**: Minimum swipe distance to reveal dock (default: 24dp)
- **Horizontal swipe threshold**: Minimum horizontal swipe to open/close app drawer (default: 80dp)
- **Multitask swipe threshold**: Minimum swipe distance for multitask gesture (default: 80dp)

## 10. Main Gestures

Inside the launcher:

- Left edge on Home: reveal Ubuntu Touch-style dock (BFB button, pinned apps, apps grid)
- Tap outside dock: close dock
- Swipe left on Home: open app drawer (side slide-in)
- Swipe up from bottom on Home: open app drawer (bottom slide-up)
- Swipe right in app drawer: close app drawer
- Tap app drawer button when drawer is open: close app drawer (toggle behavior)
- Swipe down/up on top indicators bar: expand/collapse full-width indicator panel
- Swipe notifications sideways: dismiss
- Long-press notifications: actions sheet (open, open app, snooze, dismiss)
- Tap notification: deep link to specific content (SMS thread, email, etc.)
- Right edge swipe: system back (requires accessibility service)
- Multitask view: swipe app cards horizontally to dismiss

On the lock screen:

- Swipe up: unlock
- Double-tap the InfoGraphic circle: cycle through metric messages

On the keyboard:

- Swipe across letter keys: gesture/swipe typing
- Hold and drag spacebar: move cursor left/right

Outside the launcher:

- Left edge overlay handle: reveal dock sidebar if overlay permission is enabled

## 11. Troubleshooting

### Notifications not appearing

- Make sure notification access is enabled for Lumo
- Open `Lumo System` and refresh the state by leaving and returning to the app

### Dismissed notifications reappearing

- This is normal for some apps that re-post notifications frequently
- Lumo applies a 5-second suppression cooldown to prevent flicker
- If a notification keeps reappearing beyond 5 seconds, it is being re-posted by the source app

### Sidebar does not appear in other apps

- Confirm overlay permission is granted
- Confirm the gesture sidebar is enabled in `Lumo System`
- Some OEM software may make overlay behavior less predictable than stock Android

### Back gesture not working

- Confirm `Lumo Back Gesture` is enabled in Android accessibility settings
- The back gesture is automatically disabled when the app drawer is open
- Check that the back gesture toggle is enabled in Lumo System → Gesture Toggles

### Keyboard does not appear

- Confirm `Lumo Keyboard` is enabled in Android input settings
- Use the input method picker to switch to it

### Lock screen feels inconsistent

- That is expected to some degree because Android still owns the real keyguard

## 12. Project Orientation

If you are modifying the code:

- Android implementation lives in `android-launcher/`
- Legacy GTK prototype lives in `src/`
- Migration notes live in `docs/android-launcher-migration.md`
- Settings data class: `LumoLauncherSettings.kt`
- Preferences keys: `LauncherPreferences.kt`
- Settings flow: `LauncherRepository.observeLauncherSettings()` → `LauncherViewModel.launcherSettings` → `LumoLauncherApp(settings=...)`
