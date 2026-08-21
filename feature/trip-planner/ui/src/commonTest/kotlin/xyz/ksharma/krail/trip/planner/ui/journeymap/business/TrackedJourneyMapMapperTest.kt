package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.feature.track.TrackedJourneyDisplay
import xyz.ksharma.krail.feature.track.TrackedLeg
import xyz.ksharma.krail.feature.track.TrackedStop
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.TrackedJourneyMapMapper.toJourneyMapState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.RouteSegment
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `TrackedJourneyMapMapper` is the live-tracking counterpart of `JourneyMapMapper`. It is a pure
 * function over a `TrackedJourneyDisplay` plus a caller-supplied coordinate lookup, so the
 * behaviours that matter are: which legs are drawn at all, how a stop with no known coordinate is
 * handled, and how the ORIGIN / INTERCHANGE / DESTINATION roles fall out across several legs.
 */
class TrackedJourneyMapMapperTest {

    @Test
    fun `GIVEN a walk leg between two transport legs WHEN mapped THEN only transport legs are drawn`() {
        val display = display(
            transportLeg(lineName = "T1", stops = listOf(stop("a"), stop("b"))),
            TrackedLeg.Walk(durationText = "4 mins"),
            transportLeg(lineName = "T4", stops = listOf(stop("c"), stop("d"))),
        )

        val state = display.toJourneyMapState(coordinates("a", "b", "c", "d"))

        assertEquals(listOf("tracked_leg_0", "tracked_leg_1"), state.mapDisplay.legs.map { it.legId })
        assertEquals(listOf("T1", "T4"), state.mapDisplay.legs.map { it.lineName })
    }

    @Test
    fun `GIVEN a known line key WHEN mapped THEN the brand colour overrides the API colour code`() {
        val display = display(
            transportLeg(lineName = "T1", lineColorCode = "#000000", stops = listOf(stop("a"), stop("b"))),
        )

        val legFeature = display.toJourneyMapState(coordinates("a", "b")).mapDisplay.legs.single()

        assertEquals("#F99D1C", legFeature.lineColor)
    }

    @Test
    fun `GIVEN a line key that is not in the catalogue WHEN mapped THEN the API colour code is kept`() {
        val display = display(
            transportLeg(
                lineName = "333",
                transportMode = TransportMode.Bus,
                lineColorCode = "#123456",
                stops = listOf(stop("a"), stop("b")),
            ),
        )

        val legFeature = display.toJourneyMapState(coordinates("a", "b")).mapDisplay.legs.single()

        assertEquals("#123456", legFeature.lineColor)
    }

    @Test
    fun `GIVEN route path coordinates WHEN mapped THEN the curved path is drawn instead of stop connectors`() {
        val path = persistentListOf(
            LatLng(latitude = -33.80, longitude = 151.20),
            LatLng(latitude = -33.85, longitude = 151.25),
        )
        val display = display(
            transportLeg(lineName = "T1", stops = listOf(stop("a"), stop("b")), routePath = path),
        )

        val state = display.toJourneyMapState(coordinates("a", "b"))
        val segment = state.mapDisplay.legs.single().routeSegment

        assertEquals(path, assertIs<RouteSegment.PathSegment>(segment).points)
    }

    @Test
    fun `GIVEN no route path WHEN mapped THEN the leg falls back to connecting its stops`() {
        val display = display(transportLeg(lineName = "T1", stops = listOf(stop("a"), stop("b"))))

        val state = display.toJourneyMapState(coordinates("a", "b"))
        val segment = state.mapDisplay.legs.single().routeSegment

        assertEquals(listOf("a", "b"), assertIs<RouteSegment.StopConnectorSegment>(segment).stops.map { it.stopId })
    }

    @Test
    fun `GIVEN a stop with no known coordinate WHEN mapped THEN it is skipped and the rest still render`() {
        val display = display(
            transportLeg(lineName = "T1", stops = listOf(stop("a"), stop("missing"), stop("b"))),
        )

        val stops = display.toJourneyMapState(coordinates("a", "b")).mapDisplay.stops

        assertEquals(listOf("a", "b"), stops.map { it.stopId })
    }

