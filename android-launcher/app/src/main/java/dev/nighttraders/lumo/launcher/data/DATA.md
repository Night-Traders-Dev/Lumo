# Data / Core

Shared data layer — models, repository, preferences, debug logging, migration.

## Files

- `LauncherRepository.kt` — Central repository: app loading, favorites, recent apps (UsageStats + ActivityManager cross-reference), settings persistence via DataStore
- `LaunchableApp.kt` — Data class for launchable apps (componentKey, packageName, className, label, icon, accentSeed, category)
- `LauncherPreferences.kt` — DataStore preference keys (favorites, recents, settings, security)
- `LumoLauncherSettings.kt` — Settings data class with defaults (icon sizes, gesture toggles, thresholds, wallpaper path)
- `LumoDebugLog.kt` — In-memory debug log with `StateFlow<List<Entry>>`, color-coded levels (INFO, WARN, ERROR, DEBUG, FIX), known issues tracker
- `LumoMigration.kt` — Version migration: clears cache/code cache on update while preserving DataStore (settings, security)

## Features

- App loading via `LauncherApps` API with fallback to `PackageManager`
- Reactive settings via DataStore + Flow
- Recent apps from system UsageStats cross-referenced with `ActivityManager.getRecentTasks()` (2-hour window)
- Favorite app management with initial seeding heuristics
- Debug logging with structured entries and known-issue tracking
- Automatic cache clearing on launcher version update

## Known Issues / Bugs

- `getRecentTasks()` is deprecated but still works for launcher (HOME category) apps
- UsageStats requires explicit user permission grant

## Plans

- App category inference improvements
- Search indexing for faster app filtering
