package dev.nighttraders.lumo.launcher.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device admin / device owner receiver for Lumo Launcher.
 *
 * As device owner, Lumo gains access to:
 * - lockNow() — programmatic screen lock
 * - Password/PIN policy enforcement
 * - Kiosk mode (lock task)
 * - App suspension / hiding
 * - Silent app install/uninstall (device owner)
 * - Set lock screen info message
 * - Global settings control (WiFi, Bluetooth, etc.)
 *
 * To set as device owner (requires no other accounts on device):
 *   adb shell dpm set-device-owner dev.nighttraders.lumo.launcher/.admin.LumoDeviceAdminReceiver
 *
 * To remove device owner:
 *   The app must call clearDeviceOwnerApp() or:
 *   adb shell dpm remove-active-admin dev.nighttraders.lumo.launcher/.admin.LumoDeviceAdminReceiver
 */
class LumoDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
