package dev.nighttraders.lumo.launcher.overlay

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
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
import dev.nighttraders.lumo.launcher.LockScreenActivity
import dev.nighttraders.lumo.launcher.MainActivity
import dev.nighttraders.lumo.launcher.SettingsActivity
import dev.nighttraders.lumo.launcher.data.LaunchableApp
import dev.nighttraders.lumo.launcher.data.LauncherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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

    private fun showRail() {
        if (railView != null || !Settings.canDrawOverlays(this)) {
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
            setPadding(dp(8), dp(18), dp(8), dp(18))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(30).toFloat()
                setColor(Color.parseColor("#D9160D18"))
                setStroke(dp(1), Color.parseColor("#33FFFFFF"))
            }

            addView(createTextActionButton("Home") {
                hideRail()
                startActivity(MainActivity.createHomeIntent(this@LumoGestureSidebarService))
            })
            addView(createTextActionButton("Apps") {
                hideRail()
                startActivity(MainActivity.createAppsIntent(this@LumoGestureSidebarService))
            })

            railApps.forEach { app ->
                addView(createAppButton(app))
            }

            addView(Space(this@LumoGestureSidebarService).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0,
                    1f,
                )
            })

            addView(createTextActionButton("Lock", compact = true) {
                hideRail()
                startActivity(LockScreenActivity.createIntent(this@LumoGestureSidebarService))
            })
            addView(createTextActionButton("System", compact = true) {
                hideRail()
                startActivity(SettingsActivity.createIntent(this@LumoGestureSidebarService))
            })
        }

    private fun createTextActionButton(
        label: String,
        compact: Boolean = false,
        onClick: () -> Unit,
    ): View =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = if (compact) 11f else 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#E95420"))
            }
            setPadding(dp(10), dp(9), dp(10), dp(9))
            layoutParams = LinearLayout.LayoutParams(dp(88), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            setOnClickListener { onClick() }
        }

    private fun createAppButton(app: LaunchableApp): View =
        FrameLayout(this).apply {
            val size = dp(60)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                bottomMargin = dp(10)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#26000000"))
            }
            foregroundGravity = Gravity.CENTER
            setOnClickListener {
                hideRail()
                repository.launchApp(app)
            }

            if (app.icon != null) {
                addView(
                    ImageView(this@LumoGestureSidebarService).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER)
                        setImageDrawable(BitmapDrawable(resources, app.icon))
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
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    },
                )
            }
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
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.MATCH_PARENT,
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = dp(4)
            y = dp(8)
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
                    if (event.rawX - startX > dp(26)) {
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
