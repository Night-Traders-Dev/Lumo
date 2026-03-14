package dev.nighttraders.lumo.launcher.overlay

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.Service
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import dev.nighttraders.lumo.launcher.MainActivity
import dev.nighttraders.lumo.launcher.R
import dev.nighttraders.lumo.launcher.data.LaunchableApp
import dev.nighttraders.lumo.launcher.data.LauncherRepository
import dev.nighttraders.lumo.launcher.lockscreen.LumoLockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class LumoGestureSidebarService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var repository: LauncherRepository

    private var edgeHandleView: View? = null
    private var dismissScrimView: View? = null
    private var railView: View? = null
    private var railApps: List<LaunchableApp> = emptyList()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        repository = LauncherRepository(applicationContext)
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        ensureEdgeHandle()
        refreshRailApps()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_REFRESH, ACTION_START, null -> {
                if (!Settings.canDrawOverlays(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ensureEdgeHandle()
                refreshRailApps()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        hideRail()
        removeOverlayView(edgeHandleView)
        edgeHandleView = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureEdgeHandle() {
        if (edgeHandleView != null) {
            return
        }

        val handleView = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener(EdgeRevealTouchListener())
        }

        windowManager.addView(handleView, edgeHandleLayoutParams())
        edgeHandleView = handleView
    }

    private fun refreshRailApps() {
        serviceScope.launch {
            railApps = repository.loadRailApps()
            if (railView != null) {
                hideRail()
                showRail()
            }
        }
    }

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        return keyguardManager?.isKeyguardLocked == true
    }

    private fun showRail() {
        if (railView != null || !Settings.canDrawOverlays(this) || isDeviceLocked()
            || LumoLockState.isLocked.value
        ) {
            return
        }

        val scrimView = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { hideRail() }
        }
        windowManager.addView(scrimView, dismissScrimLayoutParams())
        dismissScrimView = scrimView

        val railContainer = buildRailView()
        windowManager.addView(railContainer, railLayoutParams())
        railView = railContainer
    }

    private fun hideRail() {
        removeOverlayView(railView)
        railView = null
        removeOverlayView(dismissScrimView)
        dismissScrimView = null
    }

    private fun buildRailView(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.parseColor("#CC0E0A10"))

            // Pinned apps
            railApps.forEach { app ->
                addView(createAppButton(app))
            }

            // Spacer
            addView(Space(this@LumoGestureSidebarService).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0,
                    1f,
                )
            })

            // Separator
            addView(createSeparator())

            // Ubuntu symbol button at bottom — opens app drawer
            addView(createUbuntuButton {
                hideRail()
                startActivity(MainActivity.createAppsIntent(this@LumoGestureSidebarService))
            })
        }

    private fun createUbuntuButton(onClick: () -> Unit): View =
        FrameLayout(this).apply {
            val size = dp(52)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                topMargin = dp(6)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E95420"))
            }
            setOnClickListener { onClick() }

            addView(
                ImageView(this@LumoGestureSidebarService).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER)
                    setImageDrawable(
                        ContextCompat.getDrawable(
                            this@LumoGestureSidebarService,
                            R.drawable.ic_ubuntu_symbol,
                        ),
                    )
                    setColorFilter(Color.WHITE)
                },
            )
        }

    private fun createSeparator(): View =
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(1)).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
            setBackgroundColor(Color.parseColor("#44FFFFFF"))
        }

    private fun createAppButton(app: LaunchableApp): View =
        FrameLayout(this).apply {
            val size = dp(52)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                bottomMargin = dp(6)
            }
            background = squircleDrawable(Color.parseColor("#33FFFFFF"))
            setOnClickListener {
                hideRail()
                repository.launchApp(app)
            }

            if (app.icon != null) {
                addView(
                    ImageView(this@LumoGestureSidebarService).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER)
                        setImageDrawable(BitmapDrawable(resources, app.icon))
                        clipToOutline = true
                    },
                )
            } else {
                addView(
                    TextView(this@LumoGestureSidebarService).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER,
                        )
                        text = app.label.take(1).uppercase()
                        setTextColor(Color.WHITE)
                        textSize = 18f
                    },
                )
            }
        }

    private fun squircleDrawable(fillColor: Int): Drawable =
        object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fillColor
                style = Paint.Style.FILL
            }

            override fun draw(canvas: Canvas) {
                val w = bounds.width().toFloat()
                val h = bounds.height().toFloat()
                val halfW = w / 2f
                val halfH = h / 2f
                val n = 4f
                val path = Path()
                val steps = 180
                path.moveTo(w, halfH)
                for (i in 1..steps) {
                    val angle = 2.0 * Math.PI * i / steps
                    val cosA = kotlin.math.cos(angle).toFloat()
                    val sinA = kotlin.math.sin(angle).toFloat()
                    val x = halfW + halfW * sign(cosA) * abs(cosA).pow(2f / n)
                    val y = halfH + halfH * sign(sinA) * abs(sinA).pow(2f / n)
                    path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, paint)
            }

            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
            @Suppress("DEPRECATION")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }

    private fun edgeHandleLayoutParams(): WindowManager.LayoutParams =
        baseLayoutParams(
            width = dp(18),
            height = WindowManager.LayoutParams.MATCH_PARENT,
        ).apply {
            gravity = Gravity.START or Gravity.TOP
        }

    private fun dismissScrimLayoutParams(): WindowManager.LayoutParams =
        baseLayoutParams(
            width = WindowManager.LayoutParams.MATCH_PARENT,
            height = WindowManager.LayoutParams.MATCH_PARENT,
        ).apply {
            gravity = Gravity.START or Gravity.TOP
        }

    private fun railLayoutParams(): WindowManager.LayoutParams =
        baseLayoutParams(
            width = dp(68),
            height = WindowManager.LayoutParams.MATCH_PARENT,
        ).apply {
            gravity = Gravity.START or Gravity.TOP
        }

    private fun baseLayoutParams(width: Int, height: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width,
            height,
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
        )

    private fun removeOverlayView(view: View?) {
        if (view == null) {
            return
        }

        runCatching {
            windowManager.removeView(view)
        }
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
                    if (event.rawX - startX > dp(26) && !isDeviceLocked()) {
                        showRail()
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
        private const val ACTION_REFRESH = "dev.nighttraders.lumo.launcher.overlay.REFRESH"

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                return
            }

            context.startService(
                Intent(context, LumoGestureSidebarService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, LumoGestureSidebarService::class.java).setAction(ACTION_STOP),
            )
        }

        fun refresh(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                return
            }

            context.startService(
                Intent(context, LumoGestureSidebarService::class.java).setAction(ACTION_REFRESH),
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
