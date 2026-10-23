package io.github.ankitchanki.circlet.gesture

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Fine-grained haptic texture for the corner swipe: a dial-like tick every few dp, stronger as
 * progress grows and softer when pulling back, then a crisp double-tap "confirm" on activation.
 * Uses haptic primitives where supported, otherwise amplitude-scaled one-shots, otherwise the
 * predefined tick.
 */
class GestureHaptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    private val steps = HapticSteps(STEPS)

    private val primitivesSupported: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator?.areAllPrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_CLICK,
        ) == true

    /** Many motors without primitives (common on Xiaomi) can still scale a short pulse. */
    private val amplitudeControl: Boolean = vibrator?.hasAmplitudeControl() == true

    fun onProgress(progress: Float) {
        val crossed = steps.onProgress(progress) ?: return
        tick(steps.intensity(crossed))
    }

    fun reset() = steps.reset()

    fun activate() {
        steps.reset()
        val effect = when {
            primitivesSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f, CONFIRM_GAP_MS)
                    .compose()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            else -> VibrationEffect.createWaveform(longArrayOf(0, 18, 60, 12), intArrayOf(0, 255, 0, 160), -1)
        }
        vibrate(effect)
    }

    private fun tick(intensity: Float) {
        val effect = when {
            primitivesSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                    .compose()
            amplitudeControl ->
                VibrationEffect.createOneShot(TICK_MS, (MIN_AMPLITUDE + (255 - MIN_AMPLITUDE) * intensity).toInt().coerceIn(1, 255))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            else -> VibrationEffect.createOneShot(TICK_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrate(effect)
    }

    private fun vibrate(effect: VibrationEffect) {
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            } else {
                v.vibrate(effect)
            }
        }
    }

    companion object {
        /** One tick about every 6dp of the 140dp activation travel. */
        private const val STEPS = 24
        private const val CONFIRM_GAP_MS = 70
        private const val TICK_MS = 8L
        private const val MIN_AMPLITUDE = 50
    }
}
