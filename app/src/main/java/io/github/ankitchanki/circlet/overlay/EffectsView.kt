package io.github.ankitchanki.circlet.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Build
import android.util.TypedValue
import android.view.RoundedCorner
import android.view.View
import io.github.ankitchanki.circlet.gesture.Corner
import io.github.ankitchanki.circlet.gesture.GestureVisuals
import io.github.ankitchanki.circlet.gesture.GestureVisualsModel
import io.github.ankitchanki.circlet.gesture.PixelPalette
import io.github.ankitchanki.circlet.gesture.ShimmerFrame
import io.github.ankitchanki.circlet.gesture.lerp

/**
 * Full-screen, non-touchable layer that renders the invocation animation:
 *  - the frozen frame recessed into a rounded card ([GestureVisuals.windowScale]),
 *  - the corner handle lines inverting into a multi-colour crescent beam,
 *  - the white shimmer wave once the viewport is locked.
 *
 * It only draws; all numbers come from [GestureVisualsModel] / ShimmerModel.
 */
@SuppressLint("ViewConstructor")
class EffectsView(context: Context, private val corner: Corner) : View(context) {

    var visuals: GestureVisuals = GestureVisualsModel.forProgress(0f)
        set(value) { field = value; invalidate() }

    /** Frozen screen frame; until it arrives only the beam is drawn over the live screen. */
    var frame: Bitmap? = null
        set(value) { field = value; invalidate() }

    var shimmer: ShimmerFrame? = null
        set(value) { field = value; invalidate() }

    /** Overrides [GestureVisuals.windowScale] while the card settles back after the shimmer. */
    var scaleOverride: Float? = null
        set(value) { field = value; invalidate() }

    private val maxCardRadius = dp(32f)
    private val maxBeamReach = dp(200f)
    private val maxBeamStroke = dp(40f)
    private val handleInset = dp(6f)
    private val handleExtension = dp(22f)
    private val displayCornerRadius by lazy { resolveDisplayCornerRadius() }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shimmerPaint = Paint()
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(3f)
        color = Color.WHITE
    }
    private val beamGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        maskFilter = BlurMaskFilter(dp(22f), BlurMaskFilter.Blur.NORMAL)
    }
    private val beamCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val cardRect = RectF()
    private val cardPath = Path()
    private val handlePath = Path()
    private val arcRect = RectF()
    private var beamShader: SweepGradient? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        beamShader = createBeamShader(h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = frame
        if (bitmap != null) {
            drawRecessedCard(canvas, bitmap)
        } else {
            shimmer?.let { drawShimmer(canvas, it) }
        }
        // Beam and handle are authored for the bottom-left corner and mirrored for bottom-right.
        val save = canvas.save()
        if (corner == Corner.BottomRight) canvas.scale(-1f, 1f, width / 2f, 0f)
        drawBeam(canvas)
        drawHandle(canvas)
        canvas.restoreToCount(save)
    }

    private fun drawRecessedCard(canvas: Canvas, bitmap: Bitmap) {
        val v = visuals
        val scale = scaleOverride ?: v.windowScale
        canvas.drawColor(Color.argb((v.scrimAlpha * 255).toInt(), 0, 0, 0))
        val save = canvas.save()
        canvas.scale(scale, scale, width / 2f, height / 2f)
        cardRect.set(0f, 0f, width.toFloat(), height.toFloat())
        val radius = maxCardRadius * v.cornerRadiusFraction
        cardPath.rewind()
        cardPath.addRoundRect(cardRect, radius, radius, Path.Direction.CW)
        canvas.clipPath(cardPath)
        canvas.drawBitmap(bitmap, null, cardRect, bitmapPaint)
        shimmer?.let { drawShimmer(canvas, it) }
        canvas.restoreToCount(save)
    }

    /** A soft white band sweeping along the inward diagonal, from the invoking corner outward. */
    private fun drawShimmer(canvas: Canvas, s: ShimmerFrame) {
        if (s.alpha <= 0f) return
        val w = width.toFloat()
        val h = height.toFloat()
        val (sx, ex) = if (corner == Corner.BottomLeft) 0f to w else w to 0f
        // Gradient axis runs from the corner (sx, h) to the opposite corner (ex, 0).
        fun at(f: Float) = floatArrayOf(lerp(sx, ex, f), lerp(h, 0f, f))
        val a = at(s.bandCenter - s.bandHalfWidth)
        val b = at(s.bandCenter + s.bandHalfWidth)
        val white = Color.argb((s.alpha * 255).toInt(), 255, 255, 255)
        shimmerPaint.shader = LinearGradient(
            a[0], a[1], b[0], b[1],
            intArrayOf(Color.TRANSPARENT, white, Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, shimmerPaint)
    }

    /** Multi-colour crescent: a quarter ring around the corner that grows with progress. */
    private fun drawBeam(canvas: Canvas) {
        val v = visuals
        if (v.beamAlpha <= 0f) return
        val h = height.toFloat()
        val radius = maxBeamReach * v.beamReach
        val stroke = lerp(dp(4f), maxBeamStroke, v.beamThickness)
        arcRect.set(-radius, h - radius, radius, h + radius)

        beamGlowPaint.shader = beamShader
        beamGlowPaint.strokeWidth = stroke * 1.6f
        beamGlowPaint.alpha = (v.beamAlpha * 0.75f * 255).toInt()
        // 270° (straight up) to 360° (along the bottom edge): the quadrant inside the screen.
        canvas.drawArc(arcRect, 270f, 90f, false, beamGlowPaint)

        beamCorePaint.shader = beamShader
        beamCorePaint.strokeWidth = stroke * 0.45f
        beamCorePaint.alpha = (v.beamAlpha * 255).toInt()
        canvas.drawArc(arcRect, 270f, 90f, false, beamCorePaint)
    }

    /** The resting corner indicator: two short lines joined by an arc that hugs the display corner. */
    private fun drawHandle(canvas: Canvas) {
        val v = visuals
        if (v.handleAlpha <= 0f) return
        val h = height.toFloat()
        val r = displayCornerRadius
        val ext = handleExtension * v.handleLength
        val inner = r - handleInset
        handlePath.rewind()
        handlePath.moveTo(r + ext, h - handleInset)
        handlePath.lineTo(r, h - handleInset)
        arcRect.set(r - inner, h - r - inner, r + inner, h - r + inner)
        handlePath.arcTo(arcRect, 90f, 90f, false)
        handlePath.lineTo(handleInset, h - r - ext)
        handlePaint.alpha = (v.handleAlpha * 0.9f * 255).toInt()
        canvas.drawPath(handlePath, handlePaint)
    }

    /** Sweep gradient with the Pixel palette spread over the 270°..360° quadrant, faded at both ends. */
    private fun createBeamShader(h: Float): SweepGradient {
        val start = 0.75f
        val span = 0.25f
        val colors = intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, *PixelPalette.BEAM, Color.TRANSPARENT)
        val positions = FloatArray(colors.size)
        positions[0] = 0f
        positions[1] = start
        PixelPalette.BEAM.indices.forEach { i ->
            positions[i + 2] = start + span * (0.1f + 0.8f * i / (PixelPalette.BEAM.size - 1))
        }
        positions[colors.size - 1] = 1f
        return SweepGradient(0f, h, colors, positions)
    }

    private fun resolveDisplayCornerRadius(): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius
                ?.takeIf { it > 0 }
                ?.let { return it.toFloat() }
        }
        return dp(24f)
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
}
