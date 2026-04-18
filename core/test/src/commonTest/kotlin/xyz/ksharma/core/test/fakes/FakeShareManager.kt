package xyz.ksharma.core.test.fakes

import androidx.compose.ui.graphics.ImageBitmap
import xyz.ksharma.krail.core.share.ShareManager

class FakeShareManager : ShareManager {

    var shareImageCalled = false
        private set
    var lastSharedBitmap: ImageBitmap? = null
        private set
    var lastSharedText: String? = null
        private set
    var shouldFail = false

    override suspend fun shareImage(
        bitmap: ImageBitmap,
        title: String,
        text: String?,
    ): Result<Unit> {
        shareImageCalled = true
        lastSharedBitmap = bitmap
        lastSharedText = text
        return if (shouldFail) Result.failure(RuntimeException("share failed")) else Result.success(Unit)
    }

    fun reset() {
        shareImageCalled = false
        lastSharedBitmap = null
        lastSharedText = null
        shouldFail = false
    }
}
