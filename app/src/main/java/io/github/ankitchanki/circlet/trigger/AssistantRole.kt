package io.github.ankitchanki.circlet.trigger

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import io.github.ankitchanki.circlet.shizuku.ShizukuManager

/**
 * Circlet can be the "Default digital assistant app" (it has an ACTION_ASSIST activity). The
 * system's corner swipe / long-press home then opens Circlet instead of Gemini, and Circlet
 * bridges to its Circle to Search mode.
 */
object AssistantRole {
    private const val ROLE_ASSISTANT = "android.app.role.ASSISTANT"

    fun isHeld(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return runCatching {
                context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)
            }.getOrDefault(false)
        }
        // Pre-Q: the assistant component is stored in a secure setting.
        return Settings.Secure.getString(context.contentResolver, "assistant")
            ?.startsWith(context.packageName + "/") == true
    }

    /** Makes Circlet the assistant through Shizuku. Returns false if Shizuku isn't available. */
    suspend fun makeDefault(context: Context): Boolean =
        ShizukuManager.isGranted &&
            ShizukuManager.exec("cmd role add-role-holder $ROLE_ASSISTANT ${context.packageName}")

    /** System page where the user can pick the assistant app by hand. */
    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
