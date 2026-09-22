package xyz.ksharma.krail.splash

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiUnavailableReasons
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppInfoProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `app_start` is the one event every launch sends. The Ask KRAIL fields were added to it, so
 * the property held here is that **nothing about them can stop it being sent**: a check that
 * throws, hangs, or a flag that cannot be read each still produce exactly one event.
 */
class AppStartTrackerTest {

    private val analytics = FakeAnalytics()

    @Test
    fun `app_start carries the capability and the flag`() = runTest {
        tracker(peek = { AiAvailability.Available }, flag = { true }).track(this, krailThemeId = 1)
        advanceUntilIdle()

        val props = appStart().properties.orEmpty()
        assertEquals(AiCapability.AVAILABLE, props["aiCapability"])
        assertEquals(true, props["aiSearchEnabled"])
    }

    @Test
    fun `a rider who switched on-device AI off is reported as such`() = runTest {
        tracker(
            peek = { AiAvailability.Unavailable(AiUnavailableReasons.NEEDS_DEVICE_SETTING) },
            flag = { false },
        ).track(this, krailThemeId = 1)
        advanceUntilIdle()

        val props = appStart().properties.orEmpty()
        assertEquals(AiUnavailableReasons.NEEDS_DEVICE_SETTING, props["aiCapability"])
        assertEquals(false, props["aiSearchEnabled"])
    }

    @Test
    fun `a check that throws still sends app_start`() = runTest {
        tracker(peek = { error("platform exploded") }, flag = { true }).track(this, krailThemeId = 1)
        advanceUntilIdle()

        assertEquals(AiUnavailableReasons.CHECK_FAILED, appStart().properties?.get("aiCapability"))
    }

    @Test
    fun `a flag that cannot be read still sends app_start, without the flag`() = runTest {
        tracker(peek = { AiAvailability.Available }, flag = { error("remote config not ready") })
            .track(this, krailThemeId = 1)
        advanceUntilIdle()

        val props = appStart().properties.orEmpty()
        assertFalse("aiSearchEnabled" in props)
        assertEquals(AiCapability.AVAILABLE, props["aiCapability"])
    }

    @Test
    fun `a check that never answers sends app_start once the timeout passes`() = runTest {
        tracker(peek = { awaitCancellation() }, flag = { true }).track(this, krailThemeId = 1)

        advanceTimeBy(AiCapability.TIMEOUT_MS - 1)
        assertTrue(analytics.getTrackedEvents("app_start").isEmpty())

        advanceUntilIdle()
        assertEquals(AiCapability.TIMEOUT, appStart().properties?.get("aiCapability"))
    }

    private fun TestScope.tracker(
        peek: suspend () -> AiAvailability,
        flag: () -> Boolean,
    ) = AppStartTracker(
        analytics = analytics,
        appInfoProvider = FakeAppInfoProvider(),
        peekAiAvailability = peek,
        isAiSearchEnabled = flag,
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )

    /** Exactly one, which also asserts nothing sent it twice. */
    private fun appStart(): AnalyticsEvent = analytics.getTrackedEvents("app_start").single()
}
