package xyz.ksharma.krail.core.appreview

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.sandook.LifecycleCounter
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.UserLifecycleStore

/**
 * @param savedTripCount reads how many trips the user has saved right now. A live count query,
 *   not a stored counter, so ask 1 gates on what the user actually has.
 */
class RealAppReviewManager(
    private val requester: AppReviewRequester,
    private val lifecycleStore: UserLifecycleStore,
    private val preferences: SandookPreferences,
    private val flag: Flag,
    private val analytics: Analytics,
    private val savedTripCount: () -> Long,
) : AppReviewManager {

    // In memory on purpose: a delight moment and the landing on Saved Trips that consumes it
    // are a within-session pair, so process death between them means the landing being guarded
    // never happened.
    private var pendingDelightMoment: DelightMoment? = null

    override fun onDelightMoment(moment: DelightMoment) {
        pendingDelightMoment = moment
    }

    override fun onSavedTripsScreenShown() {
        // Consume on arrival whether or not it fires: a landing is a one-time event, and
        // delight moments recur often enough that spending one on a not-yet-eligible user
        // costs nothing.
        val moment = pendingDelightMoment ?: return
        pendingDelightMoment = null

        if (!isReviewRequestDue()) return

        // Recorded before the call, not after: the platform never reports an outcome, so
        // "we asked" is the only fact there is. Its count is the lifetime cap and its
        // last-seen time is the spacing between the two asks.
        lifecycleStore.increment(LifecycleCounter.REVIEW_PROMPT_REQUESTED)
        analytics.track(AnalyticsEvent.ReviewPromptRequestedEvent(source = moment.source))
        log("AppReview: requesting platform review sheet (source=${moment.source})")

        requester.requestReview()
    }

    private fun isReviewRequestDue(): Boolean {
        if (!flag.isInAppReviewEnabled()) return false
        if (!hasFinishedOnboarding()) return false
        return when (lifecycleStore.count(LifecycleCounter.REVIEW_PROMPT_REQUESTED)) {
            0L -> isFirstAskDue()
            1L -> isSecondAskDue()
            else -> false // Lifetime cap of two asks; never prompt again.
        }
    }

    /** Ask 1: a tenured, invested user. */
    private fun isFirstAskDue(): Boolean =
        isAccountOldEnough() && savedTripCount() >= MIN_SAVED_TRIPS

    /** Ask 2: a long, deliberate gap after ask 1, keeping both inside the OS quota. */
    private fun isSecondAskDue(): Boolean {
        val millisSinceAsk1 =
            lifecycleStore.millisSinceLast(LifecycleCounter.REVIEW_PROMPT_REQUESTED) ?: return false
        return millisSinceAsk1 >= MIN_DAYS_BETWEEN_ASKS * MILLIS_PER_DAY
    }

    /**
     * Apple's guideline is explicit that a review must not be requested during onboarding.
     * The intro is the only onboarding surface, so having finished it is the whole check.
     */
    private fun hasFinishedOnboarding(): Boolean =
        preferences.getBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO) == true

    private fun isAccountOldEnough(): Boolean {
        val ageDays = lifecycleStore.daysSinceFirstInstall() ?: return false
        return ageDays >= MIN_ACCOUNT_AGE_DAYS
    }

    private companion object {

        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
