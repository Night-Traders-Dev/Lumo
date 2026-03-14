package dev.nighttraders.lumo.launcher.lockscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.nighttraders.lumo.launcher.ui.InfoGraphicMetric
import dev.nighttraders.lumo.launcher.ui.SystemStatusSnapshot
import dev.nighttraders.lumo.launcher.ui.buildInfoGraphicMetrics
import dev.nighttraders.lumo.launcher.ui.rememberStepCount
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

private const val AUTO_CYCLE_INTERVAL_MS = 6_000L

@Composable
fun LumoLockScreenScreen(
    status: SystemStatusSnapshot,
    notifications: List<LauncherNotification>,
    securityType: String,
    onUnlock: () -> Unit,
    onVerifyPin: (String) -> Boolean,
) {
    val today = remember { LocalDate.now() }
    val daysInMonth = remember(today) { today.lengthOfMonth() }
    val currentDay = remember(today) { today.dayOfMonth }
    val notificationCount = notifications.size

    val stepCount by rememberStepCount()

    val metrics = remember(notifications, status, stepCount, today) {
        buildInfoGraphicMetrics(
            notifications = notifications,
            status = status,
            stepCount = stepCount,
            today = today,
        )
    }

    var metricIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentMetric = metrics.getOrElse(metricIndex % metrics.size.coerceAtLeast(1)) {
        InfoGraphicMetric(text = status.networkLabel)
    }

    var showPinEntry by rememberSaveable { mutableStateOf(false) }
    var pinInput by rememberSaveable { mutableStateOf("") }
    var pinError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(metrics.size) {
        if (metrics.size <= 1) return@LaunchedEffect
        while (true) {
            delay(AUTO_CYCLE_INTERVAL_MS)
            metricIndex = (metricIndex + 1) % metrics.size
        }
    }

    // Clear error after a delay
    LaunchedEffect(pinError) {
        if (pinError) {
            delay(1500)
            pinError = false
            pinInput = ""
        }
    }

    fun attemptUnlock() {
        if (securityType == "none") {
            onUnlock()
        } else {
            showPinEntry = true
        }
    }

    fun submitPin() {
        if (onVerifyPin(pinInput)) {
            onUnlock()
        } else {
            pinError = true
        }
    }

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
            .pointerInput(showPinEntry) {
                if (!showPinEntry) {
                    var totalDrag = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        },
                        onDragEnd = {
                            if (totalDrag < -48f) {
                                attemptUnlock()
                            }
                            totalDrag = 0f
                        },
                        onDragCancel = {
                            totalDrag = 0f
                        },
                    )
                }
            }
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        // InfoGraphic circle + content centered
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .pointerInput(metrics.size) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (metrics.isNotEmpty()) {
                                metricIndex = (metricIndex + 1) % metrics.size
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            InfoGraphicRing(
                daysInMonth = daysInMonth,
                currentDay = currentDay,
                hasNotifications = notificationCount > 0,
                modifier = Modifier.size(280.dp),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 28.dp),
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
                AnimatedContent(
                    targetState = currentMetric,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "metric",
                ) { metric ->
                    Text(
                        text = if (metric.truncate && metric.text.length > 32) {
                            metric.text.take(30) + "\u2026"
                        } else {
                            metric.text
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (metric.highlight) {
                            Color(0xFFE95420)
                        } else {
                            Color(0xFFB8AFBA)
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Bottom area: either PIN pad or "Swipe to unlock"
        AnimatedVisibility(
            visible = showPinEntry,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)),
        ) {
            PinEntryPanel(
                pinInput = pinInput,
                pinError = pinError,
                securityType = securityType,
                onDigit = { digit ->
                    val maxLen = if (securityType == "pin") 10 else 32
                    if (!pinError && pinInput.length < maxLen) {
                        pinInput += digit
                    }
                },
                onBackspace = {
                    if (pinInput.isNotEmpty()) {
                        pinInput = pinInput.dropLast(1)
                    }
                },
                onSubmit = ::submitPin,
                onCancel = {
                    showPinEntry = false
                    pinInput = ""
                    pinError = false
                },
            )
        }

        AnimatedVisibility(
            visible = !showPinEntry,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Text(
                    text = if (securityType != "none") "Swipe up to enter ${securityType.uppercase()}" else "Swipe up to unlock",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0x99FFFFFF),
                )
            }
        }
    }
}

