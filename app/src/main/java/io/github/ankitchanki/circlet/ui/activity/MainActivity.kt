package io.github.ankitchanki.circlet.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import io.github.ankitchanki.circlet.R
import io.github.ankitchanki.circlet.config.AppConfig.CONFIG_NAME
import io.github.ankitchanki.circlet.config.AppConfig.DEFAULT_CONFIG
import io.github.ankitchanki.circlet.config.AppConfig.KEY_ASYNC_TRIGGER
import io.github.ankitchanki.circlet.config.AppConfig.KEY_DEFAULT_DELAY
import io.github.ankitchanki.circlet.config.AppConfig.KEY_TILE_DELAY
import io.github.ankitchanki.circlet.config.AppConfig.KEY_TRIGGER_SERVICE
import io.github.ankitchanki.circlet.config.AppConfig.KEY_VIBRATE
import io.github.ankitchanki.circlet.config.TriggerService
import io.github.ankitchanki.circlet.trigger.CircleToSearchTrigger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    suspend fun delayAndTrigger(delayMs: Long, vibrate: Boolean) {
        if (delayMs > 0) {
            delay(delayMs)
        }
        if (!CircleToSearchTrigger.trigger(this, 1, vibrate)) {
            Toast.makeText(this, getString(R.string.trigger_failed), Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val prefs = getSharedPreferences(CONFIG_NAME, MODE_PRIVATE)
        val fromGesture = intent.getBooleanExtra(EXTRA_FROM_GESTURE, false)
        val key = if (intent.getBooleanExtra("from_tile", false)) KEY_TILE_DELAY else KEY_DEFAULT_DELAY
        // The corner gesture already waited for its overlay to go and played its own haptics.
        val delayMs = if (fromGesture) 0L else prefs.getLong(key, DEFAULT_CONFIG[key] as Long)
        val vibrate = !fromGesture && prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)

        val triggerService = TriggerService.entries.getOrNull(
            prefs.getInt(KEY_TRIGGER_SERVICE, DEFAULT_CONFIG[KEY_TRIGGER_SERVICE] as Int)
        )
        // Shizuku delivers the user service connection on the main thread, so never block it.
        val async = triggerService?.needsShizukuRoot == true ||
            prefs.getBoolean(KEY_ASYNC_TRIGGER, DEFAULT_CONFIG[KEY_ASYNC_TRIGGER] as Boolean)
        if (async) {
            lifecycleScope.launch {
                delayAndTrigger(delayMs, vibrate)
            }
        } else {
            runBlocking {
                delayAndTrigger(delayMs, vibrate)
            }
        }
    }

    companion object {
        private const val EXTRA_FROM_GESTURE = "from_gesture"

        /** Started by the corner gesture so its trigger runs from an activity, like the app icon. */
        fun gestureIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_FROM_GESTURE, true)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
    }
}
