package io.github.ankitchanki.circlet.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import io.github.ankitchanki.circlet.R
import io.github.ankitchanki.circlet.overlay.GestureOverlayService

/**
 * Entry point when Circlet is the default assistant: the system's corner swipe, long-press home
 * or power-button assistant gesture land here. Uses Theme.NoDisplay, so it never draws a window
 * (the screenshot stays clean); it only hands off to the overlay service and finishes.
 */
class AssistActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Settings.canDrawOverlays(this)) {
            runCatching { startForegroundService(GestureOverlayService.invokeIntent(this)) }
        } else {
            Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }
}