@Composable
private fun PinEntryPanel(
    pinInput: String,
    pinError: Boolean,
    securityType: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        color = Color(0xCC120B14),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (pinError) "Wrong ${securityType}" else "Enter ${securityType}",
                style = MaterialTheme.typography.titleMedium,
                color = if (pinError) Color(0xFFED3146) else Color.White,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                repeat(pinInput.length.coerceAtMost(12)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (pinError) Color(0xFFED3146) else Color(0xFFE95420)),
                    )
                }
                if (pinInput.isEmpty()) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0x44FFFFFF)),
                        )
                    }
                }
            }

            // Number pad (3x4 grid + bottom row)
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
            )
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (digit in row) {
                        PinKey(label = digit, onClick = { onDigit(digit) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Bottom row: Cancel, 0, Backspace/Enter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PinKey(label = "\u2716", small = true, onClick = onCancel) // X cancel
                PinKey(label = "0", onClick = { onDigit("0") })
                if (pinInput.isNotEmpty()) {
                    PinKey(label = "\u2714", small = true, accent = true, onClick = onSubmit) // checkmark
                } else {
                    PinKey(label = "\u232B", small = true, onClick = onBackspace) // backspace
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    label: String,
    small: Boolean = false,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(if (small) 64.dp else 72.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = if (accent) Color(0xFFE95420) else Color(0x22FFFFFF),
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = if (small) 20.sp else 26.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
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

    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val orbit1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit1",
    )
    val orbit2 by infiniteTransition.animateFloat(
        initialValue = 120f,
        targetValue = 480f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit2",
    )
    val orbit3 by infiniteTransition.animateFloat(
        initialValue = 240f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 35_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit3",
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension / 2f - 16f

        val startAngle = -90.0
        for (day in 1..daysInMonth) {
            val angle = Math.toRadians(startAngle + (day - 1) * (360.0 / daysInMonth))
            val dx = centerX + radius * cos(angle).toFloat()
            val dy = centerY + radius * sin(angle).toFloat()

            val isCurrentDay = day == currentDay
            val isPastDay = day < currentDay
            val circleRadius = when {
                isCurrentDay -> 7f
                isPastDay -> 5f
                else -> 4f
            }
            val color = when {
                isCurrentDay -> activeDotColor
                isPastDay -> dotColor.copy(alpha = 0.7f)
                else -> dotColor.copy(alpha = 0.3f)
            }
            val strokeWidth = when {
                isCurrentDay -> 2.5f
                isPastDay -> 1.8f
                else -> 1.2f
            }

            drawCircle(
                color = color,
                radius = circleRadius,
                center = Offset(dx, dy),
                style = Stroke(width = strokeWidth),
            )

            if (isCurrentDay) {
                drawCircle(
                    color = activeDotColor,
                    radius = 3f,
                    center = Offset(dx, dy),
                )
            }
        }

        if (currentDay > 1) {
            val sweepAngle = ((currentDay - 1).toFloat() / daysInMonth) * 360f
            drawArc(
                color = activeDotColor.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            )
        }

        val orbitRadius = radius + 18f
        val orbiters = listOf(
            Triple(orbit1, 4.5f, activeDotColor.copy(alpha = 0.7f)),
            Triple(orbit2, 3.5f, dotColor.copy(alpha = 0.5f)),
            Triple(orbit3, 3f, activeDotColor.copy(alpha = 0.4f)),
        )
        for ((angleDeg, dotRadius, color) in orbiters) {
            val angle = Math.toRadians(angleDeg.toDouble() - 90.0)
            val ox = centerX + orbitRadius * cos(angle).toFloat()
            val oy = centerY + orbitRadius * sin(angle).toFloat()
            drawCircle(
                color = color.copy(alpha = color.alpha * 0.3f),
                radius = dotRadius * 2.5f,
                center = Offset(ox, oy),
            )
            drawCircle(
                color = color,
                radius = dotRadius,
                center = Offset(ox, oy),
            )
        }
    }
}
