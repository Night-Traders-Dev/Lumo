package dev.nighttraders.lumo.launcher.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Locale

class LumoNotificationListenerService : NotificationListenerService() {
    override fun onCreate() {
        super.onCreate()
        listenerInstance = this
        LauncherNotificationCenter.setAccessEnabled(applicationContext.hasNotificationListenerAccess())
    }

    override fun onDestroy() {
        if (listenerInstance === this) {
            listenerInstance = null
        }
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        listenerInstance = this
        LauncherNotificationCenter.setAccessEnabled(true)
        refreshFromActiveNotifications()
    }

    override fun onListenerDisconnected() {
        LauncherNotificationCenter.setAccessEnabled(applicationContext.hasNotificationListenerAccess())
        if (listenerInstance === this) {
            listenerInstance = null
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        sbn.toLauncherNotification(applicationContext)?.let { notification ->
            LauncherNotificationCenter.upsert(
                notification = notification,
                alert = shouldShowHeadsUp(sbn),
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        LauncherNotificationCenter.remove(sbn.key)
    }

    private fun refreshFromActiveNotifications() {
        val notifications = activeNotifications
            ?.mapNotNull { it.toLauncherNotification(applicationContext) }
            .orEmpty()
        LauncherNotificationCenter.replaceAll(notifications)
    }

    private fun shouldShowHeadsUp(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return false
        }
        if ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) {
            return false
        }
        if (notification.category == Notification.CATEGORY_SERVICE) {
            return false
        }

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = preferredText(extras)
        return title.isNotBlank() || text.isNotBlank()
    }

    companion object {
        private var listenerInstance: LumoNotificationListenerService? = null

        fun requestRefresh() {
            listenerInstance?.refreshFromActiveNotifications()
        }

        fun openNotification(key: String): Result<Boolean> {
            val listener = listenerInstance
                ?: return Result.failure(IllegalStateException("Notification listener is not connected"))
            val sbn = listener.activeNotifications?.firstOrNull { it.key == key }
                ?: return Result.success(false)
            val intent = sbn.notification.contentIntent ?: sbn.notification.fullScreenIntent
                ?: return Result.success(false)

            return runCatching {
                intent.send()
                LauncherNotificationCenter.dismissHeadsUp(key)
                true
            }
        }

        fun dismissNotification(key: String): Result<Unit> {
            val listener = listenerInstance
                ?: return Result.failure(IllegalStateException("Notification listener is not connected"))
            val sbn = listener.activeNotifications?.firstOrNull { it.key == key }
                ?: return Result.success(Unit)

            return runCatching {
                listener.cancelNotification(sbn.key)
                LauncherNotificationCenter.remove(key)
            }
        }

        fun snoozeNotification(key: String, durationMillis: Long): Result<Unit> {
            val listener = listenerInstance
                ?: return Result.failure(IllegalStateException("Notification listener is not connected"))
            val sbn = listener.activeNotifications?.firstOrNull { it.key == key }
                ?: return Result.success(Unit)

            return runCatching {
                listener.snoozeNotification(sbn.key, durationMillis)
                LauncherNotificationCenter.remove(key)
            }
        }
    }
}

fun Context.hasNotificationListenerAccess(): Boolean {
    val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        ?: return false
    return listeners
        .split(':')
        .mapNotNull(ComponentName::unflattenFromString)
        .any { componentName ->
            componentName.packageName == packageName
        }
}

private fun StatusBarNotification.toLauncherNotification(context: Context): LauncherNotification? {
    val notification = notification
    if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
        return null
    }

    val extras = notification.extras
    val appLabel = runCatching {
        val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(applicationInfo)?.toString()
    }.getOrNull().orEmpty().ifBlank { packageName }

    val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
        ?.toString()
        .orEmpty()
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        ?.toString()
        .orEmpty()
    val message = preferredText(extras)
    val bestTitle = conversationTitle.ifBlank { title }.ifBlank { appLabel }

    if (bestTitle.isBlank() && message.isBlank()) {
        return null
    }

    return LauncherNotification(
        key = key,
        packageName = packageName,
        appLabel = appLabel,
        title = bestTitle,
        message = message,
        postedAt = postTime,
        isMessaging = isMessagingNotification(notification, packageName, appLabel),
    )
}

private fun preferredText(extras: android.os.Bundle): String {
    val primary = extras.getCharSequence(Notification.EXTRA_TEXT)
        ?.toString()
        .orEmpty()
    if (primary.isNotBlank()) {
        return primary
    }

    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        ?.toString()
        .orEmpty()
    if (bigText.isNotBlank()) {
        return bigText
    }

    return extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        ?.toString()
        .orEmpty()
}

private fun isMessagingNotification(
    notification: Notification,
    packageName: String,
    appLabel: String,
): Boolean {
    val packageHint = packageName.lowercase(Locale.getDefault())
    val labelHint = appLabel.lowercase(Locale.getDefault())
    return notification.category == Notification.CATEGORY_MESSAGE ||
        packageHint.contains("message") ||
        packageHint.contains("sms") ||
        packageHint.contains("mms") ||
        packageHint.contains("whatsapp") ||
        packageHint.contains("telegram") ||
        labelHint.contains("message")
}
