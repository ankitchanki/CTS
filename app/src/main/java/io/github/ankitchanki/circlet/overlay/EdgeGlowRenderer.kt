package io.github.ankitchanki.circlet.overlay

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.RoundedCorner
import android.view.View
import io.github.ankitchanki.circlet.gesture.PixelPalette
import kotlin.math.PI
import kotlin.math.sin

/**
 * Rotating multi-colour glow hugging the screen edges (in the spirit of the Apple Intelligence
 * glow): a wide soft halo, a mid glow and a thin bright core, all sharing one sweep gradient that
 * slowly spins and breathes. Keeps the host [view] animating while visible.
 */
class EdgeGlowRenderer(private val view: View) {
    private val startTime = SystemClock.uptimeMillis()
    private val rect = RectF()
    private val matrix = Matrix()
    private var shader: SweepGradient? = null
    private var cornerRadius = -1f

    private val halo = strokePaint(blur = dp(26f))
    private val glow = strokePaint(blur = dp(9f))
    private val core = strokePaint(blur = 0f)

    fun onSizeChanged(w: Int, h: Int) {
        shader = SweepGradient(w / 2f, h / 2f, COLORS, null).also {
            halo.shader = it
            glow.shader = it
            core.shader = it
        }
    }

    /** Draws the glow at [intensity] (0..1); [boost] (0..1) adds a brief swell, e.g. on activation. */
    fun draw(canvas: Canvas, intensity: Float, boost: Float = 0f) {
        val s = shader ?: return
        if (intensity <= 0f) return
        val elapsed = SystemClock.uptimeMillis() - startTime
        val w = view.width.toFloat()
        val h = view.height.toFloat()

        matrix.setRotate(360f * (elapsed % ROTATION_MS) / ROTATION_MS, w / 2f, h / 2f)
        s.setLocalMatrix(matrix)

        val breathe = 1f + 0.12f * sin(2 * PI * (elapsed % BREATH_MS) / BREATH_MS).toFloat()
        val strength = (intensity * (1f + 0.6f * boost)).coerceAtMost(1.6f)
        if (cornerRadius < 0f) cornerRadius = resolveCornerRadius()
        // Strokes are centred on the physical edge, so only the inner half is visible.
        rect.set(0f, 0f, w, h)

        halo.strokeWidth = dp(46f) * strength * breathe
        halo.alpha = (0.55f * intensity * 255).toInt().coerceIn(0, 255)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, halo)

        glow.strokeWidth = dp(18f) * strength * breathe
        glow.alpha = (0.85f * intensity * 255).toInt().coerceIn(0, 255)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glow)

        core.strokeWidth = dp(4f) * strength
        core.alpha = (intensity * 255).toInt().coerceIn(0, 255)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, core)

        view.postInvalidateOnAnimation()
    }

    private fun strokePaint(blur: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        if (blur > 0f) maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
    }

    private fun resolveCornerRadius(): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius
                ?.takeIf { it > 0 }
                ?.let { return it.toFloat() }
        }
        return dp(28f)
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, view.resources.displayMetrics)

    companion object {
        private const val ROTATION_MS = 3600L
        private const val BREATH_MS = 1700L
        private val COLORS = intArrayOf(
            PixelPalette.BLUE, PixelPalette.RED, PixelPalette.YELLOW, PixelPalette.GREEN, PixelPalette.BLUE,
        )
    }
}
