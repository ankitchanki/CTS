package io.github.ankitchanki.circlet.overlay

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import io.github.ankitchanki.circlet.R
import io.github.ankitchanki.circlet.gesture.SelectionGeometry
import io.github.ankitchanki.circlet.gesture.SelectionRect
import io.github.ankitchanki.circlet.trigger.LensLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One Circle to Search session over a frozen [frame]: shows the selection layer, then sends the
 * selected region to Lens. Owns and recycles [frame].
 */
class CircleToSearchSession(
    private val context: Context,
    private val scope: CoroutineScope,
    private val frame: Bitmap,
    private val onClosed: () -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: SelectionOverlayView? = null
    private var searching = false

    /** Returns false if the overlay could not be shown; the frame is released in that case. */
    fun show(): Boolean {
        val overlay = SelectionOverlayView(
            context = context,
            frame = frame,
            hint = context.getString(R.string.cts_hint),
            onSelection = ::onSelection,
            // Ignore dismissal while the frame is still being cropped for Lens.
            onDismiss = { if (!searching) close() },
        )
        return runCatching { windowManager.addView(overlay, OverlayWindows.selection()) }
            .onSuccess { view = overlay }
            .onFailure { e ->
                Log.e(TAG, "adding selection window failed", e)
                close()
            }
            .isSuccess
    }

    private fun onSelection(rect: SelectionRect) {
        val overlay = view ?: return
        if (searching) return
        searching = true
        scope.launch {
            // Let the selection outline register before leaving.
            delay(SELECTION_LINGER_MS)
            val region = SelectionGeometry.toBitmapRect(
                rect, overlay.width.toFloat(), overlay.height.toFloat(), frame.width, frame.height
            )
            val launched = region != null && LensLauncher.search(context, frame, region)
            if (!launched) {
                Toast.makeText(context, R.string.lens_failed, Toast.LENGTH_SHORT).show()
            }
            close()
        }
    }

    fun close() {
        // removeView() tears the window down asynchronously, and the edge glow keeps a draw pending
        // every frame; recycling the frame before the window is gone crashed that draw. Remove the
        // window synchronously first.
        view?.let { runCatching { windowManager.removeViewImmediate(it) } }
        view = null
        if (!frame.isRecycled) frame.recycle()
        onClosed()
    }

    companion object {
        private const val TAG = "Circlet-Session"
        private const val SELECTION_LINGER_MS = 300L
    }
}
