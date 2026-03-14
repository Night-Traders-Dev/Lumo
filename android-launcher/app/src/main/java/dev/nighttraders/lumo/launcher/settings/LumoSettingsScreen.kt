package dev.nighttraders.lumo.launcher.settings

import android.content.Context
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

data class LumoKeyboardStatus(
    val isEnabled: Boolean = false,
    val isSelected: Boolean = false,
)

private const val LUMO_INPUT_METHOD_ID = "dev.nighttraders.lumo.launcher/.input.LumoInputMethodService"
private const val LUMO_INPUT_METHOD_CLASS = "dev.nighttraders.lumo.launcher/dev.nighttraders.lumo.launcher.input.LumoInputMethodService"

@Composable
fun rememberLumoKeyboardStatus(): State<LumoKeyboardStatus> {
    val context = LocalContext.current
    val state = remember(context) { mutableStateOf(context.readLumoKeyboardStatus()) }

    DisposableEffect(context) {
        state.value = context.readLumoKeyboardStatus()
        onDispose {}
    }

    return state
}

@Composable
fun LumoSettingsScreen(
    isDefaultHome: Boolean,
    hasNotificationAccess: Boolean,
    keyboardStatus: LumoKeyboardStatus,
    hasOverlayPermission: Boolean,
    isGestureSidebarEnabled: Boolean,
    hasFullScreenIntentPermission: Boolean,
    supportsLockScreenCompanion: Boolean,
    isLockScreenCompanionEnabled: Boolean,
    lockScreenSecurityType: String,
    settings: dev.nighttraders.lumo.launcher.data.LumoLauncherSettings = dev.nighttraders.lumo.launcher.data.LumoLauncherSettings(),
    onRequestDefaultHome: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onEnableGestureSidebar: () -> Unit,
    onDisableGestureSidebar: () -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onShowInputMethodPicker: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onOpenLockScreenPermissionSettings: () -> Unit,
    onEnableLockScreenCompanion: () -> Unit,
    onDisableLockScreenCompanion: () -> Unit,
    onSetLockScreenPin: (String) -> Unit,
    onSetLockScreenPassword: (String) -> Unit,
    onClearLockScreenSecurity: () -> Unit,
    onVerifyCurrentSecurity: (String) -> Boolean = { true },
    onOpenWifiSettings: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    onOpenWallpaperSettings: () -> Unit,
    hasUsageStatsPermission: Boolean = false,
    onOpenUsageAccessSettings: () -> Unit = {},
    onUpdateIntSetting: (String, Int) -> Unit = { _, _ -> },
    onUpdateBoolSetting: (String, Boolean) -> Unit = { _, _ -> },
    onOpenDebugLog: () -> Unit = {},
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "System",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Text(
                    text = "Ubuntu Touch-style controls for launcher, keyboard, and lock screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB8AFBA),
                )
            }
        }

        item {
            SettingSection(
                title = "Launcher",
                subtitle = if (isDefaultHome) {
                    "Lumo is your current Home app."
                } else {
                    "Set Lumo as the default Home app."
                },
                actions = listOf(
                    SettingAction(
                        icon = Icons.Rounded.Home,
                        label = if (isDefaultHome) "Home role active" else "Set as default launcher",
                        onClick = onRequestDefaultHome,
                    ),
                    SettingAction(
                        icon = Icons.Rounded.Refresh,
                        label = "Refresh launcher data",
                        onClick = onRefresh,
                    ),
                ),
            )
        }

        item {
            SettingSection(
                title = "Notifications",
                subtitle = if (hasNotificationAccess) {
                    "Notification access is enabled."
                } else {
                    "Turn on notification access so Lumo can show messages and alerts."
                },
                actions = listOf(
                    SettingAction(
                        icon = Icons.Rounded.Notifications,
                        label = if (hasNotificationAccess) "Notification access enabled" else "Open notification access",
                        onClick = onRequestNotificationAccess,
                    ),
                ),
            )
        }

        item {
            SettingSection(
                title = "Gesture Sidebar",
                subtitle = when {
                    hasOverlayPermission && isGestureSidebarEnabled ->
                        "The left-edge Ubuntu Touch sidebar is enabled for other apps."
                    hasOverlayPermission ->
                        "Overlay permission is ready. Turn on the sidebar to reach Lumo from other apps."
                    else ->
                        "Allow Lumo to draw over other apps so the left-edge sidebar can appear system-wide."
                },
                actions = buildList {
                    add(
                        SettingAction(
                            icon = Icons.Rounded.Settings,
                            label = if (hasOverlayPermission) {
                                "Overlay permission enabled"
                            } else {
                                "Allow display over other apps"
                            },
                            onClick = onRequestOverlayPermission,
                        ),
                    )
                    add(
                        SettingAction(
                            icon = Icons.Rounded.Home,
                            label = if (isGestureSidebarEnabled) {
                                "Disable gesture sidebar"
                            } else {
                                "Enable gesture sidebar"
                            },
                            onClick = if (isGestureSidebarEnabled) {
                                onDisableGestureSidebar
                            } else {
                                onEnableGestureSidebar
                            },
                        ),
                    )
                },
            )
        }

        item {
            SettingSection(
                title = "Keyboard",
                subtitle = when {
                    keyboardStatus.isSelected -> "Lumo Keyboard is active."
                    keyboardStatus.isEnabled -> "Lumo Keyboard is enabled and ready to select."
                    else -> "Enable Lumo Keyboard from Android input settings."
                },
                actions = listOf(
                    SettingAction(
                        icon = Icons.Rounded.Keyboard,
                        label = "Open keyboard settings",
                        onClick = onOpenKeyboardSettings,
                    ),
                    SettingAction(
                        icon = Icons.Rounded.Keyboard,
                        label = "Show input method picker",
                        onClick = onShowInputMethodPicker,
                    ),
                ),
            )
        }

        item {
            SettingSection(
                title = "Lock Screen",
                subtitle = when {
                    !supportsLockScreenCompanion ->
                        "Android 14 and newer block Lumo's wake companion here. You can still preview the Ubuntu Touch-style lock screen manually."
                    isLockScreenCompanionEnabled && hasFullScreenIntentPermission ->
                        "Lumo will surface over the wake flow, then hand unlock back to Android."
                    hasFullScreenIntentPermission ->
                        "Full-screen access is ready. Enable the wake companion to show Lumo on wake."
                    else ->
                        "Grant full-screen access so Lumo can appear over the stock lock screen on wake."
                },
                actions = buildList {
                    add(
                        SettingAction(
                            icon = Icons.Rounded.Lock,
                            label = if (hasFullScreenIntentPermission) {
                                "Full-screen lock access enabled"
                            } else {
                                "Allow full-screen lock screen"
                            },
                            onClick = onOpenLockScreenPermissionSettings,
                        ),
                    )
                    add(
                        SettingAction(
                            icon = Icons.Rounded.Lock,
                            label = if (!supportsLockScreenCompanion) {
                                "Wake companion unavailable on this Android version"
                            } else if (isLockScreenCompanionEnabled) {
                                "Disable wake lock screen"
                            } else {
                                "Enable wake lock screen"
                            },
                            onClick = if (!supportsLockScreenCompanion) {
                                onOpenLockScreen
                            } else if (isLockScreenCompanionEnabled) {
                                onDisableLockScreenCompanion
                            } else {
                                onEnableLockScreenCompanion
                            },
                        ),
                    )
                    add(
                        SettingAction(
                            icon = Icons.Rounded.Lock,
                            label = "Preview Lumo lock screen",
                            onClick = onOpenLockScreen,
                        ),
                    )
                },
            )
        }

        item {
            LockScreenSecuritySection(
                currentType = lockScreenSecurityType,
                onSetPin = onSetLockScreenPin,
                onSetPassword = onSetLockScreenPassword,
                onClearSecurity = onClearLockScreenSecurity,
                onVerifyCurrentSecurity = onVerifyCurrentSecurity,
            )
        }

        // ── Appearance ───────────────────────────────────────────────────
        item {
            AppearanceSection(
                iconSizeDp = settings.appIconSizeDp,
                gridColumns = settings.appGridColumns,
                railWidthDp = settings.dashRailWidthDp,
                dashIconSizeDp = settings.dashIconSizeDp,
                onIconSizeChange = { onUpdateIntSetting("app_icon_size_dp", it) },
                onGridColumnsChange = { onUpdateIntSetting("app_grid_columns", it) },
                onRailWidthChange = { onUpdateIntSetting("dash_rail_width_dp", it) },
                onDashIconSizeChange = { onUpdateIntSetting("dash_icon_size_dp", it) },
            )
        }

        // ── Gestures ────────────────────────────────────────────────────
        item {
            GestureTogglesSection(
                settings = settings,
                onToggle = onUpdateBoolSetting,
            )
        }

        item {
            GestureSensitivitySection(
                settings = settings,
                onUpdate = onUpdateIntSetting,
            )
        }

        // ── Permissions ─────────────────────────────────────────────────
        item {
            SettingSection(
                title = "Permissions",
                subtitle = "Grant permissions for full launcher functionality.",
                actions = buildList {
                    if (!hasUsageStatsPermission) {
                        add(
                            SettingAction(
                                icon = Icons.Rounded.Info,
                                label = "Grant usage access (for recent apps tracking)",
                                onClick = onOpenUsageAccessSettings,
                            ),
                        )
                    } else {
                        add(
                            SettingAction(
                                icon = Icons.Rounded.Info,
                                label = "Usage access granted",
                                onClick = {},
                            ),
                        )
                    }
                },
            )
        }

        item {
            SettingSection(
                title = "System Shortcuts",
                subtitle = "Quick links for common phone settings.",
                actions = listOf(
                    SettingAction(
                        icon = Icons.Rounded.Wifi,
                        label = "Wi-Fi settings",
                        onClick = onOpenWifiSettings,
                    ),
                    SettingAction(
                        icon = Icons.Rounded.Settings,
                        label = "Display settings",
                        onClick = onOpenDisplaySettings,
                    ),
                    SettingAction(
                        icon = Icons.Rounded.Palette,
                        label = "Wallpaper settings",
                        onClick = onOpenWallpaperSettings,
                    ),
                ),
            )
        }

        item {
            SettingSection(
                title = "Developer",
                subtitle = "Debug tools and diagnostics.",
                actions = listOf(
                    SettingAction(
                        icon = Icons.Rounded.Terminal,
                        label = "Open debug log",
                        onClick = onOpenDebugLog,
                    ),
                ),
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private data class SettingAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
private fun SettingSection(
    title: String,
    subtitle: String,
    actions: List<SettingAction>,
) {
    Surface(
        color = Color(0x55120B14),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8AFBA),
            )
            actions.forEach { action ->
                SettingActionRow(action = action)
            }
        }
    }
}

