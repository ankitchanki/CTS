package io.github.ankitchanki.circlet.trigger

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.FileProvider
import io.github.ankitchanki.circlet.gesture.PixelRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Sends the selected region of the frozen frame to Google Lens (via the Google app). */
object LensLauncher {
    private const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox"
    private const val SHARE_DIR = "selection"

    suspend fun search(context: Context, frame: Bitmap, region: PixelRect): Boolean {
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val crop = Bitmap.createBitmap(frame, region.left, region.top, region.width, region.height)
                val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
                File(dir, "selection.png").also { out ->
                    out.outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    if (crop !== frame) crop.recycle()
                }
            }.onFailure { e -> Log.e(LOG_TAG, "saving selection failed", e) }.getOrNull()
        } ?: return false

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        send.clipData = ClipData.newRawUri(null, uri)

        return try {
            context.startActivity(Intent(send).setPackage(GOOGLE_PACKAGE))
            true
        } catch (_: ActivityNotFoundException) {
            runCatching {
                context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
        }
    }
}
