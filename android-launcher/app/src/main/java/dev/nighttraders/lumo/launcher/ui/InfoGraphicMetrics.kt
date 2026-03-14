package dev.nighttraders.lumo.launcher.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.nighttraders.lumo.launcher.notifications.LauncherNotification
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Builds the list of metric messages displayed inside the InfoGraphic circle.
 * Sources: notifications (count, messaging, weather apps, email apps),
 * system status (battery, network), step counter sensor, and calendar facts.
 */
fun buildInfoGraphicMetrics(
    notifications: List<LauncherNotification>,
    status: SystemStatusSnapshot,
    stepCount: Int?,
    today: LocalDate = LocalDate.now(),
): List<InfoGraphicMetric> = buildList {
    val notificationCount = notifications.size

    // 1. Notification count
    if (notificationCount > 0) {
        add(
            InfoGraphicMetric(
                text = "$notificationCount notification${if (notificationCount != 1) "s" else ""}",
                highlight = true,
            )
        )
    }

    // 2. Messages received
    val messagingCount = notifications.count { it.isMessaging }
    if (messagingCount > 0) {
        add(
            InfoGraphicMetric(
                text = "$messagingCount message${if (messagingCount != 1) "s" else ""} received",
            )
        )
    }

    // 3. Unread email count (from email app notifications)
    val emailCount = notifications.count { notification ->
        EMAIL_PACKAGES.any { notification.packageName.contains(it, ignoreCase = true) }
    }
    if (emailCount > 0) {
        add(
            InfoGraphicMetric(
                text = "$emailCount unread email${if (emailCount != 1) "s" else ""}",
            )
        )
    }

    // 4. Latest email subject
    notifications.firstOrNull { notification ->
        EMAIL_PACKAGES.any { notification.packageName.contains(it, ignoreCase = true) }
    }?.let { email ->
        val subject = email.title.takeIf { it.isNotBlank() } ?: email.message
        if (subject.isNotBlank()) {
            add(InfoGraphicMetric(text = subject, truncate = true))
        }
    }

    // 5. Weather from weather app notifications
    val weatherText = extractWeatherFromNotifications(notifications)
    if (weatherText != null) {
        add(InfoGraphicMetric(text = weatherText))
    }

    // 6. Step count
    if (stepCount != null && stepCount > 0) {
        add(InfoGraphicMetric(text = "%,d steps".format(stepCount)))
    }

    // 7. Battery level
    status.batteryPercent?.let { battery ->
        val label = when {
            battery <= 15 -> "Battery low: $battery%"
            battery >= 95 -> "Battery full: $battery%"
            else -> "Battery: $battery%"
        }
        add(InfoGraphicMetric(text = label))
    }

    // 8. Network
    add(InfoGraphicMetric(text = status.networkLabel))

    // 9. Calendar facts
    val dayOfYear = today.dayOfYear
    val daysRemaining = today.lengthOfYear() - dayOfYear
    add(InfoGraphicMetric(text = "Day $dayOfYear of ${today.year}"))
    add(InfoGraphicMetric(text = "$daysRemaining days left this year"))

    val monthName = today.format(DateTimeFormatter.ofPattern("MMMM"))
    val currentDay = today.dayOfMonth
    val daysInMonth = today.lengthOfMonth()
    add(InfoGraphicMetric(text = "$currentDay of $daysInMonth days in $monthName"))
}

data class InfoGraphicMetric(
    val text: String,
    val highlight: Boolean = false,
    val truncate: Boolean = false,
)

/**
 * Extracts a weather summary from known weather app notifications.
 * Returns something like "72°F Partly Cloudy" or null if no weather data found.
 */
private fun extractWeatherFromNotifications(
    notifications: List<LauncherNotification>,
): String? {
    val weatherNotification = notifications.firstOrNull { notification ->
        WEATHER_PACKAGES.any { notification.packageName.contains(it, ignoreCase = true) }
    } ?: return null

    // Weather apps typically put conditions in title or message.
    // Try to build a concise summary from whatever is available.
    val title = weatherNotification.title.trim()
    val message = weatherNotification.message.trim()

    // If the title contains a temperature (digits followed by °), prefer it
    val tempPattern = Regex("""(\d+°\s*[FCfc]?)""")
    val tempMatch = tempPattern.find(title) ?: tempPattern.find(message)

    return when {
        // Title has temperature — use it directly (e.g. "72°F Partly Cloudy")
        tempMatch != null && title.contains(tempMatch.value) ->
            title.take(40)
        // Message has temperature
        tempMatch != null ->
            message.take(40)
        // Just use whatever the weather app says
        title.isNotBlank() -> title.take(40)
        message.isNotBlank() -> message.take(40)
        else -> null
    }
}

private val WEATHER_PACKAGES = listOf(
    "com.handmark.expressweather", // 1Weather
    "com.weather",        // Weather Channel, etc.
    "weather",            // Generic weather apps
    "accuweather",
    "com.accuweather",
    "org.climasense",
    "com.samhi.weather",  // Samsung Weather
    "cz.martykan.forecastie",
    "com.google.android.apps.weather",
)

private val EMAIL_PACKAGES = listOf(
    "com.google.android.gm",   // Gmail
    "com.microsoft.office.outlook",
    "com.samsung.android.email",
    "com.yahoo.mobile.client.android.mail",
    "me.bluemail",
    "org.mozilla.thunderbird",
)

/**
 * Reads the device step counter sensor and returns steps since last reboot.
 * TYPE_STEP_COUNTER reports a cumulative total since reboot.
 */
@Composable
fun rememberStepCount(): State<Int?> {
    val context = LocalContext.current
    val steps = remember { mutableFloatStateOf(-1f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        val listener = if (stepSensor != null && sensorManager != null) {
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    steps.floatValue = event.values[0]
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }.also { l ->
                sensorManager.registerListener(l, stepSensor, SensorManager.SENSOR_DELAY_UI)
            }
        } else {
            null
        }

        onDispose {
            if (listener != null && sensorManager != null) {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    return remember {
        object : State<Int?> {
            override val value: Int?
                get() {
                    val s = steps.floatValue
                    return if (s >= 0f) s.toInt() else null
                }
        }
    }
}
