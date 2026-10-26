package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.network.error.NetworkError
import xyz.ksharma.krail.core.network.error.NetworkException
import xyz.ksharma.krail.core.testing.fakes.FakeCarriageForecastRepository
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.feature.crowding.CarriageIndex
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.CrowdingWording
import xyz.ksharma.krail.feature.crowding.SeatLevel.FEW_SEATS
import xyz.ksharma.krail.feature.crowding.SeatLevel.SEATS_AVAILABLE
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo.Leg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class JourneyCarriagesTest {

    private val bothEnds = CarriageLevels(listOf(SEATS_AVAILABLE) + List(6) { FEW_SEATS } + SEATS_AVAILABLE)
    private val index = CarriageIndex(mapOf(TRIP to mapOf(PLATFORM to bothEnds)))

    private fun journey(mode: TransportMode = TransportMode.Train, originUtc: String = MORNING_UTC): JourneyCardInfo {
        val line = TransportModeLine(transportMode = mode, lineName = "T1")
        return JourneyCardInfo(
            timeText = "in 5 mins",
            originTime = "8:59am",
            originUtcDateTime = originUtc,
            destinationTime = "9:26am",
            destinationUtcDateTime = originUtc,
            travelTime = "27 mins",
            transportModeLines = persistentListOf(line),
            legs = persistentListOf(
                Leg.TransportLeg(
                    transportModeLine = line,
                    displayText = "towards Penrith",
                    totalDuration = "27 mins",
                    stops = persistentListOf(),
                    seatLevel = FEW_SEATS,
                    realtimeTripId = TRIP,
                    boardingStopId = PLATFORM,
                ),
            ),
            totalUniqueServiceAlerts = 0,
        )
    }

    private fun JourneyCardInfo.leg() = legs.single() as Leg.TransportLeg

    @Test
    fun `train leg gets its cars sentence and tag`() {
        val result = listOf(journey()).withCarriages(index).single()
        assertEquals(bothEnds, result.leg().carriages)
        assertEquals("Seats expected at both ends.", result.leg().carriageSentence)
        assertEquals("Seats at both ends", result.seatTag)
    }

    @Test
    fun `without cars the tag falls back to the whole train level`() {
        assertEquals("Few seats", listOf(journey()).withCarriages(CarriageIndex.EMPTY).single().seatTag)
    }

    @Test
    fun `after 9pm the sentence is the Nightsafe line`() {
        // 10:30pm in Sydney during daylight saving.
        val result = listOf(journey(originUtc = "2026-10-26T11:30:00Z")).withCarriages(index).single()
        assertEquals(CrowdingWording.NIGHT_LINE, result.leg().carriageSentence)
    }

    @Test
    fun `bus legs are left alone and never ask for the download`() {
        val bus = journey(mode = TransportMode.Bus)
        assertNull(listOf(bus).withCarriages(index).single().leg().carriages)
        assertEquals(false, listOf(bus).wantsCarriages())
        assertEquals(true, listOf(journey()).wantsCarriages())
    }

    @Test
    fun `loader downloads once for a train result and never for a bus one`() = runTest {
        val repository = FakeCarriageForecastRepository(Result.success(index))
        val loader = CarriageForecastLoader(repository, this, StandardTestDispatcher(testScheduler))
        var refreshed = 0
        loader.loadOnceFor(listOf(journey(mode = TransportMode.Bus))) { refreshed++ }
        loader.loadOnceFor(listOf(journey())) { refreshed++ }
        loader.loadOnceFor(listOf(journey())) { refreshed++ }
        advanceUntilIdle()
        assertEquals(1, repository.loadCount)
        assertEquals(1, refreshed)
        assertSame(index, loader.index)
    }

    @Test
    fun `a failed download leaves the index empty and redraws nothing`() = runTest {
        val repository = FakeCarriageForecastRepository(
            Result.failure(NetworkException(NetworkError.Unknown(RuntimeException("boom")))),
        )
        val loader = CarriageForecastLoader(repository, this, StandardTestDispatcher(testScheduler))
        var refreshed = 0
        loader.loadOnceFor(listOf(journey())) { refreshed++ }
        advanceUntilIdle()
        assertEquals(CarriageIndex.EMPTY, loader.index)
        assertEquals(0, refreshed)
    }

    private companion object {
        const val TRIP = "204R.1324.165.12.H.8.92002332"
        const val PLATFORM = "2141313"
        const val MORNING_UTC = "2026-10-26T22:00:00Z" // 9am in Sydney
    }
}
