package dev.nighttraders.lumo.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.Bluetooth
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
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.nighttraders.lumo.launcher.notifications.LauncherNotification
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private enum class ScopePage(val title: String) {
    Home("Today"),
    Apps("Apps"),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LumoLauncherApp(
    uiState: LauncherUiState,
    systemStatus: SystemStatusSnapshot,
    isDefaultHome: Boolean,
    requestedPageIndex: Int,
    onRequestDefaultHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onOpenNotification: (LauncherNotification) -> Unit,
    onOpenNotificationApp: (LauncherNotification) -> Unit,
    onDismissNotification: (LauncherNotification) -> Result<Unit>,
    onSnoozeNotification: (LauncherNotification, Long) -> Result<Unit>,
    onDismissHeadsUpNotification: (String) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    onOpenAppInfo: (LaunchableApp) -> Unit,
    onRequestUninstall: (LaunchableApp) -> Unit,
    onRefresh: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var indicatorsExpanded by rememberSaveable { mutableStateOf(false) }
    var railVisible by rememberSaveable { mutableStateOf(false) }
    var notificationActionTarget by remember { mutableStateOf<LauncherNotification?>(null) }
    var appActionTarget by remember { mutableStateOf<LaunchableApp?>(null) }
    val pagerState = rememberPagerState(
        initialPage = requestedPageIndex.coerceIn(0, ScopePage.entries.lastIndex),
        pageCount = { ScopePage.entries.size },
    )
    val coroutineScope = rememberCoroutineScope()

    val currentPage = ScopePage.entries[pagerState.currentPage]
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

    LaunchedEffect(currentPage) {
        if (currentPage != ScopePage.Home) {
            railVisible = false
        }
    }

    LaunchedEffect(indicatorsExpanded) {
        if (indicatorsExpanded) {
            railVisible = false
        }
    }

    LaunchedEffect(uiState.headsUpNotification?.key) {
        val headsUp = uiState.headsUpNotification ?: return@LaunchedEffect
        delay(6_000)
        onDismissHeadsUpNotification(headsUp.key)
    }

    LaunchedEffect(requestedPageIndex) {
        val pageIndex = requestedPageIndex.coerceIn(0, ScopePage.entries.lastIndex)
        if (pagerState.currentPage != pageIndex) {
            pagerState.scrollToPage(pageIndex)
        }
    }

    fun navigateTo(page: ScopePage) {
        railVisible = false
        if (page == ScopePage.Apps) {
            indicatorsExpanded = false
        }
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = page.ordinal,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
        UbuntuTouchBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            UbuntuTouchTopBar(
                currentPage = currentPage,
                status = systemStatus,
                activeNotificationCount = uiState.activeNotificationCount,
                hasNotificationAccess = uiState.hasNotificationAccess,
                indicatorsExpanded = indicatorsExpanded,
                onToggleIndicators = { indicatorsExpanded = !indicatorsExpanded },
                onExpandIndicators = { indicatorsExpanded = true },
                onCollapseIndicators = { indicatorsExpanded = false },
                onOpenApps = { navigateTo(ScopePage.Apps) },
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapAnimationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    ),
                ) { page ->
                    when (ScopePage.entries[page]) {
                        ScopePage.Home -> HomeScopePage(
                            status = systemStatus,
                            pinnedApps = uiState.favorites,
                            recentApps = uiState.recentApps,
                            hasNotificationAccess = uiState.hasNotificationAccess,
                            notifications = uiState.recentNotifications,
                            onRequestNotificationAccess = onRequestNotificationAccess,
                            onOpenNotification = onOpenNotification,
                            onLongPressNotification = { notificationActionTarget = it },
                            onDismissNotification = onDismissNotification,
                            onLaunchApp = onLaunchApp,
                        )

                        ScopePage.Apps -> AppsScopePage(
                            apps = visibleApps,
                            favoriteKeys = uiState.favoriteKeys,
                            isLoading = uiState.isLoading,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onLaunchApp = onLaunchApp,
                            onLongPressApp = { appActionTarget = it },
                        )
                    }
                }

                PagerDots(
                    currentPage = pagerState.currentPage,
                    pageCount = ScopePage.entries.size,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                )

                BottomEdgeGestureHandle(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    currentPage = currentPage,
                    onGoHome = { navigateTo(ScopePage.Home) },
                    onOpenApps = { navigateTo(ScopePage.Apps) },
                )

                if (currentPage == ScopePage.Home && !indicatorsExpanded) {
                    LeftEdgeRevealHandle(
                        modifier = Modifier.align(Alignment.CenterStart),
                        railVisible = railVisible,
                        onRevealRail = { railVisible = true },
                    )
                }

                if (railVisible && currentPage == ScopePage.Home) {
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
                    visible = railVisible && currentPage == ScopePage.Home,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp, top = 6.dp, bottom = 8.dp),
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
                        currentPage = currentPage,
                        apps = launcherApps,
                        onGoHome = { navigateTo(ScopePage.Home) },
                        onOpenApps = { navigateTo(ScopePage.Apps) },
                        onOpenSettings = onOpenSettings,
                        onLaunchApp = { app ->
                            railVisible = false
                            onLaunchApp(app)
                        },
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
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
                    onRefresh = onRefresh,
                    onRequestDefaultHome = onRequestDefaultHome,
                    onOpenSettings = onOpenSettings,
                    onOpenLockScreen = onOpenLockScreen,
                    onRequestNotificationAccess = onRequestNotificationAccess,
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
    }
}

