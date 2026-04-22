package xyz.ksharma.krail.trip.planner.ui.journeymap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.collections.immutable.ImmutableList
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.core.maps.state.geoJsonProperties
import xyz.ksharma.krail.feature.track.LiveTrackingOverlay
import xyz.ksharma.krail.feature.track.TrackedLeg

private const val CIRCLE_LAYER_ID = "live-vehicles-circle"
private const val LABEL_LAYER_ID = "live-vehicles-label"
private const val PROP_LINE_NAME = "lineName"

/**
 * MapLibre layers that render live vehicle positions on top of the journey map.
 *
 * Renders a filled circle in the line colour with the line name as a text label above it.
 * The base map already shows transport infrastructure (rail lines, roads) so no separate
 * route-ahead polyline is drawn — it would require the vehicle's actual approach geometry
 * which is not available from the GTFS-RT feed.
 *
 * Updates on each GTFS-RT poll (every ~30 s) via recomposition driven by [overlay] changes.
 */
@Composable
fun LiveVehicleLayer(
    overlay: LiveTrackingOverlay,
    legs: ImmutableList<TrackedLeg>,
) {
    if (overlay.vehiclePositions.isEmpty()) return

    val vehicleCollection = remember(overlay) {
        val features = overlay.vehiclePositions.mapNotNull { (legIndex, position) ->
            val leg = legs.getOrNull(legIndex) as? TrackedLeg.Transport ?: return@mapNotNull null
            Feature(
                geometry = Point(Position(longitude = position.longitude, latitude = position.latitude)),
                properties = geoJsonProperties {
                    property(PROP_LINE_NAME, leg.lineName)
                    property(GeoJsonPropertyKeys.COLOR, leg.lineColorCode)
                },
            )
        }
        FeatureCollection(features = features)
    }

    val vehicleSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(vehicleCollection),
    )

    // Filled circle in the line colour with a white border.
    CircleLayer(
        id = CIRCLE_LAYER_ID,
        source = vehicleSource,
        radius = const(14.dp),
        color = get(GeoJsonPropertyKeys.COLOR).asString().convertToColor(),
        strokeWidth = const(2.dp),
        strokeColor = const(Color.White),
    )

    // Line name label centred inside the circle.
    SymbolLayer(
        id = LABEL_LAYER_ID,
        source = vehicleSource,
        textField = get(PROP_LINE_NAME).asString(),
        textFont = const(listOf("Noto Sans Regular")),
        textSize = const(0.7f.em),
        textColor = const(Color.White),
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true),
    )
}
