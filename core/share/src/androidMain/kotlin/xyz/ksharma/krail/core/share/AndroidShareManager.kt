package xyz.ksharma.krail.core.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class AndroidShareManager(private val context: Context) : ShareManager {

    override suspend fun shareImage(bitmap: ImageBitmap, title: String, text: String?): Result<Unit> =
        runCatching {
            val uri = withContext(Dispatchers.IO) {
                saveBitmapToCache(bitmap.asAndroidBitmap())
            }
            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    if (text != null) putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val cacheDir = File(context.cacheDir, "share").also { it.mkdirs() }
        val file = File(cacheDir, "journey_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            // compress() returns false on failure — treat that as an error.
            val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            check(success) { "Bitmap.compress() failed — bitmap may be recycled or hardware-backed" }
        }
        // Throws IllegalArgumentException if the file path is not covered by share_file_paths.xml.
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.share.fileprovider",
            file,
        )
    }
}
