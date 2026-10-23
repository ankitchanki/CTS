package io.github.ankitchanki.circlet.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

/** Layout params for the three overlay layers, kept in one place. */
object OverlayWindows {

    private val baseFlags = LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        LayoutParams.FLAG_LAYOUT_NO_LIMITS or
        LayoutParams.FLAG_HARDWARE_ACCELERATED

    /**
     * Small touchable square in a bottom corner that receives the start of the swipe, lifted
     * [bottomOffsetPx] above the bottom edge so it doesn't sit under the navigation bar.
     */
    fun cornerZone(sizePx: Int, gravity: Int, bottomOffsetPx: Int) = base(sizePx, sizePx).apply {
        flags = baseFlags or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCH_MODAL
        this.gravity = Gravity.BOTTOM or gravity
        y = bottomOffsetPx
        title = "Circlet corner"
    }

    /**
     * Height of the bottom strip the system owns: the navigation bar (3-button) or the gesture
     * handle area. Overlay windows sit below the navigation bar layer, so touches there go to the
     * system (e.g. the Recents button) instead of the corner zones.
     */
    fun bottomSystemInset(context: Context): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val insets = context.getSystemService(WindowManager::class.java).currentWindowMetrics.windowInsets
                return insets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.navigationBars() or WindowInsets.Type.mandatorySystemGestures()
                ).bottom
            }
        }
        val res = context.resources
        @Suppress("DiscouragedApi", "InternalInsetResource")
        val id = res.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id != 0) res.getDimensionPixelSize(id) else 0
    }

    /** Full-screen drawing layer for the animation; never intercepts touches. */
    fun effects() = base(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
        flags = baseFlags or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCHABLE
        title = "Circlet effects"
    }

    /** Full-screen interactive Circle to Search layer; focusable so it receives Back. */
    fun selection() = base(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
        flags = baseFlags
        title = "Circlet selection"
    }

    private fun base(width: Int, height: Int) = LayoutParams(
        width,
        height,
        LayoutParams.TYPE_APPLICATION_OVERLAY,
        0,
        PixelFormat.TRANSLUCENT,
    ).apply {
        layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else {
            LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Don't let the system push the layer above the navigation bar.
            fitInsetsTypes = 0
        }
    }

    /** Full display bounds in the same coordinate space as MotionEvent raw coordinates. */
    fun screenBounds(context: Context): Rect {
        val wm = context.getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.maximumWindowMetrics.bounds
        } else {
            val size = android.graphics.Point()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(size)
            Rect(0, 0, size.x, size.y)
        }
    }
}
