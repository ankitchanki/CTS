package io.github.ankitchanki.circlet.gesture

import kotlin.math.PI
import kotlin.math.sin

// Maps gesture progress / animation time to plain numbers the drawing layer consumes.
// Kept free of Android types so the curves can be unit tested.

object PixelPalette {
    const val BLUE = 0xFF4285F4.toInt()
    const val RED = 0xFFEA4335.toInt()
    const val YELLOW = 0xFFFBBC04.toInt()
    const val GREEN = 0xFF34A853.toInt()

    /** Order along the crescent, starting from the screen edge. */
    val BEAM = intArrayOf(BLUE, RED, YELLOW, GREEN, BLUE)
}

data class GestureVisuals(
    /** Scale of the frozen app frame: 1.0 at rest, [GestureVisualsModel.MIN_WINDOW_SCALE] when armed. */
    val windowScale: Float,
    /** 0..1 multiplier for the card corner radius. */
    val cornerRadiusFraction: Float,
    /** Opacity of the dark layer behind the recessed card. */
    val scrimAlpha: Float,
    /** Opacity of the resting corner handle lines; fades out as they invert into the beam. */
    val handleAlpha: Float,
    /** 0..1 length of the corner handle lines. */
    val handleLength: Float,
    /** 0..1 radius of the multi-colour crescent relative to its maximum reach. */
    val beamReach: Float,
    /** 0..1 stroke thickness of the crescent relative to its maximum. */
    val beamThickness: Float,
    val beamAlpha: Float,
    /** 0..1 strength of the rotating edge glow around the whole screen. */
    val glow: Float,
)

object GestureVisualsModel {
    const val MIN_WINDOW_SCALE = 0.96f
    const val MAX_SCRIM_ALPHA = 0.55f

    /** The handle lines finish inverting into the beam at this progress. */
    private const val HANDLE_INVERSION_END = 0.35f

    fun forProgress(progress: Float): GestureVisuals {
        val p = progress.coerceIn(0f, 1f)
        val eased = Easing.easeOutCubic(p)
        val inversion = Easing.smoothstep(0f, HANDLE_INVERSION_END, p)
        return GestureVisuals(
            windowScale = lerp(1f, MIN_WINDOW_SCALE, eased),
            cornerRadiusFraction = eased,
            scrimAlpha = MAX_SCRIM_ALPHA * p,
            handleAlpha = 1f - inversion,
            handleLength = lerp(0.4f, 1f, inversion),
            beamReach = lerp(0.15f, 1f, eased),
            beamThickness = lerp(0.2f, 1f, p),
            beamAlpha = Easing.smoothstep(0.05f, 0.4f, p),
            glow = Easing.smoothstep(0f, 0.85f, p),
        )
    }
}

data class ShimmerFrame(
    /** Band centre along the inward diagonal, as a fraction of the card's diagonal (may be <0 or >1). */
    val bandCenter: Float,
    /** Half width of the band as a fraction of the card's diagonal. */
    val bandHalfWidth: Float,
    /** Peak white opacity of the band. */
    val alpha: Float,
)

object ShimmerModel {
    const val DURATION_MS = 450L
    const val SETTLE_DURATION_MS = 250L
    const val BAND_HALF_WIDTH = 0.22f
    private const val PEAK_ALPHA = 0.65f

    fun frame(t: Float): ShimmerFrame {
        val tt = t.coerceIn(0f, 1f)
        return ShimmerFrame(
            bandCenter = lerp(-BAND_HALF_WIDTH, 1f + BAND_HALF_WIDTH, Easing.easeInOutCubic(tt)),
            bandHalfWidth = BAND_HALF_WIDTH,
            alpha = PEAK_ALPHA * sin(PI * tt).toFloat(),
        )
    }
}

object Easing {
    fun easeOutCubic(t: Float): Float {
        val inv = 1f - t.coerceIn(0f, 1f)
        return 1f - inv * inv * inv
    }

    fun easeInOutCubic(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).let { it * it * it } / 2f
    }

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        if (edge1 <= edge0) return if (x < edge0) 0f else 1f
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}

fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
