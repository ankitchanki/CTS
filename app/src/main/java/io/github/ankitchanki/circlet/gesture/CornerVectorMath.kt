package io.github.ankitchanki.circlet.gesture

import kotlin.math.acos
import kotlin.math.sqrt

// Pure geometry for the diagonal corner swipe. No Android types here so it can be unit tested on the JVM.
// Screen coordinates: origin at the top-left, y grows downward.

enum class Corner { BottomLeft, BottomRight }

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    infix fun dot(o: Vec2) = x * o.x + y * o.y
    val length: Float get() = sqrt(x * x + y * y)
    fun normalized(): Vec2 = length.let { if (it == 0f) ZERO else Vec2(x / it, y / it) }

    companion object {
        val ZERO = Vec2(0f, 0f)
    }
}

object CornerVectorMath {
    private const val INV_SQRT2 = 0.70710677f

    /** The physical screen corner a gesture is measured from. */
    fun cornerOrigin(corner: Corner, screenWidth: Float, screenHeight: Float): Vec2 = when (corner) {
        Corner.BottomLeft -> Vec2(0f, screenHeight)
        Corner.BottomRight -> Vec2(screenWidth, screenHeight)
    }

    /** Unit vector pointing inward and upward at 45° from the corner, toward the screen centre. */
    fun inwardDirection(corner: Corner): Vec2 = when (corner) {
        Corner.BottomLeft -> Vec2(INV_SQRT2, -INV_SQRT2)
        Corner.BottomRight -> Vec2(-INV_SQRT2, -INV_SQRT2)
    }

    /** Returns the corner whose [zoneSize] x [zoneSize] active square strictly contains the point, if any. */
    fun cornerAt(x: Float, y: Float, screenWidth: Float, screenHeight: Float, zoneSize: Float): Corner? {
        if (y <= screenHeight - zoneSize || y > screenHeight) return null
        return when {
            x >= 0f && x < zoneSize -> Corner.BottomLeft
            x > screenWidth - zoneSize && x <= screenWidth -> Corner.BottomRight
            else -> null
        }
    }

    /** Angle in degrees between [vector] and the unit [direction]; 0 for a zero-length vector. */
    fun deviationDegrees(vector: Vec2, direction: Vec2): Float {
        val len = vector.length
        if (len == 0f) return 0f
        val cos = ((vector dot direction) / len).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos).toDouble()).toFloat()
    }

    /**
     * Linear activation progress from the raw distance between the finger and the corner origin.
     * [startDistance] is that distance at touch-down, so progress starts at exactly 0 wherever
     * inside the zone the finger lands.
     */
    fun progress(distanceFromCorner: Float, startDistance: Float, activationDistance: Float): Float {
        if (activationDistance <= 0f) return 1f
        return ((distanceFromCorner - startDistance) / activationDistance).coerceIn(0f, 1f)
    }
}
