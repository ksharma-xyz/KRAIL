package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyMapFeatureMapper.toFeatureCollection
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyLegFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.RouteSegment
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `JourneyMapFeatureMapper` is the last hop before MapLibre: it turns the platform-agnostic journey
 * state into GeoJSON. Two things here are only ever wrong at runtime — the (longitude, latitude)
 * flip GeoJSON requires against the NSW API's (latitude, longitude), and the property keys the
 * layer filters match on.
 */
class JourneyMapFeatureMapperTest {

    @Test
    fun `GIVEN a path leg WHEN converted THEN coordinates are flipped into GeoJSON lon-lat order`() {
        val state = ready(
            legs = listOf(
                pathLeg(
                    points = listOf(
                        LatLng(latitude = -33.87, longitude = 151.21),
                        LatLng(latitude = -33.88, longitude = 151.22),
                    ),
                ),
            ),
        )

        val geometry = assertIs<LineString>(state.toFeatureCollection().features.single().geometry)

        val position = geometry.coordinates.first()
        assertEquals(151.21, position.longitude)
        assertEquals(-33.87, position.latitude)
    }

    @Test
    fun `GIVEN a transit path leg WHEN converted THEN the layer-filter properties are all present`() {
        val state = ready(legs = listOf(pathLeg(lineName = "T1", lineColor = "#F99D1C")))

        val props = state.toFeatureCollection().features.single().props()

        assertEquals("journey_leg", props.str("type"))
        assertEquals("leg_0", props.str("legId"))
        assertEquals("#F99D1C", props.str("color"))
        assertEquals(false, props.bool("isWalking"))
        assertEquals("T1", props.str("lineName"))
        assertEquals(TransportMode.Train.productClass, props.int("modeType"))
    }

    @Test
    fun `GIVEN a walking leg with no line name WHEN converted THEN it is labelled Walking and has no mode`() {
        val state = ready(legs = listOf(pathLeg(transportMode = null, lineName = null)))

        val props = state.toFeatureCollection().features.single().props()

        assertEquals(true, props.bool("isWalking"))
        assertEquals("Walking", props.str("lineName"))
        assertNull(props["modeType"])
    }

    @Test
    fun `GIVEN a transit leg with no line name WHEN converted THEN no lineName property is emitted`() {
        val state = ready(legs = listOf(pathLeg(lineName = null)))

        assertNull(state.toFeatureCollection().features.single().props()["lineName"])
    }

    @Test
    fun `GIVEN a path leg with no points WHEN converted THEN the leg is dropped`() {
        val state = ready(legs = listOf(pathLeg(points = emptyList())), stops = listOf(stop()))

        val features = state.toFeatureCollection().features

        assertEquals(listOf("journey_stop"), features.map { it.props().str("type") })
    }

    @Test
    fun `GIVEN a stop-connector leg WHEN converted THEN it draws a line through the stop positions`() {
        val state = ready(
            legs = listOf(
                connectorLeg(
                    stops = listOf(
                        stop(stopId = "a", position = LatLng(latitude = -33.80, longitude = 151.20)),
                        stop(stopId = "b", position = LatLng(latitude = -33.90, longitude = 151.30)),
                    ),
                ),
            ),
        )

        val feature = state.toFeatureCollection().features.single()

        val geometry = assertIs<LineString>(feature.geometry)
        assertEquals(listOf(151.20, 151.30), geometry.coordinates.map { it.longitude })
        assertEquals(false, feature.props().bool("isWalking"))
    }

    @Test
    fun `GIVEN a stop-connector leg with one locatable stop WHEN converted THEN it is dropped`() {
        val state = ready(
            legs = listOf(
                connectorLeg(
                    stops = listOf(
                        stop(stopId = "a", position = LatLng(latitude = -33.80, longitude = 151.20)),
                        stop(stopId = "b", position = null),
                    ),
                ),
            ),
        )

        // Nothing renderable is left, so the mapper falls back to its placeholder feature rather
        // than emitting a one-point "line".
        assertEquals(listOf("empty"), state.toFeatureCollection().features.map { it.props().str("type") })
    }

