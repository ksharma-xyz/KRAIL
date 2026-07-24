package xyz.ksharma.krail.gtfs_static

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToFile
import xyz.ksharma.krail.core.log.log

internal class IosFileStorage : FileStorage {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun saveFile(fileName: String, data: ByteArray) {
        log("Saving file: $fileName")
        val fileManager = NSFileManager.defaultManager
        val directory = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )?.path

        val filePath = "$directory/$fileName"
        data.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
                .writeToFile(filePath, true)
        }
        log("File saved: $filePath")
    }
}
