package xyz.ksharma.krail.platform.ops

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController
import xyz.ksharma.krail.core.log.log

class IosContentSharing : ContentSharing {
    override fun sharePlainText(text: String) {
        handleShareClick(text)
    }
}

@OptIn(ExperimentalForeignApi::class,)
fun handleShareClick(text: String) {
    val activityItems = listOf(text)
    val activityViewController = UIActivityViewController(activityItems, null)

    // Configure popover presentation for iPad
    activityViewController.popoverPresentationController?.apply {
        val application = UIApplication.sharedApplication
        sourceView = application.keyWindow

        // Use CGRectMake to create a rectangle directly
        val window = application.keyWindow
        if (window != null) {
            log("keywindow is not null")

            // CValue<CGRect> needs to be unwrapped using special getters
            val frame: CValue<CGRect> = window.frame

            sourceRect = cValue {
                // Initialize using the getter methods for CValue<CGRect>
                this.origin.x = frame.useContents { this.origin.x }
                this.origin.y = frame.useContents { this.origin.y }
                this.size.width = frame.useContents { this.size.width }
                this.size.height = frame.useContents { this.size.height }
            }
        } else {
            log("keywindow is null")
            // Fallback to zero rectangle when window is null
            sourceRect = cValue {
                this.origin.x = 0.0
                this.origin.y = 0.0
                this.size.width = 0.0
                this.size.height = 0.0
            }
        }

        permittedArrowDirections = 0u
    }

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
