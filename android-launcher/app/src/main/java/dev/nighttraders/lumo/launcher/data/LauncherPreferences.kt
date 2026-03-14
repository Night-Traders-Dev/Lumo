package dev.nighttraders.lumo.launcher.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal object LauncherPreferences {
    val favoriteComponents = stringSetPreferencesKey("favorite_components")
    val favoriteOrder = stringPreferencesKey("favorite_order") // pipe-separated ordered keys
    val recentAppKeys = stringPreferencesKey("recent_app_keys")
    val overlaySidebarEnabled = booleanPreferencesKey("overlay_sidebar_enabled")
    val lockScreenCompanionEnabled = booleanPreferencesKey("lock_screen_companion_enabled")
    val lockScreenSecurityType = stringPreferencesKey("lock_screen_security_type")
    val lockScreenSecurityHash = stringPreferencesKey("lock_screen_security_hash")
    val lockScreenSecuritySalt = stringPreferencesKey("lock_screen_security_salt")

    // Appearance
    val appIconSizeDp = intPreferencesKey("app_icon_size_dp")             // default 56
    val appGridColumns = intPreferencesKey("app_grid_columns")             // default 4
    val dashRailWidthDp = intPreferencesKey("dash_rail_width_dp")          // default 68
    val dashIconSizeDp = intPreferencesKey("dash_icon_size_dp")           // default 52
    val wallpaperPath = stringPreferencesKey("wallpaper_path")            // "" = gradient, "asset:X" or file URI

    // Gestures — toggles
    val bottomEdgeGestureEnabled = booleanPreferencesKey("bottom_edge_gesture_enabled")
    val leftEdgeGestureEnabled = booleanPreferencesKey("left_edge_gesture_enabled")
    val multitaskGestureEnabled = booleanPreferencesKey("multitask_gesture_enabled")
    val indicatorSwipeEnabled = booleanPreferencesKey("indicator_swipe_enabled")

    // Gestures — sensitivity / dimensions
    val bottomEdgeHeightDp = intPreferencesKey("bottom_edge_height_dp")        // default 70
    val bottomEdgeThresholdDp = intPreferencesKey("bottom_edge_threshold_dp")  // default 42
    val leftEdgeWidthDp = intPreferencesKey("left_edge_width_dp")              // default 20
    val leftEdgeThresholdDp = intPreferencesKey("left_edge_threshold_dp")      // default 24
    val horizontalSwipeThresholdDp = intPreferencesKey("horizontal_swipe_threshold_dp") // default 80
    val multitaskSwipeThresholdDp = intPreferencesKey("multitask_swipe_threshold_dp")   // default 80
}
