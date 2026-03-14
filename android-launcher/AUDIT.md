# Android Launcher Audit

Date: 2026-03-14

Scope: `android-launcher`

Verification:
- `./gradlew :app:assembleDebug` succeeds
- No `test` or `androidTest` source sets were found under `android-launcher/app/src`

## Summary

This launcher is feature-rich and builds cleanly, but there are a few correctness bugs and several hotspots that are likely to cause battery drain, UI jank, or state inconsistencies on real devices.

Highest-priority issues:
1. The settings screen leaks DataStore collectors and compounds the leak during slider changes.
2. The custom lock state is cleared before Android confirms that the system keyguard was actually dismissed.

## Findings

### 1. High: `SettingsActivity` creates unbounded DataStore collectors

Evidence:
- `SettingsActivity.loadLauncherSettings()` starts a new `lifecycleScope.launch { collect { ... } }` every time it is called: `app/src/main/java/dev/nighttraders/lumo/launcher/settings/SettingsActivity.kt:205-210`
- That method is called in `onCreate()` and again after wallpaper updates and every settings write: `SettingsActivity.kt:68-70`, `SettingsActivity.kt:100-102`, `SettingsActivity.kt:191-193`, `SettingsActivity.kt:199-201`

Why this is a bug:
- `collect` does not complete on its own, so each call leaves another live collector behind.
- Over time, the settings screen can accumulate duplicate observers that all write the same state, increasing memory use, UI churn, and DataStore work.

Impact:
- Duplicate recompositions and state writes while the settings screen is open.
- Gradually increasing CPU and memory cost during a long session.
- The slider bottleneck below amplifies this issue.

Recommendation:
- Collect launcher settings once in `onCreate()` or convert the screen to use a `StateFlow`/`collectAsStateWithLifecycle`.
- Remove all follow-up `loadLauncherSettings()` calls after individual writes.

### 2. High: The launcher unlock state is cleared before keyguard dismissal succeeds

Evidence:
- `LockScreenActivity.attemptUnlock()` calls `LumoLockState.unlock()` before `requestDismissKeyguard(...)`: `app/src/main/java/dev/nighttraders/lumo/launcher/lockscreen/LockScreenActivity.kt:84-98`

Why this is a bug:
- If `requestDismissKeyguard` fails, is canceled, or is ignored by the system/OEM, Lumo's internal lock state is still marked unlocked.
- That leaves the app's own lock surface out of sync with the actual device lock state.

Impact:
- The launcher can believe it is unlocked even when Android did not dismiss the system keyguard.
- This is especially risky because the main activity gates its in-app lock UI off `LumoLockState`: `MainActivity.kt:79-90`

Recommendation:
- Move `LumoLockState.unlock()` into `onDismissSucceeded()`.
- Handle `onDismissCancelled()` and `onDismissError()` by keeping the lock state set.

### 3. Medium: Keyboard status detection still references an old IME service ID

Evidence:
- Settings uses `dev.nighttraders.lumo.launcher/.input.LumoInputMethodService` and `...launcher.input.LumoInputMethodService`: `app/src/main/java/dev/nighttraders/lumo/launcher/settings/LumoSettingsScreen.kt:71-72`
- The manifest registers `.keyboard.LumoInputMethodService`: `app/src/main/AndroidManifest.xml:78-89`

Why this is a bug:
- The settings screen is checking the wrong service identifier.
- `isSelected` can be false even when the Lumo keyboard is active.

Impact:
- Users can be told the keyboard is not selected even when it is.
- Keyboard onboarding and troubleshooting flows become misleading.

Recommendation:
- Replace the old `.input...` constants with the actual `.keyboard.LumoInputMethodService` ID.
- Prefer deriving the expected ID from `ComponentName` to avoid future package drift.

### 4. Medium: Notification removals are treated as user dismissals and suppress legitimate re-posts

