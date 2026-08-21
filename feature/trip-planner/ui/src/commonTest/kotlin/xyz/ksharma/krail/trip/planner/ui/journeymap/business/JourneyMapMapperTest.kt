package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyMapMapper.toJourneyMapState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.RouteSegment
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `JourneyMapMapper` turns an NSW `TripResponse.Journey` into the platform-agnostic map state.
 *
 * The behaviours worth pinning are the ones a wrong branch silently breaks on a rendered map:
 * which of the three geometry sources a leg picks, the (latitude, longitude) ordering the NSW
 * API uses, which line colour a leg inherits, and which stop gets marked origin / interchange /
 * destination.
 */
class JourneyMapMapperTest {

    private val trainT1 = transportation(productClass = 1, disassembledName = "T1")

    // region geometry source precedence

    @Test
    fun `GIVEN a leg with coords WHEN mapped THEN a path segment keeps NSW lat-lon ordering`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    coords = listOf(listOf(-33.8, 151.2), listOf(-33.9, 151.3)),
                    transportation = trainT1,
                ),
            ),
        )

        val state = journey.toJourneyMapState()

        val segment = assertIs<RouteSegment.PathSegment>(state.mapDisplay.legs.single().routeSegment)
        assertEquals(
            listOf(
                LatLng(latitude = -33.8, longitude = 151.2),
                LatLng(latitude = -33.9, longitude = 151.3),
            ),
            segment.points,
        )
    }

    @Test
    fun `GIVEN coords contains a malformed pair WHEN mapped THEN only complete pairs survive`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    coords = listOf(listOf(-33.8), listOf(-33.9, 151.3)),
                    transportation = trainT1,
                ),
            ),
        )

        val state = journey.toJourneyMapState()

        val segment = assertIs<RouteSegment.PathSegment>(state.mapDisplay.legs.single().routeSegment)
        assertEquals(listOf(LatLng(latitude = -33.9, longitude = 151.3)), segment.points)
    }

    @Test
    fun `GIVEN no coords but an interchange path WHEN mapped THEN the leg is drawn as a walk`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    interchange = TripResponse.Interchange(
                        coords = listOf(listOf(-33.8, 151.2), listOf(-33.81, 151.21)),
                    ),
                    transportation = trainT1,
                ),
            ),
        )

        val legFeature = journey.toJourneyMapState().mapDisplay.legs.single()

        // The interchange branch deliberately drops the transport identity: it is a footpath
        // between two services, not the service itself.
        assertNull(legFeature.transportMode)
        assertNull(legFeature.lineName)
        val segment = assertIs<RouteSegment.PathSegment>(legFeature.routeSegment)
        assertEquals(2, segment.points.size)
    }

    @Test
    fun `GIVEN neither coords nor interchange WHEN the leg has stops THEN stops are connected`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    transportation = trainT1,
                    stopSequence = listOf(
                        stopSequence(id = "a", lat = -33.8, lon = 151.2),
                        stopSequence(id = "b", lat = -33.9, lon = 151.3),
                    ),
                ),
            ),
        )

        val legFeature = journey.toJourneyMapState().mapDisplay.legs.single()

        val segment = assertIs<RouteSegment.StopConnectorSegment>(legFeature.routeSegment)
        assertEquals(listOf("a", "b"), segment.stops.map { it.stopId })
        assertEquals(TransportMode.Train, legFeature.transportMode)
    }

    @Test
    fun `GIVEN a leg with no geometry at all WHEN mapped THEN it is dropped rather than empty-drawn`() {
        val journey = TripResponse.Journey(legs = listOf(leg(transportation = trainT1)))

        val state = journey.toJourneyMapState()

        assertTrue(state.mapDisplay.legs.isEmpty())
        assertNull(state.cameraFocus)
    }

    // endregion

    // region line colour

    @Test
    fun `GIVEN a known line key WHEN mapped THEN the line colour wins over the mode colour`() {
        val journey = TripResponse.Journey(
            legs = listOf(leg(coords = listOf(listOf(-33.8, 151.2)), transportation = trainT1)),
        )

        val legFeature = journey.toJourneyMapState().mapDisplay.legs.single()

        assertEquals("#F99D1C", legFeature.lineColor)
    }

    @Test
    fun `GIVEN an unknown line key WHEN mapped THEN the leg falls back to the mode colour`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    coords = listOf(listOf(-33.8, 151.2)),
                    transportation = transportation(productClass = 5, disassembledName = "333"),
                ),
            ),
        )

        val legFeature = journey.toJourneyMapState().mapDisplay.legs.single()

        assertEquals(TransportMode.Bus.colorCode, legFeature.lineColor)
    }

    @Test
    fun `GIVEN a walking leg WHEN mapped THEN it gets the walking grey, not a mode colour`() {
        val journey = TripResponse.Journey(
            legs = listOf(leg(coords = listOf(listOf(-33.8, 151.2)))),
        )

        val legFeature = journey.toJourneyMapState().mapDisplay.legs.single()

        assertNull(legFeature.transportMode)
        assertEquals("#757575", legFeature.lineColor)
    }

    @Test
    fun `GIVEN an unrecognised product class WHEN mapped THEN the leg is treated as walking`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    coords = listOf(listOf(-33.8, 151.2)),
                    transportation = transportation(productClass = 99, disassembledName = "walk"),
                ),
            ),
        )

        val legFeature = journey.toJourneyMapState().mapDisplay.legs.single()

        assertNull(legFeature.transportMode)
        assertEquals("#757575", legFeature.lineColor)
    }

    // endregion

    // region stop roles

    @Test
    fun `GIVEN a two-leg journey WHEN mapped THEN origin, interchange and destination are marked`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    transportation = trainT1,
                    origin = stopSequence(id = "origin", lat = -33.80, lon = 151.20),
                    destination = stopSequence(id = "swap", lat = -33.85, lon = 151.25),
                    stopSequence = listOf(
                        stopSequence(id = "origin", lat = -33.80, lon = 151.20),
                        stopSequence(id = "mid", lat = -33.82, lon = 151.22),
                        stopSequence(id = "swap", lat = -33.85, lon = 151.25),
                    ),
                ),
                leg(
                    transportation = transportation(productClass = 5, disassembledName = "333"),
                    origin = stopSequence(id = "swap", lat = -33.85, lon = 151.25),
                    destination = stopSequence(id = "end", lat = -33.90, lon = 151.30),
                    stopSequence = listOf(
                        stopSequence(id = "swap", lat = -33.85, lon = 151.25),
                        stopSequence(id = "end", lat = -33.90, lon = 151.30),
                    ),
                ),
            ),
        )

        val stops = journey.toJourneyMapState().mapDisplay.stops

        assertEquals(
            listOf("origin" to StopType.ORIGIN, "mid" to StopType.REGULAR, "swap" to StopType.INTERCHANGE),
            stops.take(3).map { it.stopId to it.stopType },
        )
        assertEquals("end" to StopType.DESTINATION, stops.last().let { it.stopId to it.stopType })
    }

    @Test
    fun `GIVEN an interchange stop WHEN mapped THEN it carries the NEXT leg's line, not the arriving one`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    transportation = trainT1,
                    origin = stopSequence(id = "origin", lat = -33.80, lon = 151.20),
                    destination = stopSequence(id = "swap", lat = -33.85, lon = 151.25),
                ),
                leg(
                    transportation = transportation(productClass = 1, disassembledName = "T4"),
                    destination = stopSequence(id = "end", lat = -33.90, lon = 151.30),
                ),
            ),
        )

        val swap = journey.toJourneyMapState().mapDisplay.stops.single { it.stopId == "swap" }

        assertEquals("T4", swap.lineName)
        assertEquals("#005AA3", swap.lineColor)
    }

    @Test
    fun `GIVEN a stop with no coordinates WHEN mapped THEN it is skipped instead of landing at zero`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    transportation = trainT1,
                    origin = stopSequence(id = "origin"),
                    destination = stopSequence(id = "end", lat = -33.90, lon = 151.30),
                ),
            ),
        )

        val stops = journey.toJourneyMapState().mapDisplay.stops

        assertEquals(listOf("end"), stops.map { it.stopId })
    }

    @Test
    fun `GIVEN a platform stop WHEN mapped THEN the parent station coordinates are used`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    transportation = trainT1,
                    origin = stopSequence(id = "platform", parentLat = -33.88, parentLon = 151.21),
                ),
            ),
        )

        val stop = journey.toJourneyMapState().mapDisplay.stops.single()

        assertEquals(LatLng(latitude = -33.88, longitude = 151.21), stop.position)
    }

    // endregion

    // region stop field fallbacks

    @Test
    fun `GIVEN a stop with no short name WHEN mapped THEN the long name is shown before Unknown Stop`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    transportation = trainT1,
                    origin = stopSequence(
                        id = null,
                        disassembledName = null,
                        name = "Central Station, Platform 16",
                        lat = -33.88,
                        lon = 151.21,
                    ),
                ),
            ),
        )

        val stop = journey.toJourneyMapState().mapDisplay.stops.single()

        assertEquals("Central Station, Platform 16", stop.stopName)
        assertEquals("", stop.stopId)
    }

    @Test
    fun `GIVEN a stop with neither name WHEN mapped THEN it reads Unknown Stop`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    transportation = trainT1,
                    origin = stopSequence(name = null, disassembledName = null, lat = -33.88, lon = 151.21),
                ),
            ),
        )

        assertEquals("Unknown Stop", journey.toJourneyMapState().mapDisplay.stops.single().stopName)
    }

    @Test
    fun `GIVEN a stop with only a platform name WHEN mapped THEN platformName is the fallback`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    transportation = trainT1,
                    origin = stopSequence(platformName = "Platform 16", lat = -33.88, lon = 151.21),
                ),
            ),
        )

        assertEquals("Platform 16", journey.toJourneyMapState().mapDisplay.stops.single().platform)
    }

    @Test
    fun `GIVEN both planned and estimated times WHEN mapped THEN the realtime estimate wins`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    transportation = trainT1,
                    origin = stopSequence(
                        lat = -33.88,
                        lon = 151.21,
                        arrivalTimePlanned = "2024-09-24T19:00:00Z",
                        arrivalTimeEstimated = "2024-09-24T19:04:00Z",
                        departureTimePlanned = "2024-09-24T19:02:00Z",
                        departureTimeEstimated = "2024-09-24T19:06:00Z",
                    ),
                ),
            ),
        )

        val stop = journey.toJourneyMapState().mapDisplay.stops.single()

        assertEquals("2024-09-24T19:04:00Z", stop.arrivalTime)
        assertEquals("2024-09-24T19:06:00Z", stop.departureTime)
    }

    // endregion

    // region camera

    @Test
    fun `GIVEN legs spanning a corridor WHEN mapped THEN the camera bounds enclose every point`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                leg(
                    coords = listOf(listOf(-33.95, 151.10), listOf(-33.80, 151.35)),
                    transportation = trainT1,
                ),
            ),
        )

        val bounds = journey.toJourneyMapState().cameraFocus?.bounds

        assertEquals(LatLng(latitude = -33.95, longitude = 151.10), bounds?.southwest)
        assertEquals(LatLng(latitude = -33.80, longitude = 151.35), bounds?.northeast)
    }

    @Test
    fun `GIVEN a journey with no legs WHEN mapped THEN the state is empty and has no camera focus`() {
        val state = TripResponse.Journey(legs = null).toJourneyMapState()

        assertTrue(state.mapDisplay.legs.isEmpty())
        assertTrue(state.mapDisplay.stops.isEmpty())
        assertNull(state.cameraFocus)
    }

    // endregion
}