@Composable
private fun LeftEdgeRevealHandle(
    modifier: Modifier = Modifier,
    railVisible: Boolean,
    onRevealRail: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(20.dp)
            .pointerInput(railVisible) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount > 0f) {
                            totalDrag += dragAmount
                        }
                    },
                    onDragCancel = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag > 24f) {
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
    currentPage: ScopePage,
    onGoHome: () -> Unit,
    onOpenApps: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .pointerInput(currentPage) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragCancel = { totalDrag = 0f },
                    onDragEnd = {
                        when {
                            currentPage == ScopePage.Home && totalDrag < -42f -> onOpenApps()
                            currentPage == ScopePage.Apps && totalDrag > 42f -> onGoHome()
                        }
                        totalDrag = 0f
                    },
                )
            },
    )
}

@Composable
private fun UbuntuTouchBackdrop() {
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
    currentPage: ScopePage,
    status: SystemStatusSnapshot,
    activeNotificationCount: Int,
    hasNotificationAccess: Boolean,
    indicatorsExpanded: Boolean,
    onToggleIndicators: () -> Unit,
    onExpandIndicators: () -> Unit,
    onCollapseIndicators: () -> Unit,
    onOpenApps: () -> Unit,
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
                    onClick = onOpenApps,
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
    onRefresh: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
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
                    onClick = {},
                )
                QuickToggleTile(
                    icon = Icons.Rounded.Bluetooth,
                    label = "Bluetooth",
                    active = false,
                    modifier = Modifier.weight(1f),
                    onClick = {},
                )
                QuickToggleTile(
                    icon = Icons.Rounded.AirplanemodeActive,
                    label = "Airplane",
                    active = false,
                    modifier = Modifier.weight(1f),
                    onClick = {},
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickToggleTile(
                    icon = Icons.Rounded.FlashlightOn,
                    label = "Flashlight",
                    active = false,
                    modifier = Modifier.weight(1f),
                    onClick = {},
                )
                QuickToggleTile(
                    icon = Icons.Rounded.LocationOn,
                    label = "Location",
                    active = false,
                    modifier = Modifier.weight(1f),
                    onClick = {},
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
    currentPage: ScopePage,
    apps: List<LaunchableApp>,
    onGoHome: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
) {
    val dockSquircle = remember { SquircleShape() }

    Surface(
        modifier = Modifier
            .width(68.dp)
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
            // Ubuntu BFB button at top
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentPage == ScopePage.Home) {
                            Color(0xFFE95420)
                        } else {
                            Color(0x44FFFFFF)
                        },
                    )
                    .clickable(onClick = onGoHome),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        id = dev.nighttraders.lumo.launcher.R.drawable.ic_ubuntu_bfb,
                    ),
                    contentDescription = "Home",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            // Separator
            DockSeparator()

            // Pinned / favorite apps
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                apps.forEach { app ->
                    DockAppIcon(
                        app = app,
                        squircle = dockSquircle,
                        onLaunchApp = onLaunchApp,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }

            // Separator
            DockSeparator()

            // App drawer button at bottom
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(52.dp)
                    .clip(dockSquircle)
                    .background(
                        if (currentPage == ScopePage.Apps) {
                            Color(0xFFE95420)
                        } else {
                            Color(0x33FFFFFF)
                        },
                    )
                    .clickable(onClick = onOpenApps),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = "Apps",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockAppIcon(
    app: LaunchableApp,
    squircle: Shape,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(squircle)
            .background(Color(0x33FFFFFF))
            .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { onToggleFavorite(app) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(app = app, size = 44.dp)
    }
}

@Composable
private fun HomeScopePage(
    status: SystemStatusSnapshot,
    pinnedApps: List<LaunchableApp>,
    recentApps: List<LaunchableApp>,
    hasNotificationAccess: Boolean,
    notifications: List<LauncherNotification>,
    onRequestNotificationAccess: () -> Unit,
    onOpenNotification: (LauncherNotification) -> Unit,
    onLongPressNotification: (LauncherNotification) -> Unit,
    onDismissNotification: (LauncherNotification) -> Result<Unit>,
    onLaunchApp: (LaunchableApp) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
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

            if (!hasNotificationAccess) {
                NotificationAccessCard(
                    modifier = Modifier.padding(top = 20.dp),
                    compact = true,
                    onRequestNotificationAccess = onRequestNotificationAccess,
                )
            } else if (notifications.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(284.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    notifications.take(2).forEach { notification ->
                        SimpleNotificationCard(
                            notification = notification,
                            onOpenNotification = onOpenNotification,
                        )
                    }
                }
            }

            // Dash: Pinned apps
            if (pinnedApps.isNotEmpty()) {
                DashAppRow(
                    label = "Pinned",
                    apps = pinnedApps.take(5),
                    onLaunchApp = onLaunchApp,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            // Dash: Recent apps (exclude pinned to avoid duplicates)
            val pinnedKeys = remember(pinnedApps) { pinnedApps.mapTo(hashSetOf()) { it.componentKey } }
            val filteredRecent = remember(recentApps, pinnedKeys) {
                recentApps.filter { it.componentKey !in pinnedKeys }
            }
            if (filteredRecent.isNotEmpty()) {
                DashAppRow(
                    label = "Recent",
                    apps = filteredRecent.take(5),
                    onLaunchApp = onLaunchApp,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }

        Text(
            text = "Swipe up for Apps",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0x99FFFFFF),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashAppRow(
    label: String,
    apps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF888888),
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.forEach { app ->
                Column(
                    modifier = Modifier
                        .clickable { onLaunchApp(app) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AppIcon(app = app, size = 44.dp)
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB8AFBA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(52.dp),
                    )
                }
            }
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
private fun SimpleNotificationCard(
    notification: LauncherNotification,
    onOpenNotification: (LauncherNotification) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenNotification(notification) },
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
    onSearchQueryChange: (String) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onLongPressApp: (LaunchableApp) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                columns = GridCells.Fixed(4),
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
            AppIcon(app = app, size = 52.dp)
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .padding(top = 52.dp)
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
