package dev.nighttraders.lumo.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.nighttraders.lumo.launcher.data.LauncherPreferences
import dev.nighttraders.lumo.launcher.data.LauncherRepository
import dev.nighttraders.lumo.launcher.data.LumoLauncherSettings
import dev.nighttraders.lumo.launcher.lockscreen.LumoLockScreenCompanionService
import dev.nighttraders.lumo.launcher.overlay.LumoGestureSidebarService
import dev.nighttraders.lumo.launcher.notifications.hasNotificationListenerAccess
import dev.nighttraders.lumo.launcher.ui.LumoKeyboardStatus
import dev.nighttraders.lumo.launcher.ui.LumoSettingsScreen
import dev.nighttraders.lumo.launcher.ui.readLumoKeyboardStatus
import dev.nighttraders.lumo.launcher.ui.theme.LumoLauncherTheme
import dev.nighttraders.lumo.launcher.ui.isLauncherDefault
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    private val repository by lazy { LauncherRepository(applicationContext) }
    private val requestRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshStatus()
        }

    private val isDefaultHome = mutableStateOf(false)
    private val hasNotificationAccess = mutableStateOf(false)
    private val keyboardStatus = mutableStateOf(LumoKeyboardStatus())
    private val hasOverlayPermission = mutableStateOf(false)
    private val isGestureSidebarEnabled = mutableStateOf(false)
    private val hasFullScreenIntentPermission = mutableStateOf(false)
    private val supportsLockScreenCompanion = mutableStateOf(LumoLockScreenCompanionService.isWakeCompanionSupported())
    private val isLockScreenCompanionEnabled = mutableStateOf(false)
    private val lockScreenSecurityType = mutableStateOf("none")
    private val launcherSettings = mutableStateOf(LumoLauncherSettings())
    private var lockScreenSecurityHash = ""
    private var lockScreenSecuritySalt = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()
        refreshStatus()
        loadLauncherSettings()

        setContent {
            LumoLauncherTheme {
                LumoSettingsScreen(
                    isDefaultHome = isDefaultHome.value,
                    hasNotificationAccess = hasNotificationAccess.value,
                    keyboardStatus = keyboardStatus.value,
                    hasOverlayPermission = hasOverlayPermission.value,
                    isGestureSidebarEnabled = isGestureSidebarEnabled.value,
                    hasFullScreenIntentPermission = hasFullScreenIntentPermission.value,
                    supportsLockScreenCompanion = supportsLockScreenCompanion.value,
                    isLockScreenCompanionEnabled = isLockScreenCompanionEnabled.value,
                    lockScreenSecurityType = lockScreenSecurityType.value,
                    settings = launcherSettings.value,
                    onRequestDefaultHome = ::requestDefaultHomeRole,
                    onRequestNotificationAccess = ::requestNotificationAccess,
                    onRequestOverlayPermission = ::openOverlayPermissionSettings,
                    onEnableGestureSidebar = ::enableGestureSidebar,
                    onDisableGestureSidebar = ::disableGestureSidebar,
                    onOpenKeyboardSettings = ::openInputMethodSettings,
                    onShowInputMethodPicker = ::showInputMethodPicker,
                    onOpenLockScreen = ::openLockScreen,
                    onOpenLockScreenPermissionSettings = ::openLockScreenPermissionSettings,
                    onEnableLockScreenCompanion = ::enableLockScreenCompanion,
                    onDisableLockScreenCompanion = ::disableLockScreenCompanion,
                    onSetLockScreenPin = { pin -> setLockScreenSecurity("pin", pin) },
                    onSetLockScreenPassword = { password -> setLockScreenSecurity("password", password) },
                    onClearLockScreenSecurity = ::clearLockScreenSecurity,
                    onVerifyCurrentSecurity = ::verifyCurrentSecurity,
                    onOpenWifiSettings = { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                    onOpenDisplaySettings = { startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) },
                    onOpenWallpaperSettings = { startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) },
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onUpdateIntSetting = { key, value -> updateIntSetting(key, value) },
                    onUpdateBoolSetting = { key, value -> updateBoolSetting(key, value) },
                    onRefresh = ::refreshStatus,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        configureSystemBars()
        refreshStatus()
    }

    private fun refreshStatus() {
        isDefaultHome.value = isLauncherDefault()
        hasNotificationAccess.value = hasNotificationListenerAccess()
        keyboardStatus.value = readLumoKeyboardStatus()
        hasOverlayPermission.value = Settings.canDrawOverlays(this)
        hasFullScreenIntentPermission.value = LumoLockScreenCompanionService.hasFullScreenIntentPermission(this)
        supportsLockScreenCompanion.value = LumoLockScreenCompanionService.isWakeCompanionSupported()
        lifecycleScope.launch {
            val overlayEnabled = repository.isOverlaySidebarEnabled()
            isGestureSidebarEnabled.value = overlayEnabled
            val storedLockScreenEnabled = repository.isLockScreenCompanionEnabled()
            val effectiveLockScreenEnabled =
                storedLockScreenEnabled && supportsLockScreenCompanion.value
            if (storedLockScreenEnabled != effectiveLockScreenEnabled) {
                repository.setLockScreenCompanionEnabled(effectiveLockScreenEnabled)
            }
            isLockScreenCompanionEnabled.value = effectiveLockScreenEnabled
            LumoGestureSidebarService.sync(this@SettingsActivity, overlayEnabled)
            lockScreenSecurityType.value = repository.getLockScreenSecurityType()
            lockScreenSecurityHash = repository.getLockScreenSecurityHash()
            lockScreenSecuritySalt = repository.getLockScreenSecuritySalt()
        }
    }

    private fun updateIntSetting(key: String, value: Int) {
        val prefKey = androidx.datastore.preferences.core.intPreferencesKey(key)
        lifecycleScope.launch {
            repository.updateSetting(prefKey, value)
            loadLauncherSettings()
        }
    }

    private fun updateBoolSetting(key: String, value: Boolean) {
        val prefKey = androidx.datastore.preferences.core.booleanPreferencesKey(key)
        lifecycleScope.launch {
            repository.updateSetting(prefKey, value)
            loadLauncherSettings()
        }
    }

    private fun loadLauncherSettings() {
        lifecycleScope.launch {
            repository.observeLauncherSettings().collect { settings ->
                launcherSettings.value = settings
            }
        }
    }

    private fun setLockScreenSecurity(type: String, secret: String) {
        val sanitized = LockScreenActivity.sanitizeInput(secret)
        if (sanitized.length < 4) return
        lifecycleScope.launch {
            val salt = LockScreenActivity.generateSalt()
            val hash = LockScreenActivity.hashWithSalt(sanitized, salt)
            repository.setLockScreenSecurity(type, hash, salt)
            lockScreenSecurityType.value = type
            lockScreenSecurityHash = hash
            lockScreenSecuritySalt = salt
        }
    }

    private fun verifyCurrentSecurity(input: String): Boolean {
        if (lockScreenSecurityHash.isEmpty()) return true
        val sanitized = LockScreenActivity.sanitizeInput(input)
        if (sanitized.isEmpty()) return false
        return LockScreenActivity.hashWithSalt(sanitized, lockScreenSecuritySalt) == lockScreenSecurityHash
    }

    private fun clearLockScreenSecurity() {
        lifecycleScope.launch {
            repository.clearLockScreenSecurity()
            lockScreenSecurityType.value = "none"
            lockScreenSecurityHash = ""
            lockScreenSecuritySalt = ""
        }
    }

    private fun requestDefaultHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                requestRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }

        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    private fun requestNotificationAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        startActivity(intent)
    }

    private fun enableGestureSidebar() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlayPermissionSettings()
            return
        }

        lifecycleScope.launch {
            repository.setOverlaySidebarEnabled(true)
            isGestureSidebarEnabled.value = true
            LumoGestureSidebarService.start(this@SettingsActivity)
        }
    }

    private fun disableGestureSidebar() {
        lifecycleScope.launch {
            repository.setOverlaySidebarEnabled(false)
            isGestureSidebarEnabled.value = false
            LumoGestureSidebarService.stop(this@SettingsActivity)
        }
    }

    private fun openInputMethodSettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }

    private fun showInputMethodPicker() {
        getSystemService<InputMethodManager>()?.showInputMethodPicker()
    }

    private fun openLockScreen() {
        startActivity(LockScreenActivity.createIntent(this))
    }

    private fun openLockScreenPermissionSettings() {
        startActivity(LumoLockScreenCompanionService.createFullScreenIntentSettingsIntent(this))
    }

    private fun enableLockScreenCompanion() {
        if (!LumoLockScreenCompanionService.hasFullScreenIntentPermission(this)) {
            openLockScreenPermissionSettings()
            return
        }

        if (!LumoLockScreenCompanionService.isWakeCompanionSupported()) {
            lifecycleScope.launch {
                repository.setLockScreenCompanionEnabled(false)
                isLockScreenCompanionEnabled.value = false
            }
            Toast.makeText(
                this,
                getString(R.string.lock_screen_companion_unavailable_message),
                Toast.LENGTH_LONG,
            ).show()
            openLockScreen()
            return
        }

        lifecycleScope.launch {
            repository.setLockScreenCompanionEnabled(true)
            isLockScreenCompanionEnabled.value = true
            LumoLockScreenCompanionService.start(this@SettingsActivity)
        }
    }

    private fun disableLockScreenCompanion() {
        lifecycleScope.launch {
            repository.setLockScreenCompanionEnabled(false)
            isLockScreenCompanionEnabled.value = false
            LumoLockScreenCompanionService.stop(this@SettingsActivity)
        }
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
            Intent(context, SettingsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
