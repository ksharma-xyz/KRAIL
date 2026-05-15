package xyz.ksharma.krail.trip.planner.network.api.mapper

import app.krail.bff.proto.Coord
import app.krail.bff.proto.JourneyCardInfo
import app.krail.bff.proto.JourneyList
import app.krail.bff.proto.Leg
import app.krail.bff.proto.Stop
import app.krail.bff.proto.TransportLeg
import app.krail.bff.proto.TransportModeLine
import app.krail.bff.proto.WalkInterchange
import app.krail.bff.proto.WalkPosition
import app.krail.bff.proto.WalkingLeg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Snapshot-style tests for [journeyListToTripResponse]. We construct a
 * JourneyList programmatically (rather than loading a captured proto
 * fixture) so the test stays platform-agnostic and survives schema bumps
 * that add new optional fields.
 *
 * The polyline assertions are the load-bearing ones: this is the
 * journey-map-blank-route fix.
 */
class JourneyListMapperTest {

    @Test
    fun `Given a journey with a transport leg containing coords When mapped Then leg coords are populated as lat-lon pairs`() {
        // Given
        val journeyList = JourneyList(
            journeys = listOf(
                JourneyCardInfo(
                    origin_time = "11:30am",
                    origin_utc_date_time = "2026-05-10T01:30:00Z",
                    destination_time = "11:42am",
                    destination_utc_date_time = "2026-05-10T01:42:00Z",
                    travel_time = "12 mins",
                    legs = listOf(
                        Leg(
                            transport_leg = TransportLeg(
                                transport_mode_line = TransportModeLine(
                                    line_name = "T1",
                                    transport_mode_type = TRAIN_PRODUCT_CLASS,
                                ),
                                total_duration = "12 mins",
                                stops = listOf(
                                    Stop(
                                        name = "Town Hall, Platform 1",
                                        time = "11:30am",
                                        coord = Coord(lat = -33.873, lon = 151.207),
                                    ),
                                    Stop(
                                        name = "Bondi Junction",
                                        time = "11:42am",
                                        coord = Coord(lat = -33.892, lon = 151.249),
                                    ),
                                ),
                                coords = listOf(
                                    Coord(lat = -33.873, lon = 151.207),
                                    Coord(lat = -33.880, lon = 151.220),
                                    Coord(lat = -33.892, lon = 151.249),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        // When
        val tripResponse = journeyListToTripResponse(journeyList)

        // Then
        val journeys = assertNotNull(tripResponse.journeys, "journeys list should not be null")
        assertEquals(1, journeys.size)

        val legs = assertNotNull(journeys[0].legs, "legs list should not be null")
        assertEquals(1, legs.size)

        val leg = legs[0]
        val coords = assertNotNull(leg.coords, "transit leg coords should not be null")
        assertEquals(3, coords.size, "polyline should preserve all proto Coord points")

        // Order preserved, [lat, lon] shape matches NSW JSON.
        assertEquals(2, coords[0].size, "each coord pair should have exactly 2 elements")
        assertEquals(-33.873, coords[0][0])
        assertEquals(151.207, coords[0][1])
        assertEquals(-33.892, coords[2][0])
        assertEquals(151.249, coords[2][1])
    }

    @Test
    fun `Given a journey with multiple legs When mapped Then origin and destination receive UTC times from the journey card`() {
        // Given
        val journeyList = JourneyList(
            journeys = listOf(
                JourneyCardInfo(
                    origin_time = "11:30am",
                    origin_utc_date_time = "2026-05-10T01:30:00Z",
                    destination_time = "12:10pm",
                    destination_utc_date_time = "2026-05-10T02:10:00Z",
                    travel_time = "40 mins",
                    legs = listOf(
                        Leg(
                            transport_leg = TransportLeg(
                                transport_mode_line = TransportModeLine(
                                    line_name = "T1",
                                    transport_mode_type = TRAIN_PRODUCT_CLASS,
                                ),
                                total_duration = "20 mins",
                                stops = listOf(
                                    Stop(name = "Town Hall", time = "11:30am"),
                                    Stop(name = "Central", time = "11:50am"),
                                ),
                                coords = listOf(Coord(lat = 1.0, lon = 2.0)),
                            ),
                        ),
                        Leg(
                            transport_leg = TransportLeg(
                                transport_mode_line = TransportModeLine(
                                    line_name = "T4",
                                    transport_mode_type = TRAIN_PRODUCT_CLASS,
                                ),
                                total_duration = "20 mins",
                                stops = listOf(
                                    Stop(name = "Central", time = "11:55am"),
                                    Stop(name = "Bondi Junction", time = "12:10pm"),
                                ),
                                coords = listOf(Coord(lat = 3.0, lon = 4.0)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        // When
        val tripResponse = journeyListToTripResponse(journeyList)

        // Then
        val legs = assertNotNull(tripResponse.journeys?.firstOrNull()?.legs)
        assertEquals(2, legs.size)

        // First leg origin gets the journey origin UTC.
        assertEquals("2026-05-10T01:30:00Z", legs.first().origin?.departureTimePlanned)
        assertEquals("2026-05-10T01:30:00Z", legs.first().origin?.departureTimeEstimated)
        // First leg destination has no UTC (intermediate stop).
        assertNull(legs.first().destination?.arrivalTimePlanned)

        // Last leg destination gets the journey destination UTC.
        assertEquals("2026-05-10T02:10:00Z", legs.last().destination?.arrivalTimePlanned)
        assertEquals("2026-05-10T02:10:00Z", legs.last().destination?.arrivalTimeEstimated)
        // Last leg origin has no UTC (interchange).
        assertNull(legs.last().origin?.departureTimePlanned)
    }

    @Test
    fun `Given a stop with coord When mapped Then stop coord becomes a two-element lat-lon array`() {
        // Given
        val journeyList = JourneyList(
            journeys = listOf(
                JourneyCardInfo(
                    origin_time = "10:00am",
                    origin_utc_date_time = "2026-05-10T00:00:00Z",
                    destination_time = "10:10am",
                    destination_utc_date_time = "2026-05-10T00:10:00Z",
                    travel_time = "10 mins",
                    legs = listOf(
                        Leg(
                            transport_leg = TransportLeg(
                                transport_mode_line = TransportModeLine(
                                    line_name = "T1",
                                    transport_mode_type = TRAIN_PRODUCT_CLASS,
                                ),
                                total_duration = "10 mins",
                                stops = listOf(
                                    Stop(
                                        name = "Origin",
                                        time = "10:00am",
                                        coord = Coord(lat = -33.5, lon = 151.0),
                                    ),
                                    Stop(
                                        name = "Destination",
                                        time = "10:10am",
                                        coord = Coord(lat = -33.6, lon = 151.1),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        // When
        val tripResponse = journeyListToTripResponse(journeyList)

        // Then
        val origin = assertNotNull(tripResponse.journeys?.first()?.legs?.first()?.origin)
        val coord = assertNotNull(origin.coord, "stop coord should not be null when proto provides it")
        assertEquals(2, coord.size)
        assertEquals(-33.5, coord[0])
        assertEquals(151.0, coord[1])
    }

    @Test
    fun `Given a walking leg with coords When mapped Then leg coords and walking productClass are populated`() {
        // Given
        val journeyList = JourneyList(
            journeys = listOf(
                JourneyCardInfo(
                    origin_time = "9:00am",
                    origin_utc_date_time = "2026-05-10T23:00:00Z",
                    destination_time = "9:05am",
                    destination_utc_date_time = "2026-05-10T23:05:00Z",
                    travel_time = "5 mins",
                    legs = listOf(
                        Leg(
                            walking_leg = WalkingLeg(
                                duration = "5 mins",
                                coords = listOf(
                                    Coord(lat = -33.870, lon = 151.205),
                                    Coord(lat = -33.871, lon = 151.206),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        // When
        val tripResponse = journeyListToTripResponse(journeyList)

        // Then
        val leg = assertNotNull(tripResponse.journeys?.first()?.legs?.first())
        val coords = assertNotNull(leg.coords, "walking leg coords should not be null")
        assertEquals(2, coords.size)

        // productClass 99 keeps TripResponseExt.isWalkingLeg() true on the proto path.
        assertEquals(WALKING_PRODUCT_CLASS, leg.transportation?.product?.productClass)
    }

    @Test
    fun `Given a transport leg with a walk interchange containing coords When mapped Then interchange coords are propagated`() {
        // Given
        val journeyList = JourneyList(
            journeys = listOf(
                JourneyCardInfo(
                    origin_time = "8:00am",
                    origin_utc_date_time = "2026-05-10T22:00:00Z",
                    destination_time = "8:30am",
                    destination_utc_date_time = "2026-05-10T22:30:00Z",
                    travel_time = "30 mins",
                    legs = listOf(
                        Leg(
                            transport_leg = TransportLeg(
                                transport_mode_line = TransportModeLine(
                                    line_name = "M1",
                                    transport_mode_type = METRO_PRODUCT_CLASS,
                                ),
                                total_duration = "30 mins",
                                stops = listOf(
                                    Stop(name = "A", time = "8:00am"),
                                    Stop(name = "B", time = "8:30am"),
                                ),
                                walk_interchange = WalkInterchange(
                                    duration = "3 mins",
                                    position = WalkPosition.AFTER,
                                    coords = listOf(
                                        Coord(lat = 10.0, lon = 20.0),
                                        Coord(lat = 11.0, lon = 21.0),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        // When
        val tripResponse = journeyListToTripResponse(journeyList)

        // Then
        val interchange = assertNotNull(
            tripResponse.journeys?.first()?.legs?.first()?.interchange,
        )
        val coords = assertNotNull(interchange.coords)
        assertEquals(2, coords.size)
        assertEquals(10.0, coords[0][0])
        assertEquals(20.0, coords[0][1])
    }

    @Test
    fun `Given an empty JourneyList When mapped Then TripResponse has an empty journeys list`() {
        // Given
        val journeyList = JourneyList(journeys = emptyList())

        // When
        val tripResponse = journeyListToTripResponse(journeyList)

        // Then
        val journeys = assertNotNull(tripResponse.journeys)
        assertTrue(journeys.isEmpty())
    }

    @Test
    fun `Given proto bytes round-tripped through ADAPTER decode When mapped Then polyline survives the decode`() {
        // Given
        val original = JourneyList(
            journeys = listOf(
                JourneyCardInfo(
                    origin_time = "1:00pm",
                    origin_utc_date_time = "2026-05-10T03:00:00Z",
                    destination_time = "1:30pm",
                    destination_utc_date_time = "2026-05-10T03:30:00Z",
                    travel_time = "30 mins",
                    legs = listOf(
                        Leg(
                            transport_leg = TransportLeg(
                                transport_mode_line = TransportModeLine(
                                    line_name = "T1",
                                    transport_mode_type = TRAIN_PRODUCT_CLASS,
                                ),
                                total_duration = "30 mins",
                                stops = listOf(
                                    Stop(
                                        name = "Origin",
                                        time = "1:00pm",
                                        coord = Coord(lat = 1.5, lon = 2.5),
                                    ),
                                    Stop(
                                        name = "Destination",
                                        time = "1:30pm",
                                        coord = Coord(lat = 3.5, lon = 4.5),
                                    ),
                                ),
                                coords = listOf(
                                    Coord(lat = 1.5, lon = 2.5),
                                    Coord(lat = 3.5, lon = 4.5),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val bytes = JourneyList.ADAPTER.encode(original)

        // When
        val decoded = JourneyList.ADAPTER.decode(bytes)
        val tripResponse = journeyListToTripResponse(decoded)

        // Then
        val coords = assertNotNull(
            tripResponse.journeys?.first()?.legs?.first()?.coords,
        )
        assertEquals(2, coords.size)
        assertEquals(1.5, coords[0][0])
        assertEquals(2.5, coords[0][1])
    }

    private companion object {
        // Mirrors NSW productClass enumeration.
        // 1 = Train, 2 = Metro, 99 = Walking.
        const val TRAIN_PRODUCT_CLASS = 1
        const val METRO_PRODUCT_CLASS = 2
        const val WALKING_PRODUCT_CLASS = 99L
    }
}
