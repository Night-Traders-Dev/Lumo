package dev.nighttraders.lumo.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LumoColorScheme = darkColorScheme(
    primary = Mist,
    onPrimary = InkBlack,
    secondary = Ember,
    onSecondary = InkBlack,
    background = InkBlack,
    onBackground = Mist,
    surface = NightPlum,
    onSurface = Mist,
    surfaceVariant = Aubergine,
    onSurfaceVariant = SteelRose,
)

@Composable
fun LumoLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LumoColorScheme,
        typography = LumoTypography,
        content = content,
    )
}

