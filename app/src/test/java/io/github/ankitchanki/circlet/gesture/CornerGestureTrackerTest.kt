package io.github.ankitchanki.circlet.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CornerGestureTrackerTest {
    private val w = 1080f
    private val h = 2400f
    private val config = CornerGestureConfig(zoneSize = 100f, activationDistance = 400f, touchSlop = 20f)

    @Test
    fun cornerZoneIsStrict() {
        assertEquals(Corner.BottomLeft, CornerVectorMath.cornerAt(10f, h - 10f, w, h, 100f))
        assertEquals(Corner.BottomRight, CornerVectorMath.cornerAt(w - 10f, h - 10f, w, h, 100f))
        assertNull(CornerVectorMath.cornerAt(100f, h - 10f, w, h, 100f))
        assertNull(CornerVectorMath.cornerAt(10f, h - 100f, w, h, 100f))
        assertNull(CornerVectorMath.cornerAt(w / 2, h - 10f, w, h, 100f))
    }

    @Test
    fun inwardDirectionIsUnitDiagonal() {
        val bl = CornerVectorMath.inwardDirection(Corner.BottomLeft)
        val br = CornerVectorMath.inwardDirection(Corner.BottomRight)
        assertEquals(1f, bl.length, 1e-5f)
        assertTrue(bl.x > 0 && bl.y < 0)
        assertTrue(br.x < 0 && br.y < 0)
        assertEquals(45f, CornerVectorMath.deviationDegrees(Vec2(1f, 0f), bl), 1e-3f)
    }

    @Test
    fun touchOutsideZoneIsIgnored() {
        val tracker = CornerGestureTracker(config)
        assertFalse(tracker.onDown(500f, h - 10f, w, h))
        assertEquals(GesturePhase.Idle, tracker.state.phase)
    }

    @Test
    fun progressIsLinearAlongDiagonal() {
        val tracker = CornerGestureTracker(config)
        assertTrue(tracker.onDown(20f, h - 20f, w, h))
        val start = Vec2(20f, h - 20f)
        val dir = CornerVectorMath.inwardDirection(Corner.BottomLeft)

        val quarter = start + dir * 100f
        assertEquals(0.25f, tracker.onMove(quarter.x, quarter.y).progress, 1e-3f)
        val half = start + dir * 200f
        assertEquals(0.5f, tracker.onMove(half.x, half.y).progress, 1e-3f)
        assertEquals(GesturePhase.Tracking, tracker.state.phase)
    }

    @Test
    fun reachingFullProgressActivatesAndSticks() {
        val tracker = CornerGestureTracker(config)
        tracker.onDown(w - 20f, h - 20f, w, h)
        val end = Vec2(w - 20f, h - 20f) + CornerVectorMath.inwardDirection(Corner.BottomRight) * 420f
        val state = tracker.onMove(end.x, end.y)
        assertEquals(GesturePhase.Activated, state.phase)
        assertEquals(1f, state.progress, 0f)
        assertEquals(GesturePhase.Activated, tracker.onMove(w - 20f, h - 20f).phase)
        assertEquals(GesturePhase.Activated, tracker.onUp().phase)
    }

    @Test
    fun releaseBeforeThresholdCancels() {
        val tracker = CornerGestureTracker(config)
        tracker.onDown(20f, h - 20f, w, h)
        tracker.onMove(120f, h - 120f)
        assertEquals(GesturePhase.Cancelled, tracker.onUp().phase)
    }

    @Test
    fun offAxisSwipeCancels() {
        val tracker = CornerGestureTracker(config)
        tracker.onDown(20f, h - 20f, w, h)
        // Straight up: 45° off the diagonal, beyond the 30° tolerance.
        assertEquals(GesturePhase.Cancelled, tracker.onMove(20f, h - 200f).phase)
    }

    @Test
    fun jitterInsideSlopIsTolerated() {
        val tracker = CornerGestureTracker(config)
        tracker.onDown(20f, h - 20f, w, h)
        assertEquals(GesturePhase.Tracking, tracker.onMove(30f, h - 20f).phase)
    }

    @Test
    fun moderatelyOffDiagonalStillTracks() {
        val tracker = CornerGestureTracker(config)
        tracker.onDown(20f, h - 20f, w, h)
        // 60° above horizontal = 15° off the ideal diagonal.
        assertEquals(GesturePhase.Tracking, tracker.onMove(20f + 100f, h - 20f - 173f).phase)
    }
}

class CornerZoneInsetTest {
    private val w = 1080f
    private val h = 2400f
    private val inset = 132f

    @Test
    fun zonesSitAboveTheNavigationBar() {
        // Inside the nav bar / gesture strip: not ours, the system owns it.
        assertNull(CornerVectorMath.cornerAt(10f, h - 10f, w, h, 100f, inset))
        // Just above it: ours.
        assertEquals(Corner.BottomLeft, CornerVectorMath.cornerAt(10f, h - inset - 10f, w, h, 100f, inset))
        assertEquals(Corner.BottomRight, CornerVectorMath.cornerAt(w - 10f, h - inset - 10f, w, h, 100f, inset))
        assertNull(CornerVectorMath.cornerAt(10f, h - inset - 100f, w, h, 100f, inset))
    }

    @Test
    fun progressStartsAtZeroAboveTheInset() {
        val tracker = CornerGestureTracker(CornerGestureConfig(zoneSize = 100f, activationDistance = 400f, touchSlop = 20f))
        assertTrue(tracker.onDown(20f, h - inset - 20f, w, h, inset))
        val start = Vec2(20f, h - inset - 20f)
        val half = start + CornerVectorMath.inwardDirection(Corner.BottomLeft) * 200f
        assertEquals(0.5f, tracker.onMove(half.x, half.y).progress, 1e-3f)
    }
}
