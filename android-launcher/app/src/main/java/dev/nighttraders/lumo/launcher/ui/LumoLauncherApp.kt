package dev.nighttraders.lumo.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import dev.nighttraders.lumo.launcher.lockscreen.LumoLockScreenScreen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import dev.nighttraders.lumo.launcher.data.LaunchableApp
import dev.nighttraders.lumo.launcher.data.LumoDebugLog
import dev.nighttraders.lumo.launcher.notifications.LauncherNotification
import dev.nighttraders.lumo.launcher.notifications.LauncherNotificationCenter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

private enum class ScopePage(val title: String) {
    Home("Today"),
}

/** Tracks which surface is currently showing so every button/gesture knows the global state. */
private enum class LauncherScreen {
    HOME,
    APP_DRAWER_BOTTOM,
    APP_DRAWER_SIDE,
    MULTITASK,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LumoLauncherApp(
    uiState: LauncherUiState,
    systemStatus: SystemStatusSnapshot,
    isDefaultHome: Boolean,
    requestedPageIndex: Int,
    navigationRequestId: Int = 0,
    settings: dev.nighttraders.lumo.launcher.data.LumoLauncherSettings = dev.nighttraders.lumo.launcher.data.LumoLauncherSettings(),
    onOpenWallpaperPicker: () -> Unit = {},
    isDashLocked: Boolean = false,
    lockScreenSecurityType: String = "none",
    onVerifyPin: (String) -> Boolean = { false },
    onDashUnlock: () -> Unit = {},
    onRequestDefaultHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onOpenWifiSettings: () -> Unit = {},
    onOpenBluetoothSettings: () -> Unit = {},
    onOpenAirplaneSettings: () -> Unit = {},
    onToggleFlashlight: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
    isFlashlightOn: Boolean = false,
    onLaunchApp: (LaunchableApp) -> Unit,
    onOpenNotification: (LauncherNotification) -> Unit,
    onOpenNotificationApp: (LauncherNotification) -> Unit,
    onDismissNotification: (LauncherNotification) -> Result<Unit>,
    onSnoozeNotification: (LauncherNotification, Long) -> Result<Unit>,
    onDismissHeadsUpNotification: (String) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    onAddFavorite: (String) -> Unit = {},
    onReorderFavorites: (List<String>) -> Unit = {},
    onOpenAppInfo: (LaunchableApp) -> Unit,
    onRequestUninstall: (LaunchableApp) -> Unit,
    onRefresh: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var indicatorsExpanded by rememberSaveable { mutableStateOf(false) }
    var railVisible by rememberSaveable { mutableStateOf(false) }
    var notificationActionTarget by remember { mutableStateOf<LauncherNotification?>(null) }
    var appActionTarget by remember { mutableStateOf<LaunchableApp?>(null) }
    var showSwipeHint by rememberSaveable { mutableStateOf(false) }
    var swipeHintShown by rememberSaveable { mutableStateOf(false) }
    var currentScreen by rememberSaveable { mutableStateOf(LauncherScreen.HOME) }

    // React to external navigation requests (e.g. gesture sidebar "open apps" intent)
    LaunchedEffect(navigationRequestId) {
        if (navigationRequestId > 0) {
            when (requestedPageIndex) {
                2 -> {
                    // Dash rail request (from overlay edge gesture)
                    railVisible = !railVisible
                    LumoDebugLog.d("Nav", "Intent reqId=$navigationRequestId -> toggle dash rail (visible=$railVisible)")
                }
                else -> {
                    val requested = when (requestedPageIndex) {
                        1 -> LauncherScreen.APP_DRAWER_SIDE
                        else -> LauncherScreen.HOME
                    }
                    val target = if (currentScreen == requested && requested != LauncherScreen.HOME) {
                        LauncherScreen.HOME
                    } else {
                        requested
                    }
                    LumoDebugLog.d("Nav", "Intent reqId=$navigationRequestId page=$requestedPageIndex current=$currentScreen -> $target")
                    currentScreen = target
                }
            }
        }
    }

    // Derived convenience flags from the single source of truth
    val appsOverlayFromBottom = currentScreen == LauncherScreen.APP_DRAWER_BOTTOM
    val appsOverlayFromSide = currentScreen == LauncherScreen.APP_DRAWER_SIDE
    val appsVisible = appsOverlayFromBottom || appsOverlayFromSide
    val showMultitask = currentScreen == LauncherScreen.MULTITASK


    // Show swipe hint once after first load
    LaunchedEffect(uiState.isLoading, isDashLocked) {
        if (!uiState.isLoading && !isDashLocked && !swipeHintShown) {
            swipeHintShown = true
            showSwipeHint = true
            delay(3000)
            showSwipeHint = false
        }
    }

    // If dash is locked, show the lock screen instead of the dash
    if (isDashLocked) {
        val notifications by LauncherNotificationCenter.notifications.collectAsStateWithLifecycle()
        LumoLockScreenScreen(
            status = systemStatus,
            notifications = notifications,
            securityType = lockScreenSecurityType,
            onUnlock = onDashUnlock,
            onVerifyPin = onVerifyPin,
        )
        return
    }

    val launcherApps = remember(uiState.favorites, uiState.featuredApps) {
        uiState.favorites.ifEmpty { uiState.featuredApps }.take(6)
    }
    val visibleApps = remember(uiState.apps, searchQuery) {
        val normalizedQuery = searchQuery.trim().lowercase(Locale.getDefault())
        if (normalizedQuery.isEmpty()) {
            uiState.apps
        } else {
            uiState.apps.filter { app ->
                app.label.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                    app.packageName.lowercase(Locale.getDefault()).contains(normalizedQuery)
            }
        }
    }

    LaunchedEffect(indicatorsExpanded) {
        if (indicatorsExpanded) {
            railVisible = false
        }
    }

    // Debug: log dash rail state changes
    LaunchedEffect(railVisible) {
        val effWidth = maxOf(settings.dashRailWidthDp, settings.dashIconSizeDp + 16)
        LumoDebugLog.d(
            "Dash",
            if (railVisible) "Rail shown — railWidth=${settings.dashRailWidthDp}dp iconSize=${settings.dashIconSizeDp}dp effectiveWidth=${effWidth}dp"
            else "Rail hidden",
        )
    }

    LaunchedEffect(settings.dashRailWidthDp, settings.dashIconSizeDp) {
        val effWidth = maxOf(settings.dashRailWidthDp, settings.dashIconSizeDp + 16)
        LumoDebugLog.d("Dash", "Settings changed — railWidth=${settings.dashRailWidthDp}dp iconSize=${settings.dashIconSizeDp}dp effectiveWidth=${effWidth}dp")
    }

    LaunchedEffect(uiState.headsUpNotification?.key) {
        val headsUp = uiState.headsUpNotification ?: return@LaunchedEffect
        delay(6_000)
        onDismissHeadsUpNotification(headsUp.key)
    }

    fun openApps(fromBottom: Boolean = false) {
        val target = if (fromBottom) LauncherScreen.APP_DRAWER_BOTTOM else LauncherScreen.APP_DRAWER_SIDE
        LumoDebugLog.d("Nav", "openApps from=$currentScreen to=$target (fromBottom=$fromBottom)")
        railVisible = false
        indicatorsExpanded = false
        currentScreen = target
    }

    fun goHome() {
        LumoDebugLog.d("Nav", "goHome from=$currentScreen")
        railVisible = false
        currentScreen = LauncherScreen.HOME
    }

    val blurRadius = animateDpAsState(
        targetValue = if (appsVisible) 20.dp else 0.dp,
        animationSpec = tween(300),
        label = "appDrawerBlur",
    )

    // Animated padding so content shifts right when the dash rail is visible
    val effectiveRailWidthDp = maxOf(settings.dashRailWidthDp, settings.dashIconSizeDp + 16)
    val railContentOffset = animateDpAsState(
        targetValue = if (railVisible) (effectiveRailWidthDp + 4).dp else 0.dp,
        animationSpec = tween(250),
        label = "railContentOffset",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background layer — gets blurred when app drawer is visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurRadius.value > 0.dp) Modifier.blur(blurRadius.value) else Modifier,
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0B12),
                            Color(0xFF1A0816),
                            Color(0xFF2C001E),
                            Color(0xFF0B090E),
                        ),
                    ),
                ),
        ) {
            UbuntuTouchBackdrop(wallpaperPath = settings.wallpaperPath)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            UbuntuTouchTopBar(
                status = systemStatus,
                activeNotificationCount = uiState.activeNotificationCount,
                hasNotificationAccess = uiState.hasNotificationAccess,
                indicatorsExpanded = indicatorsExpanded,
                onToggleIndicators = { indicatorsExpanded = !indicatorsExpanded },
                onExpandIndicators = { indicatorsExpanded = true },
                onCollapseIndicators = { indicatorsExpanded = false },
                onOpenMultitask = { currentScreen = LauncherScreen.MULTITASK },
            )

            if (!isDefaultHome) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DefaultHomePill(onRequestDefaultHome = onRequestDefaultHome)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = railContentOffset.value)
                        .then(
                            if (blurRadius.value > 0.dp) Modifier.blur(blurRadius.value) else Modifier,
                        )
                        .pointerInput(appsOverlayFromSide, settings.horizontalSwipeThresholdDp) {
                            val thresholdPx = settings.horizontalSwipeThresholdDp * density
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                },
                                onDragCancel = { totalDrag = 0f },
                                onDragEnd = {
                                    when {
                                        !appsOverlayFromSide && totalDrag < -thresholdPx -> openApps(fromBottom = false)
                                        appsOverlayFromSide && totalDrag > thresholdPx -> goHome()
                                    }
                                    totalDrag = 0f
                                },
                            )
                        },
                ) {
                    HomeScopePage(
                        status = systemStatus,
                        onOpenWallpaperPicker = onOpenWallpaperPicker,
                    )
                }

                // appsVisible is computed at the top of the composable

                // Apps overlay sliding from bottom when triggered by swipe-up
                androidx.compose.animation.AnimatedVisibility(
                    visible = appsOverlayFromBottom,
                    modifier = Modifier.fillMaxSize(),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ) + fadeOut(animationSpec = tween(150)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(settings.horizontalSwipeThresholdDp) {
                                val thresholdPx = settings.horizontalSwipeThresholdDp * density
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                                    onDragCancel = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag > thresholdPx) goHome()
                                        totalDrag = 0f
                                    },
                                )
                            },
                    ) {
                        AppsScopePage(
                            apps = visibleApps,
                            favoriteKeys = uiState.favoriteKeys,
                            isLoading = uiState.isLoading,
                            searchQuery = searchQuery,
                            gridColumns = settings.appGridColumns,
                            iconSizeDp = settings.appIconSizeDp,
                            onSearchQueryChange = { searchQuery = it },
                            onLaunchApp = onLaunchApp,
                            onLongPressApp = { appActionTarget = it },
                        )
                    }
                }

                // Apps overlay sliding from right when triggered by horizontal swipe
                androidx.compose.animation.AnimatedVisibility(
                    visible = appsOverlayFromSide,
                    modifier = Modifier.fillMaxSize(),
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ) + fadeOut(animationSpec = tween(150)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(settings.horizontalSwipeThresholdDp) {
                                val thresholdPx = settings.horizontalSwipeThresholdDp * density
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                                    onDragCancel = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag > thresholdPx) goHome()
                                        totalDrag = 0f
                                    },
                                )
                            },
                    ) {
                        AppsScopePage(
                            apps = visibleApps,
                            favoriteKeys = uiState.favoriteKeys,
                            isLoading = uiState.isLoading,
                            searchQuery = searchQuery,
                            gridColumns = settings.appGridColumns,
                            iconSizeDp = settings.appIconSizeDp,
                            onSearchQueryChange = { searchQuery = it },
                            onLaunchApp = onLaunchApp,
                            onLongPressApp = { appActionTarget = it },
                        )
                    }
                }

                if (settings.bottomEdgeGestureEnabled) {
                    BottomEdgeGestureHandle(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        heightDp = settings.bottomEdgeHeightDp,
                        thresholdDp = settings.bottomEdgeThresholdDp,
                        currentScreen = currentScreen,
                        onOpenApps = { openApps(fromBottom = true) },
                        onOpenMultitask = { currentScreen = LauncherScreen.MULTITASK },
                        onGoHome = { goHome() },
                    )
                }

                // Left edge gesture: always available so the rail can be revealed from any screen
                if (settings.leftEdgeGestureEnabled) {
                    LeftEdgeRevealHandle(
                        modifier = Modifier.align(Alignment.CenterStart),
                        widthDp = settings.leftEdgeWidthDp,
                        thresholdDp = settings.leftEdgeThresholdDp,
                        railVisible = railVisible,
                        onRevealRail = { railVisible = true },
                    )
                }

                if (!appsVisible && !indicatorsExpanded && settings.multitaskGestureEnabled) {
                    // Multitask gesture: left-to-right swipe on center area (not edge)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 40.dp)
                            .fillMaxHeight()
                            .fillMaxWidth(0.4f)
                            .pointerInput(settings.multitaskSwipeThresholdDp) {
                                val thresholdPx = settings.multitaskSwipeThresholdDp * density
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        if (dragAmount > 0f) totalDrag += dragAmount
                                    },
                                    onDragCancel = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag > thresholdPx) {
                                            currentScreen = LauncherScreen.MULTITASK
                                        }
                                        totalDrag = 0f
                                    },
                                )
                            },
                    )
                }

            }
        }

        // Dash rail — flush against the left edge, below the launcher top bar
        // Top bar occupies ~46dp from top (6dp column padding + 40dp bar height)
        if (railVisible) {
            val dismissInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = dismissInteraction,
                        indication = null,
                        onClick = { railVisible = false },
                    ),
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = railVisible,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 48.dp),
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) + fadeOut(animationSpec = tween(150)),
        ) {
            UbuntuTouchLauncherRail(
                appsVisible = appsVisible,
                railWidthDp = settings.dashRailWidthDp,
                iconSizeDp = settings.dashIconSizeDp,
                apps = launcherApps,
                onOpenApps = { if (appsVisible) goHome() else openApps() },
                onOpenSettings = onOpenSettings,
                onLaunchApp = { app ->
                    railVisible = false
                    onLaunchApp(app)
                },
                onToggleFavorite = onToggleFavorite,
                onReorderFavorites = onReorderFavorites,
            )
        }

        // Scrim behind indicators
        androidx.compose.animation.AnimatedVisibility(
            visible = indicatorsExpanded,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            val dismissInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = dismissInteraction,
                        indication = null,
                        onClick = { indicatorsExpanded = false },
                    ),
            )
        }

        // Indicators panel
        androidx.compose.animation.AnimatedVisibility(
            visible = indicatorsExpanded,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) + fadeOut(animationSpec = tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(top = 46.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                IndicatorsSheet(
                    status = systemStatus,
                    isDefaultHome = isDefaultHome,
                    hasNotificationAccess = uiState.hasNotificationAccess,
                    notifications = uiState.recentNotifications,
                    isFlashlightOn = isFlashlightOn,
                    onRefresh = onRefresh,
                    onRequestDefaultHome = onRequestDefaultHome,
                    onOpenSettings = onOpenSettings,
                    onOpenLockScreen = onOpenLockScreen,
                    onRequestNotificationAccess = onRequestNotificationAccess,
                    onOpenWifiSettings = onOpenWifiSettings,
                    onOpenBluetoothSettings = onOpenBluetoothSettings,
                    onOpenAirplaneSettings = onOpenAirplaneSettings,
                    onToggleFlashlight = onToggleFlashlight,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onOpenNotification = onOpenNotification,
                    onLongPressNotification = { notificationActionTarget = it },
                    onDismissNotification = onDismissNotification,
                )
            }
        }

        uiState.headsUpNotification?.let { notification ->
            NotificationHeadsUp(
                notification = notification,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(top = 54.dp, start = 12.dp, end = 12.dp),
                onOpenNotification = {
                    onOpenNotification(notification)
                },
                onLongPressNotification = { notificationActionTarget = notification },
                onDismissNotification = { onDismissNotification(notification) },
                onDismiss = {
                    onDismissHeadsUpNotification(notification.key)
                },
            )
        }

        notificationActionTarget?.let { notification ->
            NotificationActionSheet(
                notification = notification,
                onDismiss = { notificationActionTarget = null },
                onOpenNotification = {
                    notificationActionTarget = null
                    onOpenNotification(notification)
                },
                onOpenApp = {
                    notificationActionTarget = null
                    onOpenNotificationApp(notification)
                },
                onSnooze = { durationMillis ->
                    notificationActionTarget = null
                    onSnoozeNotification(notification, durationMillis)
                },
                onDismissNotification = {
                    notificationActionTarget = null
                    onDismissNotification(notification)
                },
            )
        }

        appActionTarget?.let { app ->
            AppActionSheet(
                app = app,
                isFavorite = uiState.favoriteKeys.contains(app.componentKey),
                onDismiss = { appActionTarget = null },
                onLaunchApp = {
                    appActionTarget = null
                    onLaunchApp(app)
                },
                onToggleFavorite = {
                    onToggleFavorite(app)
                    appActionTarget = null
                },
                onOpenAppInfo = {
                    appActionTarget = null
                    onOpenAppInfo(app)
                },
                onRequestUninstall = {
                    appActionTarget = null
                    onRequestUninstall(app)
                },
            )
        }

        // Multitask / recent apps overlay (Ubuntu Touch style)
        if (showMultitask) {
            LaunchedEffect(Unit) {
                LumoDebugLog.d("Multitask", "Showing spread — ${uiState.recentApps.size} recent, ${uiState.apps.size} total")
            }
            MultitaskOverlay(
                recentApps = uiState.recentApps,
                allApps = uiState.apps,
                onLaunchApp = { app ->
                    LumoDebugLog.i("Multitask", "Launching ${app.label} from spread")
                    currentScreen = LauncherScreen.HOME
                    onLaunchApp(app)
                },
                onDismiss = {
                    LumoDebugLog.d("Multitask", "Dismissed spread -> HOME")
                    currentScreen = LauncherScreen.HOME
                },
            )
        }

    }
}

