package dev.nighttraders.lumo.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import dev.nighttraders.lumo.launcher.data.LumoDebugLog
import dev.nighttraders.lumo.launcher.lockscreen.LumoLockState

/**
 * Accessibility service that blocks Android's notification shade (status bar pull-down)
 * when the Lumo lock screen is active.
 *
 * When a notification shade or quick settings panel is detected while locked,
 * the service immediately collapses it using [GLOBAL_ACTION_BACK].
 *
 * This is the only reliable way to block the notification shade on Android 12+
 * without device owner privileges, as overlay-based approaches are rendered
 * below the system notification shade.
 *
 * Users must manually enable this service in Settings → Accessibility.
 */
class LumoAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        isRunning = true
        LumoDebugLog.d("Accessibility", "LumoAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!LumoLockState.isLocked.value) return

        // Detect notification shade / quick settings panel
        val className = event.className?.toString() ?: return
        if (isNotificationShade(className)) {
            LumoDebugLog.d("Accessibility", "Blocked status bar access while locked: $className")
            performGlobalAction(GLOBAL_ACTION_BACK)
            // Double-tap back to ensure shade is fully collapsed
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    override fun onInterrupt() {
        // Required override — no cleanup needed
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    private fun isNotificationShade(className: String): Boolean {
        val lower = className.lowercase()
        return lower.contains("statusbar") ||
            lower.contains("notification") && lower.contains("shade") ||
            lower.contains("quicksettings") ||
            lower.contains("notificationpanel") ||
            lower.contains("statusbarpanel") ||
            lower.contains("shade") && lower.contains("panel") ||
            // Samsung-specific class names
            className == "com.android.systemui.statusbar.phone.StatusBarWindowView" ||
            className == "com.android.systemui.shade.NotificationShadeWindowView" ||
            className == "com.android.systemui.qs.QSPanel"
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        /** Check if the accessibility service is enabled in system settings. */
        fun isEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val serviceName = "${context.packageName}/.accessibility.LumoAccessibilityService"
            return enabledServices.contains(serviceName)
        }

        /** Open system accessibility settings so the user can enable the service. */
        fun openSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
