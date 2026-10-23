package io.github.ankitchanki.circlet.gesture

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Rising haptic texture for the corner swipe: light ticks that get stronger as progress grows,
 * then a crisp double-tap "confirm" on activation. Uses haptic primitives where the vibrator
 * supports them and falls back to predefined effects / short one-shots on older hardware.
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
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_CLICK,
        ) == true

    fun onProgress(progress: Float) {
        val step = steps.onProgress(progress) ?: return
        tick(steps.intensity(step))
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
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, intensity)
                    .compose()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            else -> VibrationEffect.createOneShot(6, (40 + 160 * intensity).toInt().coerceIn(1, 255))
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
        private const val STEPS = 12
        private const val CONFIRM_GAP_MS = 70
    }
}