@Composable
private fun LumoToast(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        ),
        exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(300),
        ),
    ) {
        Surface(
            color = Color(0xDD1A1420),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE95420)),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFE7DFEA),
                )
            }
        }
    }
}

@Composable
private fun MultitaskOverlay(
    recentApps: List<LaunchableApp>,
    allApps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    onDismiss: () -> Unit,
) {
    val appsToShow = remember(recentApps, allApps) {
        recentApps.ifEmpty { allApps.take(6) }.take(8)
    }
    var dismissedKeys by remember { mutableStateOf(emptySet<String>()) }
    val visibleApps = remember(appsToShow, dismissedKeys) {
        appsToShow.filterNot { it.componentKey in dismissedKeys }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF00C0A10),
                        Color(0xF01A0816),
                        Color(0xF02C001E),
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        if (visibleApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No recent apps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF777777),
                )
            }
        } else {
            // Ubuntu Touch spread: horizontally overlapping cards with 3D perspective
            val cardWidthDp = 240.dp
            val stepDp = 120.dp // horizontal offset per card (smaller = more overlap)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = 72.dp)
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
                contentAlignment = Alignment.Center,
            ) {
                val scrollState = rememberScrollState()
                val cardHeightDp = 320.dp
                Box(
                    modifier = Modifier
                        .height(cardHeightDp)
                        .horizontalScroll(scrollState)
                        .padding(vertical = 8.dp),
                ) {
                    // Total width: enough to hold all overlapping cards
                    val totalWidth = cardWidthDp + stepDp * (visibleApps.size - 1).coerceAtLeast(0) + 32.dp
                    Spacer(modifier = Modifier.width(totalWidth).height(cardHeightDp))

                    visibleApps.forEachIndexed { index, app ->
                        SpreadCard(
                            app = app,
                            index = index,
                            total = visibleApps.size,
                            modifier = Modifier
                                .padding(start = 16.dp + stepDp * index),
                            onClick = { onLaunchApp(app) },
                            onClose = {
                                dismissedKeys = dismissedKeys + app.componentKey
                            },
                        )
                    }
                }
            }
        }

        // Bottom dock bar
        SpreadBottomDock(
            apps = visibleApps,
            modifier = Modifier.align(Alignment.BottomCenter),
            onLaunchApp = onLaunchApp,
            onHideApps = onDismiss,
        )
    }
}

