package xyz.ksharma.krail.platform.ops

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class IosContentSharing : ContentSharing {

    override fun sharePlainText(text: String) {
        handleShareClick(text)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun handleShareClick(text: String) {
    val activityItems = listOf(text)
    val activityViewController = UIActivityViewController(activityItems, null)

    val application = UIApplication.sharedApplication

    // Ensure all UI operations are performed on the main thread
    platform.Foundation.NSOperationQueue.mainQueue.addOperationWithBlock {
        application.keyWindow?.rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null,
        )
    }
}
