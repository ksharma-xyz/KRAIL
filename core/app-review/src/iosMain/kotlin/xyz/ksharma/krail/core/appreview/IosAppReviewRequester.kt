package xyz.ksharma.krail.core.appreview

import platform.Foundation.NSOperationQueue
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import xyz.ksharma.krail.core.log.log

/**
 * StoreKit `SKStoreReviewController.requestReview(in:)`.
 *
 * iOS shows the rating alert at most a few times per year per user and reports nothing back,
 * so a call here is a request, never a guarantee.
 */
internal class IosAppReviewRequester : AppReviewRequester {

    override fun requestReview() {
        // StoreKit presents UI, so it has to be asked on the main thread.
        NSOperationQueue.mainQueue.addOperationWithBlock {
            val windowScene = UIApplication.sharedApplication.keyWindow?.windowScene
            if (windowScene == null) {
                log("AppReview: no active window scene, skipping review request")
                return@addOperationWithBlock
            }
            SKStoreReviewController.requestReviewInScene(windowScene)
        }
    }
}
