package io.github.ankitchanki.circlet.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import io.github.ankitchanki.circlet.R
import io.github.ankitchanki.circlet.config.AppConfig
import io.github.ankitchanki.circlet.config.GestureAction
import io.github.ankitchanki.circlet.gesture.CornerGestureController
import io.github.ankitchanki.circlet.trigger.CircleToSearchTrigger
import io.github.ankitchanki.circlet.ui.activity.SettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Foreground service that keeps the bottom-corner gesture zones on screen. */
class GestureOverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: CornerGestureController
    private var session: CircleToSearchSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        controller = CornerGestureController(this, scope, ::onInvoked)
        controller.attach()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        session?.close()
        controller.detach()
        scope.cancel()
        super.onDestroy()
    }

    private fun onInvoked(frame: Bitmap?, release: () -> Unit) {
        val prefs = getSharedPreferences(AppConfig.CONFIG_NAME, MODE_PRIVATE)
        val action = GestureAction.entries.getOrNull(
            prefs.getInt(AppConfig.KEY_GESTURE_ACTION, AppConfig.DEFAULT_CONFIG[AppConfig.KEY_GESTURE_ACTION] as Int)
        )
        if (action == GestureAction.BuiltInLasso && frame != null) {
            val newSession = CircleToSearchSession(this, scope, frame) { session = null }
            session = newSession
            newSession.show()
            release()
            return
        }

        // System Circle to Search, or no frozen frame available (Shizuku missing).
        release()
        frame?.recycle()
        scope.launch {
            val vibrate = prefs.getBoolean(AppConfig.KEY_VIBRATE, AppConfig.DEFAULT_CONFIG[AppConfig.KEY_VIBRATE] as Boolean)
            if (!CircleToSearchTrigger.trigger(this@GestureOverlayService, ENTRY_POINT, vibrate)) {
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
        private const val ENTRY_POINT = 1

        /** Starts or stops the service to match the saved setting and the overlay permission. */
        fun sync(context: Context) {
            val prefs = context.getSharedPreferences(AppConfig.CONFIG_NAME, MODE_PRIVATE)
            val enabled = prefs.getBoolean(AppConfig.KEY_CORNER_GESTURE, AppConfig.DEFAULT_CONFIG[AppConfig.KEY_CORNER_GESTURE] as Boolean)
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