@Composable
private fun SpreadCard(
    app: LaunchableApp,
    index: Int,
    total: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    // Slight Y-axis rotation for depth: cards further right rotate more
    val rotationY = remember(index, total) {
        if (total <= 1) 0f else -8f + (index.toFloat() / (total - 1).coerceAtLeast(1)) * 16f
    }

    // Swipe-up-to-dismiss
    var offsetY by remember { mutableStateOf(0f) }
    val animatedOffsetY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "spreadSwipe",
    )

    // Derive card background from app accent color
    val accentColor = remember(app.accentSeed) {
        if (app.accentSeed != 0) {
            Color(app.accentSeed).copy(alpha = 1f)
        } else {
            Color(0xFF242030)
        }
    }
    val cardBg = remember(accentColor) {
        // Darken the accent for a subtle tinted background
        accentColor.copy(
            red = accentColor.red * 0.25f,
            green = accentColor.green * 0.25f,
            blue = accentColor.blue * 0.25f,
        )
    }

    Surface(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight()
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12f * density
                translationY = animatedOffsetY
                alpha = (1f - (abs(animatedOffsetY) / 1200f)).coerceIn(0f, 1f)
                shadowElevation = 12f
            }
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        // Only allow upward drag
                        if (dragAmount < 0 || totalDrag < 0) {
                            totalDrag += dragAmount
                            offsetY = totalDrag
                        }
                    },
                    onDragCancel = {
                        totalDrag = 0f
                        offsetY = 0f
                    },
                    onDragEnd = {
                        if (totalDrag < -250f) {
                            offsetY = -1500f
                            onClose()
                        } else {
                            totalDrag = 0f
                            offsetY = 0f
                        }
                    },
                )
            }
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF1E1A24),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title bar mimicking Ubuntu Touch window decoration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3C3348))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app = app, size = 18.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0x55FFFFFF))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            // Window preview area — accent-tinted background with app icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardBg),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(app = app, size = 72.dp)
            }
        }
    }
}

