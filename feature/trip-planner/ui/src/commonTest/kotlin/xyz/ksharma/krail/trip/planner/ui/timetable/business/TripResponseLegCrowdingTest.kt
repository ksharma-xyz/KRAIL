package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.serialization.json.Json
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo.Leg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TripResponseLegCrowdingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun leg(firstStopOccupancy: String?): Leg.TransportLeg {
        val occupancy = firstStopOccupancy?.let { "\"occupancy\": \"$it\"," }.orEmpty()
        val raw = LEG_JSON.replace("\"__OCCUPANCY__\": 0,", occupancy)
        return json.decodeFromString<TripResponse.Leg>(raw).toTransportLegUiModel() as Leg.TransportLeg
    }

    @Test
    fun `seat level is read at the boarding stop`() {
        assertEquals(SeatLevel.FEW_SEATS, leg("FEW_SEATS").seatLevel)
    }

    @Test
    fun `no occupancy at the boarding stop is no seat level`() {
        assertNull(leg(firstStopOccupancy = null).seatLevel)
    }

    @Test
    fun `realtime trip id and boarding platform carry the GTFS join keys`() {
        val leg = leg("FEW_SEATS")
        assertEquals("154T.1324.165.4.A.8.92002812", leg.realtimeTripId)
        assertEquals("2000333", leg.boardingStopId)
    }

    private companion object {
        // Real /v1/tp/trip leg, Central to Parramatta, captured 2026-10-26 at 10:19pm and
        // trimmed to the fields the mapper reads. The later stop's FEW_SEATS must not leak
        // into the boarding stop.
        const val LEG_JSON = """
        {
          "duration": 1890,
          "transportation": {
            "id": "nsw:020T1:W:H:sj2",
            "disassembledName": "T1",
            "description": "City to Emu Plains or Richmond",
            "product": { "class": 1 },
            "destination": { "name": "Penrith via Parramatta" },
            "properties": { "RealtimeTripId": "154T.1324.165.4.A.8.92002812" }
          },
          "stopSequence": [
            {
              "id": "2000333",
              "name": "Central Station, Platform 13, Sydney",
              "disassembledName": "Central Station, Platform 13",
              "departureTimePlanned": "2026-10-26T11:22:00Z",
              "properties": { "__OCCUPANCY__": 0, "platform": "SD13" }
            },
            {
              "id": "2015132",
              "name": "Redfern Station, Platform 2, Redfern",
              "disassembledName": "Redfern Station, Platform 2",
              "departureTimePlanned": "2026-10-26T11:25:00Z",
              "properties": { "occupancy": "STANDING_ONLY" }
            }
          ]
        }
        """
    }
}
