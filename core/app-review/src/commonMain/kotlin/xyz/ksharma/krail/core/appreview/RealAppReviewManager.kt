package xyz.ksharma.krail.core.appreview

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.sandook.LifecycleCounter
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.UserLifecycleStore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * @param nowMillis injectable clock; production uses the system clock, tests drive it.
 */
class RealAppReviewManager(
    private val requester: AppReviewRequester,
    private val lifecycleStore: UserLifecycleStore,
    private val preferences: SandookPreferences,
    private val flag: Flag,
    private val analytics: Analytics,
    private val nowMillis: () -> Long = { systemNowMillis() },
) : AppReviewManager {

    private var zeroResultSearchAtMillis: Long? = null

    override fun onZeroResultSearch() {
        zeroResultSearchAtMillis = nowMillis()
    }

    override fun onSavedTripOpened() {
        // Counted before the gates so the engagement is recorded even while the feature is
        // switched off; flipping the flag on later then sees the user's real history.
        val openCount = lifecycleStore.increment(LifecycleCounter.SAVED_TRIP_OPEN)

        if (!isReviewRequestDue(openCount)) return

        // Recorded before the call, not after: the platform never reports an outcome, so
        // "we asked" is the only fact there is, and it is also what drives the cooldown.
        lifecycleStore.increment(LifecycleCounter.REVIEW_PROMPT_REQUESTED)
        analytics.track(AnalyticsEvent.ReviewPromptRequestedEvent(source = SOURCE_SAVED_TRIP_OPEN))
        log("AppReview: requesting platform review sheet (savedTripOpens=$openCount)")

        requester.requestReview()
    }

    private fun isReviewRequestDue(openCount: Long): Boolean =
        flag.isInAppReviewEnabled() &&
            hasFinishedOnboarding() &&
            openCount >= flag.minSavedTripOpens() &&
            isAccountOldEnough() &&
            isPastCooldown() &&
            !isAfterZeroResultSearch()

    /**
     * Apple's guideline is explicit that a review must not be requested during onboarding.
     * The intro is the only onboarding surface, so having finished it is the whole check.
     */
    private fun hasFinishedOnboarding(): Boolean =
        preferences.getBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO) == true

    private fun isAccountOldEnough(): Boolean {
        val ageDays = lifecycleStore.daysSinceFirstInstall() ?: return false
        return ageDays >= flag.minAccountAgeDays()
    }

    /**
     * Spaces attempts out rather than firing on every eligible open. The OS throttles too,
     * but it throttles silently, so without this we would burn the platform quota on
     * back-to-back requests and never know.
     */
    private fun isPastCooldown(): Boolean {
        val lastRequestedAt =
            lifecycleStore.lastAtMillis(LifecycleCounter.REVIEW_PROMPT_REQUESTED) ?: return true
        return nowMillis() - lastRequestedAt >= flag.reviewCooldownDays() * MILLIS_PER_DAY
    }

    /**
     * [zeroResultSearchAtMillis] is in memory on purpose: this guards against a request
     * landing seconds after a failed search, which is a within-session concern. Process
     * death clearing it is fine, because a search and a saved-trip open separated by a
     * process death were never the adjacent pair being guarded against.
     */
    private fun isAfterZeroResultSearch(): Boolean {
        val searchedAt = zeroResultSearchAtMillis ?: return false
        return nowMillis() - searchedAt < ZERO_RESULT_SUPPRESSION_MILLIS
    }

    companion object {

        private const val SOURCE_SAVED_TRIP_OPEN = "saved_trip_open"

        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        // How long a zero-result search keeps blocking a request.
        private const val ZERO_RESULT_SUPPRESSION_MILLIS = 5L * 60L * 1000L

        @OptIn(ExperimentalTime::class)
        private fun systemNowMillis(): Long = Clock.System.now().toEpochMilliseconds()
    }
}
