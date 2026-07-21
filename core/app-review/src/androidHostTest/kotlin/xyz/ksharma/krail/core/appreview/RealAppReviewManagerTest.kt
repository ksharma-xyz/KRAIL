package xyz.ksharma.krail.core.appreview

import org.junit.Before
import org.junit.Test
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.sandook.LifecycleCounter
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.UserLifecycleStore
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RealAppReviewManagerTest {

    private lateinit var requester: FakeAppReviewRequester
    private lateinit var lifecycleStore: FakeUserLifecycleStore
    private lateinit var preferences: FakeSandookPreferences
    private lateinit var flag: FakeFlag
    private lateinit var analytics: RecordingAnalytics
    private var now: Long = DAY_ZERO

    @Before
    fun setup() {
        requester = FakeAppReviewRequester()
        lifecycleStore = FakeUserLifecycleStore(nowMillis = { now })
        preferences = FakeSandookPreferences()
        flag = FakeFlag()
        analytics = RecordingAnalytics()

        // The default world is an eligible one, so each test changes exactly one thing.
        flag.booleans[FlagKeys.IN_APP_REVIEW_ENABLED.key] = true
        preferences.setBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO, true)
        lifecycleStore.installAgeDays = 10L
    }

    private fun manager() = RealAppReviewManager(
        requester = requester,
        lifecycleStore = lifecycleStore,
        preferences = preferences,
        flag = flag,
        analytics = analytics,
        nowMillis = { now },
    )

    @Test
    fun `requests a review once the open count threshold is reached`() {
        val manager = manager()

        manager.onSavedTripOpened()
        manager.onSavedTripOpened()
        assertEquals(0, requester.requestCount)

        manager.onSavedTripOpened()

        assertEquals(1, requester.requestCount)
    }

    @Test
    fun `logs review_prompt_requested with the trigger source`() {
        repeat(THRESHOLD_OPENS) { manager().onSavedTripOpened() }

        val event = analytics.tracked.filterIsInstance<AnalyticsEvent.ReviewPromptRequestedEvent>()
        assertEquals(1, event.size)
        assertEquals("saved_trip_open", event.single().source)
    }

    @Test
    fun `counts opens even while the feature is switched off`() {
        flag.booleans[FlagKeys.IN_APP_REVIEW_ENABLED.key] = false
        val manager = manager()

        repeat(THRESHOLD_OPENS) { manager.onSavedTripOpened() }

        assertEquals(0, requester.requestCount)
        // Switching the flag on later must see the real history, not a reset counter.
        assertEquals(THRESHOLD_OPENS.toLong(), lifecycleStore.count(LifecycleCounter.SAVED_TRIP_OPEN))
    }

    @Test
    fun `does not request during onboarding`() {
        preferences.setBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO, false)
        val manager = manager()

        repeat(THRESHOLD_OPENS) { manager.onSavedTripOpened() }

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `does not request before the account is old enough`() {
        lifecycleStore.installAgeDays = 1L
        val manager = manager()

        repeat(THRESHOLD_OPENS) { manager.onSavedTripOpened() }

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `does not request when the install date was never recorded`() {
        lifecycleStore.installAgeDays = null
        val manager = manager()

        repeat(THRESHOLD_OPENS) { manager.onSavedTripOpened() }

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `does not request right after a zero result search`() {
        val manager = manager()
        repeat(THRESHOLD_OPENS - 1) { manager.onSavedTripOpened() }

        manager.onZeroResultSearch()
        manager.onSavedTripOpened()

        assertEquals(0, requester.requestCount)
    }

    @Test
    fun `requests again once the zero result suppression window has passed`() {
        val manager = manager()
        repeat(THRESHOLD_OPENS - 1) { manager.onSavedTripOpened() }
        manager.onZeroResultSearch()

        now = DAY_ZERO + MILLIS_PER_DAY
        manager.onSavedTripOpened()

        assertEquals(1, requester.requestCount)
    }

    @Test
    fun `does not request again inside the cooldown`() {
        val manager = manager()
        repeat(THRESHOLD_OPENS) { manager.onSavedTripOpened() }
        assertEquals(1, requester.requestCount)

        now = DAY_ZERO + MILLIS_PER_DAY * 30
        manager.onSavedTripOpened()

        assertEquals(1, requester.requestCount)
    }

    @Test
    fun `requests again once the cooldown has elapsed`() {
        val manager = manager()
        repeat(THRESHOLD_OPENS) { manager.onSavedTripOpened() }

        now = DAY_ZERO + MILLIS_PER_DAY * (DEFAULT_COOLDOWN_DAYS + 1)
        manager.onSavedTripOpened()

        assertEquals(2, requester.requestCount)
    }

    @Test
    fun `remote config thresholds override the defaults`() {
        flag.numbers[FlagKeys.IN_APP_REVIEW_MIN_SAVED_TRIP_OPENS.key] = 1L
        val manager = manager()

        manager.onSavedTripOpened()

        assertTrue(requester.requestCount == 1)
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        const val DAY_ZERO = 1_700_000_000_000L
        const val THRESHOLD_OPENS = 3
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

private class FakeFlag : Flag {
    val booleans = mutableMapOf<String, Boolean>()
    val numbers = mutableMapOf<String, Long>()

    override fun getFlagValue(key: String): FlagValue = when {
        booleans.containsKey(key) -> FlagValue.BooleanValue(booleans.getValue(key))
        numbers.containsKey(key) -> FlagValue.NumberValue(numbers.getValue(key))
        // Anything unset falls through to the caller's own fallback, same as a missing
        // Remote Config value at runtime.
        else -> FlagValue.StringValue("")
    }
}

private class FakeUserLifecycleStore(private val nowMillis: () -> Long) : UserLifecycleStore {
    var installAgeDays: Long? = 0L

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
}

private class FakeSandookPreferences : SandookPreferences {
    private val values = mutableMapOf<String, Any>()

    override fun getLong(key: String): Long? = values[key] as? Long
    override fun setLong(key: String, value: Long) { values[key] = value }
    override fun getString(key: String): String? = values[key] as? String
    override fun setString(key: String, value: String) { values[key] = value }
    override fun getBoolean(key: String): Boolean? = values[key] as? Boolean
    override fun setBoolean(key: String, value: Boolean) { values[key] = value }
    override fun getDouble(key: String): Double? = values[key] as? Double
    override fun setDouble(key: String, value: Double) { values[key] = value }
    override fun deletePreference(key: String) { values.remove(key) }
}
