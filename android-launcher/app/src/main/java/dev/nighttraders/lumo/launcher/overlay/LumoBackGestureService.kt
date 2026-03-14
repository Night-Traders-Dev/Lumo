package dev.nighttraders.lumo.launcher.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service that adds a right-edge gesture handle.
 * Swiping from right edge to left performs a BACK action.
 *
 * The handle is automatically suppressed when Lumo's app drawer is open
 * so that the user can swipe to dismiss the drawer without triggering back.
 */
class LumoBackGestureService : AccessibilityService() {
    private var rightEdgeView: View? = null
    private lateinit var windowManager: WindowManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WindowManager::class.java)
        addRightEdgeHandle()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        removeView(rightEdgeView)
        rightEdgeView = null
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun addRightEdgeHandle() {
        if (rightEdgeView != null) return

        val handle = View(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnTouchListener(RightEdgeTouchListener())
        }

        val params = WindowManager.LayoutParams(
            dp(handleWidthDp),
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.TOP
        }

        windowManager.addView(handle, params)
        rightEdgeView = handle
    }

    /** Rebuild the edge handle with the current width. */
    private fun rebuildHandle() {
        removeView(rightEdgeView)
        rightEdgeView = null
        addRightEdgeHandle()
    }

    private fun removeView(view: View?) {
        view ?: return
        runCatching { windowManager.removeView(view) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class RightEdgeTouchListener : View.OnTouchListener {
        private var startX = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            // When Lumo has the app drawer open, pass touches through
            if (suppressBackGesture) return false

            return when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = startX - event.rawX
                    if (dx > dp(thresholdDp)) {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                MotionEvent.ACTION_MOVE -> true
                else -> false
            }
        }
    }

    companion object {
        private var instance: LumoBackGestureService? = null

        /**
         * When true, the back gesture is suppressed (e.g. while the app drawer is visible).
         * The launcher sets this flag from [LumoLauncherApp].
         */
        @Volatile
        var suppressBackGesture: Boolean = false

        /** Current handle width in dp, settable from settings. */
        @Volatile
        var handleWidthDp: Int = 20
            set(value) {
                field = value
                instance?.rebuildHandle()
            }

        /** Swipe threshold in dp, settable from settings. */
        @Volatile
        var thresholdDp: Int = 40
    }
}
