package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_filter
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.ui.config.MapTileProvider.OPEN_FREE_MAP_LIBERTY
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.SubtleButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
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
                MapContent(
                    mapState = mapUiState,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxSize(),
                )
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

    var showOptionsBottomSheet by rememberSaveable { mutableStateOf(false) }

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
            baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_LIBERTY),
            options = MapOptions(
                ornamentOptions = OrnamentOptions(
                    padding = PaddingValues(0.dp),
                    isLogoEnabled = false,
                    isAttributionEnabled = true,
                    attributionAlignment = Alignment.BottomEnd,
                    isCompassEnabled = mapState.mapDisplay.showCompass,
                    compassAlignment = Alignment.TopEnd,
                    isScaleBarEnabled = mapState.mapDisplay.showDistanceScale,
                    scaleBarAlignment = Alignment.TopStart,
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

        // Bottom left buttons (Options and Refresh)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Options button
            Button(
                onClick = { showOptionsBottomSheet = true },
                dimensions = ButtonDefaults.mediumButtonSize(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_filter),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ButtonDefaults.buttonColors().contentColor),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = "Options")
                }
            }

            // Refresh button
            Button(
                onClick = { onEvent(SearchStopUiEvent.ShowStopsHere) },
                enabled = !mapState.isLoadingNearbyStops,
                dimensions = ButtonDefaults.mediumButtonSize(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (mapState.isLoadingNearbyStops) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ButtonDefaults.buttonColors().contentColor,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Image(
                            painter = painterResource(Res.drawable.ic_filter),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(ButtonDefaults.buttonColors().contentColor),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(text = "Refresh")
                }
            }
        }

        // Options Bottom Sheet
        if (showOptionsBottomSheet) {
            MapOptionsBottomSheet(
                searchRadiusKm = mapState.mapDisplay.searchRadiusKm,
                selectedTransportModes = mapState.mapDisplay.selectedTransportModes,
                showDistanceScale = mapState.mapDisplay.showDistanceScale,
                showCompass = mapState.mapDisplay.showCompass,
                onDismiss = { showOptionsBottomSheet = false },
                onEvent = onEvent,
            )
        }
    }
}

