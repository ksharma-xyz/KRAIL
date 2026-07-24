package xyz.ksharma.krail.gtfs_static

import android.content.Context
import java.io.File

internal class AndroidFileStorage(private val context: Context) : FileStorage {
    override suspend fun saveFile(fileName: String, data: ByteArray) {
        val file = File(context.filesDir, fileName)
        file.writeBytes(data)
    }
}
