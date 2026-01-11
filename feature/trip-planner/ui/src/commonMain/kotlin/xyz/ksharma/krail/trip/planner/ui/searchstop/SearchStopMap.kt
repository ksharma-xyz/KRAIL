package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.switch
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
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature.Companion.getStringProperty
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsMapMapper.toFeatureCollection
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState

@Composable
fun SearchStopMap(
    mapUiState: MapUiState,
    modifier: Modifier = Modifier,
) {
    data class SelectedStopUi(val id: String?, val name: String?, val lineId: String?)

    var selectedStop by remember { mutableStateOf<SelectedStopUi?>(null) }
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = -33.8727, longitude = 151.2057),
            zoom = 13.0,
        ),
    )

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState,
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
            options = MapOptions(
                ornamentOptions = OrnamentOptions(
                    padding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    isLogoEnabled = false,
                    logoAlignment = Alignment.BottomStart,
                    isAttributionEnabled = true,
                    attributionAlignment = Alignment.BottomEnd,
                    isCompassEnabled = true,
                    compassAlignment = Alignment.TopEnd,
                    isScaleBarEnabled = false,
                    scaleBarAlignment = Alignment.TopStart,
                )
            )
        ) {
            when (mapUiState) {
                is MapUiState.Error -> {}
                MapUiState.Loading -> {}
                is MapUiState.Ready -> {
                    // Debug logging omitted for brevity (kept in original file)

                    val transitSource =
                        rememberGeoJsonSource(data = GeoJsonData.Features(mapUiState.toFeatureCollection()))

                    // Line for route
                    LineLayer(
                        id = "route",
                        source = transitSource,
                        filter = get("type").asString() eq const("route"),
                        color = const(themeColor()),
                        width = const(10.dp),
                        cap = const(LineCap.Round),
                        join = const(LineJoin.Round),
                    )

                    // visible stop dots (radius depends on selectedStop)
                    CircleLayer(
                        id = "stops-visible",
                        source = transitSource,
                        filter = get("type").asString() eq const("stop"),
                        // If stopId == selected -> bigger radius, otherwise default
                        radius =
                            switch(
                                input = get("stopId").asString(),
                                case(selectedStop?.id ?: "__NONE__", const(12.dp)), // selected -> larger
                                fallback = const(6.dp), // default
                            ),
                        // optional: also change fill color for selected
                        color =
                            switch(
                                input = get("stopId").asString(),
                                case(selectedStop?.id ?: "__NONE__", const(Color.White)),
                                fallback = const(Color.White),
                            ),
                        strokeColor = get("color").asString().convertToColor(),
                        strokeWidth = const(2.dp),
                    )

                    // hit target layer (transparent) with toggle behavior
                    CircleLayer(
                        id = "stops-hit",
                        source = transitSource,
                        filter = get("type").asString() eq const("stop"),
                        radius = const(18.dp),
                        color = const(Color.White.copy(alpha = 0.01f)),
                        strokeColor = const(Color.Transparent),
                        strokeWidth = const(0.dp),
                        onClick = { features ->
                            val feature = features.firstOrNull()
                            val clickedId = feature?.getStringProperty("stopId")
                            val clickedName = feature?.getStringProperty("stopName")
                            val clickedLine = feature?.getStringProperty("lineId")

                            // toggle: if same id clicked again -> clear selection, else set selection
                            selectedStop =
                                if (clickedId != null && selectedStop?.id == clickedId) {
                                    null
                                } else {
                                    clickedId?.let { id ->
                                        SelectedStopUi(
                                            id = id,
                                            name = clickedName,
                                            lineId = clickedLine,
                                        )
                                    }
                                }

                            ClickResult.Consume
                        },
                    )


                }
            }
        }

        if (selectedStop != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(KrailTheme.colors.surface)
                    .padding(vertical = 24.dp, horizontal = 16.dp)
                    .align(Alignment.BottomCenter),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedStop?.name ?: "Selected stop",
                            style = KrailTheme.typography.titleMedium,
                        )
                        Text(
                            text = listOfNotNull(
                                selectedStop?.id,
                                selectedStop?.lineId
                            ).joinToString(" • "),
                            style = KrailTheme.typography.bodySmall,
                            color = KrailTheme.colors.onSurface.copy(alpha = 0.7f),
                        )
                    }

                    Image(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = "Close",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .klickable {
                                selectedStop = null
                            }
                            .padding(4.dp),
                        colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                    )
                }
            }
        }
    }
}