@Composable
private fun SpreadBottomDock(
    apps: List<LaunchableApp>,
    modifier: Modifier = Modifier,
    onLaunchApp: (LaunchableApp) -> Unit,
    onHideApps: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        color = Color(0xDD0E0A10),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ubuntu button: "Hide Apps" — returns to home
            Column(
                modifier = Modifier
                    .clickable(onClick = onHideApps)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        id = dev.nighttraders.lumo.launcher.R.drawable.ic_ubuntu_symbol,
                    ),
                    contentDescription = "Hide Apps",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = "Hide Apps",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB8AFBA),
                    fontSize = 9.sp,
                )
            }

            // App icons for each spread card
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                apps.forEach { app ->
                    Column(
                        modifier = Modifier
                            .clickable { onLaunchApp(app) }
                            .padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIcon(app = app, size = 36.dp)
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB8AFBA),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(56.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftEdgeRevealHandle(
    modifier: Modifier = Modifier,
    widthDp: Int = 20,
    thresholdDp: Int = 24,
    railVisible: Boolean,
    onRevealRail: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(widthDp.dp)
            .pointerInput(railVisible, thresholdDp) {
                val thresholdPx = thresholdDp * density
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount > 0f) {
                            totalDrag += dragAmount
                        }
                    },
                    onDragCancel = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag > thresholdPx) {
                            onRevealRail()
                        }
                        totalDrag = 0f
                    },
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onRevealRail,
            ),
    )
}

