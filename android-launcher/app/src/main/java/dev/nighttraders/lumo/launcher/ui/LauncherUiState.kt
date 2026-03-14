package dev.nighttraders.lumo.launcher.ui

import dev.nighttraders.lumo.launcher.data.AppCategory
import dev.nighttraders.lumo.launcher.data.LaunchableApp
import dev.nighttraders.lumo.launcher.notifications.LauncherNotification

data class AppShelf(
    val category: AppCategory,
    val apps: List<LaunchableApp>,
)

data class LauncherUiState(
    val isLoading: Boolean = true,
    val apps: List<LaunchableApp> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
    val recentAppKeys: List<String> = emptyList(),
    val notifications: List<LauncherNotification> = emptyList(),
    val headsUpNotification: LauncherNotification? = null,
    val hasNotificationAccess: Boolean = false,
) {
    val favorites: List<LaunchableApp>
        get() = apps.filter { app -> favoriteKeys.contains(app.componentKey) }

    val featuredApps: List<LaunchableApp>
        get() = favorites.ifEmpty { apps.take(5) }.take(5)

    val recentApps: List<LaunchableApp>
        get() {
            val appsByKey = apps.associateBy { it.componentKey }
            return recentAppKeys.mapNotNull { key -> appsByKey[key] }
        }

    val activeNotificationCount: Int
        get() = notifications.size

    val recentNotifications: List<LauncherNotification>
        get() = notifications.take(5)

    val quickActions: List<LaunchableApp>
        get() {
            val preferred = listOf(
                AppCategory.Communication,
                AppCategory.Internet,
                AppCategory.Media,
                AppCategory.Productivity,
                AppCategory.System,
            )

            return buildList {
                addAll(favorites.take(2))
                preferred.forEach { category ->
                    apps.firstOrNull { app ->
                        app.category == category && none { existing -> existing.componentKey == app.componentKey }
                    }?.let(::add)
                }
            }.distinctBy { app -> app.componentKey }
                .take(5)
        }

    val shelves: List<AppShelf>
        get() = AppCategory.entries.mapNotNull { category ->
            val matches = apps.filter { app -> app.category == category }.take(8)
            if (matches.isEmpty()) {
                null
            } else {
                AppShelf(category = category, apps = matches)
            }
        }
}
