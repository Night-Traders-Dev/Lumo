package dev.nighttraders.lumo.launcher

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.nighttraders.lumo.launcher.data.LumoLauncherSettings
import dev.nighttraders.lumo.launcher.data.LauncherRepository
import dev.nighttraders.lumo.launcher.lockscreen.LockScreenActivity
import dev.nighttraders.lumo.launcher.lockscreen.LumoLockScreenCompanionService
import dev.nighttraders.lumo.launcher.lockscreen.LumoLockState
import dev.nighttraders.lumo.launcher.lockscreen.LumoUnlockReceiver
import dev.nighttraders.lumo.launcher.overlay.LumoGestureSidebarService
import dev.nighttraders.lumo.launcher.weather.OpenMeteoWeatherProvider
import dev.nighttraders.lumo.launcher.settings.SettingsActivity
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
    private val navigationRequestId = mutableIntStateOf(0)
    // Start as "loading" so the dash stays locked until we know the real security state
    private val lockScreenSecurityType = mutableStateOf("loading")
    private val lockScreenSecurityHash = mutableStateOf("")
    private val lockScreenSecuritySalt = mutableStateOf("")
    private val isFlashlightOn = mutableStateOf(false)
    private var torchCallback: CameraManager.TorchCallback? = null
    private val screenReceiver = LumoUnlockReceiver()
    private var statusBarBlockerView: View? = null
    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.refreshApps(force = true)
        }
    }
    private val requestRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshDefaultHomeState()
        }
    private val requestActivityRecognition =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) refreshWeather()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dev.nighttraders.lumo.launcher.data.LumoMigration.runIfNeeded(applicationContext)
        dev.nighttraders.lumo.launcher.data.LumoDebugLog.logKnownIssues()
        requestedPageIndex.intValue = parseRequestedPage(intent)
        navigationRequestId.intValue++
        enableEdgeToEdge()
        configureSystemBars()
        refreshDefaultHomeState()
        registerPackageChangeReceiver()
        registerScreenReceiver()
        registerFlashlightCallback()
        requestActivityRecognitionIfNeeded()
        requestLocationIfNeeded()
        loadSecurityState()
        refreshWeather()
        observeLockState()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDefaultHome by viewModel.isDefaultHome.collectAsStateWithLifecycle()
            val systemStatus by rememberSystemStatus()
            val isDashLocked by LumoLockState.isLocked.collectAsStateWithLifecycle()
            val launcherSettings by viewModel.launcherSettings.collectAsStateWithLifecycle()

            LumoLauncherTheme {
                LumoLauncherApp(
                    uiState = uiState,
                    systemStatus = systemStatus,
                    isDefaultHome = isDefaultHome,
                    requestedPageIndex = requestedPageIndex.intValue,
                    navigationRequestId = navigationRequestId.intValue,
                    settings = launcherSettings,
                    isDashLocked = isDashLocked && lockScreenSecurityType.value != "none" && lockScreenSecurityType.value != "loading",
                    lockScreenSecurityType = if (lockScreenSecurityType.value == "loading") "none" else lockScreenSecurityType.value,
                    onVerifyPin = { input ->
                        if (lockScreenSecurityType.value == "loading") return@LumoLauncherApp false
                        val sanitized = LockScreenActivity.sanitizeInput(input)
                        if (sanitized.isEmpty() || lockScreenSecurityHash.value.isEmpty()) false
                        else LockScreenActivity.hashWithSalt(sanitized, lockScreenSecuritySalt.value) == lockScreenSecurityHash.value
                    },
                    onDashUnlock = {
                        removeStatusBarBlocker()
                        LumoLockState.unlock()
                    },
                    onRequestDefaultHome = ::requestDefaultHomeRole,
                    onOpenSettings = ::openSettings,
                    onOpenWallpaperPicker = ::openWallpaperPicker,
                    onOpenLockScreen = ::openLockScreen,
                    onRequestNotificationAccess = ::requestNotificationAccess,
                    onOpenWifiSettings = { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                    onOpenBluetoothSettings = { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                    onOpenAirplaneSettings = { startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)) },
                    onToggleFlashlight = ::toggleFlashlight,
                    onOpenLocationSettings = { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                    isFlashlightOn = isFlashlightOn.value,
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
                    onAddFavorite = viewModel::addFavorite,
                    onReorderFavorites = viewModel::reorderFavorites,
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
                        viewModel.refreshApps(force = true)
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
        navigationRequestId.intValue++
    }

    override fun onResume() {
        super.onResume()
        configureSystemBars()
        refreshDefaultHomeState()
        refreshNotificationAccessState()
        syncLockTaskState()
        viewModel.refreshApps() // no-op unless first load; use force=true for explicit refresh
        viewModel.refreshNotifications()
        loadSecurityState()
        refreshWeather()
        lifecycleScope.launch {
            LumoGestureSidebarService.sync(this@MainActivity, repository.isOverlaySidebarEnabled())
            LumoLockScreenCompanionService.sync(this@MainActivity, repository.isLockScreenCompanionEnabled())
        }
    }

    private fun loadSecurityState() {
        lifecycleScope.launch {
            lockScreenSecurityType.value = repository.getLockScreenSecurityType()
            lockScreenSecurityHash.value = repository.getLockScreenSecurityHash()
            lockScreenSecuritySalt.value = repository.getLockScreenSecuritySalt()
            // Security state is now known — sync lock task mode in case it was
            // deferred because the type was still "loading" when observeLockState fired.
            syncLockTaskState()
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

    private fun openWallpaperPicker() {
        startActivity(SettingsActivity.createWallpaperIntent(this))
    }

    private fun openLockScreen() {
        startActivity(LockScreenActivity.createIntent(this))
    }

    private fun parseRequestedPage(intent: Intent?): Int =
        intent?.getIntExtra(EXTRA_START_PAGE, START_PAGE_HOME) ?: START_PAGE_HOME

    private fun registerPackageChangeReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageChangeReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(packageChangeReceiver, filter)
        }
    }

    /** Always register the screen-off receiver so LumoLockState.lock() fires on every screen off. */
    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenReceiver, filter)
        }
    }

    private fun hasSecurityConfigured(): Boolean =
        lockScreenSecurityType.value != "none" && lockScreenSecurityType.value != "loading"

    /** Sync the status bar blocker with the current lock state. */
    private fun syncLockTaskState() {
        val shouldLock = LumoLockState.isLocked.value && hasSecurityConfigured()
        if (shouldLock) {
            addStatusBarBlocker()
        } else {
            removeStatusBarBlocker()
        }
    }

    /**
     * Observe lock state changes to add/remove the status bar blocker overlay.
     * The overlay is a transparent fullscreen-width strip over the status bar
     * area that intercepts all touch events, preventing the notification shade
     * from being pulled down.
     */
    private fun observeLockState() {
        lifecycleScope.launch {
            LumoLockState.isLocked.collect { isLocked ->
                val shouldLock = isLocked && hasSecurityConfigured()
                if (shouldLock) {
                    addStatusBarBlocker()
                } else {
                    removeStatusBarBlocker()
                }
                configureSystemBars()
            }
        }
    }

    /**
     * Block access to the status bar by:
     * 1. Adding FLAG_FULLSCREEN to the window — suppresses the status bar
     * 2. Posting a transparent overlay above the status bar area to intercept swipe gestures
     */
    private fun addStatusBarBlocker() {
        // FLAG_FULLSCREEN tells the system to hide the status bar and not allow pull-down
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Overlay as a belt-and-suspenders measure
        if (statusBarBlockerView == null && Settings.canDrawOverlays(this)) {
            val blocker = View(this).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setOnTouchListener { _, _ -> true }
            }

            val statusBarHeight = runCatching {
                resources.getDimensionPixelSize(
                    resources.getIdentifier("status_bar_height", "dimen", "android"),
                )
            }.getOrDefault(100)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                statusBarHeight,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            runCatching {
                getSystemService(WindowManager::class.java).addView(blocker, params)
                statusBarBlockerView = blocker
            }
        }
    }

    private fun removeStatusBarBlocker() {
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        statusBarBlockerView?.let { view ->
            runCatching {
                getSystemService(WindowManager::class.java).removeView(view)
            }
            statusBarBlockerView = null
        }
    }

    override fun onDestroy() {
        removeStatusBarBlocker()
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { unregisterReceiver(packageChangeReceiver) }
        torchCallback?.let { cb ->
            runCatching { getSystemService(CameraManager::class.java)?.unregisterTorchCallback(cb) }
        }
        super.onDestroy()
    }

    /** Find the camera ID that has a flash unit, not just the first camera. */
    private fun findFlashCameraId(): String? {
        val cameraManager = getSystemService(CameraManager::class.java) ?: return null
        return runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private fun toggleFlashlight() {
        val cameraManager = getSystemService(CameraManager::class.java) ?: return
        val cameraId = findFlashCameraId() ?: return
        val newState = !isFlashlightOn.value
        runCatching { cameraManager.setTorchMode(cameraId, newState) }
            .onSuccess { isFlashlightOn.value = newState }
            .onFailure {
                Toast.makeText(this, "Could not toggle flashlight", Toast.LENGTH_SHORT).show()
            }
    }

    private fun registerFlashlightCallback() {
        val cameraManager = getSystemService(CameraManager::class.java) ?: return
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                isFlashlightOn.value = enabled
            }
        }
        torchCallback = callback
        cameraManager.registerTorchCallback(callback, null)
    }

    private fun refreshWeather() {
        lifecycleScope.launch {
            OpenMeteoWeatherProvider.refresh(applicationContext)
        }
    }

    private fun requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun requestActivityRecognitionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestActivityRecognition.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            // When locked, use DEFAULT behavior to prevent transient status bar reveals
            systemBarsBehavior = if (LumoLockState.isLocked.value && hasSecurityConfigured()) {
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            } else {
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    companion object {
        const val EXTRA_START_PAGE = "dev.nighttraders.lumo.launcher.EXTRA_START_PAGE"
        const val START_PAGE_HOME = 0
        const val START_PAGE_APPS = 1
        const val START_PAGE_DASH = 2

        fun createHomeIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_START_PAGE, START_PAGE_HOME)

        fun createDashIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_START_PAGE, START_PAGE_DASH)

        fun createAppsIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_START_PAGE, START_PAGE_APPS)
    }
}