@Composable
private fun BottomEdgeGestureHandle(
    modifier: Modifier = Modifier,
    heightDp: Int = 70,
    thresholdDp: Int = 42,
    currentScreen: LauncherScreen,
    onOpenApps: () -> Unit,
    onOpenMultitask: () -> Unit,
    onGoHome: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .pointerInput(currentScreen, thresholdDp) {
                val thresholdPx = thresholdDp * density
                val longThresholdPx = thresholdPx * 3.5f // longer swipe = app switcher
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragCancel = { totalDrag = 0f },
                    onDragEnd = {
                        when (currentScreen) {
                            LauncherScreen.HOME -> when {
                                totalDrag < -longThresholdPx -> onOpenMultitask()
                                totalDrag < -thresholdPx -> onOpenApps()
                            }
                            LauncherScreen.APP_DRAWER_BOTTOM,
                            LauncherScreen.APP_DRAWER_SIDE -> when {
                                totalDrag < -longThresholdPx -> onOpenMultitask()
                                totalDrag > thresholdPx -> onGoHome()
                            }
                            LauncherScreen.MULTITASK -> {
                                if (totalDrag > thresholdPx) onGoHome()
                            }
                        }
                        totalDrag = 0f
                    },
                )
            },
    )
}

@Composable
private fun UbuntuTouchBackdrop(wallpaperPath: String = "") {
    if (wallpaperPath.isNotEmpty()) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val bitmap = remember(wallpaperPath) {
            runCatching {
                when {
                    wallpaperPath.startsWith("asset:") -> {
                        val assetPath = wallpaperPath.removePrefix("asset:")
                        context.assets.open(assetPath).use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        }
                    }
                    wallpaperPath.startsWith("content:") -> {
                        val uri = android.net.Uri.parse(wallpaperPath)
                        context.contentResolver.openInputStream(uri)?.use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        }
                    }
                    else -> null
                }
            }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Wallpaper",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.7f,
            )
            return
        }
    }

    // Default gradient backdrop
    Box(
        modifier = Modifier
            .size(360.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x22E95420), Color.Transparent),
                ),
                shape = CircleShape,
            )
            .padding(start = 28.dp, top = 84.dp),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(280.dp)
            .padding(start = 120.dp, top = 340.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1AFFFFFF), Color.Transparent),
                ),
                shape = RoundedCornerShape(120.dp),
            ),
    )
}

@Composable
private fun UbuntuTouchTopBar(
    status: SystemStatusSnapshot,
    activeNotificationCount: Int,
    hasNotificationAccess: Boolean,
    indicatorsExpanded: Boolean,
    onToggleIndicators: () -> Unit,
    onExpandIndicators: () -> Unit,
    onCollapseIndicators: () -> Unit,
    onOpenMultitask: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(indicatorsExpanded) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragCancel = { totalDrag = 0f },
                    onDragEnd = {
                        when {
                            totalDrag > 26f -> onExpandIndicators()
                            totalDrag < -26f -> onCollapseIndicators()
                        }
                        totalDrag = 0f
                    },
                )
            },
        color = Color(0x33000000),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = status.timeLabel,
                modifier = Modifier.align(Alignment.CenterStart),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MinimalStatusAction(
                    icon = Icons.Rounded.Notifications,
                    label = if (hasNotificationAccess) {
                        activeNotificationCount.toString()
                    } else {
                        "Off"
                    },
                    onClick = onToggleIndicators,
                )
                MinimalStatusAction(
                    icon = Icons.Rounded.Wifi,
                    label = status.networkLabel,
                    onClick = onToggleIndicators,
                )
                MinimalStatusAction(
                    icon = Icons.Rounded.Battery6Bar,
                    label = status.batteryPercent?.let { "$it%" } ?: "--%",
                    onClick = onToggleIndicators,
                )
                MinimalStatusAction(
                    icon = Icons.Rounded.Apps,
                    label = null,
                    onClick = onOpenMultitask,
                )
            }
        }
    }
}

