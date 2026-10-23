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
import io.github.ankitchanki.circlet.shizuku.AssistGestureBlocker
import io.github.ankitchanki.circlet.trigger.CircleToSearchTrigger
import io.github.ankitchanki.circlet.ui.activity.SettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the bottom-corner gesture zones on screen, and plays the
 * invocation when Circlet is opened as the default assistant ([ACTION_INVOKE]).
 */
class GestureOverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: CornerGestureController
    private var session: CircleToSearchSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        controller = CornerGestureController(this, scope, ::onInvoked)
        // Keep the system assistant corner swipe (Gemini) from taking our gesture.
        scope.launch { AssistGestureBlocker.sync(this@GestureOverlayService) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (cornersEnabled(this)) controller.attach()
        if (intent?.action == ACTION_INVOKE && session == null) {
            controller.invoke()
        } else if (!cornersEnabled(this) && session == null) {
            stopSelf()
        }
        return if (cornersEnabled(this)) START_STICKY else START_NOT_STICKY
    }

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
        // While Circlet is the default assistant, VIS reaches Circlet itself, not Google.
        if (action == GestureAction.BuiltInLasso || !CircleToSearchTrigger.canReachGoogle(this)) {
            release()
            if (frame == null) {
                // No screenshot (Shizuku missing or capture failed). Don't fall back to the
                // assistant trigger here: on many devices that opens Gemini instead.
                Toast.makeText(this, R.string.lasso_needs_shizuku, Toast.LENGTH_LONG).show()
                return
            }
            val newSession = CircleToSearchSession(this, scope, frame) {
                session = null
                stopIfIdle()
            }
            session = newSession
            newSession.show()
            return
        }

        // System Circle to Search via the selected trigger service.
        release()
        frame?.recycle()
        scope.launch {
            val vibrate = prefs.getBoolean(AppConfig.KEY_VIBRATE, AppConfig.DEFAULT_CONFIG[AppConfig.KEY_VIBRATE] as Boolean)
            if (!CircleToSearchTrigger.trigger(this@GestureOverlayService, ENTRY_POINT, vibrate)) {
                Toast.makeText(this@GestureOverlayService, R.string.trigger_failed, Toast.LENGTH_SHORT).show()
            }
            stopIfIdle()
        }
    }

    /** Started only for an assistant invocation: go away once it is over. */
    private fun stopIfIdle() {
        if (!cornersEnabled(this) && session == null) stopSelf()
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
        private const val ACTION_INVOKE = "io.github.ankitchanki.circlet.action.INVOKE"

        fun invokeIntent(context: Context): Intent =
            Intent(context, GestureOverlayService::class.java).setAction(ACTION_INVOKE)

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
