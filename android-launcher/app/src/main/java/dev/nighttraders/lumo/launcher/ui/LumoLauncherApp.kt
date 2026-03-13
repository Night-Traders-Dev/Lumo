package dev.nighttraders.lumo.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nighttraders.lumo.launcher.data.LaunchableApp
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
    onRequestDefaultHome: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    onRefresh: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var indicatorsExpanded by rememberSaveable { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { ScopePage.entries.size })
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
                onToggleIndicators = { indicatorsExpanded = !indicatorsExpanded },
                onOpenApps = {
                    coroutineScope.launch { pagerState.animateScrollToPage(ScopePage.Apps.ordinal) }
                },
            )

            if (indicatorsExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    IndicatorsSheet(
                        status = systemStatus,
                        isDefaultHome = isDefaultHome,
                        onRefresh = onRefresh,
                        onRequestDefaultHome = onRequestDefaultHome,
                    )
                }
            }

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                UbuntuTouchLauncherRail(
                    currentPage = currentPage,
                    apps = launcherApps,
                    onGoHome = {
                        coroutineScope.launch { pagerState.animateScrollToPage(ScopePage.Home.ordinal) }
                    },
                    onOpenApps = {
                        coroutineScope.launch { pagerState.animateScrollToPage(ScopePage.Apps.ordinal) }
                    },
                    onLaunchApp = onLaunchApp,
                    onToggleFavorite = onToggleFavorite,
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                            when (ScopePage.entries[page]) {
                                ScopePage.Home -> HomeScopePage(status = systemStatus)

                                ScopePage.Apps -> AppsScopePage(
                                    apps = visibleApps,
                                favoriteKeys = uiState.favoriteKeys,
                                isLoading = uiState.isLoading,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onLaunchApp = onLaunchApp,
                                onToggleFavorite = onToggleFavorite,
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
                }
            }
        }
    }
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
    onToggleIndicators: () -> Unit,
    onOpenApps: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                text = currentPage.title,
                modifier = Modifier.align(Alignment.CenterStart),
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFE7DFEA),
            )

            Text(
                text = status.timeLabel,
                modifier = Modifier.align(Alignment.Center),
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
    onRefresh: () -> Unit,
    onRequestDefaultHome: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(224.dp),
        color = Color(0xEE170D18),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IndicatorRow(title = "Date", value = status.dateLabel)
            IndicatorRow(title = "Network", value = status.networkLabel)
            IndicatorRow(
                title = "Battery",
                value = status.batteryPercent?.let { "$it%" } ?: "--%",
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh) {
                    Text("Refresh")
                }
                if (!isDefaultHome) {
                    Button(onClick = onRequestDefaultHome) {
                        Text("Set Home")
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicatorRow(
    title: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFB8AFBA),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
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
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(52.dp)
            .fillMaxHeight(),
        color = Color(0x55000000),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RailActionButton(
                icon = Icons.Rounded.Home,
                selected = currentPage == ScopePage.Home,
                onClick = onGoHome,
            )

            apps.forEach { app ->
                RailAppButton(
                    app = app,
                    onLaunchApp = onLaunchApp,
                    onToggleFavorite = onToggleFavorite,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            RailActionButton(
                icon = Icons.Rounded.Apps,
                selected = currentPage == ScopePage.Apps,
                onClick = onOpenApps,
            )
        }
    }
}

@Composable
private fun RailActionButton(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.95f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RailAppButton(
    app: LaunchableApp,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0x22000000))
            .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { onToggleFavorite(app) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(app = app, size = 24.dp)
    }
}

@Composable
private fun HomeScopePage(
    status: SystemStatusSnapshot,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp),
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

        Text(
            text = "Swipe to Apps",
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
private fun AppsScopePage(
    apps: List<LaunchableApp>,
    favoriteKeys: Set<String>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
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
                    text = "Loading apps…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB8AFBA),
                )
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Adaptive(minSize = 84.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(
                    items = apps,
                    key = { app -> app.componentKey },
                ) { app ->
                    AppGridItem(
                        app = app,
                        isFavorite = favoriteKeys.contains(app.componentKey),
                        onLaunchApp = onLaunchApp,
                        onToggleFavorite = onToggleFavorite,
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
    onToggleFavorite: (LaunchableApp) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { onToggleFavorite(app) },
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
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun AppIcon(
    app: LaunchableApp,
    size: Dp,
) {
    if (app.icon != null) {
        Image(
            bitmap = app.icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    } else {
        val placeholderColor = remember(app.accentSeed) {
            val tint = 0xFF5E2750 + (app.accentSeed.toLong() and 0x00050F0F)
            Color(tint)
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 3))
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
