package io.github.ankitchanki.circlet.trigger

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import io.github.ankitchanki.circlet.BuildConfig
import io.github.ankitchanki.circlet.config.AppConfig
import io.github.ankitchanki.circlet.config.TriggerService
import io.github.ankitchanki.circlet.shizuku.ShizukuManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

const val LOG_TAG = BuildConfig.APP_NAME

object CircleToSearchTrigger {

    suspend fun trigger(context: Context, entryPoint: Int, vibrate: Boolean): Boolean {
        val prefs = context.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE)
        val service = TriggerService.entries.getOrNull(
            prefs.getInt(AppConfig.KEY_TRIGGER_SERVICE, AppConfig.DEFAULT_CONFIG[AppConfig.KEY_TRIGGER_SERVICE] as Int)
        )?.takeIf { it.isSupported } ?: TriggerService.VIS

        val result = when (service) {
            // Fall back to VIS when CSService can't run (Shizuku stopped or permission revoked).
            TriggerService.CSService -> ShizukuManager.startContextualSearch(entryPoint) || triggerViaVis(entryPoint)
            TriggerService.VIS -> triggerViaVis(entryPoint)
        }
        if (result && vibrate) {
            vibrateClick(context)
        }
        return result
    }

    /**
     * Asks VoiceInteractionManagerService to show the assistant session with the omni
     * entry point. Works without Shizuku as long as Google is the default assistant.
     */
    @SuppressLint("PrivateApi")
    private fun triggerViaVis(entryPoint: Int): Boolean {
        return runCatching {
            val bundle = Bundle()
            bundle.putLong("invocation_time_ms", SystemClock.elapsedRealtime())
            bundle.putInt("omni.entry_point", entryPoint)
            val iVimsClass = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
            val vis = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "voiceinteraction")
            val vims = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, vis)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HiddenApiBypass.invoke(iVimsClass, vims, "showSessionFromSession", null, bundle, 7, "hyperOS_home") as Boolean
            } else {
                HiddenApiBypass.invoke(iVimsClass, vims, "showSessionFromSession", null, bundle, 7) as Boolean
            }
        }.onFailure { e ->
            Log.e(LOG_TAG, "triggerCircleToSearch invoke omni failed", e)
        }.getOrDefault(false)
    }

    fun vibrateClick(context: Context) {
        runCatching {
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).run {
                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setFlags(128)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), attr)
                } else {
                    vibrate(longArrayOf(0, 1, 75, 76), -1, attr)
                }
            }
        }.onFailure { e ->
            Log.e(LOG_TAG, "triggerCircleToSearch vibrate failed", e)
        }
    }
}
