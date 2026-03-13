package dev.nighttraders.lumo.launcher.data

import android.graphics.Bitmap

data class LaunchableApp(
    val componentKey: String,
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Bitmap?,
    val accentSeed: Int,
)

