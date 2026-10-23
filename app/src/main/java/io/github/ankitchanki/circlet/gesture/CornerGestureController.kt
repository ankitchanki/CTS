package io.github.ankitchanki.circlet.gesture

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import io.github.ankitchanki.circlet.overlay.CornerTouchView
import io.github.ankitchanki.circlet.overlay.EffectsView
import io.github.ankitchanki.circlet.overlay.OverlayWindows
import io.github.ankitchanki.circlet.overlay.ScreenCapture
import io.github.ankitchanki.circlet.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.sin

/**
 * Glue between input, gesture math and the drawing layers:
 *
 *   corner windows --MotionEvent--> [CornerGestureTracker] --progress--> [GestureVisualsModel] --> [EffectsView]
 *
 * On activation it locks the frozen frame, plays the shimmer, and hands the frame to [onInvoked].
 * Must be used from the main thread.
 */
class CornerGestureController(
    private val context: Context,
    private val scope: CoroutineScope,
    /**
     * Called once the invocation animation has finished. [frame] is the frozen screen, or null if
     * it could not be captured; ownership passes to the callee. Call [release] once the next layer
     * is on screen to remove the effects layer.
     */
    private val onInvoked: (frame: Bitmap?, release: () -> Unit) -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val tracker = CornerGestureTracker(
        CornerGestureConfig(
            zoneSize = dp(ZONE_SIZE_DP),
            activationDistance = dp(ACTIVATION_DISTANCE_DP),
            touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
        )
    )

    private val haptics = GestureHaptics(context)
    private val cornerViews = mutableListOf<View>()
    private var effectsView: EffectsView? = null
    private var capture: Deferred<Bitmap?>? = null
    private var revealJob: Job? = null
    private var animator: ValueAnimator? = null
    /** True from activation or release until the effects layer is gone; new touches are ignored. */
    private var busy = false

    fun attach() {
        if (cornerViews.isNotEmpty()) return
        val size = dp(ZONE_SIZE_DP).toInt()
        for (gravity in intArrayOf(Gravity.START, Gravity.END)) {
            val view = CornerTouchView(context, ::onCornerTouch)
            runCatching { windowManager.addView(view, OverlayWindows.cornerZone(size, gravity)) }
                .onSuccess { cornerViews += view }
                .onFailure { e -> Log.e(TAG, "adding corner window failed", e) }
        }
    }

    fun detach() {
        animator?.cancel()
        cornerViews.forEach { runCatching { windowManager.removeView(it) } }
        cornerViews.clear()
        discardCapture()
        removeEffects()
        tracker.reset()
        busy = false
    }

    private fun onCornerTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (busy) return false
                val bounds = OverlayWindows.screenBounds(context)
                if (!tracker.onDown(event.rawX, event.rawY, bounds.width().toFloat(), bounds.height().toFloat())) {
                    return false
                }
                startGesture(tracker.state.corner!!)
            }
            MotionEvent.ACTION_MOVE -> if (!busy) onState(tracker.onMove(event.rawX, event.rawY))
            MotionEvent.ACTION_UP -> if (!busy) onState(tracker.onUp())
            MotionEvent.ACTION_CANCEL -> if (!busy) onState(tracker.cancel())
        }
        return true
    }

    /**
     * Plays the whole invocation without a swipe (progress animates to 1 on its own), e.g. when
     * Circlet is launched as the default assistant by the system's own gesture.
     */
    fun invoke(corner: Corner = Corner.BottomRight) {
        if (busy || effectsView != null) return
        busy = true
        startGesture(corner)
        val view = effectsView ?: run {
            busy = false
            return
        }
        scope.launch {
            animate(AUTO_PROGRESS_MS) { t ->
                val p = Easing.easeInOutCubic(t)
                view.visuals = GestureVisualsModel.forProgress(p)
                haptics.onProgress(p)
            }
            onActivated()
        }
    }

    private fun startGesture(corner: Corner) {
        haptics.reset()
        val view = EffectsView(context, corner).apply { visibility = View.INVISIBLE }
        runCatching { windowManager.addView(view, OverlayWindows.effects()) }
            .onFailure { e ->
                Log.e(TAG, "adding effects window failed", e)
                return
            }
        effectsView = view

        if (ShizukuManager.isGranted) {
            val job = scope.async { ScreenCapture.capture() }
            capture = job
            scope.launch {
                val frame = job.await() ?: return@launch
                if (effectsView === view) view.frame = frame else frame.recycle()
            }
            // Keep the beam hidden briefly so screencap doesn't bake it into the frozen frame.
            revealJob = scope.launch {
                withTimeoutOrNull(REVEAL_DELAY_MS) { job.await() }
                view.visibility = View.VISIBLE
            }
        } else {
            view.visibility = View.VISIBLE
        }
    }

    private fun onState(state: GestureState) {
        when (state.phase) {
            GesturePhase.Tracking -> {
                effectsView?.visuals = GestureVisualsModel.forProgress(state.progress)
                haptics.onProgress(state.progress)
            }
            GesturePhase.Activated -> onActivated()
            GesturePhase.Cancelled -> retract(state.progress)
            GesturePhase.Idle -> Unit
        }
    }

    /** Released early or swiped the wrong way: spring the beam and card back, then clean up. */
    private fun retract(fromProgress: Float) {
        busy = true
        val view = effectsView
        if (view == null || fromProgress <= 0f) {
            finishGesture()
            return
        }
        animator = ValueAnimator.ofFloat(fromProgress, 0f).apply {
            duration = RETRACT_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { view.visuals = GestureVisualsModel.forProgress(it.animatedValue as Float) }
            addListener(onEnd { finishGesture() })
            start()
        }
    }

    private fun finishGesture() {
        discardCapture()
        removeEffects()
        tracker.reset()
        busy = false
    }

    /** Threshold reached before release: lock the viewport, flash the shimmer, hand off. */
    private fun onActivated() {
        busy = true
        val view = effectsView ?: return finishGesture()
        view.visuals = GestureVisualsModel.forProgress(1f)
        haptics.activate()
        scope.launch {
            val job = capture
            val frame = job?.let { withTimeoutOrNull(CAPTURE_TIMEOUT_MS) { it.await() } }
            if (frame == null) job?.cancel()
            capture = null
            revealJob?.cancel()
            view.visibility = View.VISIBLE
            view.frame = frame

            // Shimmer sweeps while the edge glow swells and settles: the "viewport locked" moment.
            animate(ShimmerModel.DURATION_MS) { t ->
                view.shimmer = ShimmerModel.frame(t)
                view.glowBoost = sin(PI * t).toFloat()
            }
            view.shimmer = null
            view.glowBoost = 0f
            animate(ShimmerModel.SETTLE_DURATION_MS) { t ->
                view.scaleOverride = lerp(GestureVisualsModel.MIN_WINDOW_SCALE, 1f, Easing.easeOutCubic(t))
            }

            onInvoked(frame) {
                // The frame now belongs to the next layer; detach it before removing ours.
                view.frame = null
                finishGesture()
            }
        }
    }

    private fun removeEffects() {
        revealJob?.cancel()
        revealJob = null
        effectsView?.let { runCatching { windowManager.removeView(it) } }
        effectsView = null
    }

    /** Drops the pending or finished screenshot. Call before [removeEffects]. */
    private fun discardCapture() {
        capture?.cancel()
        capture = null
        effectsView?.let { view ->
            view.frame?.let { frame ->
                view.frame = null
                frame.recycle()
            }
        }
    }

    private suspend fun animate(durationMs: Long, onFrame: (Float) -> Unit) {
        suspendCancellableCoroutine { cont ->
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = durationMs
                addUpdateListener { onFrame(it.animatedFraction) }
                addListener(onEnd { if (cont.isActive) cont.resume(Unit) })
            }
            animator = anim
            cont.invokeOnCancellation { anim.cancel() }
            anim.start()
        }
        // Animators are disabled when the user turns off animations; still emit the final frame.
        onFrame(1f)
    }

    private fun onEnd(block: () -> Unit) = object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) = block()
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)

    companion object {
        private const val TAG = "Circlet-Gesture"
        const val ZONE_SIZE_DP = 40f
        const val ACTIVATION_DISTANCE_DP = 140f
        private const val REVEAL_DELAY_MS = 200L
        private const val CAPTURE_TIMEOUT_MS = 1500L
        private const val RETRACT_DURATION_MS = 220L
        private const val AUTO_PROGRESS_MS = 420L
    }
}