@Composable
private fun SettingActionRow(
    action: SettingAction,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = action.onClick),
        color = Color(0x33000000),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = action.label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun LockScreenSecuritySection(
    currentType: String,
    onSetPin: (String) -> Unit,
    onSetPassword: (String) -> Unit,
    onClearSecurity: () -> Unit,
    onVerifyCurrentSecurity: (String) -> Boolean,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // "none", "pin", "password"
    var verifyError by remember { mutableStateOf(false) }

    val hasExistingSecurity = currentType != "none"

    fun requestSecurityChange(targetType: String) {
        if (hasExistingSecurity) {
            pendingAction = targetType
            showVerifyDialog = true
            verifyError = false
        } else {
            when (targetType) {
                "none" -> onClearSecurity()
                "pin" -> showPinDialog = true
                "password" -> showPasswordDialog = true
            }
        }
    }

    val statusText = when (currentType) {
        "pin" -> "Lumo lock screen is secured with a PIN."
        "password" -> "Lumo lock screen is secured with a password."
        else -> "No security is set. Anyone can swipe to unlock."
    }

    Surface(
        color = Color(0x55120B14),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Lock Screen Security",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8AFBA),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecurityTypeChip(
                    label = "None",
                    active = currentType == "none",
                    modifier = Modifier.weight(1f),
                    onClick = { requestSecurityChange("none") },
                )
                SecurityTypeChip(
                    label = "PIN",
                    active = currentType == "pin",
                    modifier = Modifier.weight(1f),
                    onClick = { requestSecurityChange("pin") },
                )
                SecurityTypeChip(
                    label = "Password",
                    active = currentType == "password",
                    modifier = Modifier.weight(1f),
                    onClick = { requestSecurityChange("password") },
                )
            }
        }
    }

    // Verify current security before allowing changes
    if (showVerifyDialog) {
        SecurityVerifyDialog(
            currentType = currentType,
            error = verifyError,
            onVerify = { input ->
                if (onVerifyCurrentSecurity(input)) {
                    showVerifyDialog = false
                    verifyError = false
                    when (pendingAction) {
                        "none" -> onClearSecurity()
                        "pin" -> showPinDialog = true
                        "password" -> showPasswordDialog = true
                    }
                    pendingAction = null
                } else {
                    verifyError = true
                }
            },
            onDismiss = {
                showVerifyDialog = false
                pendingAction = null
                verifyError = false
            },
        )
    }

    if (showPinDialog) {
        SecurityInputDialog(
            title = "Set PIN",
            placeholder = "Enter 4\u201310 digit PIN",
            keyboardType = KeyboardType.NumberPassword,
            maxLength = 10,
            onConfirm = { pin ->
                if (pin.length in 4..10) {
                    onSetPin(pin)
                    showPinDialog = false
                }
            },
            onDismiss = { showPinDialog = false },
        )
    }

    if (showPasswordDialog) {
        SecurityInputDialog(
            title = "Set Password",
            placeholder = "Enter 4\u201332 char password",
            keyboardType = KeyboardType.Password,
            maxLength = 32,
            onConfirm = { password ->
                if (password.length in 4..32) {
                    onSetPassword(password)
                    showPasswordDialog = false
                }
            },
            onDismiss = { showPasswordDialog = false },
        )
    }
}

