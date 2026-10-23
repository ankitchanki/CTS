package io.github.ankitchanki.circlet.assist

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import io.github.ankitchanki.circlet.R
import io.github.ankitchanki.circlet.gesture.Corner
import io.github.ankitchanki.circlet.gesture.Easing
import io.github.ankitchanki.circlet.gesture.GestureHaptics
import io.github.ankitchanki.circlet.gesture.GestureVisualsModel
import io.github.ankitchanki.circlet.gesture.SelectionGeometry
import io.github.ankitchanki.circlet.gesture.SelectionRect
import io.github.ankitchanki.circlet.gesture.ShimmerModel
import io.github.ankitchanki.circlet.gesture.lerp
import io.github.ankitchanki.circlet.overlay.EffectsView
import io.github.ankitchanki.circlet.overlay.ScreenCapture
import io.github.ankitchanki.circlet.overlay.SelectionOverlayView
import io.github.ankitchanki.circlet.shizuku.ShizukuManager
import io.github.ankitchanki.circlet.trigger.LensLauncher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.sin

/**
 * The invocation played inside an assistant session window: wait for the frozen frame, animate the
 * corner beam + edge glow to full progress with rising haptics, flash the shimmer, then switch to
 * Circle to Search mode (lasso → Google Lens). Same visuals as the corner gesture overlay.
 */
class InvocationSequence(
    private val context: Context,
    private val root: FrameLayout,
    private val onFinished: () -> Unit,
) {
    private val scope = MainScope()
    private val haptics = GestureHaptics(context)
    private val systemFrame = CompletableDeferred<Bitmap?>()
    private var frame: Bitmap? = null
    private var searching = false

    fun start() {
        // Hidden until the frame is in hand, so a Shizuku fallback capture doesn't see our window.
        root.visibility = View.INVISIBLE
        scope.launch {
            val captured = withTimeoutOrNull(SYSTEM_SCREENSHOT_TIMEOUT_MS) { systemFrame.await() }
                ?: if (ShizukuManager.isGranted) ScreenCapture.capture() else null
            if (captured == null) {
                Toast.makeText(context, R.string.assist_no_screenshot, Toast.LENGTH_LONG).show()
                onFinished()
                return@launch
            }
            frame = captured
            playInvocation(captured)
            showSelection(captured)
        }
    }

    /** The system's assist screenshot; null when the user turned "Use screenshot" off. */
    fun onSystemScreenshot(screenshot: Bitmap?) {
        // Software copy: the system bitmap may be hardware-backed or recycled after the callback.
        val copy = screenshot?.let { runCatching { it.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull() }
        // Arrived after the timeout (a Shizuku capture is already in use): drop it.
        if (!systemFrame.complete(copy)) copy?.recycle()
    }

    private suspend fun playInvocation(captured: Bitmap) {
        val effects = EffectsView(context, Corner.BottomRight).apply { frame = captured }
        root.addView(effects, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.visibility = View.VISIBLE

        animate(PROGRESS_MS) { t ->
            val p = Easing.easeInOutCubic(t)
            effects.visuals = GestureVisualsModel.forProgress(p)
            haptics.onProgress(p)
        }
        haptics.activate()
        animate(ShimmerModel.DURATION_MS) { t ->
            effects.shimmer = ShimmerModel.frame(t)
            effects.glowBoost = sin(PI * t).toFloat()
        }
        effects.shimmer = null
        effects.glowBoost = 0f
        animate(ShimmerModel.SETTLE_DURATION_MS) { t ->
            effects.scaleOverride = lerp(GestureVisualsModel.MIN_WINDOW_SCALE, 1f, Easing.easeOutCubic(t))
        }
        effects.frame = null
        root.removeView(effects)
    }

    private fun showSelection(captured: Bitmap) {
        val selection = SelectionOverlayView(
            context = context,
            frame = captured,
            hint = context.getString(R.string.cts_hint),
            onSelection = { rect -> search(rect, captured) },
            onDismiss = { if (!searching) onFinished() },
        )
        root.addView(selection, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun search(rect: SelectionRect, captured: Bitmap) {
        if (searching) return
        searching = true
        scope.launch {
            delay(SELECTION_LINGER_MS)
            val region = SelectionGeometry.toBitmapRect(rect, root.width.toFloat(), root.height.toFloat(), captured.width, captured.height)
            val launched = region != null && LensLauncher.search(context, captured, region)
            if (!launched) Toast.makeText(context, R.string.lens_failed, Toast.LENGTH_SHORT).show()
            onFinished()
        }
    }

    fun destroy() {
        scope.cancel()
        root.removeAllViews()
        frame?.let { if (!it.isRecycled) it.recycle() }
        frame = null
    }

    private suspend fun animate(durationMs: Long, onFrame: (Float) -> Unit) {
        suspendCancellableCoroutine { cont ->
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = durationMs
                addUpdateListener { onFrame(it.animatedFraction) }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (cont.isActive) cont.resume(Unit)
                    }
                })
            }
            cont.invokeOnCancellation { anim.cancel() }
            anim.start()
        }
        // Animators are disabled when the user turns off animations; still emit the final frame.
        onFrame(1f)
    }

    companion object {
        private const val SYSTEM_SCREENSHOT_TIMEOUT_MS = 900L
        private const val PROGRESS_MS = 420L
        private const val SELECTION_LINGER_MS = 300L
    }
}
