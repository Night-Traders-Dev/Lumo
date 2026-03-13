package dev.nighttraders.lumo.launcher.ui

import dev.nighttraders.lumo.launcher.data.LaunchableApp

data class LauncherUiState(
    val isLoading: Boolean = true,
    val apps: List<LaunchableApp> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
) {
    val favorites: List<LaunchableApp>
        get() = apps.filter { app -> favoriteKeys.contains(app.componentKey) }
}

