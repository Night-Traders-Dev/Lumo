package dev.nighttraders.lumo.launcher.ui

import android.content.Context
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    onOpenWifiSettings: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    onOpenWallpaperSettings: () -> Unit,
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
