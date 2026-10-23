package io.github.ankitchanki.circlet.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.ankitchanki.circlet.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    NotInstalled,
    NotRunning,
    PermissionRequired,
    Granted,
}

/**
 * Single entry point to Shizuku: tracks binder/permission state and owns the
 * [ShellService] user service connection.
 */
object ShizukuManager {
    private const val TAG = "Circlet"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val PERMISSION_REQUEST_CODE = 1001
    private const val BIND_TIMEOUT_MS = 5000L

    private lateinit var appContext: Context

    private val _status = MutableStateFlow(ShizukuStatus.NotRunning)
    val status: StateFlow<ShizukuStatus> = _status

    private val bindMutex = Mutex()
    @Volatile private var shellService: IShellService? = null
    private var pendingBind: CompletableDeferred<IShellService?>? = null

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(appContext.packageName, ShellService::class.java.name))
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = binder?.takeIf { it.pingBinder() }?.let { IShellService.Stub.asInterface(it) }
            shellService = service
            pendingBind?.complete(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener {
            shellService = null
            refresh()
        }
        Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
        refresh()
    }

    fun refresh() {
        _status.value = when {
            Shizuku.pingBinder() -> runCatching {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    ShizukuStatus.Granted
                } else {
                    ShizukuStatus.PermissionRequired
                }
            }.getOrDefault(ShizukuStatus.NotRunning)
            isShizukuInstalled() -> ShizukuStatus.NotRunning
            else -> ShizukuStatus.NotInstalled
        }
    }

    val isGranted: Boolean
        get() = _status.value == ShizukuStatus.Granted

    fun requestPermission() {
        if (Shizuku.pingBinder()) {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        }
    }

    fun openShizukuApp(context: Context) {
        context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)?.let {
            context.startActivity(it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun isShizukuInstalled(): Boolean = runCatching {
        appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
    }.isSuccess

    private suspend fun shell(): IShellService? {
        shellService?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        refresh()
        if (!isGranted) return null
        return bindMutex.withLock {
            shellService?.takeIf { it.asBinder().pingBinder() }?.let { return@withLock it }
            val deferred = CompletableDeferred<IShellService?>()
            pendingBind = deferred
            withContext(Dispatchers.Main) {
                runCatching { Shizuku.bindUserService(userServiceArgs, connection) }
                    .onFailure { e ->
                        Log.e(TAG, "bindUserService failed", e)
                        deferred.complete(null)
                    }
            }
            withTimeoutOrNull(BIND_TIMEOUT_MS) { deferred.await() }.also { pendingBind = null }
        }
    }

    suspend fun startContextualSearch(entryPoint: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching { shell()?.startContextualSearch(entryPoint) ?: false }
            .onFailure { e -> Log.e(TAG, "startContextualSearch via Shizuku failed", e) }
            .getOrDefault(false)
    }

    suspend fun captureScreen(png: Boolean): ParcelFileDescriptor? = withContext(Dispatchers.IO) {
        runCatching { shell()?.captureScreen(png) }
            .onFailure { e -> Log.e(TAG, "captureScreen via Shizuku failed", e) }
            .getOrNull()
    }

    /** Returns true when the command exited with status 0. */
    suspend fun exec(command: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val result = shell()?.exec(command) ?: return@runCatching false
            val exitCode = result.substringBefore('\n').toIntOrNull()
            if (exitCode != 0) Log.w(TAG, "`$command` failed: $result")
            exitCode == 0
        }.onFailure { e -> Log.e(TAG, "exec via Shizuku failed", e) }.getOrDefault(false)
    }

    /** Lets the corner gesture overlay run without the user visiting system settings. */
    suspend fun grantOverlayPermission(): Boolean =
        exec("appops set ${appContext.packageName} SYSTEM_ALERT_WINDOW allow")

    suspend fun setGoogleAsAssistant(): Boolean =
        exec("cmd role add-role-holder android.app.role.ASSISTANT com.google.android.googlequicksearchbox")
}
