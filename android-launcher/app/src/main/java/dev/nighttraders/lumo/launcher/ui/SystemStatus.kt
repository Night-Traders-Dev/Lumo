package dev.nighttraders.lumo.launcher.ui

import android.app.role.RoleManager
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SystemStatusSnapshot(
    val timeLabel: String,
    val dateLabel: String,
    val networkLabel: String,
    val batteryPercent: Int?,
    val isBluetoothEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isAirplaneModeOn: Boolean = false,
)

@Composable
fun rememberSystemStatus(): State<SystemStatusSnapshot> {
    val context = LocalContext.current
    val snapshot = remember(context) { mutableStateOf(readSystemStatus(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                snapshot.value = readSystemStatus(context ?: return)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.location.MODE_CHANGED")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                snapshot.value = readSystemStatus(context)
            }

            override fun onLost(network: android.net.Network) {
                snapshot.value = readSystemStatus(context)
            }

            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                snapshot.value = readSystemStatus(context)
            }
        }

        connectivityManager?.registerDefaultNetworkCallback(callback)

        onDispose {
            context.unregisterReceiver(receiver)
            connectivityManager?.unregisterNetworkCallback(callback)
        }
    }

    return snapshot
}

fun Context.isLauncherDefault(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        }
    }

    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == packageName
}

private fun readSystemStatus(context: Context): SystemStatusSnapshot {
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)?.takeIf { it >= 0 }
    val timeLabel = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    val dateLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val networkLabel = connectivityManager?.activeNetwork
        ?.let(connectivityManager::getNetworkCapabilities)
        ?.let { capabilities ->
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Online"
            }
        }
        ?: "Offline"

    val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val isBluetoothEnabled = bluetoothManager?.adapter?.isEnabled == true

    val locationManager = context.getSystemService(LocationManager::class.java)
    val isLocationEnabled = locationManager?.isLocationEnabled == true

    val isAirplaneModeOn = AndroidSettings.Global.getInt(
        context.contentResolver,
        AndroidSettings.Global.AIRPLANE_MODE_ON,
        0,
    ) != 0

    return SystemStatusSnapshot(
        timeLabel = timeLabel,
        dateLabel = dateLabel,
        networkLabel = networkLabel,
        batteryPercent = batteryLevel,
        isBluetoothEnabled = isBluetoothEnabled,
        isLocationEnabled = isLocationEnabled,
        isAirplaneModeOn = isAirplaneModeOn,
    )
}
