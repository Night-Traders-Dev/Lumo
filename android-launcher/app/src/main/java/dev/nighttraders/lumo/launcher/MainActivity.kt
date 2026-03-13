package dev.nighttraders.lumo.launcher

import android.app.role.RoleManager
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
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nighttraders.lumo.launcher.ui.LumoLauncherApp
import dev.nighttraders.lumo.launcher.ui.LauncherViewModel
import dev.nighttraders.lumo.launcher.ui.isLauncherDefault
import dev.nighttraders.lumo.launcher.ui.rememberSystemStatus
import dev.nighttraders.lumo.launcher.ui.theme.LumoLauncherTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels { LauncherViewModel.Factory }
    private val requestRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshDefaultHomeState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    onRequestDefaultHome = ::requestDefaultHomeRole,
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
                    onToggleFavorite = viewModel::toggleFavorite,
                    onRefresh = viewModel::refreshApps,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        configureSystemBars()
        refreshDefaultHomeState()
        viewModel.refreshApps()
    }

    private fun refreshDefaultHomeState() {
        viewModel.updateDefaultHomeStatus(isLauncherDefault())
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

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
