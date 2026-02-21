package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature.Companion.getStringProperty
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.state.GeoJsonFeatureTypes
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.core.maps.state.MapLayerConfig
import xyz.ksharma.krail.core.maps.state.geoJsonProperties
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import org.maplibre.spatialk.geojson.Feature as GeoJsonFeature

@Composable
fun NearbyStopsLayer(
    stops: List<NearbyStopFeature>,
    onStopClick: (NearbyStopFeature) -> Unit,
) {
    log("[NEARBY_STOPS_UI] NearbyStopsLayer rendering ${stops.size} stops")
    stops.take(3).forEach { stop ->
        log(
            "[NEARBY_STOPS_UI] Stop: ${stop.stopName} at (${stop.position.latitude}, " +
                "${stop.position.longitude}), modes=${stop.transportModes.map { it.name }}",
        )
    }

    val featureCollection = stops.toFeatureCollection()
    log("[NEARBY_STOPS_UI] FeatureCollection created with ${featureCollection.features.size} features")

    val geoJsonSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(featureCollection),
    )

    // Stop circles with transport mode colors (visual only, no click handler)
    CircleLayer(
        id = "nearby-stops-circle",
        source = geoJsonSource,
        radius = const(MapLayerConfig.NEARBY_STOP_CIRCLE_RADIUS_DP.dp),
        color = Feature["color"].asString().convertToColor(),
        strokeColor = const(Color.White),
        strokeWidth = const(2.dp),
    )

    // Invisible hit target rendered on top — expands the tappable area to 24dp radius
    // without affecting the visual. Must be declared after the visible layer so it sits
    // on top and receives click events first.
    CircleLayer(
        id = "nearby-stops-hit",
        source = geoJsonSource,
        radius = const(MapLayerConfig.STOP_HIT_TARGET_RADIUS_DP.dp),
        color = const(Color.Transparent),
        onClick = { features ->
            log("[NEARBY_STOPS_UI] Circle clicked, features.size=${features.size}")
            val feature = features.firstOrNull()
            val stopId = feature?.getStringProperty("stopId")
            log("[NEARBY_STOPS_UI] Clicked stopId=$stopId")
            stops.find { it.stopId == stopId }?.let(onStopClick)
            ClickResult.Consume
        },
    )
}

// Mapper to GeoJSON
private fun List<NearbyStopFeature>.toFeatureCollection(): FeatureCollection<*, *> {
    log("[NEARBY_STOPS_UI] toFeatureCollection: converting ${this.size} stops")

    if (isEmpty()) {
        // Return a dummy feature to avoid serialization issues with empty collections
        val emptyFeature = GeoJsonFeature(
            geometry = Point(
                coordinates = Position(
                    longitude = 151.2057,
                    latitude = -33.8727,
                ),
            ),
            properties = geoJsonProperties {
                property(GeoJsonPropertyKeys.TYPE, GeoJsonFeatureTypes.EMPTY)
            },
        )
        return FeatureCollection(features = listOf(emptyFeature))
    }

    val features = map { stop ->
        val color = stop.transportModes.firstOrNull()?.colorCode ?: "#000000"
        log(
            "[NEARBY_STOPS_UI] Creating feature for ${stop.stopName}: color=$color, " +
                "pos=(${stop.position.latitude}, ${stop.position.longitude})",
        )
        GeoJsonFeature(
            geometry = Point(
                coordinates = Position(
                    latitude = stop.position.latitude,
                    longitude = stop.position.longitude,
                ),
            ),
            properties = geoJsonProperties {
                property(GeoJsonPropertyKeys.TYPE, GeoJsonFeatureTypes.NEARBY_STOP)
                property(GeoJsonPropertyKeys.STOP_ID, stop.stopId)
                property(GeoJsonPropertyKeys.STOP_NAME, stop.stopName)
                property(GeoJsonPropertyKeys.COLOR, color)
            },
        )
    }

    return FeatureCollection(features = features)
}
