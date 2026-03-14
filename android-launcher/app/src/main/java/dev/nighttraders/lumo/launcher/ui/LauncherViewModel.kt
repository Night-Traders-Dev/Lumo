package dev.nighttraders.lumo.launcher.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.nighttraders.lumo.launcher.data.LaunchableApp
import dev.nighttraders.lumo.launcher.data.LauncherPreferences
import dev.nighttraders.lumo.launcher.data.LauncherRepository
import dev.nighttraders.lumo.launcher.data.LumoLauncherSettings
import dev.nighttraders.lumo.launcher.notifications.LauncherNotification
import dev.nighttraders.lumo.launcher.notifications.LauncherNotificationCenter
import dev.nighttraders.lumo.launcher.notifications.LumoNotificationListenerService
import dev.nighttraders.lumo.launcher.notifications.hasNotificationListenerAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LauncherRepository(application.applicationContext)
    private val loading = MutableStateFlow(true)
    private val apps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    private var appsLoadedOnce = false
    private var appRefreshJob: kotlinx.coroutines.Job? = null
    private val favoriteKeys = repository.observeFavoriteKeys()
    private val orderedFavoriteKeys = repository.observeOrderedFavoriteKeys()
    private val recentAppKeys = repository.recentAppKeysFlow
    private val defaultHome = MutableStateFlow(false)
    private val notificationAccess = MutableStateFlow(application.hasNotificationListenerAccess())
    private val notifications = LauncherNotificationCenter.notifications
    private val headsUpNotification = LauncherNotificationCenter.headsUpNotification

    val uiState: StateFlow<LauncherUiState> = combine(
        loading,
        apps,
        favoriteKeys,
        notifications,
    ) { isLoading, installedApps, favorites, activeNotifications ->
        LauncherUiState(
            isLoading = isLoading,
            apps = installedApps,
            favoriteKeys = favorites,
            notifications = activeNotifications,
        )
    }.combine(headsUpNotification) { state, headsUp ->
        state.copy(headsUpNotification = headsUp)
    }.combine(notificationAccess) { state, hasAccess ->
        state.copy(hasNotificationAccess = hasAccess)
    }.combine(recentAppKeys) { state, recentKeys ->
        state.copy(recentAppKeys = recentKeys)
    }.combine(orderedFavoriteKeys) { state, ordered ->
        state.copy(orderedFavoriteKeys = ordered)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LauncherUiState(),
    )

    val isDefaultHome: StateFlow<Boolean> = defaultHome

    val launcherSettings: StateFlow<LumoLauncherSettings> = repository.observeLauncherSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LumoLauncherSettings(),
        )

    fun <T> updateSetting(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        viewModelScope.launch { repository.updateSetting(key, value) }
    }

    init {
        LauncherNotificationCenter.setAccessEnabled(notificationAccess.value)
        refreshNotifications()
        // Load apps first, THEN recents — recents resolution needs the apps list populated
        viewModelScope.launch {
            loading.value = true
            val installedApps = repository.loadApps()
            repository.seedFavoritesIfEmpty(installedApps)
            apps.value = installedApps
            loading.value = false
            appsLoadedOnce = true
            // Now that apps are loaded, resolve recent app keys
            launch(Dispatchers.IO) { repository.refreshRecentApps() }
        }
        startPeriodicRefresh()
    }

    /**
     * Reload the installed app list. If [force] is false and apps have already been loaded,
     * this is a no-op — avoids expensive icon re-rasterization on every Activity resume.
     */
    fun refreshApps(force: Boolean = false) {
        if (appsLoadedOnce && !force) return
        // Cancel any in-flight refresh (coalesces burst package-change events)
        appRefreshJob?.cancel()
        appRefreshJob = viewModelScope.launch {
            if (force) delay(300) // Debounce burst package-change broadcasts
            loading.value = true
            val installedApps = repository.loadApps()
            repository.seedFavoritesIfEmpty(installedApps)
            apps.value = installedApps
            loading.value = false
            appsLoadedOnce = true
        }
    }

    fun toggleFavorite(app: LaunchableApp) {
        viewModelScope.launch {
            repository.toggleFavorite(app.componentKey)
        }
    }

    fun addFavorite(componentKey: String) {
        viewModelScope.launch {
            repository.addFavorite(componentKey)
        }
    }

    fun reorderFavorites(orderedKeys: List<String>) {
        viewModelScope.launch {
            repository.reorderFavorites(orderedKeys)
        }
    }

    fun launchApp(app: LaunchableApp): Result<Unit> {
        val result = repository.launchApp(app)
        if (result.isSuccess) {
            viewModelScope.launch {
                repository.recordRecentApp(app.componentKey)
            }
        }
        return result
    }

    fun openAppInfo(app: LaunchableApp): Result<Unit> = repository.openAppInfo(app)

    fun requestUninstall(app: LaunchableApp): Result<Unit> = repository.requestUninstall(app)

    fun openNotification(notification: LauncherNotification): Result<Unit> {
        // First try the notification's own contentIntent — this deep links to the
        // specific content (e.g., the SMS conversation thread, not just the SMS app)
        val directOpen = LumoNotificationListenerService.openNotification(notification.key)
        if (directOpen.getOrNull() == true) {
            return Result.success(Unit)
        }
        // Fallback: launch the app's main activity and dismiss the notification
        val launchResult = repository.launchPackage(notification.packageName)
        if (launchResult.isFailure) {
            // Last resort: try a simple launch intent via PackageManager
            val ctx = getApplication<android.app.Application>()
            val launchIntent = ctx.packageManager.getLaunchIntentForPackage(notification.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                )
                return runCatching { ctx.startActivity(launchIntent) }
            }
            return launchResult
        }
        LumoNotificationListenerService.dismissNotification(notification.key)
        return launchResult
    }

    fun openNotificationApp(notification: LauncherNotification): Result<Unit> {
        val launchResult = repository.launchPackage(notification.packageName)
        if (launchResult.isSuccess) {
            LumoNotificationListenerService.dismissNotification(notification.key)
        }
        return launchResult
    }

    fun dismissNotification(notification: LauncherNotification): Result<Unit> =
        LumoNotificationListenerService.dismissNotification(notification.key)

    fun snoozeNotification(
        notification: LauncherNotification,
        durationMillis: Long,
    ): Result<Unit> = LumoNotificationListenerService.snoozeNotification(notification.key, durationMillis)

    fun dismissHeadsUpNotification(key: String) {
        LauncherNotificationCenter.dismissHeadsUp(key)
    }

    fun updateDefaultHomeStatus(isDefault: Boolean) {
        defaultHome.update { isDefault }
    }

    fun updateNotificationAccessStatus(hasAccess: Boolean) {
        notificationAccess.update { hasAccess }
        LauncherNotificationCenter.setAccessEnabled(hasAccess)
        if (hasAccess) {
            refreshNotifications()
        }
    }

    fun refreshNotifications() {
        LumoNotificationListenerService.requestRefresh()
    }

    fun refreshRecentApps() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.refreshRecentApps()
        }
    }

    /**
     * Periodic background refresh using a coroutine-based non-blocking timer.
     * Keeps recent apps and notifications in sync with the system.
     * - Recent apps: every 5 seconds (checks actual system usage)
     * - Notifications: triggered by listener service's own 3-second sync
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(RECENT_APPS_REFRESH_MS)
                repository.refreshRecentApps()
            }
        }
    }

    companion object {
        private const val RECENT_APPS_REFRESH_MS = 15_000L // 15s backstop — events handle most updates

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        ?: error("Application is required for LauncherViewModel")
                LauncherViewModel(application)
            }
        }
    }
}