@Composable
private fun IndicatorsSheet(
    status: SystemStatusSnapshot,
    isDefaultHome: Boolean,
    hasNotificationAccess: Boolean,
    notifications: List<LauncherNotification>,
    isFlashlightOn: Boolean,
    onRefresh: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenAirplaneSettings: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenNotification: (LauncherNotification) -> Unit,
    onLongPressNotification: (LauncherNotification) -> Unit,
    onDismissNotification: (LauncherNotification) -> Result<Unit>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xF0141018),
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        shadowElevation = 14.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // "Control Panel" header
            Text(
                text = "Control Panel",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            // Quick toggles grid (3 columns, 2 rows) — Ubuntu Touch style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickToggleTile(
                    icon = Icons.Rounded.Wifi,
                    label = status.networkLabel,
                    active = status.networkLabel != "Offline",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenWifiSettings,
                )
                QuickToggleTile(
                    icon = Icons.Rounded.Bluetooth,
                    label = if (status.isBluetoothEnabled) "On" else "Off",
                    active = status.isBluetoothEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenBluetoothSettings,
                )
                QuickToggleTile(
                    icon = Icons.Rounded.AirplanemodeActive,
                    label = if (status.isAirplaneModeOn) "On" else "Off",
                    active = status.isAirplaneModeOn,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAirplaneSettings,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickToggleTile(
                    icon = Icons.Rounded.FlashlightOn,
                    label = if (isFlashlightOn) "On" else "Off",
                    active = isFlashlightOn,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleFlashlight,
                )
                QuickToggleTile(
                    icon = Icons.Rounded.LocationOn,
                    label = if (status.isLocationEnabled) "On" else "Off",
                    active = status.isLocationEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLocationSettings,
                )
                QuickToggleTile(
                    icon = Icons.Rounded.Notifications,
                    label = if (hasNotificationAccess) "${notifications.size}" else "Off",
                    active = hasNotificationAccess && notifications.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    onClick = if (hasNotificationAccess) onRefresh else onRequestNotificationAccess,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Battery + brightness row
            Surface(
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Battery6Bar,
                        contentDescription = null,
                        tint = Color(0xFFE95420),
                        modifier = Modifier.size(22.dp),
                    )
                    // Battery bar
                    val batteryFraction = (status.batteryPercent ?: 0) / 100f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FFFFFF)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(batteryFraction)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE95420)),
                        )
                    }
                    Text(
                        text = status.batteryPercent?.let { "$it%" } ?: "--%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = Color(0xFFB8AFBA),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(onClick = onOpenSettings),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date
            Text(
                text = status.dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Thin separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x22FFFFFF)),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Notifications section
            if (!hasNotificationAccess) {
                NotificationAccessCard(onRequestNotificationAccess = onRequestNotificationAccess)
            } else if (notifications.isEmpty()) {
                Text(
                    text = "No notifications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF777777),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    notifications.forEach { notification ->
                        NotificationListItem(
                            notification = notification,
                            onOpenNotification = onOpenNotification,
                            onLongPressNotification = onLongPressNotification,
                            onDismissNotification = onDismissNotification,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = "Clear all",
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    notifications.forEach { onDismissNotification(it) }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFE95420),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IndicatorActionButton(
                    icon = Icons.Rounded.Settings,
                    label = "Settings",
                    onClick = onOpenSettings,
                )
                IndicatorActionButton(
                    icon = Icons.Rounded.Lock,
                    label = "Lock",
                    onClick = onOpenLockScreen,
                )
                if (!isDefaultHome) {
                    IndicatorActionButton(
                        icon = Icons.Rounded.PushPin,
                        label = "Set Home",
                        onClick = onRequestDefaultHome,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickToggleTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bgColor = if (active) Color(0xFFE95420) else Color(0x44FFFFFF)
    val iconColor = if (active) Color.White else Color(0xFFB8AFBA)

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = iconColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun IndicatorActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFDFDBD2),
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFB8AFBA),
        )
    }
}

