package dev.nighttraders.lumo.launcher.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Required receiver for default SMS app status.
 * MMS handling is delegated to the system.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS messages are handled by the system provider
    }
}