    @Test
    fun `GIVEN stops WHEN converted THEN only located stops become points, in lon-lat order`() {
        val state = ready(
            stops = listOf(
                stop(stopId = "here", position = LatLng(latitude = -33.87, longitude = 151.21)),
                stop(stopId = "nowhere", position = null),
            ),
        )

        val features = state.toFeatureCollection().features

        val feature = features.single()
        assertEquals("here", feature.props().str("stopId"))
        val point = assertIs<Point>(feature.geometry)
        assertEquals(151.21, point.coordinates.longitude)
        assertEquals(-33.87, point.coordinates.latitude)
    }

    @Test
    fun `GIVEN a stop with platform and line WHEN converted THEN they ride along as properties`() {
        val state = ready(
            stops = listOf(
                stop(
                    stopId = "central",
                    stopName = "Central Station",
                    stopType = StopType.INTERCHANGE,
                    platform = "Platform 16",
                    lineName = "T4",
                    lineColor = "#005AA3",
                ),
            ),
        )

        val props = state.toFeatureCollection().features.single().props()

        assertEquals("journey_stop", props.str("type"))
        assertEquals("Central Station", props.str("stopName"))
        assertEquals("INTERCHANGE", props.str("stopType"))
        assertEquals("Platform 16", props.str("platform"))
        assertEquals("T4", props.str("lineName"))
        assertEquals("#005AA3", props.str("color"))
    }

    @Test
    fun `GIVEN a stop with no platform WHEN converted THEN no platform property is emitted`() {
        val state = ready(stops = listOf(stop(platform = null)))

        val props = state.toFeatureCollection().features.single().props()

        assertNull(props["platform"])
        assertNull(props["lineName"])
        assertNull(props["color"])
    }

    @Test
    fun `GIVEN nothing to draw WHEN converted THEN a placeholder feature keeps the serializer alive`() {
        val features = ready().toFeatureCollection().features

        // An empty FeatureCollection cannot be serialized by the polymorphic GeoJson serializer,
        // so the mapper substitutes one throwaway point rather than emitting nothing.
        val feature = features.single()
        assertEquals("empty", feature.props().str("type"))
        assertTrue(assertIs<Point>(feature.geometry).coordinates.longitude != 0.0)
    }

    // region fixtures

    private fun ready(
        legs: List<JourneyLegFeature> = emptyList(),
        stops: List<JourneyStopFeature> = emptyList(),
    ) = JourneyMapUiState.Ready(mapDisplay = JourneyMapDisplay(legs = legs, stops = stops))

    private fun pathLeg(
        points: List<LatLng> = listOf(
            LatLng(latitude = -33.87, longitude = 151.21),
            LatLng(latitude = -33.88, longitude = 151.22),
        ),
        transportMode: TransportMode? = TransportMode.Train,
        lineName: String? = "T1",
        lineColor: String = "#F99D1C",
    ) = JourneyLegFeature(
        legId = "leg_0",
        transportMode = transportMode,
        lineName = lineName,
        lineColor = lineColor,
        routeSegment = RouteSegment.PathSegment(points = points),
    )

    private fun connectorLeg(stops: List<JourneyStopFeature>) = JourneyLegFeature(
        legId = "leg_0",
        transportMode = TransportMode.Train,
        lineName = "T1",
        lineColor = "#F99D1C",
        routeSegment = RouteSegment.StopConnectorSegment(stops = stops),
    )

    // Mirrors JourneyStopFeature one-for-one so each optional property can be exercised alone.
    @Suppress("LongParameterList")
    private fun stop(
        stopId: String = "stop_id",
        stopName: String = "Stop",
        position: LatLng? = LatLng(latitude = -33.87, longitude = 151.21),
        stopType: StopType = StopType.REGULAR,
        platform: String? = null,
        lineName: String? = null,
        lineColor: String? = null,
    ) = JourneyStopFeature(
        stopId = stopId,
        stopName = stopName,
        position = position,
        stopType = stopType,
        platform = platform,
        lineName = lineName,
        lineColor = lineColor,
    )

    private fun Feature<*, *>.props(): JsonObject = properties as JsonObject

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.content

    private fun JsonObject.bool(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    // endregion
}
