package dev.nighttraders.lumo.launcher.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Fetches current weather from the Open-Meteo API (no API key required).
 * Uses device location (coarse is sufficient) to get coordinates.
 */
object OpenMeteoWeatherProvider {

    private val _weather = MutableStateFlow<WeatherSnapshot?>(null)
    val weather: StateFlow<WeatherSnapshot?> = _weather.asStateFlow()

    private var lastFetchMs = 0L
    private const val MIN_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

    data class WeatherSnapshot(
        val temperatureC: Double,
        val temperatureF: Double,
        val weatherCode: Int,
        val description: String,
        val isDay: Boolean,
        val windSpeedKmh: Double,
        val humidity: Int,
    ) {
        fun displayText(useFahrenheit: Boolean = true): String {
            val temp = if (useFahrenheit) {
                "${temperatureF.toInt()}°F"
            } else {
                "${temperatureC.toInt()}°C"
            }
            return "$temp $description"
        }
    }

    /**
     * Refresh weather data. Call from a coroutine scope.
     * Respects a minimum interval to avoid excessive API calls.
     */
    suspend fun refresh(context: Context, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastFetchMs < MIN_INTERVAL_MS && _weather.value != null) return

        val location = getLastKnownLocation(context) ?: return
        val snapshot = fetchWeather(location.latitude, location.longitude) ?: return
        _weather.value = snapshot
        lastFetchMs = now
    }

    private fun getLastKnownLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return null

        // Try GPS first, then network, then any available provider
        val providers = buildList {
            if (hasFine) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }

        for (provider in providers) {
            @Suppress("MissingPermission")
            val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            if (loc != null) return loc
        }
        return null
    }

    private suspend fun fetchWeather(lat: Double, lon: Double): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            runCatching {
                val urlStr = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${String.format(Locale.US, "%.4f", lat)}" +
                    "&longitude=${String.format(Locale.US, "%.4f", lon)}" +
                    "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
                    "&temperature_unit=celsius" +
                    "&wind_speed_unit=kmh"

                val connection = URL(urlStr).openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.requestMethod = "GET"

                try {
                    if (connection.responseCode != 200) return@runCatching null

                    val body = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    val current = json.getJSONObject("current")

                    val tempC = current.getDouble("temperature_2m")
                    val tempF = tempC * 9.0 / 5.0 + 32.0
                    val code = current.getInt("weather_code")
                    val isDay = current.getInt("is_day") == 1
                    val windSpeed = current.getDouble("wind_speed_10m")
                    val humidity = current.getInt("relative_humidity_2m")

                    WeatherSnapshot(
                        temperatureC = tempC,
                        temperatureF = tempF,
                        weatherCode = code,
                        description = wmoCodeToDescription(code, isDay),
                        isDay = isDay,
                        windSpeedKmh = windSpeed,
                        humidity = humidity,
                    )
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }

    /**
     * WMO Weather interpretation codes to human-readable descriptions.
     * https://open-meteo.com/en/docs#weathervariables
     */
    private fun wmoCodeToDescription(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "Clear sky" else "Clear night"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45 -> "Foggy"
        48 -> "Rime fog"
        51 -> "Light drizzle"
        53 -> "Drizzle"
        55 -> "Dense drizzle"
        56 -> "Freezing drizzle"
        57 -> "Dense freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66 -> "Freezing rain"
        67 -> "Heavy freezing rain"
        71 -> "Light snow"
        73 -> "Snow"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80 -> "Light showers"
        81 -> "Showers"
        82 -> "Heavy showers"
        85 -> "Light snow showers"
        86 -> "Heavy snow showers"
        95 -> "Thunderstorm"
        96 -> "Thunderstorm with hail"
        99 -> "Severe thunderstorm"
        else -> "Unknown"
    }
}
