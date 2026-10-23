package com.parallelc.micts.overlay

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
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.parallelc.micts.gesture.PixelPalette
import com.parallelc.micts.gesture.SelectionGeometry
import com.parallelc.micts.gesture.SelectionRect
import com.parallelc.micts.gesture.Vec2

/**
 * Circle to Search mode: shows the frozen screen as a static texture and lets the user draw a
 * lasso (or drag a box) over it. Reports the bounding box of the stroke via [onSelection].
 */
@SuppressLint("ViewConstructor")
class SelectionOverlayView(
    context: Context,
    private val frame: Bitmap,
    private val hint: String,
    private val onSelection: (SelectionRect) -> Unit,
    private val onDismiss: () -> Unit,
) : View(context) {

    private val points = ArrayList<Vec2>()
    private val strokePath = Path()
    private var selection: SelectionRect? = null

    private val frameRect = RectF()
    private val selectionRect = RectF()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val scrimPaint = Paint().apply { color = Color.argb(46, 0, 0, 0) }
    private val outsidePaint = Paint().apply { color = Color.argb(115, 0, 0, 0) }
    private val strokeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp(14f)
        maskFilter = BlurMaskFilter(dp(10f), BlurMaskFilter.Blur.NORMAL)
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp(5f)
        color = Color.WHITE
    }
    private val hintPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 32, 33, 36) }
    private val hintTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dp(15f)
        textAlign = Paint.Align.CENTER
    }
    private val hintRect = RectF()

    private val minSelectionSize = dp(24f)
    private val selectionPadding = dp(8f)
    private val selectionRadius = dp(20f)

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestFocus()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val gradient = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), PixelPalette.BEAM, null, Shader.TileMode.CLAMP)
        strokeGlowPaint.shader = gradient
    }

    override fun onDraw(canvas: Canvas) {
        frameRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(frame, null, frameRect, bitmapPaint)

        val sel = selection
        if (sel == null) {
            canvas.drawRect(frameRect, scrimPaint)
            if (!strokePath.isEmpty) {
                canvas.drawPath(strokePath, strokeGlowPaint)
                canvas.drawPath(strokePath, strokePaint)
            } else {
                drawHint(canvas)
            }
        } else {
            selectionRect.set(sel.left, sel.top, sel.right, sel.bottom)
            val save = canvas.save()
            val clip = Path().apply { addRoundRect(selectionRect, selectionRadius, selectionRadius, Path.Direction.CW) }
            canvas.clipOutPath(clip)
            canvas.drawRect(frameRect, outsidePaint)
            canvas.restoreToCount(save)
            canvas.drawRoundRect(selectionRect, selectionRadius, selectionRadius, strokeGlowPaint)
            canvas.drawRoundRect(selectionRect, selectionRadius, selectionRadius, strokePaint)
        }
    }

    private fun drawHint(canvas: Canvas) {
        val textWidth = hintTextPaint.measureText(hint)
        val padH = dp(20f)
        val top = dp(72f)
        val height = dp(40f)
        hintRect.set(width / 2f - textWidth / 2f - padH, top, width / 2f + textWidth / 2f + padH, top + height)
        canvas.drawRoundRect(hintRect, height / 2f, height / 2f, hintPillPaint)
        val baseline = hintRect.centerY() - (hintTextPaint.descent() + hintTextPaint.ascent()) / 2f
        canvas.drawText(hint, hintRect.centerX(), baseline, hintTextPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                selection = null
                points.clear()
                points += Vec2(x, y)
                strokePath.rewind()
                strokePath.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) {
                    addPoint(event.getHistoricalX(i), event.getHistoricalY(i))
                }
                addPoint(x, y)
            }
            MotionEvent.ACTION_UP -> {
                addPoint(x, y)
                val rect = SelectionGeometry.selectionFor(points, width.toFloat(), height.toFloat(), minSelectionSize, selectionPadding)
                strokePath.rewind()
                if (rect == null) {
                    // A tap rather than a stroke: leave Circle to Search mode.
                    onDismiss()
                } else {
                    selection = rect
                    onSelection(rect)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                points.clear()
                strokePath.rewind()
            }
        }
        invalidate()
        return true
    }

    private fun addPoint(x: Float, y: Float) {
        val last = points.lastOrNull()
        if (last == null) {
            strokePath.moveTo(x, y)
        } else {
            // Quadratic smoothing through midpoints keeps the lasso fluid.
            strokePath.quadTo(last.x, last.y, (last.x + x) / 2f, (last.y + y) / 2f)
        }
        points += Vec2(x, y)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) onDismiss()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
}
