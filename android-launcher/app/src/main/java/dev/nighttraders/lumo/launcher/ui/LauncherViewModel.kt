package dev.nighttraders.lumo.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.nighttraders.lumo.launcher.data.LaunchableApp
import dev.nighttraders.lumo.launcher.data.LauncherRepository
import dev.nighttraders.lumo.launcher.notifications.LauncherNotification
import dev.nighttraders.lumo.launcher.notifications.LauncherNotificationCenter
import dev.nighttraders.lumo.launcher.notifications.LumoNotificationListenerService
import dev.nighttraders.lumo.launcher.notifications.hasNotificationListenerAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LauncherRepository(application.applicationContext)
    private val loading = MutableStateFlow(true)
    private val apps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    private val favoriteKeys = repository.observeFavoriteKeys()
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState(),
    )

    val isDefaultHome: StateFlow<Boolean> = defaultHome

    init {
        LauncherNotificationCenter.setAccessEnabled(notificationAccess.value)
        refreshApps()
        refreshNotifications()
    }

    fun refreshApps() {
        viewModelScope.launch {
            loading.value = true
            val installedApps = repository.loadApps()
            repository.seedFavoritesIfEmpty(installedApps)
            apps.value = installedApps
            loading.value = false
        }
    }

    fun toggleFavorite(app: LaunchableApp) {
        viewModelScope.launch {
            repository.toggleFavorite(app.componentKey)
        }
    }

    fun launchApp(app: LaunchableApp): Result<Unit> = repository.launchApp(app)

    fun openNotification(notification: LauncherNotification): Result<Unit> {
        val directOpen = LumoNotificationListenerService.openNotification(notification.key)
        if (directOpen.getOrNull() == true) {
            return Result.success(Unit)
        }
        return repository.launchPackage(notification.packageName)
    }

    fun openNotificationApp(notification: LauncherNotification): Result<Unit> =
        repository.launchPackage(notification.packageName)

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

    companion object {
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
