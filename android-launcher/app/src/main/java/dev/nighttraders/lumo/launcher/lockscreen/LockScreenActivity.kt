package dev.nighttraders.lumo.launcher.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.nighttraders.lumo.launcher.data.LauncherRepository
import dev.nighttraders.lumo.launcher.notifications.LauncherNotificationCenter
import dev.nighttraders.lumo.launcher.ui.rememberSystemStatus
import dev.nighttraders.lumo.launcher.ui.theme.LumoLauncherTheme
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom

class LockScreenActivity : ComponentActivity() {
    private val repository by lazy { LauncherRepository(applicationContext) }
    private var securityType by mutableStateOf("loading")
    private var securityHash by mutableStateOf("")
    private var securitySalt by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        // Only turn screen on if explicitly requested (e.g. from companion service full-screen intent).
        // Do NOT turn screen on when launched from SCREEN_OFF receiver — that would wake the device.
        if (intent?.getBooleanExtra(EXTRA_TURN_SCREEN_ON, false) == true) {
            setTurnScreenOn(true)
        }
        LumoLockScreenCompanionService.dismissWakeSurface(this)
        configureSystemBars()
        loadSecurity()

        setContent {
            val status by rememberSystemStatus()
            val notifications by LauncherNotificationCenter.notifications.collectAsStateWithLifecycle()

            LumoLauncherTheme {
                LumoLockScreenScreen(
                    status = status,
                    notifications = notifications,
                    securityType = securityType,
                    onUnlock = ::attemptUnlock,
                    onVerifyPin = ::verifyPin,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LumoLockScreenCompanionService.dismissWakeSurface(this)
        configureSystemBars()
        loadSecurity()
    }

    override fun onDestroy() {
        LumoLockScreenCompanionService.dismissWakeSurface(this)
        super.onDestroy()
    }

    private fun loadSecurity() {
        lifecycleScope.launch {
            securityType = repository.getLockScreenSecurityType()
            securityHash = repository.getLockScreenSecurityHash()
            securitySalt = repository.getLockScreenSecuritySalt()
        }
    }

    private fun verifyPin(input: String): Boolean {
        if (securityType == "loading") return false
        if (securityHash.isEmpty()) return false // Missing hash = broken state, reject all
        val sanitized = sanitizeInput(input)
        if (sanitized.isEmpty()) return false
        return hashWithSalt(sanitized, securitySalt) == securityHash
    }

    private fun attemptUnlock() {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager == null || !keyguardManager.isDeviceLocked) {
            // No system keyguard active — safe to unlock immediately
            LumoLockState.unlock()
            LumoLockScreenCompanionService.dismissWakeSurface(this)
            finish()
            return
        }

        keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                // Only mark unlocked AFTER the system confirms dismissal
                LumoLockState.unlock()
                LumoLockScreenCompanionService.dismissWakeSurface(this@LockScreenActivity)
                finish()
            }

            override fun onDismissCancelled() {
                // System refused — keep lock state set
            }

            override fun onDismissError() {
                // System error — keep lock state set
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
        const val MAX_PIN_LENGTH = 10
        const val MAX_PASSWORD_LENGTH = 32
        private const val MAX_INPUT_LENGTH = 32
        private const val SALT_BYTES = 32
        private const val EXTRA_TURN_SCREEN_ON = "dev.nighttraders.lumo.launcher.TURN_SCREEN_ON"

        fun createIntent(context: Context): Intent =
            Intent(context, LockScreenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun createWakeIntent(context: Context): Intent =
            createIntent(context).putExtra(EXTRA_TURN_SCREEN_ON, true)

        fun generateSalt(): String {
            val bytes = ByteArray(SALT_BYTES)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun hashWithSalt(input: String, salt: String): String {
            val sanitized = sanitizeInput(input)
            val salted = salt + sanitized
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(salted.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        fun sanitizeInput(input: String): String {
            return input
                .take(MAX_INPUT_LENGTH)
                .filter { it.code >= 0x20 && it.code != 0x7F }
        }
    }
}
