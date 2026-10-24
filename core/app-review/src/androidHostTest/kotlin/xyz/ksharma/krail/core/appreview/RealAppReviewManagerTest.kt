package xyz.ksharma.krail.core.appreview

import org.junit.Before
import org.junit.Test
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.sandook.LifecycleCounter
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.UserLifecycleStore
import kotlin.test.assertEquals

class RealAppReviewManagerTest {

    private lateinit var requester: FakeAppReviewRequester
    private lateinit var lifecycleStore: FakeUserLifecycleStore
    private lateinit var preferences: FakeSandookPreferences
    private lateinit var flag: FakeFlag
    private lateinit var analytics: RecordingAnalytics
    private var now: Long = DAY_ZERO
    private var savedTrips: Long = 5L

    @Before
    fun setup() {
        requester = FakeAppReviewRequester()
        lifecycleStore = FakeUserLifecycleStore(nowMillis = { now })
        preferences = FakeSandookPreferences()
        flag = FakeFlag()
        analytics = RecordingAnalytics()

        // The default world is an eligible one for ask 1, so each test changes one thing.
        flag.setFlagValue(FlagKeys.IN_APP_REVIEW_ENABLED.key, FlagValue.BooleanValue(true))
        preferences.setBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO, true)
        lifecycleStore.installAgeDays = 10L
        savedTrips = 5L
    }

    private fun manager() = RealAppReviewManager(
        requester = requester,
        lifecycleStore = lifecycleStore,
        preferences = preferences,
        flag = flag,
        analytics = analytics,
        savedTripCount = { savedTrips },
    )

    /** Arms a moment and then lands on Saved Trips, which is the pair that can fire a request. */
    private fun RealAppReviewManager.delightThenLand(
        moment: DelightMoment = DelightMoment.TIMETABLE_VIEWED,
    ) {
        onDelightMoment(moment)
        onSavedTripsScreenShown()
    }

    @Test
    fun `requests a review on a delight moment when eligible for ask 1`() {
        manager().delightThenLand()

        assertEquals(1, requester.requestCount)
    }

    @Test
    fun `does not request when no delight moment is armed`() {
        manager().onSavedTripsScreenShown()

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `logs review_prompt_requested with the moment source`() {
        manager().delightThenLand(DelightMoment.PARK_RIDE_ADDED)

        val event = analytics.tracked.filterIsInstance<AnalyticsEvent.ReviewPromptRequestedEvent>()
        assertEquals(1, event.size)
        assertEquals("park_ride_added", event.single().source)
    }

    @Test
    fun `does not request when the feature is switched off`() {
        flag.setFlagValue(FlagKeys.IN_APP_REVIEW_ENABLED.key, FlagValue.BooleanValue(false))

        manager().delightThenLand()

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `does not request during onboarding`() {
        preferences.setBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO, false)

        manager().delightThenLand()

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `does not request before the account is old enough`() {
        lifecycleStore.installAgeDays = 1L

        manager().delightThenLand()

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `does not request when the install date was never recorded`() {
        lifecycleStore.installAgeDays = null

        manager().delightThenLand()

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `does not request with fewer than the minimum saved trips`() {
        savedTrips = 0L

        manager().delightThenLand()

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `one saved trip is enough for ask 1`() {
        savedTrips = 1L

        manager().delightThenLand()

        assertEquals(1, requester.requestCount)
    }

    @Test
    fun `consumes the armed moment even when the gates fail`() {
        val manager = manager()
        savedTrips = 0L // not yet eligible
        manager.delightThenLand()
        assertEquals(0, requester.requestCount)

        // Now eligible, but the earlier moment was already consumed on landing, so a bare
        // landing with nothing armed must not fire.
        savedTrips = 5L
        manager.onSavedTripsScreenShown()

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `does not ask a second time inside the spacing window`() {
        val manager = manager()
        manager.delightThenLand() // ask 1
        assertEquals(1, requester.requestCount)

        now = DAY_ZERO + MILLIS_PER_DAY * 30 // well under the 150-day gap
        manager.delightThenLand()

        assertEquals(1, requester.requestCount)
    }

    @Test
    fun `asks a second time once the spacing window has passed`() {
        val manager = manager()
        manager.delightThenLand() // ask 1

        now = DAY_ZERO + MILLIS_PER_DAY * (MIN_DAYS_BETWEEN_ASKS + 1)
        manager.delightThenLand()

        assertEquals(2, requester.requestCount)
    }

    @Test
    fun `never asks a third time`() {
        val manager = manager()
        manager.delightThenLand() // ask 1

        now = DAY_ZERO + MILLIS_PER_DAY * (MIN_DAYS_BETWEEN_ASKS + 1)
        manager.delightThenLand() // ask 2
        assertEquals(2, requester.requestCount)

        now += MILLIS_PER_DAY * (MIN_DAYS_BETWEEN_ASKS + 1)
        manager.delightThenLand()

        assertEquals(2, requester.requestCount)
        assertEquals(2, lifecycleStore.count(LifecycleCounter.REVIEW_PROMPT_REQUESTED))
    }

    @Test
    fun `a rider with no saved trip qualifies after enough active days`() {
        savedTrips = 0L
        lifecycleStore.setCount(LifecycleCounter.ACTIVE_DAYS, 5L)

        manager().delightThenLand()

        assertEquals(1, requester.requestCount)
        val event = analytics.tracked.single() as AnalyticsEvent.ReviewPromptRequestedEvent
        assertEquals("active_days", event.gate)
    }

    @Test
    fun `a rider with no saved trip and too few active days is not asked`() {
        savedTrips = 0L
        lifecycleStore.setCount(LifecycleCounter.ACTIVE_DAYS, 4L)

        manager().delightThenLand()

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `active days still need the install to be old enough`() {
        savedTrips = 0L
        lifecycleStore.setCount(LifecycleCounter.ACTIVE_DAYS, 30L)
        lifecycleStore.installAgeDays = 1L

        manager().delightThenLand()

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `saved trips is reported when both gates pass`() {
        lifecycleStore.setCount(LifecycleCounter.ACTIVE_DAYS, 30L)

        manager().delightThenLand()

        val event = analytics.tracked.single() as AnalyticsEvent.ReviewPromptRequestedEvent
        assertEquals("saved_trips", event.gate)
    }

    @Test
    fun `the second ask reports the spacing gate`() {
        val manager = manager()
        manager.delightThenLand()
        now = DAY_ZERO + MILLIS_PER_DAY * (MIN_DAYS_BETWEEN_ASKS + 1)
        manager.delightThenLand()

        val gates = analytics.tracked.map { (it as AnalyticsEvent.ReviewPromptRequestedEvent).gate }
        assertEquals(listOf("saved_trips", "spacing"), gates)
    }

    @Test
    fun `landing on saved trips and delight moments record an active day`() {
        lifecycleStore.countActiveDays = true
        val manager = manager()

        manager.onSavedTripsScreenShown()
        manager.onDelightMoment(DelightMoment.TIMETABLE_VIEWED)

        assertEquals(2L, lifecycleStore.count(LifecycleCounter.ACTIVE_DAYS))
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        const val DAY_ZERO = 1_700_000_000_000L
        const val MIN_DAYS_BETWEEN_ASKS = 150L
    }
}

private class FakeAppReviewRequester : AppReviewRequester {
    var requestCount = 0
        private set

    override fun requestReview() {
        requestCount++
    }
}

private class RecordingAnalytics : Analytics {
    val tracked = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        tracked += event
    }

    override fun setUserId(userId: String) = Unit
    override fun setUserProperty(name: String, value: String) = Unit
}

private class FakeUserLifecycleStore(private val nowMillis: () -> Long) : UserLifecycleStore {
    var installAgeDays: Long? = 0L

    // Day boundaries are RealUserLifecycleStore's job and tested there. When true, every
    // recordActiveDay() call counts as a new day; otherwise tests set the count directly.
    var countActiveDays = false

    private val counts = mutableMapOf<LifecycleCounter, Long>()
    private val lastAt = mutableMapOf<LifecycleCounter, Long>()

    override fun recordFirstInstallIfAbsent() = Unit
    override fun firstInstallAtMillis(): Long? = installAgeDays?.let { 0L }
    override fun daysSinceFirstInstall(): Long? = installAgeDays

    override fun increment(counter: LifecycleCounter): Long {
        val next = counts.getOrElse(counter) { 0L } + 1
        counts[counter] = next
        lastAt[counter] = nowMillis()
        return next
    }

    override fun count(counter: LifecycleCounter): Long = counts.getOrElse(counter) { 0L }
    override fun lastAtMillis(counter: LifecycleCounter): Long? = lastAt[counter]

    override fun millisSinceLast(counter: LifecycleCounter): Long? {
        val last = lastAt[counter] ?: return null
        return nowMillis() - last
    }

    override fun recordActiveDay(): Long =
        if (countActiveDays) increment(LifecycleCounter.ACTIVE_DAYS) else count(LifecycleCounter.ACTIVE_DAYS)

    fun setCount(counter: LifecycleCounter, value: Long) {
        counts[counter] = value
    }

    override fun reset(counter: LifecycleCounter) {
        counts.remove(counter)
        lastAt.remove(counter)
    }
}
