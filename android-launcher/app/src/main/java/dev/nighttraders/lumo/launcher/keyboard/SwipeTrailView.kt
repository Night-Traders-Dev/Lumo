package dev.nighttraders.lumo.launcher.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.widget.LinearLayout

/**
 * A LinearLayout that draws a swipe trail overlay on top of its children.
 * Used as the keyboard root so the trail renders above keys without
 * introducing a FrameLayout that breaks IME height measurement.
 */
internal class SwipeTrailLinearLayout(context: Context) : LinearLayout(context) {
    private val trailPath = Path()
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80E95420")
        style = Paint.Style.STROKE
        strokeWidth = (4f * context.resources.displayMetrics.density)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCE95420")
        style = Paint.Style.FILL
    }
    private var dotX = 0f
    private var dotY = 0f
    private var hasPoints = false
    private val dotRadius = 6f * context.resources.displayMetrics.density

    init {
        // Enable drawing after children (dispatchDraw overlay)
        setWillNotDraw(false)
    }

    fun beginTrail(x: Float, y: Float) {
        trailPath.reset()
        trailPath.moveTo(x, y)
        dotX = x
        dotY = y
        hasPoints = true
        invalidate()
    }

    fun extendTrail(x: Float, y: Float) {
        if (!hasPoints) {
            beginTrail(x, y)
            return
        }
        trailPath.lineTo(x, y)
        dotX = x
        dotY = y
        invalidate()
    }

    fun clearTrail() {
        trailPath.reset()
        hasPoints = false
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (!hasPoints) return
        canvas.drawPath(trailPath, trailPaint)
        canvas.drawCircle(dotX, dotY, dotRadius, dotPaint)
    }
}
