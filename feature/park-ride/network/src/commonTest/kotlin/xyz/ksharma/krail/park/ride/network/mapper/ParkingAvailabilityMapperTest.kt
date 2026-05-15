package xyz.ksharma.krail.park.ride.network.mapper

import app.krail.bff.proto.ApiError
import app.krail.bff.proto.Coord
import app.krail.bff.proto.FacilityAvailability
import app.krail.bff.proto.ParkingAvailabilityResponse
import app.krail.bff.proto.StopParkingBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Snapshot-style tests for [toStopBatchResponse]. We construct a
 * [ParkingAvailabilityResponse] programmatically (rather than loading a
 * captured proto fixture) so the test stays platform-agnostic and
 * survives schema bumps that add new optional fields.
 */
class ParkingAvailabilityMapperTest {

    @Test
    fun `Given an empty stops map When mapped Then batch response stops is empty`() {
        // Given
        val response = ParkingAvailabilityResponse(
            stops = emptyMap(),
            correlation_id = "abc-123",
        )

        // When
        val mapped = response.toStopBatchResponse()

        // Then
        assertTrue(mapped.stops.isEmpty())
        assertEquals("abc-123", mapped.correlationId)
    }

    @Test
    fun `Given a single stop with one facility When mapped Then nested structure is preserved`() {
        // Given
        val response = ParkingAvailabilityResponse(
            stops = mapOf(
                "275010" to StopParkingBlock(
                    facilities = mapOf(
                        "486" to FacilityAvailability(
                            facility_id = "486",
                            facility_name = "Park&Ride - Ashfield",
                            total_spots = 100,
                            occupied_spots = 42,
                            updated_at = "2026-05-11T01:00:00Z",
                        ),
                    ),
                ),
            ),
            correlation_id = "req-1",
        )

        // When
        val mapped = response.toStopBatchResponse()

        // Then
        assertEquals(1, mapped.stops.size)
        val stopBlock = assertNotNull(mapped.stops["275010"])
        assertEquals(1, stopBlock.facilities.size)
        val facility = assertNotNull(stopBlock.facilities["486"])
        assertEquals("486", facility.facilityId)
        assertEquals("Park&Ride - Ashfield", facility.facilityName)
        assertEquals("2026-05-11T01:00:00Z", facility.messageDate)
    }

    @Test
    fun `Given multiple stops with multiple facilities When mapped Then structure is preserved`() {
        // Given
        val response = ParkingAvailabilityResponse(
            stops = mapOf(
                "275010" to StopParkingBlock(
                    facilities = mapOf(
                        "486" to FacilityAvailability(
                            facility_id = "486",
                            facility_name = "Ashfield A",
                            total_spots = 50,
                            occupied_spots = 10,
                        ),
                        "487" to FacilityAvailability(
                            facility_id = "487",
                            facility_name = "Ashfield B",
                            total_spots = 80,
                            occupied_spots = 20,
                        ),
                    ),
                ),
                "2155384" to StopParkingBlock(
                    facilities = mapOf(
                        "488" to FacilityAvailability(
                            facility_id = "488",
                            facility_name = "Seven Hills",
                            total_spots = 200,
                            occupied_spots = 150,
                        ),
                    ),
                ),
            ),
        )

        // When
        val mapped = response.toStopBatchResponse()

        // Then
        assertEquals(2, mapped.stops.size)
        assertEquals(2, mapped.stops["275010"]?.facilities?.size)
        assertEquals(1, mapped.stops["2155384"]?.facilities?.size)
        assertEquals("Seven Hills", mapped.stops["2155384"]?.facilities?.get("488")?.facilityName)
    }

    @Test
    fun `Given numeric total_spots and occupied_spots When mapped Then they become Strings to match NSW quirk`() {
        // Given
        val response = ParkingAvailabilityResponse(
            stops = mapOf(
                "275010" to StopParkingBlock(
                    facilities = mapOf(
                        "486" to FacilityAvailability(
                            facility_id = "486",
                            facility_name = "Ashfield",
                            total_spots = 123,
                            occupied_spots = 45,
                        ),
                    ),
                ),
            ),
        )

        // When
        val mapped = response.toStopBatchResponse()

        // Then
        val facility = assertNotNull(mapped.stops["275010"]?.facilities?.get("486"))
        assertEquals("123", facility.spots)
        assertEquals("45", facility.occupancy.total)
    }

