package dev.nighttraders.lumo.launcher.notifications

data class LauncherNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val message: String,
    val postedAt: Long,
    val isMessaging: Boolean,
)
