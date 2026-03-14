package dev.nighttraders.lumo.launcher.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Receives incoming SMS messages. Required for being a default SMS app.
 * The messages are automatically written to the SMS provider by the system
 * when the app is the default SMS handler.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        // Messages are handled by the system SMS provider — no additional processing needed.
        // The conversation list will refresh when the user opens the app.
    }
}
