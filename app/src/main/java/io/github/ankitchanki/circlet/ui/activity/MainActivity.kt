package io.github.ankitchanki.circlet.ui.activity

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
        val key = if (intent.getBooleanExtra("from_tile", false)) KEY_TILE_DELAY else KEY_DEFAULT_DELAY
        val delayMs = prefs.getLong(key, DEFAULT_CONFIG[key] as Long)
        val vibrate = prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)

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
}
