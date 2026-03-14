package dev.nighttraders.lumo.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nighttraders.lumo.launcher.notifications.LauncherNotification
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LumoLockScreenScreen(
    status: SystemStatusSnapshot,
    notifications: List<LauncherNotification>,
    onUnlock: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val daysInMonth = remember(today) { today.lengthOfMonth() }
    val currentDay = remember(today) { today.dayOfMonth }
    val notificationCount = notifications.size

    // Metric messages for the InfoGraphic center (Ubuntu Touch style)
    val metricMessages = remember(notifications, today) {
        buildList {
            if (notificationCount > 0) {
                add("$notificationCount notification${if (notificationCount != 1) "s" else ""}")
            }
            val messagingCount = notifications.count { it.isMessaging }
            if (messagingCount > 0) {
                add("$messagingCount message${if (messagingCount != 1) "s" else ""} received")
            }
            val dayOfYear = today.dayOfYear
            val daysRemaining = today.lengthOfYear() - dayOfYear
            add("Day $dayOfYear of ${today.year}")
            add("$daysRemaining days left this year")
            val monthName = today.format(DateTimeFormatter.ofPattern("MMMM"))
            add("$currentDay of $daysInMonth days in $monthName")
            if (isEmpty()) {
                add(status.networkLabel)
            }
        }
    }

    var metricIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentMetric = metricMessages.getOrElse(metricIndex % metricMessages.size) { "" }

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
        // InfoGraphic circle + content centered
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .pointerInput(metricMessages.size) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (metricMessages.isNotEmpty()) {
                                metricIndex = (metricIndex + 1) % metricMessages.size
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            // Dotted circular ring with day-of-month dots
            InfoGraphicRing(
                daysInMonth = daysInMonth,
                currentDay = currentDay,
                hasNotifications = notificationCount > 0,
                modifier = Modifier.size(280.dp),
            )

            // Content inside the ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = status.timeLabel,
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = status.dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE7DFEA),
                    textAlign = TextAlign.Center,
                )
                // Metric message with animated transition on double-tap
                AnimatedContent(
                    targetState = currentMetric,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "metric",
                ) { metric ->
                    Text(
                        text = metric,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (notificationCount > 0 && metricIndex == 0) {
                            Color(0xFFE95420)
                        } else {
                            Color(0xFFB8AFBA)
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Notification cards below the ring
        if (notifications.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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

@Composable
private fun InfoGraphicRing(
    daysInMonth: Int,
    currentDay: Int,
    hasNotifications: Boolean,
    modifier: Modifier = Modifier,
) {
    val dotColor = Color(0xFFB8AFBA)
    val activeDotColor = Color(0xFFE95420)
    val ringColor = Color(0x33B8AFBA)

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension / 2f - 16f

        // Thin circle outline
        drawCircle(
            color = ringColor,
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.5f),
        )

        // Day dots around the ring
        val startAngle = -90.0 // top of circle
        for (day in 1..daysInMonth) {
            val angle = Math.toRadians(startAngle + (day - 1) * (360.0 / daysInMonth))
            val dotX = centerX + radius * cos(angle).toFloat()
            val dotY = centerY + radius * sin(angle).toFloat()

            val isCurrentDay = day == currentDay
            val isPastDay = day < currentDay
            val dotRadius = when {
                isCurrentDay -> 6f
                isPastDay -> 3.5f
                else -> 2.8f
            }
            val color = when {
                isCurrentDay -> activeDotColor
                isPastDay && hasNotifications -> activeDotColor.copy(alpha = 0.5f)
                isPastDay -> dotColor.copy(alpha = 0.6f)
                else -> dotColor.copy(alpha = 0.25f)
            }

            drawCircle(
                color = color,
                radius = dotRadius,
                center = Offset(dotX, dotY),
            )
        }

        // Accent arc from day 1 to current day
        if (currentDay > 1) {
            val sweepAngle = ((currentDay - 1).toFloat() / daysInMonth) * 360f
            drawArc(
                color = activeDotColor.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 3f, cap = StrokeCap.Round),
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            )
        }
    }
}
