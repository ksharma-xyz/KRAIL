package xyz.ksharma.krail.core.maps.data.location

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.PermissionResultEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationPermissionRecorderTest {

    private val analytics = FakeAnalytics()
    private val preferences = FakeSandookPreferences()

    private fun recorder() = RealLocationPermissionRecorder(analytics, preferences)

    @Test
    fun `nothing is remembered before the rider has answered`() {
        assertFalse(recorder().wasRefused)
    }

    @Test
    fun `a refusal is remembered by the next recorder, as after a restart`() {
        recorder().wasRefused = true

        assertTrue(recorder().wasRefused)

        recorder().wasRefused = false

        assertFalse(recorder().wasRefused)
    }

    @Test
    fun `an answer is one location permission result`() {
        recorder().onRequestAnswered(
            source = PermissionResultEvent.Source.INTRO,
            outcome = PermissionResultEvent.Outcome.DENIED,
            canAskAgain = true,
        )

        assertEquals(
            listOf(
                PermissionResultEvent(
                    permission = PermissionResultEvent.Permission.LOCATION,
                    outcome = PermissionResultEvent.Outcome.DENIED,
                    source = PermissionResultEvent.Source.INTRO,
                    canAskAgain = true,
                ),
            ),
            analytics.getTrackedEvents("permission_result"),
        )
    }
}
