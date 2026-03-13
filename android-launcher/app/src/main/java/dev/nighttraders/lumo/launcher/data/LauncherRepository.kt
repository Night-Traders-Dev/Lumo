package dev.nighttraders.lumo.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

private const val ICON_SIZE_DP = 56

private val Context.launcherPreferences by preferencesDataStore(name = "lumo_launcher")

class LauncherRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    fun observeFavoriteKeys(): Flow<Set<String>> =
        context.launcherPreferences.data.map { preferences ->
            preferences[LauncherPreferences.favoriteComponents].orEmpty()
        }

    suspend fun loadApps(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val density = context.resources.displayMetrics.density
        val iconSizePx = (ICON_SIZE_DP * density).toInt()

        val launcherInfos = launcherApps
            ?.getActivityList(null, Process.myUserHandle())
            ?.map { info ->
                LaunchableApp(
                    componentKey = info.componentName.flattenToShortString(),
                    packageName = info.componentName.packageName,
                    className = info.componentName.className,
                    label = info.label?.toString().orEmpty(),
                    icon = info.getBadgedIcon(context.resources.displayMetrics.densityDpi)
                        ?.toBitmap(width = iconSizePx, height = iconSizePx),
                    accentSeed = info.componentName.packageName.hashCode(),
                )
            }
            .orEmpty()

        val apps = if (launcherInfos.isNotEmpty()) {
            launcherInfos
        } else {
            loadAppsFromPackageManager(iconSizePx)
        }

        apps
            .distinctBy { it.componentKey }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    suspend fun seedFavoritesIfEmpty(apps: List<LaunchableApp>) {
        context.launcherPreferences.edit { preferences ->
            val existing = preferences[LauncherPreferences.favoriteComponents].orEmpty()
            if (existing.isNotEmpty()) {
                return@edit
            }

            val seeded = chooseInitialFavorites(apps)
            if (seeded.isNotEmpty()) {
                preferences[LauncherPreferences.favoriteComponents] = seeded
            }
        }
    }

    suspend fun toggleFavorite(componentKey: String) {
        context.launcherPreferences.edit { preferences ->
            val current = preferences[LauncherPreferences.favoriteComponents].orEmpty().toMutableSet()
            if (!current.add(componentKey)) {
                current.remove(componentKey)
            }
            preferences[LauncherPreferences.favoriteComponents] = current
        }
    }

    fun launchApp(app: LaunchableApp): Result<Unit> {
        val componentName = ComponentName(app.packageName, app.className)

        launcherApps?.let {
            runCatching {
                it.startMainActivity(componentName, Process.myUserHandle(), null, null)
            }.onSuccess {
                return Result.success(Unit)
            }
        }

        return runCatching {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(componentName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
        }
    }

    private fun chooseInitialFavorites(apps: List<LaunchableApp>): Set<String> {
        val orderedMatches = buildList {
            favoriteMatchers.forEach { matcher ->
                apps.firstOrNull { matcher.matches(it) }?.let { add(it.componentKey) }
            }
        }

        if (orderedMatches.isNotEmpty()) {
            return orderedMatches.toSet()
        }

        return apps.take(5).mapTo(linkedSetOf()) { it.componentKey }
    }

    private fun loadAppsFromPackageManager(iconSizePx: Int): List<LaunchableApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return activities.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val componentName = ComponentName(activityInfo.packageName, activityInfo.name)
            LaunchableApp(
                componentKey = componentName.flattenToShortString(),
                packageName = activityInfo.packageName,
                className = activityInfo.name,
                label = resolveInfo.loadLabel(packageManager)?.toString().orEmpty(),
                icon = resolveInfo.loadIcon(packageManager)?.toBitmap(
                    width = iconSizePx,
                    height = iconSizePx,
                ),
                accentSeed = activityInfo.packageName.hashCode(),
            )
        }
    }

    private data class FavoriteMatcher(
        val packageHints: List<String>,
        val labelHints: List<String> = emptyList(),
    ) {
        fun matches(app: LaunchableApp): Boolean {
            val packageName = app.packageName.lowercase(Locale.getDefault())
            val label = app.label.lowercase(Locale.getDefault())
            return packageHints.any(packageName::contains) || labelHints.any(label::contains)
        }
    }

    private companion object {
        val favoriteMatchers = listOf(
            FavoriteMatcher(
                packageHints = listOf("dialer", "phone"),
                labelHints = listOf("phone", "dialer"),
            ),
            FavoriteMatcher(
                packageHints = listOf("message", "sms", "mms"),
                labelHints = listOf("message", "messages"),
            ),
            FavoriteMatcher(
                packageHints = listOf("camera"),
                labelHints = listOf("camera"),
            ),
            FavoriteMatcher(
                packageHints = listOf("browser", "chrome", "firefox"),
                labelHints = listOf("browser", "chrome", "firefox"),
            ),
            FavoriteMatcher(
                packageHints = listOf("settings"),
                labelHints = listOf("settings"),
            ),
        )
    }
}

