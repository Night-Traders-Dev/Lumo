package dev.nighttraders.lumo.launcher.data

import androidx.datastore.preferences.core.stringSetPreferencesKey

internal object LauncherPreferences {
    val favoriteComponents = stringSetPreferencesKey("favorite_components")
}

