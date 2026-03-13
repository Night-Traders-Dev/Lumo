package dev.nighttraders.lumo.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
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
import dev.nighttraders.lumo.launcher.data.LauncherRepository
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()
        refreshStatus()

        setContent {
            LumoLauncherTheme {
                LumoSettingsScreen(
                    isDefaultHome = isDefaultHome.value,
                    hasNotificationAccess = hasNotificationAccess.value,
                    keyboardStatus = keyboardStatus.value,
                    hasOverlayPermission = hasOverlayPermission.value,
                    isGestureSidebarEnabled = isGestureSidebarEnabled.value,
                    onRequestDefaultHome = ::requestDefaultHomeRole,
                    onRequestNotificationAccess = ::requestNotificationAccess,
                    onRequestOverlayPermission = ::openOverlayPermissionSettings,
                    onEnableGestureSidebar = ::enableGestureSidebar,
                    onDisableGestureSidebar = ::disableGestureSidebar,
                    onOpenKeyboardSettings = ::openInputMethodSettings,
                    onShowInputMethodPicker = ::showInputMethodPicker,
                    onOpenLockScreen = ::openLockScreen,
                    onOpenWifiSettings = { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                    onOpenDisplaySettings = { startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) },
                    onOpenWallpaperSettings = { startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) },
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
        lifecycleScope.launch {
            val overlayEnabled = repository.isOverlaySidebarEnabled()
            isGestureSidebarEnabled.value = overlayEnabled
            LumoGestureSidebarService.sync(this@SettingsActivity, overlayEnabled)
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
