package xyz.ksharma.krail.trip.planner.ui.parkride.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.value.TranslateAnchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature.Companion.getStringProperty
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.maps.state.GeoJsonFeatureTypes
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.core.maps.state.geoJsonProperties
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import org.maplibre.spatialk.geojson.Feature as GeoJsonFeature

private const val STATION_ID_PROPERTY = "stationId"
private const val PARK_RIDE_STATION_TYPE = "park_ride_station"
private val HitTargetRadius = 24.dp
private val BesideHitTargetRadius = 22.dp

/**
 * Plots every supported Park & Ride station as the parking logo ([rememberParkRideMarkerPainter]).
 *
 * The white `P` on the theme colour is the same identity the home card and picker rows use, so a station looks like
 * itself wherever it appears.
 *
 * Positions come from the local GTFS stops table, so this layer needs no network call and
 * has no bearing on the availability polling lifecycle (`docs/POLLING_LIFECYCLE.md`).
 */
@Composable
fun ParkRideStationsLayer(
    stations: List<ParkRideStationPickerItem>,
    onStationClick: (ParkRideStationPickerItem) -> Unit,
    // How far the `P` stands from the station's own position, for a map that draws the station's marker there too, so
    // that each has its own place to tap. Nothing by default: the `P` stands on the station.
    beside: DpOffset = DpOffset.Zero,
) {
    val plottable = stations.filter { it.position != null }
    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(plottable.toFeatureCollection()),
    )

    SymbolLayer(
        id = "park-ride-stations-logo",
        source = source,
        iconImage = image(
            value = rememberParkRideMarkerPainter(),
            size = DpSize(ParkRideMarkerSize, ParkRideMarkerSize),
        ),
        iconOffset = offset(beside.x, beside.y),
        iconAllowOverlap = const(true),
        // The map's own names stay: the marker does not push them out of the way.
        iconIgnorePlacement = const(true),
    )

    // Invisible larger hit target on top, same trick NearbyStopsLayer uses: a 30dp marker is
    // an awkward tap target, and this widens it without changing the visual. Must be declared
    // last so it sits above and receives the click first.
    CircleLayer(
        id = "park-ride-stations-hit",
        source = source,
        radius = const(if (beside == DpOffset.Zero) HitTargetRadius else BesideHitTargetRadius),
        color = const(Color.Transparent),
        translate = offset(beside.x, beside.y),
        // The logo's offset is measured on the screen, so the tap area's must be too. By default it is measured on
        // the map, and turning the map then carries the tap area away from the logo.
        translateAnchor = const(TranslateAnchor.Viewport),
        onClick = { features ->
            val stationId = features.firstOrNull()?.getStringProperty(STATION_ID_PROPERTY)
            plottable.find { it.stationId == stationId }?.let(onStationClick)
            ClickResult.Consume
        },
    )
}

private fun List<ParkRideStationPickerItem>.toFeatureCollection(): FeatureCollection<*, *> {
    // MapLibre cannot serialise an empty collection, so an off-screen placeholder stands in
    // until stations load. Same workaround as NearbyStopsLayer.
    if (isEmpty()) {
        return FeatureCollection(
            features = listOf(
                GeoJsonFeature(
                    geometry = Point(
                        coordinates = Position(latitude = 0.0, longitude = 0.0),
                    ),
                    properties = geoJsonProperties {
                        property(GeoJsonPropertyKeys.TYPE, GeoJsonFeatureTypes.EMPTY)
                    },
                ),
            ),
        )
    }

    return FeatureCollection(
        features = mapNotNull { station ->
            val position = station.position ?: return@mapNotNull null
            GeoJsonFeature(
                geometry = Point(
                    coordinates = Position(
                        latitude = position.latitude,
                        longitude = position.longitude,
                    ),
                ),
                properties = geoJsonProperties {
                    property(GeoJsonPropertyKeys.TYPE, PARK_RIDE_STATION_TYPE)
                    property(STATION_ID_PROPERTY, station.stationId)
                    property(GeoJsonPropertyKeys.STOP_NAME, station.stationName)
                },
            )
        },
    )
}
