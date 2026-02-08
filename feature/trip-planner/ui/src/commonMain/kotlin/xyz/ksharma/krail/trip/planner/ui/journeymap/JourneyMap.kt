package xyz.ksharma.krail.trip.planner.ui.journeymap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.and
import org.maplibre.compose.expressions.dsl.asBoolean
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.core.maps.state.GeoJsonFeatureTypes
import xyz.ksharma.krail.core.maps.ui.config.MapConfig
import xyz.ksharma.krail.core.maps.ui.config.MapTileProvider
import xyz.ksharma.krail.core.maps.ui.utils.MapCameraUtils
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyMapFeatureMapper.toFeatureCollection
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType

/**
 * Main composable for displaying a journey on a map.
 * Shows routes as lines (dashed for walking, solid for transit) and stops as circles.
 */
@Composable
fun JourneyMap(
    journeyMapState: JourneyMapUiState,
    modifier: Modifier = Modifier,
    onStopClick: (JourneyStopFeature) -> Unit = {},
) {
    when (journeyMapState) {
        JourneyMapUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        is JourneyMapUiState.Ready -> {
            JourneyMapContent(
                mapState = journeyMapState,
                onStopClick = onStopClick,
                modifier = modifier,
            )
        }

        is JourneyMapUiState.Error -> {
            Box(modifier = modifier.fillMaxSize()) {
                // TODO: Add error UI component
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

/**
 * The actual map content when data is ready.
 */
@Composable
private fun JourneyMapContent(
    mapState: JourneyMapUiState.Ready,
    onStopClick: (JourneyStopFeature) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            longitude = MapConfig.DefaultPosition.LONGITUDE
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
                data = GeoJsonData.Features(featureCollection)
            )

            // === LINE LAYERS ===

            // Walking paths - dashed gray lines
            LineLayer(
                id = "journey-walking-lines",
                source = journeySource,
                filter = (get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_LEG)) and
                        (get(GeoJsonPropertyKeys.IS_WALKING).asBoolean() eq const(true)),
                color = const(Color(0xFF757575)), // Gray
                width = const(4.dp),
                dasharray = const(listOf(2f, 2f)), // Dashed pattern
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
            )

            // Transit routes - solid colored lines
            LineLayer(
                id = "journey-transit-lines",
                source = journeySource,
                filter = (get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_LEG)) and
                        (get(GeoJsonPropertyKeys.IS_WALKING).asBoolean() eq const(false)),
                color = get(GeoJsonPropertyKeys.COLOR).asString().convertToColor(),
                width = const(6.dp),
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
            )

            // === CIRCLE LAYERS FOR STOPS ===

            // Regular stops - small white circles
            CircleLayer(
                id = "journey-stops-regular",
                source = journeySource,
                filter = (get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_STOP)) and
                        (get(GeoJsonPropertyKeys.STOP_TYPE).asString() eq const("REGULAR")),
                color = const(Color.White),
                radius = const(6.dp),
                strokeColor = const(Color.Black),
                strokeWidth = const(2.dp),
            )

            // Interchange stops - medium yellow circles
            CircleLayer(
                id = "journey-stops-interchange",
                source = journeySource,
                filter = (get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_STOP)) and
                        (get(GeoJsonPropertyKeys.STOP_TYPE).asString() eq const("INTERCHANGE")),
                color = const(Color.White), // Yellow
                radius = const(8.dp),
                strokeColor = const(Color.Black),
                strokeWidth = const(3.dp),
            )

            // Origin stop - large green circle
            CircleLayer(
                id = "journey-stops-origin",
                source = journeySource,
                filter = (get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_STOP)) and
                        (get(GeoJsonPropertyKeys.STOP_TYPE).asString() eq const("ORIGIN")),
                color = const(Color.White), // Green
                radius = const(8.dp),
                strokeColor = const(Color.Black),
                strokeWidth = const(3.dp),
            )

            // Destination stop - large red circle
            CircleLayer(
                id = "journey-stops-destination",
                source = journeySource,
                filter = (get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_STOP)) and
                        (get(GeoJsonPropertyKeys.STOP_TYPE).asString() eq const("DESTINATION")),
                color = const(Color.White), // Red
                radius = const(8.dp),
                strokeColor = const(Color.Black),
                strokeWidth = const(3.dp),
            )
        }
    }
}
