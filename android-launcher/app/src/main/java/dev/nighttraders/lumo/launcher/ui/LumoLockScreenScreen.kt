package dev.nighttraders.lumo.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nighttraders.lumo.launcher.notifications.LauncherNotification

@Composable
fun LumoLockScreenScreen(
    status: SystemStatusSnapshot,
    notifications: List<LauncherNotification>,
    onUnlock: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C0A10),
                        Color(0xFF1D0A17),
                        Color(0xFF2C001E),
                        Color(0xFF08060A),
                    ),
                ),
            )
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag < -48f) {
                            onUnlock()
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = {
                        totalDrag = 0f
                    },
                )
            }
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = status.timeLabel,
                color = Color.White,
                fontSize = 84.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
            )
            Text(
                text = status.dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE7DFEA),
            )
            Text(
                text = status.networkLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8AFBA),
            )
        }

        if (notifications.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                notifications.take(3).forEach { notification ->
                    Surface(
                        color = Color(0x44210F1D),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    }
                }
            }
        }

        Text(
            text = "Swipe up to unlock",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0x99FFFFFF),
        )
    }
}
