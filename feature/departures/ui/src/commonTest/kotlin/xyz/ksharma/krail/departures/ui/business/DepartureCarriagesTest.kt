package xyz.ksharma.krail.departures.ui.business

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.feature.crowding.CarriageIndex
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.SeatLevel.FEW_SEATS
import xyz.ksharma.krail.feature.crowding.SeatLevel.SEATS_AVAILABLE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DepartureCarriagesTest {

    private val cars = CarriageLevels(listOf(SEATS_AVAILABLE) + List(6) { FEW_SEATS } + SEATS_AVAILABLE)
    private val index = CarriageIndex(mapOf(TRIP to mapOf(PLATFORM to cars)))

    private fun departure(mode: String = "Train", tripId: String? = TRIP) = StopDeparture(
        lineNumber = "T2",
        lineColorCode = "#0098CD",
        transportModeName = mode,
        destinationName = "towards Leppington",
        departureTimeText = "8:59 am",
        departureUtcDateTime = "2026-10-26T21:59:00Z", // 8:59am in Sydney
        seatLevel = FEW_SEATS,
        realtimeTripId = tripId,
        platformStopId = PLATFORM,
    )

    @Test
    fun `a covered train gets its cars sentence and tag`() {
        val state = DeparturesState(departures = persistentListOf(departure())).withCarriages(index)
        val row = state.departures.single()
        assertEquals(cars, row.carriages)
        assertEquals("Seats expected at both ends.", row.carriageSentence)
        assertEquals("Seats at both ends", row.carriageTag)
    }

    @Test
    fun `a train the feed does not cover keeps no cars`() {
        val row = DeparturesState(departures = persistentListOf(departure(tripId = "other")))
            .withCarriages(index)
            .departures
            .single()
        assertNull(row.carriages)
        assertNull(row.carriageTag)
    }

    @Test
    fun `only a listed train asks for the download`() {
        assertEquals(true, DeparturesState(departures = persistentListOf(departure())).wantsCarriages())
        assertEquals(false, DeparturesState(departures = persistentListOf(departure(mode = "Bus"))).wantsCarriages())
        assertEquals(false, DeparturesState(departures = persistentListOf(departure(tripId = null))).wantsCarriages())
    }

    private companion object {
        const val TRIP = "204R.1324.165.12.H.8.92002332"
        const val PLATFORM = "2141313"
    }
}
