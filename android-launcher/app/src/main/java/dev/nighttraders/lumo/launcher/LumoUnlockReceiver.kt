package dev.nighttraders.lumo.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Listens for screen-off events and launches the Lumo lock screen
 * so it is visible when the user turns the screen back on.
 * Also handles USER_PRESENT to bring the launcher home to the foreground.
 */
class LumoUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                context.startActivity(LockScreenActivity.createIntent(context))
            }
            Intent.ACTION_USER_PRESENT -> {
                context.startActivity(MainActivity.createHomeIntent(context))
            }
        }
    }
}
