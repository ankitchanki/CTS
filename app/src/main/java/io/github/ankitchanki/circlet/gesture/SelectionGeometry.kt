package io.github.ankitchanki.circlet.gesture

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class SelectionRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** Turns a lasso stroke (or a dragged box) into the region to search. */
object SelectionGeometry {

    /**
     * Bounding box of the stroke, grown by [padding] and clamped to the view. Returns null when
     * the stroke is too small in both directions to be a deliberate selection (e.g. a tap).
     */
    fun selectionFor(points: List<Vec2>, viewWidth: Float, viewHeight: Float, minSize: Float, padding: Float): SelectionRect? {
        if (points.isEmpty()) return null
        var left = points[0].x
        var top = points[0].y
        var right = left
        var bottom = top
        for (p in points) {
            left = min(left, p.x)
            top = min(top, p.y)
            right = max(right, p.x)
            bottom = max(bottom, p.y)
        }
        if (right - left < minSize && bottom - top < minSize) return null
        return SelectionRect(
            left = (left - padding).coerceAtLeast(0f),
            top = (top - padding).coerceAtLeast(0f),
            right = (right + padding).coerceAtMost(viewWidth),
            bottom = (bottom + padding).coerceAtMost(viewHeight),
        )
    }

    /** Maps a selection in view coordinates onto the captured bitmap, which may differ in size. */
    fun toBitmapRect(rect: SelectionRect, viewWidth: Float, viewHeight: Float, bitmapWidth: Int, bitmapHeight: Int): PixelRect? {
        if (viewWidth <= 0f || viewHeight <= 0f) return null
        val sx = bitmapWidth / viewWidth
        val sy = bitmapHeight / viewHeight
        val out = PixelRect(
            left = (rect.left * sx).roundToInt().coerceIn(0, bitmapWidth),
            top = (rect.top * sy).roundToInt().coerceIn(0, bitmapHeight),
            right = (rect.right * sx).roundToInt().coerceIn(0, bitmapWidth),
            bottom = (rect.bottom * sy).roundToInt().coerceIn(0, bitmapHeight),
        )
        return out.takeIf { it.width > 0 && it.height > 0 }
    }
}
