package dev.nighttraders.lumo.launcher.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object LauncherNotificationCenter {
    private const val MAX_NOTIFICATIONS = 20
    private const val DISMISS_COOLDOWN_MS = 5_000L

    private val _notifications = MutableStateFlow<List<LauncherNotification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _headsUpNotification = MutableStateFlow<LauncherNotification?>(null)
    val headsUpNotification = _headsUpNotification.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess = _hasAccess.asStateFlow()

    /** Keys recently dismissed by the user — suppress re-posts for a short window. */
    private val dismissedKeys = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun setAccessEnabled(enabled: Boolean) {
        _hasAccess.value = enabled
        if (!enabled) {
            _notifications.value = emptyList()
            _headsUpNotification.value = null
        }
    }

    fun replaceAll(notifications: List<LauncherNotification>) {
        val now = System.currentTimeMillis()
        // Clean up expired cooldowns
        dismissedKeys.entries.removeAll { now - it.value >= DISMISS_COOLDOWN_MS }
        _notifications.value = notifications
            .filter { !dismissedKeys.containsKey(it.key) }
            .sortedByDescending { it.postedAt }
            .take(MAX_NOTIFICATIONS)
    }

    fun upsert(notification: LauncherNotification, alert: Boolean) {
        // Suppress re-posts of recently dismissed notifications
        val dismissedAt = dismissedKeys[notification.key]
        if (dismissedAt != null) {
            if (System.currentTimeMillis() - dismissedAt < DISMISS_COOLDOWN_MS) {
                return
            }
            dismissedKeys.remove(notification.key)
        }

        _notifications.update { existing ->
            buildList {
                add(notification)
                addAll(existing.filterNot { it.key == notification.key })
            }.sortedByDescending { it.postedAt }
                .take(MAX_NOTIFICATIONS)
        }

        if (alert) {
            _headsUpNotification.value = notification
        }
    }

    fun remove(key: String) {
        dismissedKeys[key] = System.currentTimeMillis()
        _notifications.update { existing ->
            existing.filterNot { it.key == key }
        }
        if (_headsUpNotification.value?.key == key) {
            _headsUpNotification.value = null
        }
    }

    fun dismissHeadsUp(key: String) {
        if (_headsUpNotification.value?.key == key) {
            _headsUpNotification.value = null
        }
    }
}
