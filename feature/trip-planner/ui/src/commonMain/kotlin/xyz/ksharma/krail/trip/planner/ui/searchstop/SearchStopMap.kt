package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature.Companion.getStringProperty
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.state.GeoJsonFeatureTypes
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.core.maps.state.geoJsonProperties
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeChip
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeSortOrder
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopsConfig
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent

@Composable
fun SearchStopMap(
    mapUiState: MapUiState,
    modifier: Modifier = Modifier,
    onEvent: (SearchStopUiEvent) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (mapUiState) {
            is MapUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            is MapUiState.Ready -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Empty state message at top
                    if (mapUiState.mapDisplay.nearbyStops.isEmpty() &&
                        !mapUiState.isLoadingNearbyStops
                    ) {
                        EmptyStopsMessage(modifier = Modifier.fillMaxWidth())
                    }

                    // Map content
                    MapContent(
                        mapState = mapUiState,
                        onEvent = onEvent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            is MapUiState.Error -> {
                ErrorMessage(
                    message = mapUiState.message ?: "Something went wrong",
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun EmptyStopsMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(KrailTheme.colors.errorContainer)
            .padding(vertical = 12.dp, horizontal = 16.dp),
    ) {
        Text(
            text = "No stops found in this area",
            style = KrailTheme.typography.bodyMedium,
            color = KrailTheme.colors.onErrorContainer,
        )
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        style = KrailTheme.typography.bodyMedium,
        color = KrailTheme.colors.error,
        modifier = modifier.padding(16.dp),
    )
}

@Composable
private fun MapContent(
    mapState: MapUiState.Ready,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    log(
        "[NEARBY_STOPS_UI] MapContent rendered: nearbyStops.size=${mapState.mapDisplay.nearbyStops.size}, " +
            "isLoading=${mapState.isLoadingNearbyStops}, " +
            "selectedModes=${mapState.mapDisplay.selectedTransportModes}",
    )

    // Start at default Sydney coordinates
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(
                latitude = NearbyStopsConfig.DEFAULT_CENTER_LAT,
                longitude = NearbyStopsConfig.DEFAULT_CENTER_LON,
            ),
            zoom = NearbyStopsConfig.DEFAULT_ZOOM,
        ),
    )

    // Track camera moves to update map center
    @OptIn(FlowPreview::class)
    @Suppress("MagicNumber")
    LaunchedEffect(cameraState.position) {
        snapshotFlow { cameraState.position.target }
            .debounce(500) // Only update after user stops moving
            .collect { target ->
                log(
                    "[NEARBY_STOPS_UI] Camera moved to: lat=${target.latitude}, " +
                        "lon=${target.longitude}",
                )
                onEvent(
                    SearchStopUiEvent.MapCenterChanged(
                        LatLng(target.latitude, target.longitude),
                    ),
                )
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState,
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
            options = MapOptions(
                ornamentOptions = OrnamentOptions(
                    padding = PaddingValues(0.dp),
                    isLogoEnabled = false,
                    isAttributionEnabled = true,
                    attributionAlignment = Alignment.BottomEnd,
                    isCompassEnabled = true,
                    compassAlignment = Alignment.TopEnd,
                ),
            ),
        ) {
            // Render nearby stops
            if (mapState.mapDisplay.nearbyStops.isNotEmpty()) {
                log("[NEARBY_STOPS_UI] Rendering ${mapState.mapDisplay.nearbyStops.size} stops on map")
                NearbyStopsLayer(
                    stops = mapState.mapDisplay.nearbyStops,
                    onStopClick = { stop ->
                        log("[NEARBY_STOPS_UI] Stop clicked: ${stop.stopName}")
                        onEvent(SearchStopUiEvent.NearbyStopClicked(stop))
                    },
                )
            } else {
                log("[NEARBY_STOPS_UI] No stops to render")
            }
        }

        // Transport mode filter chips (top of map)
        TransportModeFilterRow(
            selectedModes = mapState.mapDisplay.selectedTransportModes,
            onModeToggle = { mode ->
                onEvent(SearchStopUiEvent.TransportModeFilterToggled(mode))
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
        )

        // "Show stops here" button (bottom center)
        ShowStopsButton(
            isLoading = mapState.isLoadingNearbyStops,
            onClick = { onEvent(SearchStopUiEvent.ShowStopsHere) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun TransportModeFilterRow(
    selectedModes: Set<Int>,
    onModeToggle: (TransportMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allModes = remember {
        TransportMode.sortedValues(TransportModeSortOrder.PRIORITY)
    }

    LazyRow(
        modifier = modifier
            .background(
                color = KrailTheme.colors.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(allModes) { mode ->
            TransportModeChip(
                transportMode = mode,
                selected = selectedModes.contains(mode.productClass),
                onClick = { onModeToggle(mode) },
            )
        }
    }
}

@Composable
private fun ShowStopsButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Loading...", color = Color.White)
        } else {
            Text("Show stops here", color = Color.White)
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun NearbyStopsLayer(
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
    log(
        "[NEARBY_STOPS_UI] FeatureCollection created with " +
            "${featureCollection.features.size} features",
    )

    val geoJsonSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(featureCollection),
    )

    // Stop circles with transport mode colors
    CircleLayer(
        id = "nearby-stops-circle",
        source = geoJsonSource,
        radius = const(8.dp),
        color = Feature.get("color").asString().convertToColor(),
        strokeColor = const(Color.White),
        strokeWidth = const(2.dp),
        onClick = { features ->
            log("[NEARBY_STOPS_UI] Circle clicked, features.size=${features.size}")
            val feature = features.firstOrNull()
            val stopId = feature?.getStringProperty("stopId")
            log("[NEARBY_STOPS_UI] Clicked stopId=$stopId")
            stops.find { it.stopId == stopId }?.let(onStopClick)
            ClickResult.Consume
        },
    )

    // Hit target for easier clicking
    CircleLayer(
        id = "nearby-stops-hit",
        source = geoJsonSource,
        radius = const(20.dp),
        color = const(Color.Transparent),
    )
}

// Mapper to GeoJSON
private fun List<NearbyStopFeature>.toFeatureCollection(): FeatureCollection<*, *> {
    log("[NEARBY_STOPS_UI] toFeatureCollection: converting ${this.size} stops")

    if (isEmpty()) {
        // Return a dummy feature to avoid serialization issues with empty collections
        val emptyFeature = org.maplibre.spatialk.geojson.Feature(
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
        org.maplibre.spatialk.geojson.Feature(
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
