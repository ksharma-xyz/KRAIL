package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.RouteFeature
import xyz.ksharma.krail.core.maps.state.StopFeature
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.StopResultsMapMapper.toFeatureCollection
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * `StopResultsMapMapper` feeds the SearchStop map its GeoJSON. Like the journey-map mapper it has
 * to flip KRAIL's (latitude, longitude) into GeoJSON's (longitude, latitude), and it has to emit
 * the property keys the route and stop layers filter on.
 */
class StopResultsMapMapperTest {

    @Test
    fun `GIVEN a route WHEN converted THEN it is a line in lon-lat order carrying its id and colour`() {
        val state = ready(
            routes = listOf(
                RouteFeature(
                    id = "T1",
                    colorHex = "#F99D1C",
                    points = listOf(
                        LatLng(latitude = -33.80, longitude = 151.20),
                        LatLng(latitude = -33.90, longitude = 151.30),
                    ),
                ),
            ),
        )

        val feature = state.toFeatureCollection().features.single()

        val geometry = assertIs<LineString>(feature.geometry)
        assertEquals(listOf(151.20, 151.30), geometry.coordinates.map { it.longitude })
        assertEquals(listOf(-33.80, -33.90), geometry.coordinates.map { it.latitude })
        assertEquals("route", feature.props().str("type"))
        assertEquals("T1", feature.props().str("lineId"))
        assertEquals("#F99D1C", feature.props().str("color"))
    }

    @Test
    fun `GIVEN a stop WHEN converted THEN it is a point in lon-lat order carrying id and name`() {
        val state = ready(
            stops = listOf(
                StopFeature(
                    stopId = "200060",
                    stopName = "Central Station",
                    lineId = "T1",
                    position = LatLng(latitude = -33.88, longitude = 151.21),
                ),
            ),
        )

        val feature = state.toFeatureCollection().features.single()

        val point = assertIs<Point>(feature.geometry)
        assertEquals(151.21, point.coordinates.longitude)
        assertEquals(-33.88, point.coordinates.latitude)
        assertEquals("stop", feature.props().str("type"))
        assertEquals("200060", feature.props().str("stopId"))
        assertEquals("Central Station", feature.props().str("stopName"))
        assertEquals("T1", feature.props().str("lineId"))
    }

    @Test
    fun `GIVEN a stop with no line WHEN converted THEN lineId is an empty string, not a missing key`() {
        val state = ready(
            stops = listOf(
                StopFeature(
                    stopId = "200060",
                    stopName = "Central Station",
                    lineId = null,
                    position = LatLng(latitude = -33.88, longitude = 151.21),
                ),
            ),
        )

        // The layer's filter reads the key unconditionally, so it has to exist.
        assertEquals("", state.toFeatureCollection().features.single().props().str("lineId"))
    }

    @Test
    fun `GIVEN routes and stops WHEN converted THEN routes are emitted before stops so lines sit under pins`() {
        val state = ready(
            routes = listOf(RouteFeature(id = "T1", colorHex = "#F99D1C", points = twoPoints())),
            stops = listOf(
                StopFeature(
                    stopId = "200060",
                    stopName = "Central Station",
                    lineId = "T1",
                    position = LatLng(latitude = -33.88, longitude = 151.21),
                ),
            ),
        )

        assertEquals(
            listOf("route", "stop"),
            state.toFeatureCollection().features.map { it.props().str("type") },
        )
    }

    @Test
    fun `GIVEN nothing to draw WHEN converted THEN a placeholder feature keeps the serializer alive`() {
        val features = ready().toFeatureCollection().features

        assertEquals("empty", features.single().props().str("type"))
    }

    private fun ready(
        routes: List<RouteFeature> = emptyList(),
        stops: List<StopFeature> = emptyList(),
    ) = MapUiState.Ready(
        mapDisplay = MapDisplay(
            routes = persistentListOf<RouteFeature>().addAll(routes),
            stops = persistentListOf<StopFeature>().addAll(stops),
        ),
    )

    private fun twoPoints() = listOf(
        LatLng(latitude = -33.80, longitude = 151.20),
        LatLng(latitude = -33.90, longitude = 151.30),
    )

    private fun Feature<*, *>.props(): JsonObject = properties as JsonObject

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.content
}
