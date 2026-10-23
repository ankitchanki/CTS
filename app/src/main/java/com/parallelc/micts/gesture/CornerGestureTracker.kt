package com.parallelc.micts.gesture

data class CornerGestureConfig(
    /** Side of the square active zone in each bottom corner (e.g. 40dp in px). */
    val zoneSize: Float,
    /** Extra finger travel away from the corner needed to reach progress 1.0. */
    val activationDistance: Float,
    /** Travel before the direction is judged, so jitter at touch-down doesn't cancel. */
    val touchSlop: Float,
    /** Allowed deviation from the ideal 45° inward/upward diagonal. */
    val angleToleranceDegrees: Float = 30f,
)

enum class GesturePhase {
    Idle,
    Tracking,
    /** Progress hit 1.0 before release. Sticky until the next touch-down. */
    Activated,
    /** Wrong direction, released early, or cancelled by the system. */
    Cancelled,
}

data class GestureState(
    val phase: GesturePhase,
    val corner: Corner? = null,
    val progress: Float = 0f,
) {
    companion object {
        val IDLE = GestureState(GesturePhase.Idle)
    }
}

/**
 * State machine for the diagonal corner swipe. Feed it raw screen coordinates; it reports
 * the phase and the 0..1 progress that drives the animation layers.
 */
class CornerGestureTracker(private val config: CornerGestureConfig) {
    private var origin = Vec2.ZERO
    private var down = Vec2.ZERO
    private var direction = Vec2.ZERO
    private var startDistance = 0f

    var state: GestureState = GestureState.IDLE
        private set

    /** Returns true if the touch started inside a corner zone and the gesture is now tracked. */
    fun onDown(x: Float, y: Float, screenWidth: Float, screenHeight: Float): Boolean {
        val corner = CornerVectorMath.cornerAt(x, y, screenWidth, screenHeight, config.zoneSize)
        if (corner == null) {
            state = GestureState.IDLE
            return false
        }
        origin = CornerVectorMath.cornerOrigin(corner, screenWidth, screenHeight)
        direction = CornerVectorMath.inwardDirection(corner)
        down = Vec2(x, y)
        startDistance = (down - origin).length
        state = GestureState(GesturePhase.Tracking, corner, 0f)
        return true
    }

    fun onMove(x: Float, y: Float): GestureState {
        if (state.phase != GesturePhase.Tracking) return state
        val finger = Vec2(x, y)

        // Direction is judged on the travel since touch-down: the corner->finger vector of a
        // touch that lands near the bottom edge is almost horizontal even for a perfect diagonal.
        val travel = finger - down
        if (travel.length >= config.touchSlop &&
            CornerVectorMath.deviationDegrees(travel, direction) > config.angleToleranceDegrees
        ) {
            state = state.copy(phase = GesturePhase.Cancelled)
            return state
        }

        val progress = CornerVectorMath.progress((finger - origin).length, startDistance, config.activationDistance)
        state = if (progress >= 1f) {
            state.copy(phase = GesturePhase.Activated, progress = 1f)
        } else {
            state.copy(progress = progress)
        }
        return state
    }

    fun onUp(): GestureState {
        if (state.phase == GesturePhase.Tracking) {
            state = state.copy(phase = GesturePhase.Cancelled)
        }
        return state
    }

    fun cancel(): GestureState {
        if (state.phase != GesturePhase.Activated) {
            state = state.copy(phase = GesturePhase.Cancelled)
        }
        return state
    }

    fun reset() {
        state = GestureState.IDLE
    }
}