@Composable
private fun SecurityVerifyDialog(
    currentType: String,
    error: Boolean,
    onVerify: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1420),
        title = {
            Text(
                text = "Enter current ${currentType}",
                color = Color.White,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { newValue ->
                        val maxLen = if (currentType == "pin") 10 else 32
                        input = newValue
                            .take(maxLen)
                            .filter { it.code >= 0x20 && it.code != 0x7F }
                    },
                    placeholder = { Text("Current ${currentType}") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (currentType == "pin") KeyboardType.NumberPassword else KeyboardType.Password,
                    ),
                    isError = error,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error) {
                    Text(
                        text = "Incorrect ${currentType}",
                        color = Color(0xFFED3146),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onVerify(input) }) {
                Text("Verify", color = Color(0xFFE95420))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFB8AFBA))
            }
        },
    )
}

@Composable
private fun SecurityTypeChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (active) Color(0xFFE95420) else Color(0x33000000),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun SecurityInputDialog(
    title: String,
    placeholder: String,
    keyboardType: KeyboardType,
    maxLength: Int = 32,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(0) } // 0 = enter, 1 = confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1420),
        title = {
            Text(
                text = if (step == 0) title else "Confirm $title",
                color = Color.White,
            )
        },
        text = {
            OutlinedTextField(
                value = if (step == 0) input else confirm,
                onValueChange = { newValue ->
                    val sanitized = newValue
                        .take(maxLength)
                        .filter { it.code >= 0x20 && it.code != 0x7F }
                    if (step == 0) input = sanitized else confirm = sanitized
                },
                placeholder = {
                    Text(
                        text = if (step == 0) placeholder else "Re-enter to confirm",
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (step == 0 && input.length >= 4) {
                    step = 1
                } else if (step == 1 && confirm == input) {
                    onConfirm(input)
                }
            }) {
                Text(
                    text = if (step == 0) "Next" else "Set",
                    color = Color(0xFFE95420),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFB8AFBA))
            }
        },
    )
}

