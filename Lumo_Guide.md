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
adb install -r /home/kraken/Devel/Lumo/android-launcher/app/build/outputs/apk/release/app-release.apk
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

- Recent notifications on Home
- Heads-up alerts
- Swipe-to-dismiss notification cards
- Long-press actions for open, open app, snooze, and dismiss

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
- Lets you reveal a Lumo rail from other apps
- Provides quick access to Home, Apps, pinned apps, System, and Lock

Important note:

- This is an overlay-based approximation of Ubuntu Touch behavior, not full system-shell ownership

## 6. Enable the Keyboard

Lumo Keyboard is a real Android IME.

From `Lumo System`:

1. Open `Keyboard`
2. Tap `Open keyboard settings`
3. Enable `Lumo Keyboard`
4. Return and tap `Show input method picker`
5. Select `Lumo Keyboard`

Current keyboard features:

- Haptic key feedback
- Shift
- Primary symbol page
- Alternate symbol page
- Suggestion strip
- Android completion and spell-check integration

Important note:

- Lumo Keyboard can use Android's public suggestion pipeline
- It cannot directly embed Google's private Gboard autocomplete engine

## 7. Use the Lock Screen Surface

Lumo includes a themed lock-screen surface.

From `Lumo System`:

- Tap `Open Lumo lock screen`

Behavior:

- Shows time, date, network, and recent notifications
- Swipe up to dismiss into Android's real unlock flow

Important note:

- This is a themed lock surface layered over Android's keyguard flow
- It is not a full replacement for the system lock screen

## 8. Main Gestures

Inside the launcher:

- Left edge on Home: reveal launcher rail
- Tap outside rail: close rail
- Swipe up from bottom on Home: open Apps
- Swipe down/up on top indicators bar: expand/collapse indicators
- Swipe notifications sideways: dismiss
- Long-press notifications: actions sheet

Outside the launcher:

- Left edge overlay handle: reveal sidebar if overlay permission is enabled

## 9. Troubleshooting

### Notifications not appearing

- Make sure notification access is enabled for Lumo
- Open `Lumo System` and refresh the state by leaving and returning to the app

### Sidebar does not appear in other apps

- Confirm overlay permission is granted
- Confirm the gesture sidebar is enabled in `Lumo System`
- Some OEM software may make overlay behavior less predictable than stock Android

### Keyboard does not appear

- Confirm `Lumo Keyboard` is enabled in Android input settings
- Use the input method picker to switch to it

### Lock screen feels inconsistent

- That is expected to some degree because Android still owns the real keyguard

## 10. Project Orientation

If you are modifying the code:

- Android implementation lives in `android-launcher/`
- Legacy GTK prototype lives in `src/`
- Migration notes live in `docs/android-launcher-migration.md`