@Composable
private fun DefaultHomePill(
    onRequestDefaultHome: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onRequestDefaultHome),
        color = Color(0xAA2C001E),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Set Lumo as your default launcher",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun UbuntuTouchLauncherRail(
    appsVisible: Boolean,
    railWidthDp: Int = 68,
    iconSizeDp: Int = 52,
    apps: List<LaunchableApp>,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    onReorderFavorites: (List<String>) -> Unit = {},
) {
    val dockSquircle = remember { SquircleShape() }
    val effectiveRailWidth = maxOf(railWidthDp, iconSizeDp + 16)
    val itemHeightDp = iconSizeDp + 6 // icon size + spacing

    // Drag-and-drop reorder state
    var draggedIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var displayOrder by remember(apps) { mutableStateOf(apps) }

    Surface(
        modifier = Modifier
            .width(effectiveRailWidth.dp)
            .fillMaxHeight(),
        color = Color(0xCC0E0A10),
        shape = RoundedCornerShape(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pinned / favorite apps with drag-and-drop reorder
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                displayOrder.forEachIndexed { index, app ->
                    val isDragging = draggedIndex == index
                    val yOffset = if (isDragging) dragOffsetY else 0f

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationY = yOffset
                                alpha = if (isDragging) 0.8f else 1f
                                scaleX = if (isDragging) 1.1f else 1f
                                scaleY = if (isDragging) 1.1f else 1f
                            }
                            .pointerInput(index, displayOrder.size) {
                                val itemHeightPx = itemHeightDp * density
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        // Calculate target index based on drag offset
                                        val targetIndex = (index + (dragOffsetY / itemHeightPx).toInt())
                                            .coerceIn(0, displayOrder.size - 1)
                                        if (targetIndex != index && targetIndex != draggedIndex) {
                                            val mutableList = displayOrder.toMutableList()
                                            val item = mutableList.removeAt(draggedIndex)
                                            mutableList.add(targetIndex, item)
                                            displayOrder = mutableList
                                            // Adjust offset so the dragged item stays under finger
                                            dragOffsetY -= (targetIndex - draggedIndex) * itemHeightPx
                                            draggedIndex = targetIndex
                                        }
                                    },
                                    onDragEnd = {
                                        draggedIndex = -1
                                        dragOffsetY = 0f
                                        onReorderFavorites(displayOrder.map { it.componentKey })
                                    },
                                    onDragCancel = {
                                        draggedIndex = -1
                                        dragOffsetY = 0f
                                        displayOrder = apps // reset to original
                                    },
                                )
                            },
                    ) {
                        // Tap launches; long-press is consumed by the drag gesture above
                        RailAppIcon(
                            app = app,
                            sizeDp = iconSizeDp,
                            squircle = dockSquircle,
                            onLaunchApp = onLaunchApp,
                        )
                    }
                }
            }

            // Separator
            DockSeparator()

            // App drawer toggle button at bottom — Ubuntu symbol
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(iconSizeDp.dp)
                    .clip(CircleShape)
                    .background(
                        if (appsVisible) Color(0x44FFFFFF) else Color(0xFFE95420),
                    )
                    .clickable(onClick = onOpenApps),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        id = dev.nighttraders.lumo.launcher.R.drawable.ic_ubuntu_symbol,
                    ),
                    contentDescription = "Apps",
                    tint = Color.White,
                    modifier = Modifier.size((iconSizeDp * 0.6f).toInt().dp),
                )
            }
        }
    }
}

@Composable
private fun DockSeparator() {
    Box(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .width(36.dp)
            .height(1.dp)
            .background(Color(0x44FFFFFF)),
    )
}

/** Rail icon: tap to launch. Long press is handled by the drag gesture on the parent. */
@Composable
private fun RailAppIcon(
    app: LaunchableApp,
    sizeDp: Int = 52,
    squircle: Shape,
    onLaunchApp: (LaunchableApp) -> Unit,
) {
    val iconInner = (sizeDp * 0.85f).toInt()
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(squircle)
            .background(Color(0x33FFFFFF))
            .clickable { onLaunchApp(app) },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(app = app, size = iconInner.dp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockAppIcon(
    app: LaunchableApp,
    sizeDp: Int = 52,
    squircle: Shape,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
) {
    val iconInner = (sizeDp * 0.85f).toInt()
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(squircle)
            .background(Color(0x33FFFFFF))
            .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { onToggleFavorite(app) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(app = app, size = iconInner.dp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScopePage(
    status: SystemStatusSnapshot,
    onOpenWallpaperPicker: () -> Unit = {},
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { },
                onLongClick = { showContextMenu = true },
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = status.timeLabel,
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
            )
            Text(
                text = status.dateLabel,
                color = Color(0xFFE3D9E5),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = status.networkLabel,
                color = Color(0xFFB8AFBA),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Long-press context menu
        androidx.compose.material3.DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier.background(Color(0xFF1E1E2E)),
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Wallpaper,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Text("Wallpaper", color = Color.White)
                    }
                },
                onClick = {
                    showContextMenu = false
                    onOpenWallpaperPicker()
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationHeadsUp(
    notification: LauncherNotification,
    modifier: Modifier = Modifier,
    onOpenNotification: () -> Unit,
    onLongPressNotification: () -> Unit,
    onDismissNotification: () -> Result<Unit>,
    onDismiss: () -> Unit,
) {
    key(notification.key) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.Settled) {
                    false
                } else {
                    onDismissNotification().isSuccess
                }
            },
        )

        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier.fillMaxWidth(),
            backgroundContent = {
                NotificationDismissBackground()
            },
            content = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = onOpenNotification,
                            onLongClick = onLongPressNotification,
                        ),
                    color = Color(0xEE2C001E),
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 12.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (notification.isMessaging) MaterialTheme.colorScheme.primary else Color(0xFFB8AFBA)),
                        )

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = notification.appLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB8AFBA),
                            )
                            Text(
                                text = notification.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (notification.message.isNotBlank()) {
                                Text(
                                    text = notification.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE7DFEA),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(onClick = onDismiss),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun NotificationAccessCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onRequestNotificationAccess: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onRequestNotificationAccess),
        color = Color(0x66380A22),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Enable notification access",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
                if (!compact) {
                    Text(
                        text = "Turn this on to see texts and alerts in Lumo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8AFBA),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationActionSheet(
    notification: LauncherNotification,
    onDismiss: () -> Unit,
    onOpenNotification: () -> Unit,
    onOpenApp: () -> Unit,
    onSnooze: (Long) -> Unit,
    onDismissNotification: () -> Result<Unit>,
) {
    val dismissInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(
                interactionSource = dismissInteraction,
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            color = Color(0xF218101A),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = notification.appLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFB8AFBA),
                )
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (notification.message.isNotBlank()) {
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE7DFEA),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                ActionSheetRow(
                    icon = Icons.Rounded.Notifications,
                    label = "Open notification",
                    onClick = onOpenNotification,
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Apps,
                    label = "Open app",
                    onClick = onOpenApp,
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Info,
                    label = "Snooze 15 minutes",
                    onClick = { onSnooze(15 * 60 * 1000L) },
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Info,
                    label = "Snooze 1 hour",
                    onClick = { onSnooze(60 * 60 * 1000L) },
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Delete,
                    label = "Dismiss notification",
                    destructive = true,
                    onClick = {
                        onDismissNotification()
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (destructive) Color(0x33381A1A) else Color(0x22FFFFFF),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (destructive) Color(0xFFED3146) else Color(0xFFE95420),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) Color(0xFFED3146) else Color.White,
            )
        }
    }
}

