package io.github.ankitchanki.circlet.shizuku

import android.content.Context
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lite flavor: no Shizuku. Same API as the full flavor's ShizukuManager, so shared code compiles
 * unchanged; every privileged operation reports "not available" and callers fall back to their
 * non-Shizuku path (no frozen frame for the animation, manual permissions).
 */
object ShizukuManager {
    val status: StateFlow<ShizukuStatus> = MutableStateFlow(ShizukuStatus.NotInstalled)
    val isRoot: StateFlow<Boolean> = MutableStateFlow(false)

    val isGranted: Boolean
        get() = false

    fun init(context: Context) = Unit

    fun refresh() = Unit

    fun requestPermission() = Unit

    fun openShizukuApp(context: Context) = Unit

    suspend fun startContextualSearch(entryPoint: Int): Boolean = false

    suspend fun captureScreen(png: Boolean): ParcelFileDescriptor? = null

    suspend fun exec(command: String): Boolean = false

    suspend fun execOutput(command: String): String? = null

    suspend fun grantOverlayPermission(): Boolean = false

    suspend fun setGoogleAsAssistant(): Boolean = false
}
