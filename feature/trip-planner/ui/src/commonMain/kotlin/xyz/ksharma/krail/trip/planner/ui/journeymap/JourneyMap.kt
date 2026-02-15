package xyz.ksharma.krail.trip.planner.ui.journeymap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_loc
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
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
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature.Companion.getStringProperty
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.core.maps.ui.config.MapConfig
import xyz.ksharma.krail.core.maps.ui.config.MapTileProvider
import xyz.ksharma.krail.core.maps.ui.utils.MapCameraUtils
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyMapFeatureMapper.toFeatureCollection
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyMapFilters
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType

private const val LABEL_MIN_ZOOM = 0f
private const val LABEL_MAX_ZOOM = 24f
private val LOCATION_ICON_SIZE = DpSize(24.dp, 24.dp)
private val TEXT_HALO_WIDTH = 3.dp
private const val TEXT_OFFSET_BELOW_ICON = 2f // Position text below icon
private const val TEXT_OFFSET_ABOVE_ICON = -2f // Position text above icon

/**
 * Main composable for displaying a journey on a map.
 * Shows routes as lines (dashed for walking, solid for transit) and stops as circles.
 *
 * Note: In normal flow, journeyMapState is always Ready since data is pre-loaded from TimeTable.
 * Loading/Error states exist as defensive fallbacks only.
 */
@Composable
fun JourneyMap(
    journeyMapState: JourneyMapUiState,
    modifier: Modifier = Modifier,
) {
    when (journeyMapState) {
        // Defensive fallback - data transformation is instant, users never see this
        JourneyMapUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        // Expected state - journey data is already loaded
        is JourneyMapUiState.Ready -> {
            JourneyMapContent(
                mapState = journeyMapState,
                modifier = modifier,
            )
        }
    }
}

/**
 * The actual map content when data is ready.
 */
