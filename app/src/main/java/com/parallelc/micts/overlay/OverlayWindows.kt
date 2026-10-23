package com.parallelc.micts.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

/** Layout params for the three overlay layers, kept in one place. */
object OverlayWindows {

    private val baseFlags = LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        LayoutParams.FLAG_LAYOUT_NO_LIMITS or
        LayoutParams.FLAG_HARDWARE_ACCELERATED

    /** Small touchable square in a bottom corner that receives the start of the swipe. */
    fun cornerZone(sizePx: Int, gravity: Int) = base(sizePx, sizePx).apply {
        flags = baseFlags or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCH_MODAL
        this.gravity = Gravity.BOTTOM or gravity
        title = "ShizukuCTS corner"
    }

    /** Full-screen drawing layer for the animation; never intercepts touches. */
    fun effects() = base(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
        flags = baseFlags or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCHABLE
        title = "ShizukuCTS effects"
    }

    /** Full-screen interactive Circle to Search layer; focusable so it receives Back. */
    fun selection() = base(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
        flags = baseFlags
        title = "ShizukuCTS selection"
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