@Composable
private fun AppActionSheet(
    app: LaunchableApp,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onLaunchApp: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestUninstall: () -> Unit,
) {
    val dismissInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(
                interactionSource = dismissInteraction,
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .width(300.dp)
                .clickable(enabled = false, onClick = {}),
            color = Color(0xF218101A),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // App header with icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app = app, size = 44.dp)
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .height(1.dp)
                        .background(Color(0x22FFFFFF)),
                )

                ActionSheetRow(
                    icon = Icons.Rounded.Apps,
                    label = "Open",
                    onClick = onLaunchApp,
                )
                ActionSheetRow(
                    icon = Icons.Rounded.PushPin,
                    label = if (isFavorite) "Unpin from Launcher" else "Pin to Launcher",
                    onClick = onToggleFavorite,
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Info,
                    label = "App info",
                    onClick = onOpenAppInfo,
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Delete,
                    label = "Uninstall",
                    destructive = true,
                    onClick = onRequestUninstall,
                )
            }
        }
    }
}

@Composable
private fun NotificationDismissBackground() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x55E95420))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = "Dismiss",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationListItem(
    notification: LauncherNotification,
    onOpenNotification: (LauncherNotification) -> Unit,
    onLongPressNotification: (LauncherNotification) -> Unit,
    onDismissNotification: (LauncherNotification) -> Result<Unit>,
) {
    key(notification.key) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.Settled) {
                    false
                } else {
                    onDismissNotification(notification).isSuccess
                }
            },
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                NotificationDismissBackground()
            },
            content = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onOpenNotification(notification) },
                            onLongClick = { onLongPressNotification(notification) },
                        ),
                    color = Color(0x33000000),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = notification.appLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB8AFBA),
                        )
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (notification.message.isNotBlank()) {
                            Text(
                                text = notification.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE7DFEA),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppsScopePage(
    apps: List<LaunchableApp>,
    favoriteKeys: Set<String>,
    isLoading: Boolean,
    searchQuery: String,
    gridColumns: Int = 4,
    iconSizeDp: Int = 52,
    onSearchQueryChange: (String) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onLongPressApp: (LaunchableApp) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x330C0A10),
                        Color(0x441A0816),
                        Color(0x442C001E),
                    ),
                ),
            )
            .padding(horizontal = 10.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                )
            },
            placeholder = {
                Text("Search")
            },
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Loading apps...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB8AFBA),
                )
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(gridColumns),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(
                    items = apps,
                    key = { app -> app.componentKey },
                    contentType = { "app_grid_item" },
                ) { app ->
                    AppGridItem(
                        app = app,
                        isFavorite = favoriteKeys.contains(app.componentKey),
                        iconSizeDp = iconSizeDp,
                        onLaunchApp = onLaunchApp,
                        onLongPressApp = onLongPressApp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    app: LaunchableApp,
    isFavorite: Boolean,
    iconSizeDp: Int = 52,
    onLaunchApp: (LaunchableApp) -> Unit,
    onLongPressApp: (LaunchableApp) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { onLongPressApp(app) },
            )
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            AppIcon(app = app, size = iconSizeDp.dp)
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .padding(top = iconSizeDp.dp)
                        .size(width = 18.dp, height = 3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PagerDots(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == currentPage) 22.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color(0x55FFFFFF)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun MinimalStatusAction(
    icon: ImageVector,
    label: String?,
    onClick: () -> Unit,
) {
    val indicatorColor = Color(0xFFDFDBD2) // Ubuntu Touch warm gray
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = indicatorColor,
            modifier = Modifier.size(16.dp),
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = indicatorColor,
            )
        }
    }
}

@Composable
private fun AppIcon(
    app: LaunchableApp,
    size: Dp,
) {
    val squircle = remember(size) { SquircleShape() }

    if (app.icon != null) {
        val cachedBitmap = remember(app.componentKey) { app.icon.asImageBitmap() }
        Image(
            bitmap = cachedBitmap,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(squircle),
        )
    } else {
        val placeholderColor = remember(app.accentSeed) {
            val tint = 0xFF5E2750 + (app.accentSeed.toLong() and 0x00050F0F)
            Color(tint)
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(squircle)
                .background(placeholderColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.take(1).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

private class SquircleShape(private val exponent: Float = 4f) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val halfW = size.width / 2f
        val halfH = size.height / 2f
        val n = exponent
        val steps = 180

        path.moveTo(halfW + halfW, halfH)
        for (i in 1..steps) {
            val angle = 2.0 * Math.PI * i / steps
            val cosA = kotlin.math.cos(angle).toFloat()
            val sinA = kotlin.math.sin(angle).toFloat()
            val x = halfW + halfW * sign(cosA) * abs(cosA).pow(2f / n)
            val y = halfH + halfH * sign(sinA) * abs(sinA).pow(2f / n)
            path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}
