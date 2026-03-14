package dev.nighttraders.lumo.launcher.data

import android.graphics.Bitmap

enum class AppCategory(val label: String) {
    Communication("Communication"),
    Internet("Internet"),
    Media("Media"),
    Productivity("Productivity"),
    Tools("Tools"),
    System("System"),
    Other("Other"),
}

data class LaunchableApp(
    val componentKey: String,
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Bitmap?,
    val accentSeed: Int,
    val category: AppCategory,
    val taskId: Int = -1,
)

fun inferAppCategory(packageName: String, label: String): AppCategory {
    val normalizedPackage = packageName.lowercase()
    val normalizedLabel = label.lowercase()

    fun containsAny(vararg values: String): Boolean =
        values.any { value ->
            normalizedPackage.contains(value) || normalizedLabel.contains(value)
        }

    return when {
        containsAny("phone", "dialer", "message", "sms", "telegram", "whatsapp", "chat", "mail") ->
            AppCategory.Communication
        containsAny("browser", "chrome", "firefox", "web", "internet") ->
            AppCategory.Internet
        containsAny("camera", "gallery", "photo", "music", "video", "media", "spotify", "youtube") ->
            AppCategory.Media
        containsAny("calendar", "docs", "note", "drive", "task", "office", "keep") ->
            AppCategory.Productivity
        containsAny("settings", "systemui", "packageinstaller", "permission", "launcher") ->
            AppCategory.System
        containsAny("files", "clock", "calculator", "terminal", "tool", "manager", "recorder") ->
            AppCategory.Tools
        else -> AppCategory.Other
    }
}
