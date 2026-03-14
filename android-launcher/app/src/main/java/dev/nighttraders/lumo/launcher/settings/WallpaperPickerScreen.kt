package dev.nighttraders.lumo.launcher.settings

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class StockWallpaper(
    val assetPath: String,
    val label: String,
)

private val stockWallpapers = listOf(
    StockWallpaper("wallpapers/warty_final_ubuntu.jpg", "Warty (Default)"),
    StockWallpaper("wallpapers/fossa.jpg", "Fossa"),
    StockWallpaper("wallpapers/infinite_sea.jpg", "Infinite Sea"),
    StockWallpaper("wallpapers/kleiber.jpg", "Kleiber"),
    StockWallpaper("wallpapers/sunset.jpg", "Sunset"),
    StockWallpaper("wallpapers/bridge.jpg", "Bridge"),
    StockWallpaper("wallpapers/friends.jpg", "Friends"),
    StockWallpaper("wallpapers/greentock.jpg", "Greentock"),
)

@Composable
fun WallpaperPickerScreen(
    currentPath: String,
    onSelectWallpaper: (String) -> Unit,
    onPickCustomImage: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0B12), Color(0xFF1A0816), Color(0xFF2C001E)),
                ),
            )
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(
            text = "Wallpaper",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        Text(
            text = "Choose a background for the launcher.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB8AFBA),
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // "None" option — default gradient
            item {
                WallpaperTile(
                    label = "Default",
                    isSelected = currentPath.isEmpty(),
                    onClick = { onSelectWallpaper("") },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF0F0B12),
                                        Color(0xFF2C001E),
                                    ),
                                ),
                            ),
                    )
                }
            }

            // Stock Ubuntu Touch wallpapers
            items(stockWallpapers) { wallpaper ->
                val assetKey = "asset:${wallpaper.assetPath}"
                val thumbnail = remember(wallpaper.assetPath) {
                    runCatching {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        context.assets.open(wallpaper.assetPath).use {
                            BitmapFactory.decodeStream(it, null, opts)
                        }
                    }.getOrNull()
                }

                WallpaperTile(
                    label = wallpaper.label,
                    isSelected = currentPath == assetKey,
                    onClick = { onSelectWallpaper(assetKey) },
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = wallpaper.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF333333)),
                        )
                    }
                }
            }

            // Custom image option
            item {
                WallpaperTile(
                    label = "Custom",
                    isSelected = currentPath.startsWith("content:"),
                    onClick = onPickCustomImage,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1420)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color(0xFFE95420),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("Back", color = Color(0xFFB8AFBA))
        }
    }
}

@Composable
private fun WallpaperTile(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (isSelected) {
                        Modifier.border(3.dp, Color(0xFFE95420), RoundedCornerShape(12.dp))
                    } else {
                        Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    },
                )
                .clickable(onClick = onClick),
            color = Color.Transparent,
            shape = RoundedCornerShape(12.dp),
        ) {
            content()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFFE95420) else Color(0xFFB8AFBA),
        )
    }
}
