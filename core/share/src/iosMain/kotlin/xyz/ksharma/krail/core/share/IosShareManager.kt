package xyz.ksharma.krail.core.share

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.setValue
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosShareManager : ShareManager {

    override suspend fun shareImage(bitmap: ImageBitmap, title: String, text: String?): Result<Unit> =
        runCatching {
            // Skia PNG encoding is CPU-heavy — run on Default to keep the Main thread free.
            val byteArray = withContext(Dispatchers.Default) {
                val skiaBitmap = bitmap.asSkiaBitmap()
                val skiaImage = Image.makeFromBitmap(skiaBitmap)
                // encodeToData returns null when Skia cannot encode (e.g. empty bitmap).
                // Propagate as an exception so runCatching captures it.
                checkNotNull(skiaImage.encodeToData(EncodedImageFormat.PNG, 100)) {
                    "Skia encodeToData returned null — bitmap may be empty or invalid"
                }.bytes
            }

            // Pin the ByteArray so the GC cannot move it while the C pointer is live,
            // then wrap in NSData. Both are cheap and can stay on whichever thread we're on.
            val nsData = byteArray.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = byteArray.size.toULong())
            }
            val uiImage = checkNotNull(UIImage(data = nsData)) {
                "UIImage(data:) returned null — NSData may be malformed"
            }

            // All UIKit calls (UIActivityViewController, presentViewController) must be on Main.
            withContext(Dispatchers.Main) {
                val activityItems = buildList {
                    add(uiImage)
                    if (text != null) add(text)
                }
                val activityVC = UIActivityViewController(
                    activityItems = activityItems,
                    applicationActivities = null,
                )
                // Pass title as the email/AirDrop subject — ignored by apps that don't support it.
                activityVC.setValue(title, forKey = "subject")

                val topVC = checkNotNull(topmostViewController()) {
                    "No UIViewController available to present the share sheet from"
                }
                // iPad requires a popover anchor — without this the app crashes on iPad at presentation.
                activityVC.popoverPresentationController?.sourceView = topVC.view
                activityVC.popoverPresentationController?.sourceRect = topVC.view.bounds
                topVC.presentViewController(activityVC, animated = true, completion = null)
            }
        }

    private fun topmostViewController(): UIViewController? {
        // keyWindow is deprecated in iOS 13+ (unreliable in multi-scene apps).
        // Walk connectedScenes to find the active foreground key window instead.
        val keyWindow: UIWindow? = UIApplication.sharedApplication
            .connectedScenes
            .filterIsInstance<UIWindowScene>()
            .firstOrNull { it.activationState == platform.UIKit.UISceneActivationStateForegroundActive }
            ?.windows
            ?.filterIsInstance<UIWindow>()
            ?.firstOrNull { it.isKeyWindow() }
            ?: UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .firstOrNull()
                ?.windows
                ?.filterIsInstance<UIWindow>()
                ?.firstOrNull()

        var topVC = keyWindow?.rootViewController
        while (topVC?.presentedViewController != null) {
            topVC = topVC.presentedViewController
        }
        return topVC
    }
}
