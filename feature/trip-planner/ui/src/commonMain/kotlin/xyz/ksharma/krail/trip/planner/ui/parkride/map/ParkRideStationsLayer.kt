package xyz.ksharma.krail.trip.planner.ui.parkride.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.span
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
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.taj.themeContentColor
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import org.maplibre.spatialk.geojson.Feature as GeoJsonFeature

private const val STATION_ID_PROPERTY = "stationId"
private const val PARK_RIDE_STATION_TYPE = "park_ride_station"
private val StationCircleRadius = 14.dp
private val HitTargetRadius = 24.dp
private val CircleStrokeWidth = 2.dp
private const val GLYPH_TEXT_SIZE_EM = 1.1f
private const val GLYPH_VERTICAL_OFFSET_EM = 0.03f

/**
 * Plots every supported Park & Ride station as a themed disc carrying a bold `P`.
 *
 * The `P` is the same identity the home card and picker rows use, so a station looks like
 * itself wherever it appears. Disc fill is the rider's theme colour and the glyph runs
 * through [getForegroundColor], the same contrast guard as `ParkRideAddToggle`.
 *
 * Positions come from the local GTFS stops table, so this layer needs no network call and
 * has no bearing on the availability polling lifecycle (`docs/POLLING_LIFECYCLE.md`).
 */
@Composable
fun ParkRideStationsLayer(
    stations: List<ParkRideStationPickerItem>,
    onStationClick: (ParkRideStationPickerItem) -> Unit,
) {
    val plottable = stations.filter { it.position != null }
    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(plottable.toFeatureCollection()),
    )

    val discColor = themeColor()
    val glyphColor = getForegroundColor(
        backgroundColor = discColor,
        foregroundColor = themeContentColor(),
    )

    CircleLayer(
        id = "park-ride-stations-circle",
        source = source,
        radius = const(StationCircleRadius),
        color = const(discColor),
        strokeColor = const(Color.White),
        strokeWidth = const(CircleStrokeWidth),
    )

    SymbolLayer(
        id = "park-ride-stations-glyph",
        source = source,
        textField = format(span("P")),
        textFont = const(listOf("Noto Sans Bold")),
        textSize = const(GLYPH_TEXT_SIZE_EM.em),
        textColor = const(glyphColor),
        textOffset = offset(0f.em, GLYPH_VERTICAL_OFFSET_EM.em),
        textAllowOverlap = const(true),
    )

    // Invisible larger hit target on top, same trick NearbyStopsLayer uses: a 14dp disc is
    // an awkward tap target, and this widens it without changing the visual. Must be declared
    // last so it sits above and receives the click first.
    CircleLayer(
        id = "park-ride-stations-hit",
        source = source,
        radius = const(HitTargetRadius),
        color = const(Color.Transparent),
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