@Composable
private fun JourneyMapContent(
    mapState: JourneyMapUiState.Ready,
    modifier: Modifier = Modifier,
) {
    // Track selected stop for details bottom sheet
    // Keyed to mapState so it resets when viewing a different journey
    var selectedStop by remember(mapState) { mutableStateOf<JourneyStopFeature?>(null) }

    // Calculate camera position - start at the origin (journey beginning)
    val cameraPosition = remember(mapState.cameraFocus, mapState.mapDisplay.stops) {
        // Find the origin stop (where the journey starts)
        val originStop = mapState.mapDisplay.stops.firstOrNull { it.stopType == StopType.ORIGIN }

        val target = originStop?.position?.let { origin ->
            Position(longitude = origin.longitude, latitude = origin.latitude)
        } ?: mapState.cameraFocus?.let { focus ->
            // Fallback to center if no origin found
            val center = MapCameraUtils.calculateCenter(focus.bounds)
            Position(longitude = center.longitude, latitude = center.latitude)
        } ?: Position(
            latitude = MapConfig.DefaultPosition.LATITUDE,
            longitude = MapConfig.DefaultPosition.LONGITUDE,
        )

        // Calculate zoom level from bounds to fit the entire journey
        val zoom = mapState.cameraFocus?.let { focus ->
            MapCameraUtils.calculateZoomLevel(focus.bounds)
        } ?: MapConfig.DefaultPosition.ZOOM

        CameraPosition(target = target, zoom = zoom)
    }

    val cameraState = rememberCameraState(firstPosition = cameraPosition)

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState,
            baseStyle = BaseStyle.Uri(MapTileProvider.DEFAULT),
            options = MapOptions(
                ornamentOptions = OrnamentOptions(
                    padding = PaddingValues(MapConfig.Ornaments.DEFAULT_PADDING_DP.dp),
                    isLogoEnabled = MapConfig.Ornaments.LOGO_ENABLED,
                    isAttributionEnabled = MapConfig.Ornaments.ATTRIBUTION_ENABLED,
                    attributionAlignment = Alignment.BottomEnd,
                    isCompassEnabled = MapConfig.Ornaments.COMPASS_ENABLED,
                    compassAlignment = Alignment.TopEnd,
                    isScaleBarEnabled = MapConfig.Ornaments.SCALE_BAR_ENABLED,
                ),
            ),
        ) {
            // Convert state to GeoJSON FeatureCollection
            val featureCollection = remember(mapState) {
                mapState.toFeatureCollection()
            }

            val journeySource = rememberGeoJsonSource(
                data = GeoJsonData.Features(featureCollection),
            )

            // === LINE LAYERS ===

            // Walking paths - dashed gray lines
            LineLayer(
                id = "journey-walking-lines",
                source = journeySource,
                filter = JourneyMapFilters.isJourneyLeg() and
                    (get(GeoJsonPropertyKeys.IS_WALKING).asBoolean() eq const(true)),
                color = const(KrailTheme.colors.walkingPath),
                width = const(4.dp),
                dasharray = const(listOf(2f, 2f)), // Dashed pattern
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

            // === CIRCLE LAYERS FOR STOPS ===

            // Regular stops only - small white circles with click handler
            // Note: Origin, Destination, and Interchange stops use SymbolLayer with icons instead
            CircleLayer(
                id = "journey-stops-regular",
                source = journeySource,
                filter = JourneyMapFilters.isStopType(StopType.REGULAR),
                color = const(Color.White),
                radius = const(6.dp),
                strokeColor = const(Color.Black),
                strokeWidth = const(2.dp),
                onClick = { features ->
                    val feature = features.firstOrNull()
                    val stopId = feature?.getStringProperty(GeoJsonPropertyKeys.STOP_ID)
                    mapState.mapDisplay.stops.find { it.stopId == stopId }?.let { stop ->
                        selectedStop = stop
                    }
                    ClickResult.Consume
                },
            )

            // === TEXT LABELS ===

            // Prepare location icon for stop labels
            val locationIcon = painterResource(Res.drawable.ic_loc)

            // Get origin stop name for display
            val originStopName = remember(mapState.mapDisplay.stops) {
                mapState.mapDisplay.stops
                    .firstOrNull { it.stopType == StopType.ORIGIN }
                    ?.stopName
                    ?: "Start"
            }

            // Origin stop - Show stop name with icon
            SymbolLayer(
                id = "journey-origin-label",
                source = journeySource,
                minZoom = LABEL_MIN_ZOOM,
                maxZoom = LABEL_MAX_ZOOM,
                filter = JourneyMapFilters.isStopType(StopType.ORIGIN),
                iconImage = image(locationIcon, size = LOCATION_ICON_SIZE),
                textField = format(span(originStopName)),
                textFont = const(listOf("Noto Sans Regular")),
                textSize = const(1f.em),
                textColor = const(Color.Black),
                textOffset = offset(0f.em, TEXT_OFFSET_BELOW_ICON.em), // Below icon
                onClick = { features ->
                    val feature = features.firstOrNull()
                    val stopId = feature?.getStringProperty(GeoJsonPropertyKeys.STOP_ID)
                    mapState.mapDisplay.stops.find { it.stopId == stopId }?.let { stop ->
                        selectedStop = stop
                    }
                    ClickResult.Consume
                },
            )

            // Origin stop - Show line number (T1, etc.) above the stop name
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
                textHaloWidth = const(TEXT_HALO_WIDTH), // Thick background
                textOffset = offset(0f.em, TEXT_OFFSET_ABOVE_ICON.em), // Above icon (no icon on this layer)
            )

            // Destination and Interchange stops - Show stop name with icon
            SymbolLayer(
                id = "journey-stops-labels",
                source = journeySource,
                minZoom = LABEL_MIN_ZOOM,
                maxZoom = LABEL_MAX_ZOOM,
                filter = JourneyMapFilters.isStopType(StopType.DESTINATION, StopType.INTERCHANGE),
                iconImage = image(locationIcon, size = LOCATION_ICON_SIZE),
                textField = format(span(feature[GeoJsonPropertyKeys.STOP_NAME].asString())),
                textFont = const(listOf("Noto Sans Regular")),
                textSize = const(1.0f.em),
                textColor = const(Color.Black),
                textOffset = offset(0f.em, TEXT_OFFSET_BELOW_ICON.em), // Below icon
                onClick = { features ->
                    val feature = features.firstOrNull()
                    val stopId = feature?.getStringProperty(GeoJsonPropertyKeys.STOP_ID)
                    mapState.mapDisplay.stops.find { it.stopId == stopId }?.let { stop ->
                        selectedStop = stop
                    }
                    ClickResult.Consume
                },
            )

            // Destination and Interchange stops - Show line number above the stop name
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
                textHaloWidth = const(TEXT_HALO_WIDTH), // Thick background
                textOffset = offset(0f.em, TEXT_OFFSET_ABOVE_ICON.em), // Above icon (no icon on this layer)
            )
        }

        // Stop Details Bottom Sheet
        selectedStop?.let { stop ->
            JourneyStopDetailsBottomSheet(
                stop = stop,
                onDismiss = { selectedStop = null },
            )
        }
    }
}
