package dev.nighttraders.lumo.launcher.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Listens for screen-off events and locks the dash so the user must
 * authenticate when the screen comes back on.  Since Lumo is the home app,
 * MainActivity is already shown on wake — its dash-lock overlay handles
 * the PIN/password challenge.  No need to launch a separate LockScreenActivity.
 */
class LumoUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                LumoLockState.lock()
            }
        }
    }
}
