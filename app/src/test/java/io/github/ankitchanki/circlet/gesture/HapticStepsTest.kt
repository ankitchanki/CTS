package io.github.ankitchanki.circlet.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticStepsTest {

    @Test
    fun ticksOncePerRisingStep() {
        val steps = HapticSteps(10)
        assertNull(steps.onProgress(0.05f))
        assertEquals(1, steps.onProgress(0.12f))
        assertNull(steps.onProgress(0.15f))
        assertEquals(3, steps.onProgress(0.31f))
    }

    @Test
    fun goingBackDoesNotTickAgain() {
        val steps = HapticSteps(10)
        steps.onProgress(0.5f)
        assertNull(steps.onProgress(0.2f))
        assertNull(steps.onProgress(0.5f))
        assertEquals(6, steps.onProgress(0.6f))
    }

    @Test
    fun fullProgressStaysBelowLastStep() {
        // Activation has its own stronger haptic; the stepper never claims it.
        val steps = HapticSteps(10)
        assertEquals(9, steps.onProgress(1f))
        assertNull(steps.onProgress(1f))
    }

    @Test
    fun intensityRises() {
        val steps = HapticSteps(10)
        assertTrue(steps.intensity(1) < steps.intensity(5))
        assertEquals(1f, steps.intensity(9), 1e-6f)
    }

    @Test
    fun resetStartsOver() {
        val steps = HapticSteps(4)
        steps.onProgress(0.9f)
        steps.reset()
        assertEquals(1, steps.onProgress(0.3f))
    }

    @Test
    fun glowFollowsProgress() {
        assertEquals(0f, GestureVisualsModel.forProgress(0f).glow, 0f)
        assertEquals(1f, GestureVisualsModel.forProgress(1f).glow, 1e-6f)
        assertTrue(GestureVisualsModel.forProgress(0.5f).glow > 0f)
    }
}
