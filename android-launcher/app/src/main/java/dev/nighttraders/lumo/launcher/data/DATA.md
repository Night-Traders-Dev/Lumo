# Data / Core

Shared data layer — models, repository, preferences, debug logging, migration.

## Files

- `LauncherRepository.kt` — Central repository: app loading, ordered favorites (pipe-separated persistence), recent apps (UsageStats + ActivityManager cross-reference), settings persistence via DataStore, lock screen security, app launching
- `LaunchableApp.kt` — Data class for launchable apps (componentKey, packageName, className, label, icon, accentSeed, category) + `AppCategory` enum with `inferAppCategory()` pattern matching
- `LauncherPreferences.kt` — DataStore preference keys (favorites set + ordered favorites, recents, settings, security type/hash/salt, gesture toggles, edge dimensions)
- `LumoLauncherSettings.kt` — Settings data class with defaults (icon sizes, grid columns, rail width, wallpaper, gesture toggles and thresholds)
- `LumoDebugLog.kt` — In-memory debug log with `StateFlow<List<Entry>>`, color-coded levels (INFO, WARN, ERROR, DEBUG, FIX), known issues tracker
- `LumoMigration.kt` — Version migration: clears cache/code cache on update while preserving DataStore (settings, security)

## Features

- App loading via `LauncherApps` API with fallback to `PackageManager`
- Reactive settings via DataStore + Flow
- Ordered favorites: persisted as pipe-separated string (`favoriteOrder` key), maintains user-defined drag-and-drop order
- Favorite management: `toggleFavorite()`, `addFavorite()`, `reorderFavorites()` — all maintain both the set and ordered list
- Initial favorite seeding: heuristic matching for Phone, Messages, Camera, Browser, Settings
- Recent apps from system UsageStats cross-referenced with `ActivityManager.getRecentTasks()` (2-hour window, max 12)
- Lock screen security: `getLockScreenSecurityType/Hash/Salt()`, `setLockScreenSecurity()`, `clearLockScreenSecurity()`
- App launching with fallback chain (component intent → package launch intent)
- Debug logging with structured entries and known-issue tracking
- Automatic cache clearing on launcher version update (preserves DataStore)

## Known Issues / Bugs

- `getRecentTasks()` is deprecated but still works for launcher (HOME category) apps
- UsageStats requires explicit user permission grant

## Plans

- App category inference improvements
- Search indexing for faster app filtering
