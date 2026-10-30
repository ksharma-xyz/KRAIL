package xyz.ksharma.krail.trip.planner.ui.timetable

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.feature.crowding.SeatLevel.FEW_SEATS
import xyz.ksharma.krail.feature.crowding.SeatLevel.SEATS_AVAILABLE
import xyz.ksharma.krail.feature.crowding.SeatLevel.STANDING_ONLY
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo.Leg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CrowdingAnalyticsTest {

    private fun journey(level: SeatLevel? = null, cars: CarriageLevels? = null): JourneyCardInfo {
        val modeLine = TransportModeLine(transportMode = TransportMode.Train, lineName = "T1")
        val leg = Leg.TransportLeg(
            transportModeLine = modeLine,
            displayText = "towards Penrith",
            totalDuration = "27 mins",
            stops = persistentListOf(),
            tripId = "A1",
            seatLevel = level,
            carriages = cars,
        )
        return JourneyCardInfo(
            timeText = "in 5 mins",
            platformText = "Platform 1",
            originTime = "8:54am",
            originUtcDateTime = "2026-10-26T21:54:00Z",
            destinationTime = "9:26am",
            destinationUtcDateTime = "2026-10-26T22:26:00Z",
            travelTime = "27 mins",
            transportModeLines = persistentListOf(modeLine),
            legs = persistentListOf(leg),
            totalUniqueServiceAlerts = 0,
        )
    }

    @Test
    fun `carriages count as cars even with a whole train level`() {
        val cars = CarriageLevels(listOf(SEATS_AVAILABLE, FEW_SEATS, FEW_SEATS, FEW_SEATS))
        assertEquals("cars", journey(level = FEW_SEATS, cars = cars).crowdingParam())
    }

    @Test
    fun `a whole train level alone counts as level`() {
        assertEquals("level", journey(level = STANDING_ONLY).crowdingParam())
    }

    @Test
    fun `no seat data counts as none`() {
        assertEquals("none", journey().crowdingParam())
        assertEquals("none", null.crowdingParam())
    }

    @Test
    fun `seat level uses the whole train level first`() {
        assertEquals("standing", journey(level = STANDING_ONLY).analyticsSeatLevel())
    }

    @Test
    fun `seat level falls back to the level most cars share`() {
        val cars = CarriageLevels(listOf(SEATS_AVAILABLE, FEW_SEATS, FEW_SEATS, FEW_SEATS))
        assertEquals("few", journey(cars = cars).analyticsSeatLevel())
    }

    @Test
    fun `seat level is null without seat data`() {
        assertNull(journey().analyticsSeatLevel())
    }

    @Test
    fun `opening the seat tip records an info tile expand`() {
        val analytics = FakeAnalytics()
        analytics.trackSeatTipOpened()
        val event = analytics.getTrackedEvent("info_tile_interaction")
        assertIs<AnalyticsEvent.InfoTileInteraction>(event)
        assertEquals("next-service", event.key)
        assertEquals(true, event.expand)
    }

    @Test
    fun `seat level is left out of the event when there is none`() {
        val event = AnalyticsEvent.JourneyCardToggleEvent(
            expanded = true,
            hasStarted = false,
            legCount = 1,
            transportModes = "1",
        )
        assertEquals(false, event.properties?.containsKey("seatLevel"))
        assertEquals("none", event.properties?.get("crowding"))
        assertEquals(false, event.properties?.get("seatTip"))
    }
}
