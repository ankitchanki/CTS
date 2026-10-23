package io.github.ankitchanki.circlet.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticStepsTest {

    @Test
    fun ticksOncePerNewDetent() {
        val steps = HapticSteps(10)
        assertNull(steps.onProgress(0.05f))
        assertEquals(HapticTick(1, rising = true), steps.onProgress(0.12f))
        assertNull(steps.onProgress(0.15f))
        assertEquals(HapticTick(3, rising = true), steps.onProgress(0.31f))
    }

    @Test
    fun goingBackTicksAfterHysteresis() {
        val steps = HapticSteps(10)
        steps.onProgress(0.52f) // detent 5
        // Just under the boundary: within the hysteresis band, no tick.
        assertNull(steps.onProgress(0.48f))
        // Well into detent 4: a softer backward tick.
        val back = steps.onProgress(0.44f)
        assertEquals(HapticTick(4, rising = false), back)
        assertTrue(steps.intensity(back!!) < steps.intensity(HapticTick(4, rising = true)))
    }

    @Test
    fun restingOnABoundaryDoesNotBuzz() {
        val steps = HapticSteps(10)
        assertEquals(HapticTick(5, rising = true), steps.onProgress(0.501f))
        assertNull(steps.onProgress(0.499f))
        assertNull(steps.onProgress(0.501f))
        assertNull(steps.onProgress(0.497f))
    }

    @Test
    fun fullProgressStaysBelowLastStep() {
        // Activation has its own stronger haptic; the stepper never claims it.
        val steps = HapticSteps(10)
        assertEquals(9, steps.onProgress(1f)?.step)
        assertNull(steps.onProgress(1f))
    }

    @Test
    fun intensityRises() {
        val steps = HapticSteps(10)
        assertTrue(steps.intensity(HapticTick(1, true)) < steps.intensity(HapticTick(5, true)))
        assertEquals(1f, steps.intensity(HapticTick(9, true)), 1e-6f)
    }

    @Test
    fun resetStartsOver() {
        val steps = HapticSteps(4)
        steps.onProgress(0.9f)
        steps.reset()
        assertEquals(1, steps.onProgress(0.3f)?.step)
    }

    @Test
    fun glowFollowsProgress() {
        assertEquals(0f, GestureVisualsModel.forProgress(0f).glow, 0f)
        assertEquals(1f, GestureVisualsModel.forProgress(1f).glow, 1e-6f)
        assertTrue(GestureVisualsModel.forProgress(0.5f).glow > 0f)
    }
}
