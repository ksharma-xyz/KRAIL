package xyz.ksharma.krail.departures.ui.business

import kotlinx.serialization.json.Json
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import xyz.ksharma.krail.departures.ui.fixtures.CentralCrowdingDepartureFixture
import xyz.ksharma.krail.feature.crowding.SeatLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DepartureMonitorCrowdingMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val departures = json
        .decodeFromString<DepartureMonitorResponse>(CentralCrowdingDepartureFixture.JSON)
        .toStopDepartures()

    @Test
    fun `occupancy maps to a seat level per departure`() {
        assertEquals(
            listOf(SeatLevel.SEATS_AVAILABLE, SeatLevel.FEW_SEATS, null, SeatLevel.SEATS_AVAILABLE),
            departures.map { it.seatLevel },
        )
    }

    @Test
    fun `a departure without occupancy keeps every other field`() {
        val t4 = departures[2]
        assertEquals("T4", t4.lineNumber)
        assertNull(t4.seatLevel)
        assertEquals("600L.1324.165.4.T.8.92002027", t4.realtimeTripId)
    }

    @Test
    fun `realtime trip id and platform stop id carry the GTFS join keys`() {
        val t2 = departures[0]
        assertEquals("13XX.1324.165.36.B.8.92004580", t2.realtimeTripId)
        assertEquals("2000339", t2.platformStopId)
    }
}
