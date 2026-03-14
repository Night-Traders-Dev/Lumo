package dev.nighttraders.lumo.launcher.lockscreen

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.ShaderBrush
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
    var failedAttempts by rememberSaveable { mutableIntStateOf(0) }
    var lockedUntil by rememberSaveable { mutableStateOf(0L) }
    var cooldownSecondsLeft by rememberSaveable { mutableIntStateOf(0) }

    // Exponential backoff: 0, 0, 0, 5s, 15s, 30s, 60s, 120s...
    fun cooldownForAttempt(attempts: Int): Long = when {
        attempts < 3 -> 0L
        attempts == 3 -> 5_000L
        attempts == 4 -> 15_000L
        attempts == 5 -> 30_000L
        else -> (60_000L * (1L shl (attempts - 6).coerceAtMost(4)))
            .coerceAtMost(600_000L) // cap at 10 minutes
    }

    // Countdown timer for lockout display
    LaunchedEffect(lockedUntil) {
        if (lockedUntil <= 0L) { cooldownSecondsLeft = 0; return@LaunchedEffect }
        while (true) {
            val remaining = lockedUntil - System.currentTimeMillis()
            if (remaining <= 0L) { cooldownSecondsLeft = 0; break }
            cooldownSecondsLeft = ((remaining + 999) / 1000).toInt()
            delay(1000)
        }
    }

    LaunchedEffect(metrics.size) {
        if (metrics.size <= 1) return@LaunchedEffect
        while (true) {
            delay(AUTO_CYCLE_INTERVAL_MS)
            metricIndex = (metricIndex + 1) % metrics.size
        }
    }

    LaunchedEffect(pinError) {
        if (pinError) {
            delay(1500)
            pinError = false
            pinInput = ""
        }
    }

    fun attemptUnlock() {
        if (securityType == "loading") return
        if (securityType == "none") {
            onUnlock()
        } else {
            showPinEntry = true
        }
    }

    fun submitPin() {
        if (System.currentTimeMillis() < lockedUntil) return // Still in cooldown
        if (onVerifyPin(pinInput)) {
            failedAttempts = 0
            lockedUntil = 0L
            onUnlock()
        } else {
            failedAttempts++
            val cooldown = cooldownForAttempt(failedAttempts)
            if (cooldown > 0L) {
                lockedUntil = System.currentTimeMillis() + cooldown
            }
            pinError = true
        }
    }

    // On API 33+ the GPU shader renders its own gradient; on older devices use Brush
    val bgModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Modifier.background(Color(0xFF2C001E)) // solid fallback behind shader
    } else {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2C001E),
                    Color(0xFF5E2750),
                    Color(0xFFAA3926),
                    Color(0xFF2C001E),
                ),
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(bgModifier)
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
            },
    ) {
        // Bokeh circles orbiting behind the infographic ring
        UbuntuBokehBackground(
            modifier = Modifier.fillMaxSize(),
        )

        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
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

            // Bottom area: PIN pad or swipe hint
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
                    cooldownSecondsLeft = cooldownSecondsLeft,
                    onDigit = { digit ->
                        val maxLen = if (securityType == "pin") 10 else 32
                        if (!pinError && cooldownSecondsLeft <= 0 && pinInput.length < maxLen) {
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
                        text = when (securityType) {
                            "loading" -> "Loading\u2026"
                            "none" -> "Swipe up to unlock"
                            else -> "Swipe up to enter ${securityType.uppercase()}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0x99FFFFFF),
                    )
                }
            }
        }
    }
}

// ── Ubuntu Touch bokeh circles ──────────────────────────────────────────────

/**
 * GPU-accelerated Ubuntu Touch bokeh effect (API 33+) with CPU Canvas fallback.
 * On supported devices, the entire effect (background gradient + 19 orbiting bokeh circles)
 * runs as a single AGSL fragment shader on the GPU.
 */
@Composable
private fun UbuntuBokehBackground(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GpuBokehBackground(modifier)
    } else {
        CpuBokehBackground(modifier)
    }
}

/**
 * GPU path: AGSL shader renders the entire bokeh effect per-pixel on the GPU.
 */
@Composable
private fun GpuBokehBackground(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val shader = remember { BokehShader.create() }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val start = withInfiniteAnimationFrameMillis { it }
        while (true) {
            val now = withInfiniteAnimationFrameMillis { it }
            time = (now - start) / 1000f
        }
    }

    Canvas(modifier = modifier) {
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform("iResolution", size.width, size.height)
        drawRect(brush = shaderBrush)
    }
}

/**
 * CPU fallback: original Canvas-based bokeh for devices below API 33.
 */
