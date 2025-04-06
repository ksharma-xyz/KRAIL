package xyz.ksharma.krail.trip.planner.ui.settings

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class IosSharer : Sharer {
    override fun shareText() {
        handleShareClick(Sharer.REFER_FRIEND_TEXT)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun handleShareClick(text: String) {
    val activityItems = listOf(text)
    val activityViewController = UIActivityViewController(activityItems, null)

    val application = UIApplication.sharedApplication
    application.keyWindow?.rootViewController?.presentViewController(
        activityViewController,
        animated = true,
        completion = null
    )
}