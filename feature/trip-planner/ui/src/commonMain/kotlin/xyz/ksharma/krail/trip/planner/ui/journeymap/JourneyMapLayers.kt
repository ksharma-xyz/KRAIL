package xyz.ksharma.krail.trip.planner.ui.journeymap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.krail.taj.resources.ic_location
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.and
import org.maplibre.compose.expressions.dsl.asBoolean
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature.Companion.getStringProperty
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.MapLayerConfig
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyMapFeatureMapper.toFeatureCollection
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyMapFilters
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.UserLocationLayer
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType
import app.krail.taj.resources.Res as TajRes

private const val LABEL_MIN_ZOOM = 0f
private const val LABEL_MAX_ZOOM = 24f
private val LOCATION_ICON_SIZE = DpSize(24.dp, 24.dp)
private val TEXT_HALO_WIDTH = 3.dp
private const val TEXT_OFFSET_BELOW_ICON = 2f
private const val TEXT_OFFSET_ABOVE_ICON = -2f

/**
 * All MapLibre layers rendered inside the journey map.
 * Extracted to keep [JourneyMapContent] below the cyclomatic complexity threshold.
 */
@Composable
internal fun JourneyMapLayers(
    mapState: JourneyMapUiState.Ready,
    userLocation: LatLng?,
    onStopSelect: (JourneyStopFeature) -> Unit,
) {
    val featureCollection = remember(mapState) { mapState.toFeatureCollection() }
    val journeySource = rememberGeoJsonSource(data = GeoJsonData.Features(featureCollection))

    // Walking paths - dashed gray lines
    LineLayer(
        id = "journey-walking-lines",
        source = journeySource,
        filter = JourneyMapFilters.isJourneyLeg() and
            (get(GeoJsonPropertyKeys.IS_WALKING).asBoolean() eq const(true)),
        color = const(KrailTheme.colors.walkingPath),
        width = const(4.dp),
        dasharray = const(listOf(2f, 2f)),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )

    // Transit routes - solid colored lines
    LineLayer(
        id = "journey-transit-lines",
        source = journeySource,
        filter = JourneyMapFilters.isJourneyLeg() and
            (get(GeoJsonPropertyKeys.IS_WALKING).asBoolean() eq const(false)),
        color = get(GeoJsonPropertyKeys.COLOR).asString().convertToColor(),
        width = const(6.dp),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )

    // Regular stops - small white circles (visual only, no click handler)
    CircleLayer(
        id = "journey-stops-regular",
        source = journeySource,
        filter = JourneyMapFilters.isStopType(StopType.REGULAR),
        color = const(Color.White),
        radius = const(MapLayerConfig.JOURNEY_STOP_CIRCLE_RADIUS_DP.dp),
        strokeColor = const(Color.Black),
        strokeWidth = const(2.dp),
    )

    // Invisible hit target for regular stops — expands tappable area to STOP_HIT_TARGET_RADIUS_DP
    CircleLayer(
        id = "journey-stops-regular-hit",
        source = journeySource,
        filter = JourneyMapFilters.isStopType(StopType.REGULAR),
        color = const(Color.Transparent),
        radius = const(MapLayerConfig.STOP_HIT_TARGET_RADIUS_DP.dp),
        onClick = { features ->
            features.firstOrNull()
                ?.getStringProperty(GeoJsonPropertyKeys.STOP_ID)
                ?.let { id -> mapState.mapDisplay.stops.find { it.stopId == id } }
                ?.let(onStopSelect)
            ClickResult.Consume
        },
    )

    val locationIcon = painterResource(TajRes.drawable.ic_location)
    val originStopName = remember(mapState.mapDisplay.stops) {
        mapState.mapDisplay.stops.firstOrNull { it.stopType == StopType.ORIGIN }?.stopName ?: "Start"
    }

    // Origin stop label + icon
    SymbolLayer(
        id = "journey-origin-label",
        source = journeySource,
        minZoom = LABEL_MIN_ZOOM,
        maxZoom = LABEL_MAX_ZOOM,
        filter = JourneyMapFilters.isStopType(StopType.ORIGIN),
        iconImage = image(locationIcon, size = LOCATION_ICON_SIZE),
        iconColor = const(Color.Black),
        textField = format(span(originStopName)),
        textFont = const(listOf("Noto Sans Regular")),
        textSize = const(1f.em),
        textColor = const(Color.Black),
        textOffset = offset(0f.em, TEXT_OFFSET_BELOW_ICON.em),
        onClick = { features ->
            features.firstOrNull()
                ?.getStringProperty(GeoJsonPropertyKeys.STOP_ID)
                ?.let { id -> mapState.mapDisplay.stops.find { it.stopId == id } }
                ?.let(onStopSelect)
            ClickResult.Consume
        },
    )

    // Origin stop line number
    SymbolLayer(
        id = "journey-origin-line-label",
        source = journeySource,
        minZoom = LABEL_MIN_ZOOM,
        maxZoom = LABEL_MAX_ZOOM,
        filter = JourneyMapFilters.isStopType(StopType.ORIGIN),
        textField = format(span(get(GeoJsonPropertyKeys.LINE_NAME).asString())),
        textFont = const(listOf("Noto Sans Regular")),
        textSize = const(1.2f.em),
        textColor = const(Color.White),
        textHaloColor = get(GeoJsonPropertyKeys.COLOR).asString().convertToColor(),
        textHaloWidth = const(TEXT_HALO_WIDTH),
        textOffset = offset(0f.em, TEXT_OFFSET_ABOVE_ICON.em),
    )

    // Destination/Interchange stop labels + icons
    SymbolLayer(
        id = "journey-stops-labels",
        source = journeySource,
        minZoom = LABEL_MIN_ZOOM,
        maxZoom = LABEL_MAX_ZOOM,
        filter = JourneyMapFilters.isStopType(StopType.DESTINATION, StopType.INTERCHANGE),
        iconImage = image(locationIcon, size = LOCATION_ICON_SIZE),
        iconColor = const(Color.Black),
        textField = format(span(feature[GeoJsonPropertyKeys.STOP_NAME].asString())),
        textFont = const(listOf("Noto Sans Regular")),
        textSize = const(1.0f.em),
        textColor = const(Color.Black),
        textOffset = offset(0f.em, TEXT_OFFSET_BELOW_ICON.em),
        onClick = { features ->
            features.firstOrNull()
                ?.getStringProperty(GeoJsonPropertyKeys.STOP_ID)
                ?.let { id -> mapState.mapDisplay.stops.find { it.stopId == id } }
                ?.let(onStopSelect)
            ClickResult.Consume
        },
    )

    // Destination/Interchange stop line numbers
    SymbolLayer(
        id = "journey-stops-line-labels",
        source = journeySource,
        minZoom = LABEL_MIN_ZOOM,
        maxZoom = LABEL_MAX_ZOOM,
        filter = JourneyMapFilters.isStopType(StopType.DESTINATION, StopType.INTERCHANGE),
        textField = format(span(get(GeoJsonPropertyKeys.LINE_NAME).asString())),
        textFont = const(listOf("Noto Sans Regular")),
        textSize = const(1.2f.em),
        textColor = const(Color.White),
        textHaloColor = get(GeoJsonPropertyKeys.COLOR).asString().convertToColor(),
        textHaloWidth = const(TEXT_HALO_WIDTH),
        textOffset = offset(0f.em, TEXT_OFFSET_ABOVE_ICON.em),
    )

    // User location pulsing dot (always on top)
    UserLocationLayer(userLocation = userLocation)
}
