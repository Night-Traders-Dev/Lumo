package dev.nighttraders.lumo.launcher.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal object LauncherPreferences {
    val favoriteComponents = stringSetPreferencesKey("favorite_components")
    val recentAppKeys = stringPreferencesKey("recent_app_keys")
    val overlaySidebarEnabled = booleanPreferencesKey("overlay_sidebar_enabled")
    val lockScreenCompanionEnabled = booleanPreferencesKey("lock_screen_companion_enabled")
}
