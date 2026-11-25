package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.asNumber
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.Source
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
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import org.maplibre.spatialk.geojson.Feature as GeoJsonFeature

private const val MARKER_PROPERTY = "marker"
private const val RANK_PROPERTY = "rank"
private const val NO_MODE_PRIORITY = 100

@Composable
fun NearbyStopsLayer(
    stops: List<NearbyStopFeature>,
    onStopClick: (NearbyStopFeature) -> Unit,
) {
    log("[NEARBY_STOPS_UI] NearbyStopsLayer rendering ${stops.size} stops")

    val featureCollection = stops.toFeatureCollection()
    log("[NEARBY_STOPS_UI] FeatureCollection created with ${featureCollection.features.size} features")

    val geoJsonSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(featureCollection),
    )

    // Further out, a dot in the colour of the stop's mode (visual only, no click handler).
    CircleLayer(
        id = "nearby-stops-circle",
        source = geoJsonSource,
        maxZoom = MODE_LETTERS_MIN_ZOOM,
        radius = const(MapLayerConfig.NEARBY_STOP_CIRCLE_RADIUS_DP.dp),
        color = Feature["color"].asString().convertToColor(),
        strokeColor = const(Color.White),
        strokeWidth = const(2.dp),
    )

    StopModeLetterLayer(source = geoJsonSource)

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

/** Close up, each stop is its mode's transport icon. Where two overlap, the mode first in priority is on top. */
@Composable
private fun StopModeLetterLayer(source: Source) {
    val size = DpSize(StopModeMarkerSize, StopModeMarkerSize)
    val icons = TransportMode.all.map { mode ->
        key(mode.productClass) {
            case(mode.productClass.toString(), image(rememberStopModeMarkerPainter(mode), size))
        }
    }
    // A stop with no mode keeps a plain black marker, as its dot is black.
    val unknown = image(rememberMapLetterPainter(letter = "", fill = Color.Black, letterSize = 1.dp), size)
    SymbolLayer(
        id = "nearby-stops-letters",
        source = source,
        minZoom = MODE_LETTERS_MIN_ZOOM,
        iconImage = switch(
            input = Feature[MARKER_PROPERTY].asString(),
            cases = icons.toTypedArray(),
            fallback = unknown,
        ),
        sortKey = Feature[RANK_PROPERTY].asNumber(),
        iconAllowOverlap = const(true),
        // The map's own names stay: these markers do not push them out of the way.
        iconIgnorePlacement = const(true),
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
        val mode = markerModeOf(stop.transportModes)
        val color = mode?.let { NswTransportConfig.colorFor(it) } ?: "#000000"
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
                property(MARKER_PROPERTY, mode?.productClass?.toString().orEmpty())
                // A higher key is drawn later, so on top.
                property(RANK_PROPERTY, -(mode?.searchPriority ?: NO_MODE_PRIORITY))
            },
        )
    }

    return FeatureCollection(features = features)
}
