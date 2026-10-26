package io.github.ankitchanki.circlet.shizuku

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Shizuku user service. Shizuku starts it with app_process as shell (or root),
 * so everything here runs with that identity instead of the app's.
 */
class ShellService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    @SuppressLint("PrivateApi")
    override fun startContextualSearch(entryPoint: Int): Boolean {
        return runCatching {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "contextual_search") as IBinder? ?: return false
            val icsm = Class.forName("android.app.contextualsearch.IContextualSearchManager\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, binder)
            // oneway call: a permission failure inside system_server is not reported back here.
            Class.forName("android.app.contextualsearch.IContextualSearchManager")
                .getMethod("startContextualSearch", Int::class.javaPrimitiveType)
                .invoke(icsm, entryPoint)
            true
        }.onFailure { e ->
            Log.e(TAG, "startContextualSearch failed", e)
        }.getOrDefault(false)
    }

    override fun captureScreen(png: Boolean): ParcelFileDescriptor {
        val (read, write) = ParcelFileDescriptor.createPipe()
        val command = if (png) arrayOf("screencap", "-p") else arrayOf("screencap")
        thread(name = "screencap") {
            ParcelFileDescriptor.AutoCloseOutputStream(write).use { out ->
                runCatching {
                    val process = Runtime.getRuntime().exec(command)
                    process.inputStream.use { it.copyTo(out, BUFFER_SIZE) }
                    process.waitFor()
                }.onFailure { e ->
                    Log.e(TAG, "screencap failed", e)
                }
            }
        }
        return read
    }

    override fun exec(command: String): String {
        return runCatching {
            val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            "${process.waitFor()}\n$output"
        }.getOrElse { e -> "-1\n${e.stackTraceToString()}" }
    }

    companion object {
        private const val TAG = "Circlet-Shell"
        private const val BUFFER_SIZE = 1 shl 16
    }
}
