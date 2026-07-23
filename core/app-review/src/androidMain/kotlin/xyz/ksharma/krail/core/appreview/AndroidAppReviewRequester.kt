package xyz.ksharma.krail.core.appreview

import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.platform.ops.CurrentActivityHolder

/**
 * Play In-App Review. Play decides whether the card appears and never tells us the outcome,
 * so success here only means "Play accepted the request".
 *
 * The flow needs a real Activity, which is why it goes through [CurrentActivityHolder]
 * rather than the application Context.
 */
class AndroidAppReviewRequester(
    private val context: Context,
    private val activityHolder: CurrentActivityHolder,
    private val debugSignal: AppReviewDebugSignal,
) : AppReviewRequester {

    override fun requestReview(source: String) {
        // Proof for on-device testing: a sideloaded debug build can never show the real card,
        // so a debug composable observes this. Inert in release (nothing subscribes).
        debugSignal.signalRequested(source)

        val activity = activityHolder.current
        if (activity == null) {
            // Backgrounded between the trigger and here. Nothing to attach a card to.
            log("AppReview: no resumed Activity, skipping review flow")
            return
        }

        val manager = ReviewManagerFactory.create(context)
        manager.requestReviewFlow()
            .addOnCompleteListener { request ->
                if (!request.isSuccessful) {
                    // Commonly just "no quota left" or a Play Store that cannot serve the
                    // card. Expected, not exceptional.
                    logError("AppReview: requestReviewFlow failed", request.exception)
                    return@addOnCompleteListener
                }
                manager.launchReviewFlow(activity, request.result)
                    .addOnCompleteListener {
                        // Deliberately empty. Play returns the same result whether the card
                        // was shown, dismissed, or silently skipped, so there is nothing
                        // here worth branching on.
                        log("AppReview: review flow finished")
                    }
            }
    }
}
