package dev.nighttraders.lumo.launcher.data

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Locale

private const val ICON_SIZE_DP = 56
private const val MAX_RECENT_APPS = 12
private const val RECENT_SEPARATOR = "|"
private const val FAVORITE_SEPARATOR = "|"

private val Context.launcherPreferences by preferencesDataStore(name = "lumo_launcher")

class LauncherRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
    private val activityManager = context.getSystemService(ActivityManager::class.java)

    /** Reactive recent app keys — updated by periodic polling or manual recording. */
    private val _recentAppKeys = MutableStateFlow(loadSavedRecentKeys())
    val recentAppKeysFlow: StateFlow<List<String>> = _recentAppKeys.asStateFlow()

    /** Per-task recent apps with task IDs for proper resume/dismiss. */
    private val _recentTasks = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val recentTasksFlow: StateFlow<List<LaunchableApp>> = _recentTasks.asStateFlow()

    /** Seed recent keys from DataStore synchronously so there's no empty-list gap at startup. */
    private fun loadSavedRecentKeys(): List<String> = runBlocking {
        val raw = context.launcherPreferences.data.first()[LauncherPreferences.recentAppKeys].orEmpty()
        if (raw.isBlank()) emptyList() else raw.split(RECENT_SEPARATOR)
    }

    fun observeFavoriteKeys(): Flow<Set<String>> =
        context.launcherPreferences.data.map { preferences ->
            preferences[LauncherPreferences.favoriteComponents].orEmpty()
        }

    /** Ordered favorite keys — preserves user-defined order for the dash rail. */
    fun observeOrderedFavoriteKeys(): Flow<List<String>> =
        context.launcherPreferences.data.map { preferences ->
            val order = preferences[LauncherPreferences.favoriteOrder].orEmpty()
            val set = preferences[LauncherPreferences.favoriteComponents].orEmpty()
            if (order.isBlank()) {
                set.toList()
            } else {
                val ordered = order.split(FAVORITE_SEPARATOR).filter { it in set }
                // Append any keys in the set that are not yet in the order list
                val remaining = set - ordered.toSet()
                ordered + remaining
            }
        }

    fun observeOverlaySidebarEnabled(): Flow<Boolean> =
        context.launcherPreferences.data.map { preferences ->
            preferences[LauncherPreferences.overlaySidebarEnabled] ?: false
        }

    fun observeLockScreenCompanionEnabled(): Flow<Boolean> =
        context.launcherPreferences.data.map { preferences ->
            preferences[LauncherPreferences.lockScreenCompanionEnabled] ?: false
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
                    category = inferAppCategory(
                        packageName = info.componentName.packageName,
                        label = info.label?.toString().orEmpty(),
                    ),
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

    suspend fun loadRailApps(limit: Int = 6): List<LaunchableApp> {
        val installedApps = loadApps()
        val favoriteKeys = observeFavoriteKeys().first()
        val preferredKeys = favoriteKeys.ifEmpty { chooseInitialFavorites(installedApps) }

        val favorites = installedApps.filter { app ->
            preferredKeys.contains(app.componentKey)
        }

        return favorites.ifEmpty { installedApps.take(limit) }.take(limit)
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
            val order = preferences[LauncherPreferences.favoriteOrder].orEmpty()
                .split(FAVORITE_SEPARATOR).filter { it.isNotBlank() }.toMutableList()
            if (!current.add(componentKey)) {
                current.remove(componentKey)
                order.remove(componentKey)
            } else {
                order.add(componentKey)
            }
            preferences[LauncherPreferences.favoriteComponents] = current
            preferences[LauncherPreferences.favoriteOrder] = order.joinToString(FAVORITE_SEPARATOR)
        }
    }

    suspend fun addFavorite(componentKey: String) {
        context.launcherPreferences.edit { preferences ->
            val current = preferences[LauncherPreferences.favoriteComponents].orEmpty().toMutableSet()
            val order = preferences[LauncherPreferences.favoriteOrder].orEmpty()
                .split(FAVORITE_SEPARATOR).filter { it.isNotBlank() }.toMutableList()
            if (current.add(componentKey)) {
                order.add(componentKey)
                preferences[LauncherPreferences.favoriteComponents] = current
                preferences[LauncherPreferences.favoriteOrder] = order.joinToString(FAVORITE_SEPARATOR)
            }
        }
    }

    suspend fun reorderFavorites(orderedKeys: List<String>) {
        context.launcherPreferences.edit { preferences ->
            preferences[LauncherPreferences.favoriteOrder] = orderedKeys.joinToString(FAVORITE_SEPARATOR)
        }
    }

    suspend fun isOverlaySidebarEnabled(): Boolean =
        context.launcherPreferences.data.first()[LauncherPreferences.overlaySidebarEnabled] ?: false

    suspend fun setOverlaySidebarEnabled(enabled: Boolean) {
        context.launcherPreferences.edit { preferences ->
            preferences[LauncherPreferences.overlaySidebarEnabled] = enabled
        }
    }

    suspend fun isLockScreenCompanionEnabled(): Boolean =
        context.launcherPreferences.data.first()[LauncherPreferences.lockScreenCompanionEnabled] ?: false

    suspend fun setLockScreenCompanionEnabled(enabled: Boolean) {
        context.launcherPreferences.edit { preferences ->
            preferences[LauncherPreferences.lockScreenCompanionEnabled] = enabled
        }
    }

    suspend fun getLockScreenSecurityType(): String =
        context.launcherPreferences.data.first()[LauncherPreferences.lockScreenSecurityType] ?: "none"

    suspend fun getLockScreenSecurityHash(): String =
        context.launcherPreferences.data.first()[LauncherPreferences.lockScreenSecurityHash].orEmpty()

    suspend fun getLockScreenSecuritySalt(): String =
        context.launcherPreferences.data.first()[LauncherPreferences.lockScreenSecuritySalt].orEmpty()

    suspend fun setLockScreenSecurity(type: String, hash: String, salt: String) {
        context.launcherPreferences.edit { preferences ->
            preferences[LauncherPreferences.lockScreenSecurityType] = type
            preferences[LauncherPreferences.lockScreenSecurityHash] = hash
            preferences[LauncherPreferences.lockScreenSecuritySalt] = salt
        }
    }

    suspend fun clearLockScreenSecurity() {
        context.launcherPreferences.edit { preferences ->
            preferences[LauncherPreferences.lockScreenSecurityType] = "none"
            preferences.remove(LauncherPreferences.lockScreenSecurityHash)
            preferences.remove(LauncherPreferences.lockScreenSecuritySalt)
        }
    }

    // ── Appearance & gesture settings ──────────────────────────────────

    fun observeLauncherSettings(): Flow<LumoLauncherSettings> =
        context.launcherPreferences.data.map { prefs ->
            LumoLauncherSettings(
                appIconSizeDp = prefs[LauncherPreferences.appIconSizeDp] ?: 56,
                appGridColumns = prefs[LauncherPreferences.appGridColumns] ?: 4,
                dashRailWidthDp = prefs[LauncherPreferences.dashRailWidthDp] ?: 68,
                dashIconSizeDp = prefs[LauncherPreferences.dashIconSizeDp] ?: 52,
                wallpaperPath = prefs[LauncherPreferences.wallpaperPath] ?: "",
                bottomEdgeGestureEnabled = prefs[LauncherPreferences.bottomEdgeGestureEnabled] ?: true,
                leftEdgeGestureEnabled = prefs[LauncherPreferences.leftEdgeGestureEnabled] ?: true,
                multitaskGestureEnabled = prefs[LauncherPreferences.multitaskGestureEnabled] ?: true,
                indicatorSwipeEnabled = prefs[LauncherPreferences.indicatorSwipeEnabled] ?: true,
                bottomEdgeHeightDp = prefs[LauncherPreferences.bottomEdgeHeightDp] ?: 70,
                bottomEdgeThresholdDp = prefs[LauncherPreferences.bottomEdgeThresholdDp] ?: 42,
                leftEdgeWidthDp = prefs[LauncherPreferences.leftEdgeWidthDp] ?: 20,
                leftEdgeThresholdDp = prefs[LauncherPreferences.leftEdgeThresholdDp] ?: 24,
                horizontalSwipeThresholdDp = prefs[LauncherPreferences.horizontalSwipeThresholdDp] ?: 80,
                multitaskSwipeThresholdDp = prefs[LauncherPreferences.multitaskSwipeThresholdDp] ?: 80,
            )
        }

    suspend fun <T> updateSetting(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.launcherPreferences.edit { it[key] = value }
    }

    fun openAppInfo(app: LaunchableApp): Result<Unit> = runCatching {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun requestUninstall(app: LaunchableApp): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_DELETE)
            .setData(Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Record a launched app. Persists to DataStore and updates the reactive flow immediately.
     */
    suspend fun recordRecentApp(componentKey: String) {
        context.launcherPreferences.edit { preferences ->
            val raw = preferences[LauncherPreferences.recentAppKeys].orEmpty()
            val current = if (raw.isBlank()) mutableListOf() else raw.split(RECENT_SEPARATOR).toMutableList()
            current.remove(componentKey)
            current.add(0, componentKey)
            val updated = current.take(MAX_RECENT_APPS)
            preferences[LauncherPreferences.recentAppKeys] =
                updated.joinToString(RECENT_SEPARATOR)
            _recentAppKeys.value = updated
        }
    }

    /**
     * Observe recent app keys from DataStore (legacy flow, kept for compatibility).
     */
    fun observeRecentAppKeys(): Flow<List<String>> =
        context.launcherPreferences.data.map { preferences ->
            val raw = preferences[LauncherPreferences.recentAppKeys].orEmpty()
            if (raw.isBlank()) emptyList() else raw.split(RECENT_SEPARATOR)
        }

    /**
     * Check if the app has usage stats permission.
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Query the system for actual recent tasks (per-task, not per-package).
     *
     * Uses getRecentTasks() which returns per-task entries. For ordering,
     * the task list from ActivityManager is already ordered by recency.
     * UsageStats is used as a secondary signal when available.
     */
    suspend fun loadRecentTasks(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val density = context.resources.displayMetrics.density
        val iconSizePx = (ICON_SIZE_DP * density).toInt()

        @Suppress("DEPRECATION")
        val recentTasks = runCatching {
            activityManager?.getRecentTasks(MAX_RECENT_APPS, ActivityManager.RECENT_WITH_EXCLUDED)
        }.getOrNull().orEmpty()

        val taskApps = recentTasks.mapNotNull { task ->
            val component = task.baseIntent?.component ?: return@mapNotNull null
            val pkg = component.packageName
            if (pkg == context.packageName) return@mapNotNull null

            val info = launcherApps?.getActivityList(pkg, Process.myUserHandle())
                ?.firstOrNull()
                ?: return@mapNotNull null

            LaunchableApp(
                componentKey = info.componentName.flattenToShortString(),
                packageName = info.componentName.packageName,
                className = info.componentName.className,
                label = info.label?.toString().orEmpty(),
                icon = info.getBadgedIcon(context.resources.displayMetrics.densityDpi)
                    ?.toBitmap(width = iconSizePx, height = iconSizePx),
                accentSeed = info.componentName.packageName.hashCode(),
                category = inferAppCategory(
                    packageName = info.componentName.packageName,
                    label = info.label?.toString().orEmpty(),
                ),
                taskId = @Suppress("DEPRECATION") task.id,
            )
        }

        // Update the legacy key flow for any consumers still using it
        _recentAppKeys.value = taskApps.map { it.componentKey }
        // Update the task-level flow
        _recentTasks.value = taskApps
        return@withContext taskApps
    }

    /**
     * Refresh recent tasks from the system. Updates both reactive flows.
     */
    suspend fun refreshRecentApps() {
        loadRecentTasks()
    }

    /**
     * Resume an existing task by moving it to the foreground.
     * Returns success if the task was moved, failure if it needs to be launched fresh.
     */
    fun resumeTask(taskId: Int): Result<Unit> {
        if (taskId < 0) return Result.failure(IllegalArgumentException("No task ID"))
        return runCatching {
            activityManager?.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
                ?: throw IllegalStateException("No ActivityManager")
        }
    }

    /**
     * Remove a task from the local recents list.
     * Android does not expose a public API for third-party launchers to remove
     * other apps' tasks from the system recents, so we only hide it locally.
     * The system recents will update on the next refresh cycle.
     */
    fun removeTask(taskId: Int) {
        if (taskId < 0) return
        _recentTasks.value = _recentTasks.value.filter { it.taskId != taskId }
        _recentAppKeys.value = _recentTasks.value.map { it.componentKey }
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

    fun launchPackage(packageName: String): Result<Unit> {
        launcherApps
            ?.getActivityList(packageName, Process.myUserHandle())
            ?.firstOrNull()
            ?.let { info ->
                return launchApp(
                    LaunchableApp(
                        componentKey = info.componentName.flattenToShortString(),
                        packageName = info.componentName.packageName,
                        className = info.componentName.className,
                        label = info.label?.toString().orEmpty(),
                        icon = null,
                        accentSeed = info.componentName.packageName.hashCode(),
                        category = inferAppCategory(
                            packageName = info.componentName.packageName,
                            label = info.label?.toString().orEmpty(),
                        ),
                    ),
                )
            }

        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        return if (intent != null) {
            runCatching { context.startActivity(intent) }
        } else {
            Result.failure(IllegalArgumentException("No launchable activity found for $packageName"))
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
                category = inferAppCategory(
                    packageName = activityInfo.packageName,
                    label = resolveInfo.loadLabel(packageManager)?.toString().orEmpty(),
                ),
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