// ── Appearance Section ──────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(
    iconSizeDp: Int,
    gridColumns: Int,
    railWidthDp: Int,
    dashIconSizeDp: Int,
    onIconSizeChange: (Int) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onRailWidthChange: (Int) -> Unit,
    onDashIconSizeChange: (Int) -> Unit,
) {
    Surface(
        color = Color(0x55120B14),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                text = "Customise icon sizes, grid layout, and the launcher rail.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8AFBA),
            )

            SettingSlider(
                label = "App Icon Size",
                value = iconSizeDp,
                valueLabel = "${iconSizeDp}dp",
                range = 36f..72f,
                steps = 5,
                onValueChange = onIconSizeChange,
            )
            SettingSlider(
                label = "Grid Columns",
                value = gridColumns,
                valueLabel = "$gridColumns",
                range = 3f..6f,
                steps = 2,
                onValueChange = onGridColumnsChange,
            )
            SettingSlider(
                label = "Dash Rail Width",
                value = railWidthDp,
                valueLabel = "${railWidthDp}dp",
                range = 52f..84f,
                steps = 3,
                onValueChange = onRailWidthChange,
            )
            SettingSlider(
                label = "Dash Icon Size",
                value = dashIconSizeDp,
                valueLabel = "${dashIconSizeDp}dp",
                range = 36f..64f,
                steps = 6,
                onValueChange = onDashIconSizeChange,
            )
        }
    }
}

// ── Gesture Toggles ─────────────────────────────────────────────────────────