    @Test
    fun `Given a Coord location When mapped Then lat and lon become String latitude and longitude`() {
        // Given
        val response = ParkingAvailabilityResponse(
            stops = mapOf(
                "275010" to StopParkingBlock(
                    facilities = mapOf(
                        "486" to FacilityAvailability(
                            facility_id = "486",
                            facility_name = "Ashfield",
                            total_spots = 100,
                            occupied_spots = 0,
                            location = Coord(lat = -33.887, lon = 151.125),
                            suburb = "Ashfield",
                            address = "Knox St",
                        ),
                    ),
                ),
            ),
        )

        // When
        val mapped = response.toStopBatchResponse()

        // Then
        val facility = assertNotNull(mapped.stops["275010"]?.facilities?.get("486"))
        assertEquals("-33.887", facility.location.latitude)
        assertEquals("151.125", facility.location.longitude)
        assertEquals("Ashfield", facility.location.suburb)
        assertEquals("Knox St", facility.location.address)
    }

    @Test
    fun `Given errors map When mapped Then BatchError carries code and message`() {
        // Given
        val response = ParkingAvailabilityResponse(
            stops = mapOf(
                "275010" to StopParkingBlock(
                    facilities = emptyMap(),
                    errors = mapOf(
                        "486" to ApiError(
                            code = "upstream_404",
                            message = "facility not found",
                        ),
                        "487" to ApiError(
                            code = "upstream_429",
                            message = "rate limited",
                        ),
                    ),
                ),
            ),
        )

        // When
        val mapped = response.toStopBatchResponse()

        // Then
        val errors = assertNotNull(mapped.stops["275010"]?.errors)
        assertEquals(2, errors.size)
        assertEquals("upstream_404", errors["486"]?.code)
        assertEquals("facility not found", errors["486"]?.message)
        assertEquals("upstream_429", errors["487"]?.code)
    }

    @Test
    fun `Given unknown_stops list When mapped Then it propagates`() {
        // Given
        val response = ParkingAvailabilityResponse(
            stops = emptyMap(),
            unknown_stops = listOf("999000", "999001"),
            correlation_id = "req-2",
        )

        // When
        val mapped = response.toStopBatchResponse()

        // Then
        assertEquals(listOf("999000", "999001"), mapped.unknownStops)
    }

    @Test
    fun `Given a missing correlation_id When mapped Then correlationId is null`() {
        // Given
        val response = ParkingAvailabilityResponse(
            stops = emptyMap(),
            correlation_id = "",
        )

        // When
        val mapped = response.toStopBatchResponse()

        // Then
        assertNull(mapped.correlationId)
    }

    @Test
    fun `Given proto bytes round-tripped through ADAPTER decode When mapped Then shape survives`() {
        // Given
        val original = ParkingAvailabilityResponse(
            stops = mapOf(
                "275010" to StopParkingBlock(
                    facilities = mapOf(
                        "486" to FacilityAvailability(
                            facility_id = "486",
                            facility_name = "Ashfield",
                            total_spots = 100,
                            occupied_spots = 50,
                            location = Coord(lat = -33.887, lon = 151.125),
                            suburb = "Ashfield",
                            updated_at = "2026-05-11T01:00:00Z",
                        ),
                    ),
                ),
            ),
            correlation_id = "rt-1",
        )
        val bytes = ParkingAvailabilityResponse.ADAPTER.encode(original)

        // When
        val decoded = ParkingAvailabilityResponse.ADAPTER.decode(bytes)
        val mapped = decoded.toStopBatchResponse()

        // Then
        assertEquals("rt-1", mapped.correlationId)
        val facility = assertNotNull(mapped.stops["275010"]?.facilities?.get("486"))
        assertEquals("100", facility.spots)
        assertEquals("50", facility.occupancy.total)
        assertEquals("-33.887", facility.location.latitude)
        assertEquals("Ashfield", facility.location.suburb)
        assertEquals("2026-05-11T01:00:00Z", facility.messageDate)
    }
}
