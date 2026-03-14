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
 */
class LumoBackGestureService : AccessibilityService() {
    private var rightEdgeView: View? = null
    private lateinit var windowManager: WindowManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WindowManager::class.java)
        addRightEdgeHandle()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        removeView(rightEdgeView)
        rightEdgeView = null
        super.onDestroy()
    }

    private fun addRightEdgeHandle() {
        if (rightEdgeView != null) return

        val handle = View(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnTouchListener(RightEdgeTouchListener())
        }

        val params = WindowManager.LayoutParams(
            dp(20),
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

    private fun removeView(view: View?) {
        view ?: return
        runCatching { windowManager.removeView(view) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class RightEdgeTouchListener : View.OnTouchListener {
        private var startX = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean =
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = startX - event.rawX
                    if (dx > dp(40)) {
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
