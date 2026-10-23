package io.github.ankitchanki.circlet.shizuku

import android.content.Context
import io.github.ankitchanki.circlet.config.AppConfig

/**
 * The system's own "swipe from a bottom corner to invoke the assistant" gesture (AOSP/Pixel
 * launcher) grabs the same diagonal swipe as Circlet and opens the default assistant, e.g. Gemini.
 * Shell can flip its secure setting, so we turn it off while the corner gesture is on and put the
 * user's original value back afterwards.
 */
object AssistGestureBlocker {
    private const val SETTING = "assist_touch_gesture_enabled"
    /** Marker for "the setting did not exist", restored by deleting it. */
    private const val UNSET = "null"

    /** Disables the system corner assistant swipe. Returns false if Shizuku couldn't do it. */
    suspend fun block(context: Context): Boolean {
        if (!ShizukuManager.isGranted) return false
        val prefs = prefs(context)
        if (!prefs.contains(AppConfig.KEY_SAVED_ASSIST_GESTURE)) {
            val current = ShizukuManager.execOutput("settings get secure $SETTING") ?: return false
            prefs.edit().putString(AppConfig.KEY_SAVED_ASSIST_GESTURE, current.ifEmpty { UNSET }).commit()
        }
        return ShizukuManager.exec("settings put secure $SETTING 0")
    }

    /** Puts back the value saved by [block], if any. */
    suspend fun restore(context: Context): Boolean {
        val prefs = prefs(context)
        val saved = prefs.getString(AppConfig.KEY_SAVED_ASSIST_GESTURE, null) ?: return true
        if (!ShizukuManager.isGranted) return false
        val ok = if (saved == UNSET) {
            ShizukuManager.exec("settings delete secure $SETTING")
        } else {
            ShizukuManager.exec("settings put secure $SETTING $saved")
        }
        if (ok) prefs.edit().remove(AppConfig.KEY_SAVED_ASSIST_GESTURE).commit()
        return ok
    }

    /** Applies the user's choice: blocked only while both the corner gesture and the option are on. */
    suspend fun sync(context: Context): Boolean {
        val prefs = prefs(context)
        val gesture = prefs.getBoolean(AppConfig.KEY_CORNER_GESTURE, AppConfig.DEFAULT_CONFIG[AppConfig.KEY_CORNER_GESTURE] as Boolean)
        val wantBlock = prefs.getBoolean(AppConfig.KEY_BLOCK_ASSIST_GESTURE, AppConfig.DEFAULT_CONFIG[AppConfig.KEY_BLOCK_ASSIST_GESTURE] as Boolean)
        return if (gesture && wantBlock) block(context) else restore(context)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE)
}
