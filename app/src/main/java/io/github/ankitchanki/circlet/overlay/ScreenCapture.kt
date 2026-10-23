package io.github.ankitchanki.circlet.overlay

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.ankitchanki.circlet.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Grabs the current screen as a bitmap through `screencap` running in the Shizuku shell service. */
object ScreenCapture {
    private const val TAG = "Circlet-Capture"
    private const val FORMAT_RGBA_8888 = 1
    private const val FORMAT_RGBX_8888 = 2

    suspend fun capture(): Bitmap? = withContext(Dispatchers.IO) {
        // Raw output skips PNG encoding in screencap, which is several times faster.
        captureRaw() ?: capturePng()
    }

    private suspend fun captureRaw(): Bitmap? {
        val pfd = ShizukuManager.captureScreen(png = false) ?: return null
        return runCatching {
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { decodeRaw(it) }
        }.onFailure { e -> Log.w(TAG, "raw screencap decode failed", e) }.getOrNull()
    }

    private suspend fun capturePng(): Bitmap? {
        val pfd = ShizukuManager.captureScreen(png = true) ?: return null
        return runCatching {
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { BitmapFactory.decodeStream(it) }
        }.onFailure { e -> Log.w(TAG, "png screencap decode failed", e) }.getOrNull()
    }

    /**
     * screencap's raw format: little-endian width, height, pixel format, then on newer Android
     * a 4-byte dataspace, then width * height RGBA pixels. The optional field is detected
     * from the number of bytes that follow.
     */
    private fun decodeRaw(input: InputStream): Bitmap? {
        val data = DataInputStream(input.buffered(1 shl 16))
        val header = ByteArray(12).also { data.readFully(it) }
        val hb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val width = hb.int
        val height = hb.int
        val format = hb.int
        if (width <= 0 || height <= 0 || (format != FORMAT_RGBA_8888 && format != FORMAT_RGBX_8888)) return null

        val pixelBytes = width * height * 4
        val buffer = ByteArray(pixelBytes + 4)
        val read = readUpTo(data, buffer)
        val offset = when (read) {
            pixelBytes + 4 -> 4
            pixelBytes -> 0
            else -> return null
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            copyPixelsFromBuffer(ByteBuffer.wrap(buffer, offset, pixelBytes))
            if (format == FORMAT_RGBX_8888) setHasAlpha(false)
        }
    }

    private fun readUpTo(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val n = input.read(buffer, total, buffer.size - total)
            if (n < 0) break
            total += n
        }
        // Anything left means the size assumption was wrong.
        if (total == buffer.size && input.read() != -1) throw EOFException("unexpected trailing screencap data")
        return total
    }
}
