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

    /**
     * Remove a notification from the list. Only starts the re-post cooldown
     * when [dismissedByUser] is true (i.e., the user explicitly dismissed it in Lumo UI).
     * System-side removals (app update, notification rewrite) should NOT suppress re-posts.
     */
    fun remove(key: String, dismissedByUser: Boolean = false) {
        if (dismissedByUser) {
            dismissedKeys[key] = System.currentTimeMillis()
        }
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
     * Deduplicate notifications that are true duplicates (same key or same content
     * posted within a short window). Uses key-based dedup first, then content-based
     * only for notifications posted within 2 seconds of each other — so distinct
     * alerts with the same text (e.g., multiple identical email subjects) are preserved.
     */
    private fun deduplicateByContent(
        notifications: List<LauncherNotification>,
    ): List<LauncherNotification> {
        val byKey = LinkedHashMap<String, LauncherNotification>()
        // Key-based dedup first — always correct
        for (n in notifications.sortedByDescending { it.postedAt }) {
            byKey.putIfAbsent(n.key, n)
        }
        // Content-based dedup only for near-simultaneous posts (within 2s)
        val result = mutableListOf<LauncherNotification>()
        val contentSeen = mutableMapOf<String, Long>() // fingerprint -> postedAt
        for (n in byKey.values.sortedByDescending { it.postedAt }) {
            val fingerprint = "${n.packageName}|${n.title}|${n.message}"
            val lastSeen = contentSeen[fingerprint]
            if (lastSeen == null || kotlin.math.abs(lastSeen - n.postedAt) > 2_000L) {
                result.add(n)
                contentSeen[fingerprint] = n.postedAt
            }
        }
        return result
    }
}
