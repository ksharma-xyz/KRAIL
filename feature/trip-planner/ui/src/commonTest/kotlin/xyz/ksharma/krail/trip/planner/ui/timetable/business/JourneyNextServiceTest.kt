package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.feature.crowding.SeatLevel.SEATS_AVAILABLE
import xyz.ksharma.krail.feature.crowding.SeatLevel.STANDING_ONLY
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo.Leg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JourneyNextServiceTest {

    // 8:54am and 8:59am in Sydney (daylight saving).
    private val full = journey("A1", "2026-10-26T21:54:00Z", "8:54am", STANDING_ONLY)
    private val quieter = journey("B2", "2026-10-26T21:59:00Z", "8:59am", SEATS_AVAILABLE)

    private fun journey(
        id: String,
        originUtc: String,
        originTime: String,
        level: SeatLevel,
        mode: TransportMode = TransportMode.Train,
        line: String = "T1",
        stand: String = "2000333",
        legs: Int = 1,
    ): JourneyCardInfo {
        val modeLine = TransportModeLine(transportMode = mode, lineName = line)
        val leg = Leg.TransportLeg(
            transportModeLine = modeLine,
            displayText = "towards Penrith",
            totalDuration = "27 mins",
            stops = persistentListOf(),
            tripId = id,
            seatLevel = level,
            boardingStopId = stand,
        )
        return JourneyCardInfo(
            timeText = "in 5 mins",
            platformText = "Platform 1",
            originTime = originTime,
            originUtcDateTime = originUtc,
            destinationTime = "9:26am",
            destinationUtcDateTime = originUtc,
            travelTime = "27 mins",
            transportModeLines = persistentListOf(modeLine),
            legs = persistentListOf(*Array(legs) { leg }),
            totalUniqueServiceAlerts = 0,
        )
    }

    @Test
    fun `a full train names the quieter one five minutes later`() {
        val result = listOf(full, quieter).withNextServices("Central").first()
        assertEquals(
            "Want a seat? The next T1, at 8:59am from Platform 1, has seats available and is due at Central at 9:26am.",
            result.nextServiceLine,
        )
        assertEquals(quieter.journeyId, result.nextServiceJourneyId)
    }

    @Test
    fun `with the flag off no card names a later service`() {
        val result = listOf(full, quieter).withNextServices("Central", enabled = false).first()
        assertNull(result.nextServiceLine)
        assertNull(result.nextServiceJourneyId)
    }

    @Test
    fun `the quieter train itself names nothing`() {
        assertNull(listOf(full, quieter).withNextServices("Central").last().nextServiceLine)
    }

    @Test
    fun `nothing at night`() {
        val lateFull = journey("A1", "2026-10-26T11:54:00Z", "10:54pm", STANDING_ONLY)
        val lateQuiet = journey("B2", "2026-10-26T11:59:00Z", "10:59pm", SEATS_AVAILABLE)
        assertNull(listOf(lateFull, lateQuiet).withNextServices("Central").first().nextServiceLine)
    }

    @Test
    fun `nothing when the trip changes later`() {
        val withChange = journey("A1", "2026-10-26T21:54:00Z", "8:54am", STANDING_ONLY, legs = 2)
        assertNull(listOf(withChange, quieter).withNextServices("Central").first().nextServiceLine)
    }

    @Test
    fun `a bus is named only from the same stand`() {
        val bus = journey("A1", "2026-10-26T21:54:00Z", "8:54am", STANDING_ONLY, TransportMode.Bus, "333", "200039")
        val sameStand = journey(
            "B2",
            "2026-10-26T22:04:00Z",
            "9:04am",
            SEATS_AVAILABLE,
            TransportMode.Bus,
            "333",
            "200039",
        )
        val otherStand = sameStand.copy(
            legs = persistentListOf((sameStand.legs.single() as Leg.TransportLeg).copy(boardingStopId = "200041")),
        )
        assertEquals(
            "Want a seat? The next 333, at 9:04am, has seats available.",
            listOf(bus, sameStand).withNextServices("Bondi Beach").first().nextServiceLine,
        )
        assertNull(listOf(bus, otherStand).withNextServices("Bondi Beach").first().nextServiceLine)
    }
}
