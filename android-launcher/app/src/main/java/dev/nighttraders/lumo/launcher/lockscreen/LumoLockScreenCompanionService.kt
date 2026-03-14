package dev.nighttraders.lumo.launcher.lockscreen

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.nighttraders.lumo.launcher.LockScreenActivity
import dev.nighttraders.lumo.launcher.R

class LumoLockScreenCompanionService : Service() {
    private lateinit var notificationManager: NotificationManager
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var powerManager: PowerManager

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> maybeTriggerWakeLockScreen()
                Intent.ACTION_USER_PRESENT,
                Intent.ACTION_SCREEN_OFF,
                -> dismissWakeSurface(this@LumoLockScreenCompanionService)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        keyguardManager = getSystemService(KeyguardManager::class.java)
        powerManager = getSystemService(PowerManager::class.java)
        ensureNotificationChannels()
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                dismissWakeSurface(this)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_DISMISS_WAKE -> {
                dismissWakeSurface(this)
            }
        }

        if (!startForegroundCompat()) {
            stopSelf()
            return START_NOT_STICKY
        }
        maybeTriggerWakeLockScreen()
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenStateReceiver) }
        dismissWakeSurface(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun maybeTriggerWakeLockScreen() {
        if (!powerManager.isInteractive || !keyguardManager.isDeviceLocked) {
            dismissWakeSurface(this)
            return
        }

        notificationManager.notify(WAKE_NOTIFICATION_ID, buildWakeNotification())
    }

    private fun buildServiceNotification() =
        NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(getString(R.string.lock_screen_service_notification_title))
            .setContentText(getString(R.string.lock_screen_service_notification_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun buildWakeNotification() =
        NotificationCompat.Builder(this, WAKE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(getString(R.string.lock_screen_wake_notification_title))
            .setContentText(getString(R.string.lock_screen_wake_notification_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setAutoCancel(false)
            .setContentIntent(lockScreenPendingIntent())
            .setFullScreenIntent(lockScreenPendingIntent(), true)
            .build()

    private fun lockScreenPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            1001,
            LockScreenActivity.createIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        notificationManager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    getString(R.string.lock_screen_service_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.lock_screen_service_channel_description)
                    setShowBadge(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
                },
                NotificationChannel(
                    WAKE_CHANNEL_ID,
                    getString(R.string.lock_screen_wake_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = getString(R.string.lock_screen_wake_channel_description)
                    setShowBadge(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                },
            ),
        )
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenStateReceiver, filter)
        }
    }

    private fun startForegroundCompat(): Boolean =
        runCatching {
            val notification = buildServiceNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(SERVICE_NOTIFICATION_ID, notification)
            }
        }.isSuccess

    companion object {
        private const val ACTION_START = "dev.nighttraders.lumo.launcher.lockscreen.START"
        private const val ACTION_STOP = "dev.nighttraders.lumo.launcher.lockscreen.STOP"
        private const val ACTION_DISMISS_WAKE = "dev.nighttraders.lumo.launcher.lockscreen.DISMISS_WAKE"

        private const val SERVICE_CHANNEL_ID = "lumo_lockscreen_service"
        private const val WAKE_CHANNEL_ID = "lumo_lockscreen_wake"
        private const val SERVICE_NOTIFICATION_ID = 4101
        private const val WAKE_NOTIFICATION_ID = 4102

        fun start(context: Context) {
            if (!isWakeCompanionSupported()) {
                return
            }

            ContextCompat.startForegroundService(
                context,
                Intent(context, LumoLockScreenCompanionService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            dismissWakeSurface(context)
            context.stopService(Intent(context, LumoLockScreenCompanionService::class.java))
        }

        fun sync(context: Context, enabled: Boolean) {
            if (enabled && isWakeCompanionSupported()) {
                start(context)
            } else {
                stop(context)
            }
        }

        fun isWakeCompanionSupported(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        fun dismissWakeSurface(context: Context) {
            context.getSystemService(NotificationManager::class.java)?.cancel(WAKE_NOTIFICATION_ID)
        }

        fun hasFullScreenIntentPermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return true
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            return notificationManager?.canUseFullScreenIntent() ?: false
        }

        fun createFullScreenIntentSettingsIntent(context: Context): Intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:${context.packageName}"),
                )
            } else {
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                )
            }
    }
}
