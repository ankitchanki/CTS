package com.parallelc.micts.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureVisualsTest {

    @Test
    fun windowRecessesFromFullToMinScale() {
        assertEquals(1f, GestureVisualsModel.forProgress(0f).windowScale, 0f)
        assertEquals(0.96f, GestureVisualsModel.forProgress(1f).windowScale, 1e-6f)
        assertEquals(0.96f, GestureVisualsModel.forProgress(2f).windowScale, 1e-6f)
    }

    @Test
    fun scaleDecreasesMonotonically() {
        var last = 1f
        for (i in 0..100) {
            val scale = GestureVisualsModel.forProgress(i / 100f).windowScale
            assertTrue(scale <= last)
            last = scale
        }
    }

    @Test
    fun handleInvertsIntoBeam() {
        val rest = GestureVisualsModel.forProgress(0f)
        val armed = GestureVisualsModel.forProgress(1f)
        assertEquals(1f, rest.handleAlpha, 0f)
        assertEquals(0f, rest.beamAlpha, 0f)
        assertEquals(0f, armed.handleAlpha, 0f)
        assertEquals(1f, armed.beamAlpha, 0f)
        assertEquals(1f, armed.beamReach, 1e-6f)
    }

    @Test
    fun shimmerSweepsAcrossAndFades() {
        val start = ShimmerModel.frame(0f)
        val end = ShimmerModel.frame(1f)
        assertTrue(start.bandCenter < 0f)
        assertTrue(end.bandCenter > 1f)
        assertEquals(0f, start.alpha, 1e-6f)
        assertEquals(0f, end.alpha, 1e-6f)
        assertTrue(ShimmerModel.frame(0.5f).alpha > 0.5f)
    }

    @Test
    fun tapIsNotASelection() {
        assertNull(SelectionGeometry.selectionFor(listOf(Vec2(50f, 50f), Vec2(52f, 51f)), 100f, 100f, 10f, 4f))
    }

    @Test
    fun selectionIsPaddedAndClamped() {
        val rect = SelectionGeometry.selectionFor(listOf(Vec2(2f, 10f), Vec2(60f, 40f), Vec2(30f, 98f)), 100f, 100f, 10f, 5f)
        assertNotNull(rect)
        assertEquals(SelectionRect(0f, 5f, 65f, 100f), rect)
    }

    @Test
    fun selectionMapsToBitmapPixels() {
        val px = SelectionGeometry.toBitmapRect(SelectionRect(10f, 20f, 50f, 60f), 100f, 200f, 200, 400)
        assertEquals(PixelRect(20, 40, 100, 120), px)
    }
}
