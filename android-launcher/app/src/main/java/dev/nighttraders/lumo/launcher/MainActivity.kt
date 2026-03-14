package dev.nighttraders.lumo.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.nighttraders.lumo.launcher.data.LauncherRepository
import dev.nighttraders.lumo.launcher.overlay.LumoGestureSidebarService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nighttraders.lumo.launcher.notifications.hasNotificationListenerAccess
import dev.nighttraders.lumo.launcher.ui.LumoLauncherApp
import dev.nighttraders.lumo.launcher.ui.LauncherViewModel
import dev.nighttraders.lumo.launcher.ui.isLauncherDefault
import dev.nighttraders.lumo.launcher.ui.rememberSystemStatus
import dev.nighttraders.lumo.launcher.ui.theme.LumoLauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels { LauncherViewModel.Factory }
    private val repository by lazy { LauncherRepository(applicationContext) }
    private val requestedPageIndex = mutableIntStateOf(START_PAGE_HOME)
    private val requestRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshDefaultHomeState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedPageIndex.intValue = parseRequestedPage(intent)
        enableEdgeToEdge()
        configureSystemBars()
        refreshDefaultHomeState()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDefaultHome by viewModel.isDefaultHome.collectAsStateWithLifecycle()
            val systemStatus by rememberSystemStatus()

            LumoLauncherTheme {
                LumoLauncherApp(
                    uiState = uiState,
                    systemStatus = systemStatus,
                    isDefaultHome = isDefaultHome,
                    requestedPageIndex = requestedPageIndex.intValue,
                    onRequestDefaultHome = ::requestDefaultHomeRole,
                    onOpenSettings = ::openSettings,
                    onOpenLockScreen = ::openLockScreen,
                    onRequestNotificationAccess = ::requestNotificationAccess,
                    onLaunchApp = { app ->
                        val result = viewModel.launchApp(app)
                        if (result.isFailure) {
                            Toast.makeText(
                                this,
                                getString(R.string.launch_failed_message),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    onOpenNotification = { notification ->
                        val result = viewModel.openNotification(notification)
                        if (result.isFailure) {
                            Toast.makeText(
                                this,
                                getString(R.string.notification_open_failed_message),
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            viewModel.dismissHeadsUpNotification(notification.key)
                        }
                    },
                    onOpenNotificationApp = { notification ->
                        val result = viewModel.openNotificationApp(notification)
                        if (result.isFailure) {
                            Toast.makeText(
                                this,
                                getString(R.string.notification_app_open_failed_message),
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            viewModel.dismissHeadsUpNotification(notification.key)
                        }
                    },
                    onDismissNotification = { notification ->
                        val result = viewModel.dismissNotification(notification)
                        if (result.isFailure) {
                            Toast.makeText(
                                this,
                                getString(R.string.notification_dismiss_failed_message),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        result
                    },
                    onSnoozeNotification = { notification, durationMillis ->
                        val result = viewModel.snoozeNotification(notification, durationMillis)
                        if (result.isFailure) {
                            Toast.makeText(
                                this,
                                getString(R.string.notification_snooze_failed_message),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        result
                    },
                    onDismissHeadsUpNotification = viewModel::dismissHeadsUpNotification,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenAppInfo = { app ->
                        val result = viewModel.openAppInfo(app)
                        if (result.isFailure) {
                            Toast.makeText(this, "Could not open app info", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRequestUninstall = { app ->
                        val result = viewModel.requestUninstall(app)
                        if (result.isFailure) {
                            Toast.makeText(this, "Could not uninstall app", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRefresh = {
                        viewModel.refreshApps()
                        viewModel.refreshNotifications()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedPageIndex.intValue = parseRequestedPage(intent)
    }

    override fun onResume() {
        super.onResume()
        configureSystemBars()
        refreshDefaultHomeState()
        refreshNotificationAccessState()
        viewModel.refreshApps()
        viewModel.refreshNotifications()
        lifecycleScope.launch {
            LumoGestureSidebarService.sync(this@MainActivity, repository.isOverlaySidebarEnabled())
        }
    }

    private fun refreshDefaultHomeState() {
        viewModel.updateDefaultHomeStatus(isLauncherDefault())
    }

    private fun refreshNotificationAccessState() {
        viewModel.updateNotificationAccessStatus(hasNotificationListenerAccess())
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

    private fun openSettings() {
        startActivity(SettingsActivity.createIntent(this))
    }

    private fun openLockScreen() {
        startActivity(LockScreenActivity.createIntent(this))
    }

    private fun parseRequestedPage(intent: Intent?): Int =
        intent?.getIntExtra(EXTRA_START_PAGE, START_PAGE_HOME) ?: START_PAGE_HOME

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        const val EXTRA_START_PAGE = "dev.nighttraders.lumo.launcher.EXTRA_START_PAGE"
        const val START_PAGE_HOME = 0
        const val START_PAGE_APPS = 1

        fun createHomeIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_START_PAGE, START_PAGE_HOME)

        fun createAppsIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_START_PAGE, START_PAGE_APPS)
    }
}
