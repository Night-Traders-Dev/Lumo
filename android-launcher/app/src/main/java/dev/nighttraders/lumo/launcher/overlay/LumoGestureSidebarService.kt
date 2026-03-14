package dev.nighttraders.lumo.launcher.overlay

import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import dev.nighttraders.lumo.launcher.MainActivity
import dev.nighttraders.lumo.launcher.lockscreen.LumoLockState

/**
 * Thin edge-detection overlay service. When the user swipes from the left
 * edge in any app, this brings the launcher to the foreground with the
 * Compose dash rail visible.  All dash rendering, drag-and-drop, and
 * settings are handled by the single Compose implementation in
 * [dev.nighttraders.lumo.launcher.ui.LumoLauncherApp].
 */
class LumoGestureSidebarService : Service() {
    private lateinit var windowManager: WindowManager
    private var edgeHandleView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        ensureEdgeHandle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START, null -> {
                if (!Settings.canDrawOverlays(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ensureEdgeHandle()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlayView(edgeHandleView)
        edgeHandleView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureEdgeHandle() {
        if (edgeHandleView != null) return

        val handleView = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener(EdgeRevealTouchListener())
        }

        windowManager.addView(handleView, edgeHandleLayoutParams())
        edgeHandleView = handleView
    }

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        return keyguardManager?.isKeyguardLocked == true
    }

    private fun openDashRail() {
        if (isDeviceLocked() || LumoLockState.isLocked.value) return
        startActivity(MainActivity.createDashIntent(this))
    }

    private fun edgeHandleLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            dp(18),
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.START or Gravity.TOP
        }

    private fun removeOverlayView(view: View?) {
        if (view == null) return
        runCatching { windowManager.removeView(view) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class EdgeRevealTouchListener : View.OnTouchListener {
        private var startX = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean =
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.rawX - startX > dp(26)) {
                        openDashRail()
                        startX = event.rawX
                    }
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> true

                else -> false
            }
    }

    companion object {
        private const val ACTION_START = "dev.nighttraders.lumo.launcher.overlay.START"
        private const val ACTION_STOP = "dev.nighttraders.lumo.launcher.overlay.STOP"

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            context.startService(
                Intent(context, LumoGestureSidebarService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, LumoGestureSidebarService::class.java).setAction(ACTION_STOP),
            )
        }

        fun sync(context: Context, overlayEnabled: Boolean) {
            if (overlayEnabled && Settings.canDrawOverlays(context)) {
                start(context)
            } else {
                stop(context)
            }
        }

        fun isRunning(context: Context): Boolean {
            val activityManager = context.getSystemService(ActivityManager::class.java) ?: return false
            @Suppress("DEPRECATION")
            return activityManager.getRunningServices(Int.MAX_VALUE).any { service ->
                service.service.className == LumoGestureSidebarService::class.java.name
            }
        }
    }
}
