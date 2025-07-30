package xyz.ksharma.krail.platform.ops

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError

class IosPlatformOps : PlatformOps {
    override fun sharePlainText(text: String, title: String) {
        handleShareClick(text = text, title = title)
    }

    override fun openUrl(url: String) {
        if (url.isBlank()) {
            logError("Cannot open URL: URL is blank")
            return
        }

        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            // openUrl() is deprecated so using other overload
            // https://developer.apple.com/documentation/uikit/uiapplication/openurl(_:)
            UIApplication.sharedApplication.openURL(
                nsUrl,
                options = mapOf<Any?, Any?>(),
                completionHandler = { result: Boolean ->
                    log("Attempted to open URL: $url, result: $result")
                })
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun handleShareClick(text: String, title: String) {
        // TODO - test title or list etc.
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
}