@Composable
private fun GestureTogglesSection(
    settings: dev.nighttraders.lumo.launcher.data.LumoLauncherSettings,
    onToggle: (String, Boolean) -> Unit,
) {
    Surface(
        color = Color(0x55120B14),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Gestures",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                text = "Toggle Ubuntu Touch-style gestures on or off.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8AFBA),
            )

            SettingToggle(
                label = "Bottom edge (app drawer)",
                subtitle = "Swipe up from bottom to open apps",
                checked = settings.bottomEdgeGestureEnabled,
                onCheckedChange = { onToggle("bottom_edge_gesture_enabled", it) },
            )
            SettingToggle(
                label = "Left edge (launcher rail)",
                subtitle = "Swipe from left edge to reveal the dash",
                checked = settings.leftEdgeGestureEnabled,
                onCheckedChange = { onToggle("left_edge_gesture_enabled", it) },
            )
            SettingToggle(
                label = "Multitask swipe",
                subtitle = "Swipe right on home to show recent apps",
                checked = settings.multitaskGestureEnabled,
                onCheckedChange = { onToggle("multitask_gesture_enabled", it) },
            )
            SettingToggle(
                label = "Indicator pull-down",
                subtitle = "Pull down from the top bar to expand indicators",
                checked = settings.indicatorSwipeEnabled,
                onCheckedChange = { onToggle("indicator_swipe_enabled", it) },
            )

        }
    }
}

// ── Gesture Sensitivity ──────────────────────────────────────────────────────

@Composable
private fun GestureSensitivitySection(
    settings: dev.nighttraders.lumo.launcher.data.LumoLauncherSettings,
    onUpdate: (String, Int) -> Unit,
) {
    Surface(
        color = Color(0x55120B14),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Gesture Sensitivity",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                text = "Adjust the size and sensitivity of each gesture zone.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8AFBA),
            )

            SettingSlider(
                label = "Bottom Edge Height",
                value = settings.bottomEdgeHeightDp,
                valueLabel = "${settings.bottomEdgeHeightDp}dp",
                range = 40f..120f,
                steps = 7,
                onValueChange = { onUpdate("bottom_edge_height_dp", it) },
            )
            SettingSlider(
                label = "Bottom Edge Threshold",
                value = settings.bottomEdgeThresholdDp,
                valueLabel = "${settings.bottomEdgeThresholdDp}dp",
                range = 20f..80f,
                steps = 5,
                onValueChange = { onUpdate("bottom_edge_threshold_dp", it) },
            )
            SettingSlider(
                label = "Left Edge Width",
                value = settings.leftEdgeWidthDp,
                valueLabel = "${settings.leftEdgeWidthDp}dp",
                range = 10f..40f,
                steps = 5,
                onValueChange = { onUpdate("left_edge_width_dp", it) },
            )
            SettingSlider(
                label = "Left Edge Threshold",
                value = settings.leftEdgeThresholdDp,
                valueLabel = "${settings.leftEdgeThresholdDp}dp",
                range = 12f..48f,
                steps = 5,
                onValueChange = { onUpdate("left_edge_threshold_dp", it) },
            )
            SettingSlider(
                label = "Horizontal Swipe Threshold",
                value = settings.horizontalSwipeThresholdDp,
                valueLabel = "${settings.horizontalSwipeThresholdDp}dp",
                range = 40f..160f,
                steps = 5,
                onValueChange = { onUpdate("horizontal_swipe_threshold_dp", it) },
            )
            SettingSlider(
                label = "Multitask Swipe Threshold",
                value = settings.multitaskSwipeThresholdDp,
                valueLabel = "${settings.multitaskSwipeThresholdDp}dp",
                range = 40f..160f,
                steps = 5,
                onValueChange = { onUpdate("multitask_swipe_threshold_dp", it) },
            )
        }
    }
}

// ── Reusable slider & toggle ────────────────────────────────────────────────

@Composable
private fun SettingSlider(
    label: String,
    value: Int,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(text = valueLabel, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE95420))
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0x33000000),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8AFBA))
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

fun Context.readLumoKeyboardStatus(): LumoKeyboardStatus {
    val inputMethodManager = getSystemService(InputMethodManager::class.java)
    val enabledMethods = runCatching {
        inputMethodManager?.enabledInputMethodList.orEmpty()
    }.getOrDefault(emptyList())
    val currentMethod = runCatching {
        inputMethodManager?.let(::currentInputMethodInfoCompat)
    }.getOrNull()

    val isEnabled = enabledMethods.any { method ->
        method.id == LUMO_INPUT_METHOD_ID ||
            method.id == LUMO_INPUT_METHOD_CLASS ||
            method.packageName == packageName
    }
    val isSelected = currentMethod?.id == LUMO_INPUT_METHOD_ID ||
        currentMethod?.id == LUMO_INPUT_METHOD_CLASS

    return LumoKeyboardStatus(
        isEnabled = isEnabled,
        isSelected = isSelected,
    )
}

private fun currentInputMethodInfoCompat(inputMethodManager: InputMethodManager): InputMethodInfo? =
    runCatching {
        InputMethodManager::class.java
            .getMethod("getCurrentInputMethodInfo")
            .invoke(inputMethodManager) as? InputMethodInfo
    }.getOrNull()
