package io.github.ankitchanki.circlet.gesture

/**
 * Splits gesture progress into [steps] detents and reports when the finger moves into a new,
 * higher one. Only rising crossings tick, so jitter back and forth doesn't buzz repeatedly.
 * Pure logic; the vibration itself lives in GestureHaptics.
 */
class HapticSteps(private val steps: Int) {
    private var highest = 0

    /** Returns the newly reached step (1..steps-1) or null if no new detent was crossed. */
    fun onProgress(progress: Float): Int? {
        val step = (progress.coerceIn(0f, 1f) * steps).toInt().coerceAtMost(steps - 1)
        if (step <= highest) return null
        highest = step
        return step
    }

    /** 0..1 strength for a tick at [step], rising as the gesture nears activation. */
    fun intensity(step: Int): Float = 0.25f + 0.75f * step / (steps - 1).coerceAtLeast(1)

    fun reset() {
        highest = 0
    }
}