    @Test
    fun `GIVEN three stops on one leg WHEN mapped THEN the middle stop is regular and the ends are terminals`() {
        val display = display(
            transportLeg(lineName = "T1", stops = listOf(stop("a"), stop("mid"), stop("b"))),
        )

        val stops = display.toJourneyMapState(coordinates("a", "mid", "b")).mapDisplay.stops

        assertEquals(
            listOf(StopType.ORIGIN, StopType.REGULAR, StopType.DESTINATION),
            stops.map { it.stopType },
        )
    }

    @Test
    fun `GIVEN two transport legs WHEN mapped THEN the changeover stops are interchanges, not terminals`() {
        val display = display(
            transportLeg(lineName = "T1", stops = listOf(stop("a"), stop("swap"))),
            transportLeg(lineName = "T4", stops = listOf(stop("swap"), stop("b"))),
        )

        val stops = display.toJourneyMapState(coordinates("a", "swap", "b")).mapDisplay.stops

        assertEquals(
            listOf(
                "a" to StopType.ORIGIN,
                "swap" to StopType.INTERCHANGE,
                "swap" to StopType.INTERCHANGE,
                "b" to StopType.DESTINATION,
            ),
            stops.map { it.stopId to it.stopType },
        )
    }

    @Test
    fun `GIVEN a tracked stop WHEN mapped THEN it carries its leg's line and its own departure time`() {
        val display = display(
            transportLeg(
                lineName = "T1",
                stops = listOf(stop("a", utcTime = "2026-04-09T02:00:00Z"), stop("b")),
            ),
        )

        val stop = display.toJourneyMapState(coordinates("a", "b")).mapDisplay.stops.first()

        assertEquals("T1", stop.lineName)
        assertEquals("#F99D1C", stop.lineColor)
        assertEquals("2026-04-09T02:00:00Z", stop.departureTime)
    }

    @Test
    fun `GIVEN a journey with only a walk leg WHEN mapped THEN nothing is drawn and there is no camera focus`() {
        val display = display(TrackedLeg.Walk(durationText = "4 mins"))

        val state = display.toJourneyMapState(emptyMap())

        assertTrue(state.mapDisplay.legs.isEmpty())
        assertTrue(state.mapDisplay.stops.isEmpty())
        assertNull(state.cameraFocus)
    }

    @Test
    fun `GIVEN stops spread across the network WHEN mapped THEN camera bounds enclose every stop`() {
        val display = display(transportLeg(lineName = "T1", stops = listOf(stop("a"), stop("b"))))
        val coords = mapOf(
            "a" to LatLng(latitude = -33.95, longitude = 151.10),
            "b" to LatLng(latitude = -33.80, longitude = 151.35),
        )

        val bounds = display.toJourneyMapState(coords).cameraFocus?.bounds

        assertEquals(LatLng(latitude = -33.95, longitude = 151.10), bounds?.southwest)
        assertEquals(LatLng(latitude = -33.80, longitude = 151.35), bounds?.northeast)
    }

    // region fixtures

    private fun display(vararg legs: TrackedLeg) = TrackedJourneyDisplay(
        fromStopId = "a",
        toStopId = "b",
        fromStopName = "A",
        toStopName = "B",
        originTime = "12:00pm",
        scheduledOriginTime = null,
        destinationTime = "12:30pm",
        originUtcDateTime = "2026-04-09T02:00:00Z",
        destinationUtcDateTime = "2026-04-09T02:30:00Z",
        travelTime = "30 mins",
        legs = legs.toList().toImmutableList(),
    )

    private fun transportLeg(
        lineName: String,
        stops: List<TrackedStop>,
        transportMode: TransportMode = TransportMode.Train,
        lineColorCode: String = "#FFFFFF",
        routePath: List<LatLng> = emptyList(),
    ) = TrackedLeg.Transport(
        transportMode = transportMode,
        lineName = lineName,
        lineColorCode = lineColorCode,
        headsign = null,
        stops = stops.toImmutableList(),
        routePathCoordinates = routePath.toImmutableList(),
    )

    private fun stop(stopId: String, utcTime: String = "2026-04-09T02:00:00Z") = TrackedStop(
        stopId = stopId,
        name = stopId.uppercase(),
        scheduledTime = "12:00pm",
        estimatedTime = null,
        utcTime = utcTime,
        scheduledUtcTime = utcTime,
    )

    private fun coordinates(vararg stopIds: String): Map<String, LatLng> =
        stopIds.mapIndexed { index, id ->
            id to LatLng(latitude = -33.8 - index * 0.01, longitude = 151.2 + index * 0.01)
        }.toMap()

    // endregion
}
