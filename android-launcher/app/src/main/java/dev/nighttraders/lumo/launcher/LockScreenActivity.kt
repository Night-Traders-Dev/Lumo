package dev.nighttraders.lumo.launcher

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nighttraders.lumo.launcher.lockscreen.LumoLockScreenCompanionService
import dev.nighttraders.lumo.launcher.notifications.LauncherNotificationCenter
import dev.nighttraders.lumo.launcher.ui.LumoLockScreenScreen
import dev.nighttraders.lumo.launcher.ui.rememberSystemStatus
import dev.nighttraders.lumo.launcher.ui.theme.LumoLauncherTheme

class LockScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        LumoLockScreenCompanionService.dismissWakeSurface(this)
        configureSystemBars()

        setContent {
            val status by rememberSystemStatus()
            val notifications by LauncherNotificationCenter.notifications.collectAsStateWithLifecycle()

            LumoLauncherTheme {
                LumoLockScreenScreen(
                    status = status,
                    notifications = notifications,
                    onUnlock = ::attemptUnlock,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LumoLockScreenCompanionService.dismissWakeSurface(this)
        configureSystemBars()
    }

    override fun onDestroy() {
        LumoLockScreenCompanionService.dismissWakeSurface(this)
        super.onDestroy()
    }

    private fun attemptUnlock() {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager == null || !keyguardManager.isDeviceLocked) {
            LumoLockScreenCompanionService.dismissWakeSurface(this)
            finish()
            return
        }

        keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                LumoLockScreenCompanionService.dismissWakeSurface(this@LockScreenActivity)
                finish()
            }
        })
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, LockScreenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
