package io.github.ankitchanki.circlet.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import io.github.ankitchanki.circlet.R
import io.github.ankitchanki.circlet.config.AppConfig
import io.github.ankitchanki.circlet.gesture.CornerGestureController
import io.github.ankitchanki.circlet.shizuku.AssistGestureBlocker
import io.github.ankitchanki.circlet.ui.activity.MainActivity
import io.github.ankitchanki.circlet.ui.activity.SettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the bottom-corner gesture zones on screen. A completed swipe plays
 * the invocation animation, then opens native Circle to Search exactly like tapping the app icon.
 */
class GestureOverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: CornerGestureController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        controller = CornerGestureController(this, scope, ::onInvoked)
        // Keep the system assistant corner swipe (Gemini) from taking our gesture.
        scope.launch { AssistGestureBlocker.sync(this@GestureOverlayService) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!cornersEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        controller.attach()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        controller.relayout()
    }

    override fun onDestroy() {
        controller.detach()
        scope.cancel()
        super.onDestroy()
    }

    private fun onInvoked(frame: Bitmap?, release: () -> Unit) {
        // The frozen frame only served the animation.
        release()
        frame?.recycle()
        scope.launch {
            // Let the compositor drop our layer so Google's own screenshot doesn't include it.
            delay(SYSTEM_TRIGGER_DELAY_MS)
            // Trigger from an activity, exactly like the app icon: Google treats an invocation
            // coming from a background service differently (it opened Lens instead).
            runCatching { startActivity(MainActivity.gestureIntent(this@GestureOverlayService)) }
                .onFailure { e ->
                    Log.e(TAG, "starting trigger activity failed", e)
                    Toast.makeText(this@GestureOverlayService, R.string.trigger_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.gesture_notification_channel), NotificationManager.IMPORTANCE_MIN)
        )
        val openSettings = PendingIntent.getActivity(
            this, 0, Intent(this, SettingsActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.gesture_notification_title))
            .setContentIntent(openSettings)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val TAG = "Circlet-Service"
        private const val CHANNEL_ID = "corner_gesture"
        private const val NOTIFICATION_ID = 1
        private const val SYSTEM_TRIGGER_DELAY_MS = 120L

        private fun cornersEnabled(context: Context): Boolean =
            context.getSharedPreferences(AppConfig.CONFIG_NAME, MODE_PRIVATE)
                .getBoolean(AppConfig.KEY_CORNER_GESTURE, AppConfig.DEFAULT_CONFIG[AppConfig.KEY_CORNER_GESTURE] as Boolean)

        /** Starts or stops the service to match the saved setting and the overlay permission. */
        fun sync(context: Context) {
            val enabled = cornersEnabled(context)
            val intent = Intent(context, GestureOverlayService::class.java)
            if (enabled && Settings.canDrawOverlays(context)) {
                runCatching { context.startForegroundService(intent) }
                    .onFailure { e -> Log.e(TAG, "starting gesture service failed", e) }
            } else {
                context.stopService(intent)
            }
        }
    }
}
