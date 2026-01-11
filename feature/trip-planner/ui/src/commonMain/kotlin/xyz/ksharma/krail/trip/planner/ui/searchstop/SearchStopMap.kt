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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
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
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsMapMapper.toFeatureCollection
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import kotlin.toString

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
                is MapUiState.Error -> {

                }

                MapUiState.Loading -> {

                }

                is MapUiState.Ready -> {
                    // Debug: log what the composable received
                    val routeIds = mapUiState.mapDisplay.routes.map { it.id }
                    val stopIds = mapUiState.mapDisplay.stops.map { it.stopId }
                    log("SearchStopMap: Ready received routes=${routeIds.joinToString()} (count=${routeIds.size}), stops=${stopIds.joinToString()} (count=${stopIds.size})")

                    try {
                        val featureCollection = mapUiState.toFeatureCollection()
                        log("SearchStopMap: featureCollection.features.size=${featureCollection.features.size}")

                        featureCollection.features.forEachIndexed { idx, feature ->
                            // Serialize properties to a JSON string and parse with regex to avoid relying on
                            // kotlinx.serialization json extensions that were causing receiver/type resolution issues.
                            val propsStr = feature.properties?.toString() ?: "{}"

                            val typeMatch = Regex("\"type\"\\s*:\\s*\"([^\"]+)\"").find(propsStr)
                            val propType = typeMatch?.groupValues?.getOrNull(1) ?: "unknown"

                            val stopIdMatch = Regex("\"stopId\"\\s*:\\s*\"([^\"]+)\"").find(propsStr)
                            val lineIdMatch = Regex("\"lineId\"\\s*:\\s*\"([^\"]+)\"").find(propsStr)
                            val propId = stopIdMatch?.groupValues?.getOrNull(1)
                                ?: lineIdMatch?.groupValues?.getOrNull(1)
                                ?: "no-id"

                            val geomSummary = when (val g = feature.geometry) {
                                is org.maplibre.spatialk.geojson.Point -> {
                                    val p = g.coordinates
                                    "Point(lat=${p.latitude}, lon=${p.longitude})"
                                }
                                is org.maplibre.spatialk.geojson.LineString -> {
                                    val coords = g.coordinates.take(5).joinToString(";") { "${it.latitude},${it.longitude}" }
                                    "LineString(len=${g.coordinates.size}, sample=[$coords])"
                                }
                                null -> "geometry=null"
                                else -> "geometry=${g::class.simpleName}"
                            }

                            log("SearchStopMap: feature[$idx] type=$propType id=$propId geom=$geomSummary properties=$propsStr")
                        }
                    } catch (t: Throwable) {
                        log("SearchStopMap: feature collection inspect failed: ${t.message}")
                    }

                    val transitSource = rememberGeoJsonSource(data = GeoJsonData.Features(mapUiState.toFeatureCollection()))

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

                    // visible stop dot
                    CircleLayer(
                        id = "stops-visible",
                        source = transitSource,
                        filter = get("type").asString() eq const("stop"),
                        radius = const(6.dp),
                        color = const(Color.Black),
                        strokeColor = const(KrailTheme.colors.onSurface),
                        strokeWidth = const(2.dp),
                    )

                    // larger hit target, translucent, to receive clicks
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
                            log("Map dot clicked")
                            // Safely read properties without relying on companion extensions/generics
                            val id = feature?.properties.let { props ->
                                props?.get("stopId")?.jsonPrimitive?.contentOrNull
                            }
                            val name = feature?.properties.let { props ->
                                props?.get("stopName")?.jsonPrimitive?.contentOrNull
                            }
                            val lineId = feature?.properties.let { props ->
                                props?.get("lineId")?.jsonPrimitive?.contentOrNull
                            }

                            selectedStop = SelectedStopUi(id = id, name = name, lineId = lineId)
                            //onStopSelectId(id)
                            ClickResult.Consume
                        }
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
                                selectedStop = null;
                                //    onStopSelectId(null)
                            }
                            .padding(4.dp),
                        colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                    )
                }
            }
        }
    }
}
