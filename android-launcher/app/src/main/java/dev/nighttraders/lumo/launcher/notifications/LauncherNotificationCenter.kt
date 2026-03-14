package dev.nighttraders.lumo.launcher.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

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
    private val dismissedKeys = ConcurrentHashMap<String, Long>()

    fun setAccessEnabled(enabled: Boolean) {
        _hasAccess.value = enabled
        if (!enabled) {
            _notifications.value = emptyList()
            _headsUpNotification.value = null
        }
    }

    /**
     * Full replacement from the system's active notifications.
     * This is the ground-truth sync — deduplicates by content fingerprint,
     * filters dismissed keys still in cooldown, and replaces the entire list atomically.
     */
    fun replaceAll(notifications: List<LauncherNotification>) {
        val now = System.currentTimeMillis()
        // Clean up expired cooldowns
        dismissedKeys.entries.removeAll { now - it.value >= DISMISS_COOLDOWN_MS }

        val deduped = deduplicateByContent(notifications)
        _notifications.value = deduped
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
            val merged = buildList {
                add(notification)
                addAll(existing.filterNot { it.key == notification.key })
            }
            deduplicateByContent(merged)
                .sortedByDescending { it.postedAt }
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

    /**
     * Deduplicate notifications that have the same package + title + message.
     * Some apps post multiple notifications with different keys but identical content.
     * Keeps the most recently posted one.
     */
    private fun deduplicateByContent(
        notifications: List<LauncherNotification>,
    ): List<LauncherNotification> {
        val seen = LinkedHashMap<String, LauncherNotification>()
        // Process newest first so the LinkedHashMap keeps the most recent
        for (n in notifications.sortedByDescending { it.postedAt }) {
            val fingerprint = "${n.packageName}|${n.title}|${n.message}"
            if (!seen.containsKey(fingerprint)) {
                seen[fingerprint] = n
            }
        }
        return seen.values.toList()
    }
}
