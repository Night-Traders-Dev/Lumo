package dev.nighttraders.lumo.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.nighttraders.lumo.launcher.data.LaunchableApp
import dev.nighttraders.lumo.launcher.data.LauncherRepository
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

    val uiState: StateFlow<LauncherUiState> = combine(
        loading,
        apps,
        favoriteKeys,
    ) { isLoading, installedApps, favorites ->
        LauncherUiState(
            isLoading = isLoading,
            apps = installedApps,
            favoriteKeys = favorites,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState(),
    )

    val isDefaultHome: StateFlow<Boolean> = defaultHome

    init {
        refreshApps()
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

    fun updateDefaultHomeStatus(isDefault: Boolean) {
        defaultHome.update { isDefault }
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
