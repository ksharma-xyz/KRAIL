package xyz.ksharma.krail.core.share

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform-specific manager that handles sharing an [ImageBitmap] to other apps.
 */
interface ShareManager {
    /**
     * Share the given [bitmap] as an image to other apps.
     *
     * Returns [Result.success] when the OS share sheet has been shown.
     * Returns [Result.failure] for any encoding, file-system, or OS-level error.
     * The caller can inspect the failure now (e.g. log it) and hook up UI later.
     *
     * @param bitmap The image to share.
     * @param title Optional chooser title (Android) or subject.
     */
    suspend fun shareImage(bitmap: ImageBitmap, title: String = "Krail Journey"): Result<Unit>
}