@Composable
private fun CpuBokehBackground(modifier: Modifier = Modifier) {
    data class Bokeh(
        val startDeg: Float,
        val orbitFraction: Float,
        val radiusFraction: Float,
        val alpha: Float,
        val durationMs: Int,
    )

    val bokehs = remember {
        listOf(
            Bokeh(0f, 0.32f, 0.22f, 0.25f, 40_000),
            Bokeh(72f, 0.30f, 0.20f, 0.22f, 44_000),
            Bokeh(144f, 0.34f, 0.24f, 0.28f, 38_000),
            Bokeh(216f, 0.31f, 0.21f, 0.20f, 46_000),
            Bokeh(288f, 0.33f, 0.23f, 0.26f, 42_000),
            Bokeh(20f, 0.45f, 0.30f, 0.30f, 50_000),
            Bokeh(65f, 0.48f, 0.34f, 0.28f, 55_000),
            Bokeh(110f, 0.44f, 0.28f, 0.25f, 48_000),
            Bokeh(155f, 0.50f, 0.36f, 0.32f, 52_000),
            Bokeh(200f, 0.46f, 0.32f, 0.27f, 56_000),
            Bokeh(245f, 0.43f, 0.26f, 0.22f, 54_000),
            Bokeh(290f, 0.49f, 0.34f, 0.30f, 50_000),
            Bokeh(335f, 0.47f, 0.30f, 0.26f, 58_000),
            Bokeh(30f, 0.62f, 0.28f, 0.20f, 65_000),
            Bokeh(90f, 0.66f, 0.30f, 0.18f, 70_000),
            Bokeh(150f, 0.60f, 0.26f, 0.16f, 62_000),
            Bokeh(210f, 0.64f, 0.28f, 0.14f, 68_000),
            Bokeh(270f, 0.58f, 0.24f, 0.15f, 72_000),
            Bokeh(330f, 0.63f, 0.27f, 0.17f, 66_000),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bokeh")
    val angles = bokehs.mapIndexed { i, bokeh ->
        infiniteTransition.animateFloat(
            initialValue = bokeh.startDeg,
            targetValue = bokeh.startDeg + 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = bokeh.durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "bokeh$i",
        )
    }

    val colors = remember {
        listOf(
            Color(0xFFE95420),
            Color(0xFFDD4814),
            Color(0xFFCF3721),
            Color(0xFFB83018),
            Color(0xFFED764D),
        )
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val halfMin = minOf(size.width, size.height) / 2f

        bokehs.forEachIndexed { i, bokeh ->
            val angleDeg = angles[i].value
            val angle = Math.toRadians(angleDeg.toDouble() - 90.0)
            val orbitR = halfMin * bokeh.orbitFraction
            val bx = cx + orbitR * cos(angle).toFloat()
            val by = cy + orbitR * sin(angle).toFloat()
            val r = halfMin * bokeh.radiusFraction
            val color = colors[i % colors.size]

            drawCircle(
                color = color.copy(alpha = bokeh.alpha * 0.25f),
                radius = r * 1.5f,
                center = Offset(bx, by),
            )
            drawCircle(
                color = color.copy(alpha = bokeh.alpha),
                radius = r,
                center = Offset(bx, by),
            )
        }
    }
}

// ── Infographic ring ────────────────────────────────────────────────────────

@Composable
private fun InfoGraphicRing(
    daysInMonth: Int,
    currentDay: Int,
    modifier: Modifier = Modifier,
) {
    val dotColor = Color(0xFFB8AFBA)
    val activeDotColor = Color(0xFFE95420)

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension / 2f - 16f

        // Day-of-month dots around the ring
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

        // Progress arc showing elapsed days
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
    }
}

// ── PIN entry ───────────────────────────────────────────────────────────────

@Composable
private fun PinEntryPanel(
    pinInput: String,
    pinError: Boolean,
    securityType: String,
    cooldownSecondsLeft: Int = 0,
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
                text = when {
                    cooldownSecondsLeft > 0 -> "Try again in ${cooldownSecondsLeft}s"
                    pinError -> "Wrong ${securityType}"
                    else -> "Enter ${securityType}"
                },
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    cooldownSecondsLeft > 0 -> Color(0xFFED3146)
                    pinError -> Color(0xFFED3146)
                    else -> Color.White
                },
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

            // Number pad
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PinKey(label = "\u2716", small = true, onClick = onCancel)
                PinKey(label = "0", onClick = { onDigit("0") })
                if (pinInput.isNotEmpty()) {
                    PinKey(label = "\u2714", small = true, accent = true, onClick = onSubmit)
                } else {
                    PinKey(label = "\u232B", small = true, onClick = onBackspace)
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