Evidence:
- `LauncherNotificationCenter.remove()` always adds the key to the dismissal cooldown map: `app/src/main/java/dev/nighttraders/lumo/launcher/notifications/LauncherNotificationCenter.kt:74-81`
- `onNotificationRemoved()` routes every system removal through that method: `app/src/main/java/dev/nighttraders/lumo/launcher/notifications/LumoNotificationListenerService.kt:69-71`
- `upsert()` then suppresses the notification if it reappears within the cooldown window: `LauncherNotificationCenter.kt:49-57`

Why this is a bug:
- A notification can be removed for many reasons besides an explicit user dismissal inside Lumo.
- System refreshes, app updates, or notification rewrites can briefly remove and repost the same key.

Impact:
- Legitimate notifications can disappear for up to 5 seconds after a normal system-side refresh.
- Messaging and ongoing conversation alerts are the most likely to look flaky here.

Recommendation:
- Split the API into `remove()` and `markDismissedByUser()` or add a `suppressRepost` flag.
- Only start the cooldown when the dismissal originated from Lumo UI actions.

### 5. Medium: Notification deduplication drops distinct alerts that share the same text

Evidence:
- `deduplicateByContent()` collapses notifications purely by `packageName|title|message`: `app/src/main/java/dev/nighttraders/lumo/launcher/notifications/LauncherNotificationCenter.kt:90-107`

Why this is a bug:
- Many apps emit multiple valid notifications with the same visible copy.
- Examples include multiple identical email subjects, repeated chat alerts, delivery/status events, and batched service notifications.

Impact:
- Users may see fewer notifications in Lumo than exist in the system shade.
- The launcher can undercount alerts and hide individual items.

Recommendation:
- Prefer key-based deduplication and only apply content dedup for clearly duplicated reposts.
- If content dedup remains, include more discriminators such as post time bucket, channel, group key, or conversation metadata.

### 6. Medium: The launcher reloads and re-rasterizes the entire app list on every resume

Evidence:
- `MainActivity.onResume()` always calls `viewModel.refreshApps()`: `app/src/main/java/dev/nighttraders/lumo/launcher/MainActivity.kt:195-205`
- `refreshApps()` calls `repository.loadApps()`: `app/src/main/java/dev/nighttraders/lumo/launcher/ui/LauncherViewModel.kt:85-92`
- `loadApps()` enumerates launchable apps and converts each icon to a bitmap: `app/src/main/java/dev/nighttraders/lumo/launcher/data/LauncherRepository.kt:62-94`

Why this is a bottleneck:
- Icon decoding is one of the most expensive parts of launcher refresh.
- Returning home from another app will trigger a full reload even when the installed app set has not changed.

Impact:
- Extra heap churn and potential frame drops when returning to the home screen.
- The cost scales with the number of installed apps.

Recommendation:
- Cache the app list and invalidate it only on package change broadcasts or explicit refresh.
- Cache icon bitmaps or use lazy icon loading for drawer/rail surfaces.

### 7. Medium: Appearance sliders write to DataStore on every drag tick

Evidence:
- The reusable slider writes on every `onValueChange`: `app/src/main/java/dev/nighttraders/lumo/launcher/settings/LumoSettingsScreen.kt:977-1000`
- Those updates go straight to DataStore through `updateIntSetting(...)`: `app/src/main/java/dev/nighttraders/lumo/launcher/settings/SettingsActivity.kt:189-194`

Why this is a bottleneck:
- Continuous dragging can generate dozens of DataStore writes per second.
- Each write fans out through collectors and UI state updates.

Impact:
- Noticeable lag while dragging sliders on slower devices.
- Unnecessary disk I/O and recomposition churn.
- The collector leak in finding 1 makes this worse over time.

Recommendation:
- Keep slider state locally while dragging and persist only in `onValueChangeFinished`.
- If live preview is needed, separate in-memory preview state from persisted state.

### 8. Medium: The IME suggestion pipeline does too much work on every key/selection update

