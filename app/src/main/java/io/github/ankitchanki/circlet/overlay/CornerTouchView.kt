package io.github.ankitchanki.circlet.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import io.github.ankitchanki.circlet.gesture.PixelPalette

/**
 * Touch target sitting in a bottom corner. Once it receives ACTION_DOWN the whole gesture is
 * delivered here, even after the finger leaves its bounds, so it simply forwards events to the
 * controller. Normally invisible; [hintAlpha] makes it glow with an arrow showing which way to
 * swipe, so people can find it.
 */
@SuppressLint("ViewConstructor")
class CornerTouchView(
    context: Context,
    /** True for the bottom-left zone (arrow points up-right), false for bottom-right (up-left). */
    private val leftCorner: Boolean,
    private val onTouch: (MotionEvent) -> Boolean,
) : View(context) {

    var hintAlpha: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    private val rect = RectF()
    private val arrow = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val gradient = LinearGradient(0f, h.toFloat(), w.toFloat(), 0f, PixelPalette.BEAM, null, Shader.TileMode.CLAMP)
        fillPaint.shader = gradient
        strokePaint.shader = gradient
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Keep the system back gesture from claiming swipes that start in this corner.
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (hintAlpha <= 0f) return
        val inset = strokePaint.strokeWidth
        rect.set(inset, inset, width - inset, height - inset)
        val radius = dp(10f)
        fillPaint.alpha = (hintAlpha * 0.45f * 255).toInt()
        canvas.drawRoundRect(rect, radius, radius, fillPaint)
        strokePaint.alpha = (hintAlpha * 255).toInt()
        canvas.drawRoundRect(rect, radius, radius, strokePaint)

        // Diagonal arrow from the outer bottom corner towards the screen centre.
        val w = width.toFloat()
        val h = height.toFloat()
        val m = w * 0.28f
        val (sx, ex) = if (leftCorner) m to w - m else w - m to m
        val sy = h - m
        val ey = m
        val head = w * 0.22f
        arrow.rewind()
        arrow.moveTo(sx, sy)
        arrow.lineTo(ex, ey)
        arrow.moveTo(ex, ey)
        arrow.lineTo(ex + (if (leftCorner) -head else head), ey)
        arrow.moveTo(ex, ey)
        arrow.lineTo(ex, ey + head)
        arrowPaint.alpha = (hintAlpha * 255).toInt()
        canvas.drawPath(arrow, arrowPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = onTouch(event)

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
}
