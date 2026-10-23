package com.parallelc.micts.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import android.view.View

/**
 * Invisible touch target sitting in a bottom corner. Once it receives ACTION_DOWN the whole
 * gesture is delivered here, even after the finger leaves its bounds, so it simply forwards
 * events to the controller.
 */
@SuppressLint("ViewConstructor")
class CornerTouchView(
    context: Context,
    private val onTouch: (MotionEvent) -> Boolean,
) : View(context) {

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Keep the system back gesture from claiming swipes that start in this corner.
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = onTouch(event)
}
