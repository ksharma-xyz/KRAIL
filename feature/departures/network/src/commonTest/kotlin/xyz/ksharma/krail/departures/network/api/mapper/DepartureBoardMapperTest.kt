package xyz.ksharma.krail.departures.network.api.mapper

import app.krail.bff.proto.Coord
import app.krail.bff.proto.DepartureBoardResponse
import app.krail.bff.proto.DepartureRow
import app.krail.bff.proto.StopRef
import app.krail.bff.proto.StopTime
import app.krail.bff.proto.TransitLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Snapshot-style tests for [toDepartureMonitorResponse]. We construct a
 * [DepartureBoardResponse] programmatically (rather than loading a captured
 * proto fixture) so the test stays platform-agnostic and survives schema
 * bumps that add new optional fields.
 */
class DepartureBoardMapperTest {

    @Test
    fun `Given an empty departures list When mapped Then stopEvents is empty`() {
        // Given
        val response = DepartureBoardResponse(
            stop = StopRef(id = "200060", name = "Town Hall Station"),
            departures = emptyList(),
        )

        // When
        val mapped = response.toDepartureMonitorResponse()

        // Then
        val events = assertNotNull(mapped.stopEvents)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `Given a row with realtime estimate When mapped Then both planned and estimated UTCs are populated`() {
        // Given
        val response = DepartureBoardResponse(
            stop = StopRef(id = "200060", name = "Town Hall Station"),
            departures = listOf(
                DepartureRow(
                    line = TransitLine(
                        display_name = "T1",
                        transport_mode_type = TRAIN_PRODUCT_CLASS,
                    ),
                    destination = "Bondi Junction",
                    time = StopTime(
                        planned_utc = "2026-05-11T01:30:00Z",
                        estimated_utc = "2026-05-11T01:32:00Z",
                    ),
                    is_realtime = true,
                ),
            ),
        )

        // When
        val mapped = response.toDepartureMonitorResponse()

        // Then
        val event = assertNotNull(mapped.stopEvents?.firstOrNull())
        assertEquals("2026-05-11T01:30:00Z", event.departureTimePlanned)
        assertEquals("2026-05-11T01:32:00Z", event.departureTimeEstimated)
    }

    @Test
    fun `Given a row without estimated_utc When mapped Then only planned is populated`() {
        // Given
        val response = DepartureBoardResponse(
            stop = StopRef(id = "200060", name = "Town Hall Station"),
            departures = listOf(
                DepartureRow(
                    line = TransitLine(
                        display_name = "T1",
                        transport_mode_type = TRAIN_PRODUCT_CLASS,
                    ),
                    destination = "Bondi Junction",
                    time = StopTime(planned_utc = "2026-05-11T01:30:00Z"),
                    is_realtime = false,
                ),
            ),
        )

        // When
        val mapped = response.toDepartureMonitorResponse()

        // Then
        val event = assertNotNull(mapped.stopEvents?.firstOrNull())
        assertEquals("2026-05-11T01:30:00Z", event.departureTimePlanned)
        assertNull(event.departureTimeEstimated)
    }

    @Test
    fun `Given a row with platform_text When mapped Then location disassembledName carries the platform`() {
        // Given
        val response = DepartureBoardResponse(
            stop = StopRef(id = "200060", name = "Town Hall Station"),
            departures = listOf(
                DepartureRow(
                    line = TransitLine(
                        display_name = "T1",
                        transport_mode_type = TRAIN_PRODUCT_CLASS,
                    ),
                    destination = "Bondi Junction",
                    time = StopTime(planned_utc = "2026-05-11T01:30:00Z"),
                    platform_text = "Platform 1",
                ),
            ),
        )

        // When
        val mapped = response.toDepartureMonitorResponse()

        // Then
        val location = assertNotNull(mapped.stopEvents?.firstOrNull()?.location)
        assertEquals("Platform 1", location.disassembledName)
        assertEquals("200060", location.id)
        assertEquals("Town Hall Station", location.name)
    }

    @Test
    fun `Given transport_mode_type When mapped Then product cls round-trips`() {
        // Given
        val response = DepartureBoardResponse(
            stop = StopRef(id = "200060", name = "Town Hall Station"),
            departures = listOf(
                DepartureRow(
                    line = TransitLine(
                        display_name = "M1",
                        transport_mode_type = METRO_PRODUCT_CLASS,
                    ),
                    destination = "Tallawong",
                    time = StopTime(planned_utc = "2026-05-11T01:30:00Z"),
                ),
            ),
        )

        // When
        val mapped = response.toDepartureMonitorResponse()

        // Then
        val transportation = assertNotNull(
            mapped.stopEvents?.firstOrNull()?.transportation,
        )
        assertEquals(METRO_PRODUCT_CLASS, transportation.product?.cls)
        assertEquals("M1", transportation.disassembledName)
        assertEquals("Tallawong", transportation.destination?.name)
    }

    @Test
    fun `Given a stop ref When mapped Then every stop event carries the same location id and name`() {
        // Given
        val response = DepartureBoardResponse(
            stop = StopRef(
                id = "200060",
                name = "Town Hall Station",
                coord = Coord(lat = -33.873, lon = 151.207),
            ),
            departures = listOf(
                DepartureRow(
                    line = TransitLine(
                        display_name = "T1",
                        transport_mode_type = TRAIN_PRODUCT_CLASS,
                    ),
                    destination = "Bondi Junction",
                    time = StopTime(planned_utc = "2026-05-11T01:30:00Z"),
                ),
                DepartureRow(
                    line = TransitLine(
                        display_name = "T4",
                        transport_mode_type = TRAIN_PRODUCT_CLASS,
                    ),
                    destination = "Cronulla",
                    time = StopTime(planned_utc = "2026-05-11T01:33:00Z"),
                ),
            ),
        )

        // When
        val mapped = response.toDepartureMonitorResponse()

        // Then
        val events = assertNotNull(mapped.stopEvents)
        assertEquals(2, events.size)
        events.forEach { event ->
            assertEquals("200060", event.location?.id)
            assertEquals("Town Hall Station", event.location?.name)
        }
    }

    @Test
    fun `Given proto bytes round-tripped through ADAPTER decode When mapped Then shape survives the decode`() {
        // Given
        val original = DepartureBoardResponse(
            stop = StopRef(id = "200060", name = "Town Hall Station"),
            departures = listOf(
                DepartureRow(
                    line = TransitLine(
                        display_name = "T1",
                        transport_mode_type = TRAIN_PRODUCT_CLASS,
                    ),
                    destination = "Bondi Junction",
                    time = StopTime(
                        planned_utc = "2026-05-11T01:30:00Z",
                        estimated_utc = "2026-05-11T01:32:00Z",
                    ),
                    platform_text = "Platform 1",
                    is_realtime = true,
                ),
            ),
        )
        val bytes = DepartureBoardResponse.ADAPTER.encode(original)

        // When
        val decoded = DepartureBoardResponse.ADAPTER.decode(bytes)
        val mapped = decoded.toDepartureMonitorResponse()

        // Then
        val event = assertNotNull(mapped.stopEvents?.firstOrNull())
        val transportation = assertNotNull(event.transportation)
        assertEquals("T1", transportation.disassembledName)
        assertEquals("Bondi Junction", transportation.destination?.name)
        assertEquals("Platform 1", event.location?.disassembledName)
        assertEquals("2026-05-11T01:30:00Z", event.departureTimePlanned)
        assertEquals("2026-05-11T01:32:00Z", event.departureTimeEstimated)
    }

    private companion object {
        // Mirrors NSW productClass enumeration.
        // 1 = Train, 2 = Metro.
        const val TRAIN_PRODUCT_CLASS = 1
        const val METRO_PRODUCT_CLASS = 2
    }
}
