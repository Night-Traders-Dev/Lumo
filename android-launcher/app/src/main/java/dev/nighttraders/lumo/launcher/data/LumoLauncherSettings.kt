package dev.nighttraders.lumo.launcher.data

data class LumoLauncherSettings(
    // Appearance
    val appIconSizeDp: Int = 56,
    val appGridColumns: Int = 4,
    val dashRailWidthDp: Int = 68,
    val dashIconSizeDp: Int = 52,
    val wallpaperPath: String = "",  // "" = default gradient, "asset:filename" or content URI

    // Gesture toggles
    val bottomEdgeGestureEnabled: Boolean = true,
    val leftEdgeGestureEnabled: Boolean = true,
    val multitaskGestureEnabled: Boolean = true,
    val indicatorSwipeEnabled: Boolean = true,

    // Gesture sensitivity / dimensions
    val bottomEdgeHeightDp: Int = 70,
    val bottomEdgeThresholdDp: Int = 42,
    val leftEdgeWidthDp: Int = 20,
    val leftEdgeThresholdDp: Int = 24,
    val horizontalSwipeThresholdDp: Int = 80,
    val multitaskSwipeThresholdDp: Int = 80,
)