Evidence:
- `updateSuggestions()` runs on every selection change and key path, refreshes local suggestions, and fires both spell APIs: `app/src/main/java/dev/nighttraders/lumo/launcher/keyboard/LumoInputMethodService.kt:580-599`
- `refreshSuggestionStrip()` destroys and recreates all suggestion buttons every time: `LumoInputMethodService.kt:601-646`
- Local prediction and autocorrection scoring are computed synchronously via `KeyboardWordEngine`: `LumoInputMethodService.kt:772-783`, `LumoInputMethodService.kt:809-833`
- The engine uses edit-distance scoring over candidate sets on the calling thread: `app/src/main/java/dev/nighttraders/lumo/launcher/keyboard/KeyboardWordEngine.kt:15-121`, `KeyboardWordEngine.kt:249-260`

Why this is a bottleneck:
- This work happens in the IME service path, where latency is directly visible as keyboard lag.
- The code currently does local scoring, spell-check requests, and view recreation per update.

Impact:
- Higher key latency and more GC/layout churn while typing or swiping.
- The risk is highest on lower-end devices or with large generated dictionaries.

Recommendation:
- Debounce suggestion refreshes.
- Move heavy dictionary scoring off the UI thread.
- Reuse suggestion views instead of `removeAllViews()` plus full button recreation each time.
- Consider picking either `getSuggestions` or `getSentenceSuggestions` instead of both per update.

### 9. Medium: Recents and notifications are polled continuously instead of relying more on events

Evidence:
- Notifications are resynced every 3 seconds in the notification listener service: `app/src/main/java/dev/nighttraders/lumo/launcher/notifications/LumoNotificationListenerService.kt:74-87`
- Recent apps are refreshed every 5 seconds in the view model: `app/src/main/java/dev/nighttraders/lumo/launcher/ui/LauncherViewModel.kt:182-195`

Why this is a bottleneck:
- Both subsystems already have event-style hooks available: notification posted/removed callbacks and explicit app launches.
- Polling keeps doing work even when nothing has changed.

Impact:
- Ongoing background CPU use and battery cost.
- Extra work in `LauncherRepository.loadRecentAppsFromSystem()` because it walks recents and usage events: `app/src/main/java/dev/nighttraders/lumo/launcher/data/LauncherRepository.kt:261-357`

Recommendation:
- Treat polling as a fallback or backstop, not the primary sync path.
- Increase intervals substantially or stop polling when the launcher is not visible.

### 10. Low: Flashlight handling is device-fragile and the callback is never unregistered

Evidence:
- Toggling uses the first camera ID instead of a flash-capable camera: `app/src/main/java/dev/nighttraders/lumo/launcher/MainActivity.kt:273-281`
- `registerFlashlightCallback()` registers an anonymous torch callback without storing or unregistering it: `MainActivity.kt:284-290`

Why this is a bug/bottleneck:
- On some devices the first camera ID is not the rear flash unit.
- The callback lifetime is tied to the process rather than the activity lifecycle.

Impact:
- Flashlight toggle can fail on multi-camera devices.
- Recreated activities can leave stale callbacks behind.

Recommendation:
- Resolve a flash-capable camera via characteristics instead of `firstOrNull()`.
- Keep a callback instance field and unregister it in `onDestroy()`.

## Suggested Remediation Order

1. Fix the settings collector leak and stop persisting slider changes on every drag frame.
2. Fix the lock-state ordering bug in `LockScreenActivity`.
3. Correct the IME service ID mismatch in the settings screen.
4. Split notification "removed" and "dismissed by user" behavior.
5. Cache installed apps/icons instead of rebuilding them on every resume.
6. Reduce polling and move IME suggestion work off the hot path.

## Residual Risk

There is no automated test coverage in this module today, so regressions in these areas are most likely to show up only through manual device testing. The most sensitive flows are:
- returning home repeatedly from heavy apps
- opening settings and dragging appearance sliders for an extended time
- lock/unlock transitions on Android 13/14/15 devices
- high-volume notification apps
- fast typing and swipe typing in the custom IME
