package xyz.ksharma.krail.trip.planner.ui.timetable.business

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionReason
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionSeverity
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DisruptionAnalyticsTest {

    private val fake = FakeAnalytics()
    private val analytics = DisruptionAnalytics(fake)
    private val trip = Trip(fromStopId = "A", fromStopName = "Bankstown", toStopId = "B", toStopName = "Central")

    private fun banner(id: String = "metro:x:1", reason: DisruptionReason = DisruptionReason.MODE_MISSING) =
        DisruptionBannerState.Shown(
            disruption = Disruption(
                id = id,
                feed = DisruptionFeed.METRO,
                severity = DisruptionSeverity.NOT_RUNNING,
                headline = "Buses replace metro services between Bankstown and Sydenham",
                detail = "secret detail that must never be sent",
                startEpochSec = 1L,
                endEpochSec = 2L,
                moreInfoUrl = null,
                replacedByBuses = true,
            ),
            reason = reason,
        )

    @Test
    fun `Given a banner Then shown carries the mode, reason, trip and journey count and no text`() {
        analytics.trackShown(banner(reason = DisruptionReason.REPLACEMENT_BUS), trip, visibleJourneyCount = 6)

        val event = fake.getTrackedEvent("disruption_status")
        assertEquals(
            mapOf(
                "action" to "shown",
                "transportMode" to "metro",
                "fromStopId" to "A",
                "toStopId" to "B",
                "reason" to "replacement_bus",
                "visibleJourneyCount" to 6,
            ),
            event?.properties,
        )
    }

    @Test
    fun `Given the banner composes again Then shown is reported once`() {
        repeat(3) { analytics.trackShown(banner(), trip, visibleJourneyCount = 6) }

        assertEquals(1, fake.getTrackedEvents("disruption_status").size)
    }

    @Test
    fun `Given a different alert on the same trip Then it is reported separately`() {
        analytics.trackShown(banner(id = "metro:x:1"), trip, visibleJourneyCount = 6)
        analytics.trackShown(banner(id = "metro:x:2"), trip, visibleJourneyCount = 6)

        assertEquals(2, fake.getTrackedEvents("disruption_status").size)
    }

    @Test
    fun `Given the same alert on a different trip Then it is reported separately`() {
        analytics.trackShown(banner(), trip, visibleJourneyCount = 6)
        analytics.trackShown(banner(), trip.copy(toStopId = "C"), visibleJourneyCount = 6)

        assertEquals(2, fake.getTrackedEvents("disruption_status").size)
    }

    @Test
    fun `Given repeated feed failures Then error is reported once and carries no reason`() {
        repeat(4) { analytics.trackUnavailable(DisruptionFeed.METRO, trip) }

        val events = fake.getTrackedEvents("disruption_status")
        assertEquals(1, events.size)
        assertEquals("error", events.single().properties?.get("action"))
        assertNull(events.single().properties?.get("reason"))
    }

    @Test
    fun `Given expand and more info Then they ride the info tile event with a fixed key`() {
        analytics.trackTile(banner(), expand = true)
        analytics.trackTile(banner(), ctaUrl = "https://transportnsw.info/alerts/details#/ems-8150")

        val events = fake.getTrackedEvents("info_tile_interaction")
        assertEquals(2, events.size)
        assertEquals("disruption_metro", events[0].properties?.get("key"))
        assertEquals(true, events[0].properties?.get("expand"))
        assertEquals("https://transportnsw.info/alerts/details#/ems-8150", events[1].properties?.get("cta_click"))
    }

    @Test
    fun `Given the shown event Then it is not the info tile event`() {
        analytics.trackShown(banner(), trip, visibleJourneyCount = 1)

        assertEquals(0, fake.getTrackedEvents("info_tile_interaction").size)
        assertEquals(AnalyticsEvent.DisruptionStatusEvent::class, fake.getTrackedEvent("disruption_status")!!::class)
    }
}
