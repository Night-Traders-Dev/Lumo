package dev.nighttraders.lumo.launcher.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class LumoNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        listenerInstance = this
        LauncherNotificationCenter.setAccessEnabled(applicationContext.hasNotificationListenerAccess())
    }

    override fun onDestroy() {
        syncJob?.cancel()
        serviceScope.cancel()
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
        startPeriodicSync()
    }

    override fun onListenerDisconnected() {
        syncJob?.cancel()
        syncJob = null
        LauncherNotificationCenter.setAccessEnabled(applicationContext.hasNotificationListenerAccess())
        if (listenerInstance === this) {
            listenerInstance = null
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        // Skip our own package notifications (service, overlay, etc.)
        if (sbn.packageName == applicationContext.packageName) return
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

    /**
     * Periodic background sync ensures our notification list matches the system's
     * active notifications. Runs every 3 seconds on a background coroutine.
     * Non-blocking — uses suspend + delay, not Thread.sleep.
     */
    private fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (isActive) {
                delay(SYNC_INTERVAL_MS)
                refreshFromActiveNotifications()
            }
        }
    }

    private fun refreshFromActiveNotifications() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        val ownPackage = applicationContext.packageName
        val notifications = active
            .filter { it.packageName != ownPackage }
            .mapNotNull { it.toLauncherNotification(applicationContext) }
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
        private const val SYNC_INTERVAL_MS = 3_000L
        private var listenerInstance: LumoNotificationListenerService? = null

        fun requestRefresh() {
            listenerInstance?.refreshFromActiveNotifications()
        }

        fun openNotification(key: String): Result<Boolean> {
            val listener = listenerInstance
                ?: return Result.failure(IllegalStateException("Notification listener is not connected"))
            val sbn = listener.activeNotifications?.firstOrNull { it.key == key }
                ?: return Result.success(false)

            // Prefer contentIntent (deep links to specific content like SMS conversation)
            // Fall back to fullScreenIntent (used by calls, alarms)
            val pendingIntent = sbn.notification.contentIntent ?: sbn.notification.fullScreenIntent

            if (pendingIntent != null) {
                return runCatching {
                    pendingIntent.send()
                    listener.cancelNotification(sbn.key)
                    LauncherNotificationCenter.remove(key)
                    LauncherNotificationCenter.dismissHeadsUp(key)
                    true
                }
            }

            // No PendingIntent — try launching the package's main activity
            return runCatching {
                val launchIntent = listener.packageManager
                    .getLaunchIntentForPackage(sbn.packageName)
                    ?: throw IllegalStateException("No launch intent for ${sbn.packageName}")
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                )
                listener.startActivity(launchIntent)
                listener.cancelNotification(sbn.key)
                LauncherNotificationCenter.remove(key)
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
    // Skip foreground service notifications (e.g. "displaying over other apps")
    if ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) {
        return null
    }
    // Skip ongoing system notifications (Android system overlay alerts, etc.)
    if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 &&
        notification.category == Notification.CATEGORY_SERVICE
    ) {
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
