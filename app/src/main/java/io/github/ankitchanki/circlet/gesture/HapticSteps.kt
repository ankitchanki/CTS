package io.github.ankitchanki.circlet.gesture

/** A detent crossed by the finger: [step] is the new detent, [rising] whether progress went up. */
data class HapticTick(val step: Int, val rising: Boolean)

/**
 * Splits gesture progress into [steps] detents, like a dial with fine clicks. Moving into a new
 * detent ticks in either direction; going back needs [hysteresis] of a detent beyond the boundary,
 * so a finger resting on a boundary doesn't buzz. Pure logic; the vibration lives in GestureHaptics.
 */
class HapticSteps(private val steps: Int, private val hysteresis: Float = 0.35f) {
    private var current = 0

    /** Returns the detent just entered, or null if the finger is still in the same one. */
    fun onProgress(progress: Float): HapticTick? {
        val raw = progress.coerceIn(0f, 1f) * steps
        val maxStep = steps - 1
        val next = when {
            raw.toInt() > current -> raw.toInt().coerceAtMost(maxStep)
            raw < current - hysteresis -> (raw + hysteresis).toInt().coerceAtLeast(0)
            else -> current
        }
        if (next == current) return null
        val rising = next > current
        current = next
        return HapticTick(next, rising)
    }

    /** 0..1 strength for a tick, rising as the gesture nears activation; backward ticks are softer. */
    fun intensity(tick: HapticTick): Float {
        val base = 0.3f + 0.7f * tick.step / (steps - 1).coerceAtLeast(1)
        return if (tick.rising) base else base * 0.6f
    }

    fun reset() {
        current = 0
    }
}
