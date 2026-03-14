package dev.nighttraders.lumo.launcher.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal object LauncherPreferences {
    val favoriteComponents = stringSetPreferencesKey("favorite_components")
    val overlaySidebarEnabled = booleanPreferencesKey("overlay_sidebar_enabled")
    val lockScreenCompanionEnabled = booleanPreferencesKey("lock_screen_companion_enabled")
}
