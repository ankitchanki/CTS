package io.github.ankitchanki.circlet.assist

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * Assistant session shown when the system invokes Circlet (corner swipe, long-press power/home).
 * The system hands over a screenshot of the current screen, so this path needs no Shizuku; the
 * animation and Circle to Search mode run inside the session's own full-screen window.
 */
class CircletSession(context: Context) : VoiceInteractionSession(context) {
    private var sequence: InvocationSequence? = null
    private var root: FrameLayout? = null
    /** Screenshot that arrived before onShow created the sequence. */
    private var pendingScreenshot: Bitmap? = null
    private var hasPendingScreenshot = false

    override fun onCreate() {
        super.onCreate()
        window.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0f)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
                attributes = attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            }
        }
    }

    override fun onCreateContentView(): View =
        FrameLayout(context).also { root = it }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val container = root ?: return
        sequence?.destroy()
        sequence = InvocationSequence(context, container, onFinished = { hide() }).also {
            it.start()
            if (hasPendingScreenshot) it.onSystemScreenshot(pendingScreenshot)
        }
        pendingScreenshot = null
        hasPendingScreenshot = false
    }

    @Deprecated("Still delivered on all supported versions")
    override fun onHandleScreenshot(screenshot: Bitmap?) {
        val current = sequence
        if (current != null) {
            current.onSystemScreenshot(screenshot)
        } else {
            pendingScreenshot = screenshot
            hasPendingScreenshot = true
        }
    }

    override fun onHide() {
        sequence?.destroy()
        sequence = null
        super.onHide()
    }

    override fun onDestroy() {
        sequence?.destroy()
        sequence = null
        super.onDestroy()
    }
}
