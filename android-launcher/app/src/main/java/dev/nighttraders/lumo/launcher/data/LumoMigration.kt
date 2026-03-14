package dev.nighttraders.lumo.launcher.data

import android.content.Context
import android.content.pm.PackageManager

/**
 * Handles version-based migrations on app startup.
 *
 * Clears the app cache when the version changes to avoid stale state,
 * while preserving all DataStore preferences (settings, security, wallpaper).
 */
object LumoMigration {
    private const val PREFS_NAME = "lumo_migration"
    private const val KEY_LAST_VERSION = "last_version_code"

    fun runIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentVersion = getCurrentVersionCode(context)
        val lastVersion = prefs.getInt(KEY_LAST_VERSION, -1)

        if (lastVersion == currentVersion) return

        LumoDebugLog.i("Migration", "Version changed: $lastVersion -> $currentVersion")

        // Clear the cache directory (bitmap caches, glide caches, etc.)
        context.cacheDir.deleteRecursively()
        context.codeCacheDir.deleteRecursively()
        LumoDebugLog.i("Migration", "Cleared cache directories")

        // DataStore prefs (settings, security hash/salt, wallpaper path, favorites)
        // are in files/datastore/ and are NOT cleared — they survive the update.

        prefs.edit().putInt(KEY_LAST_VERSION, currentVersion).apply()
        LumoDebugLog.i("Migration", "Migration complete for version $currentVersion")
    }

    private fun getCurrentVersionCode(context: Context): Int =
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                info.versionCode
            }
        }.getOrDefault(0)
}
