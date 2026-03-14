package dev.nighttraders.lumo.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.nighttraders.lumo.launcher.data.LauncherRepository
import dev.nighttraders.lumo.launcher.overlay.LumoGestureSidebarService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LumoStartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = LauncherRepository(appContext)
                if (repository.isOverlaySidebarEnabled()) {
                    LumoGestureSidebarService.start(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
