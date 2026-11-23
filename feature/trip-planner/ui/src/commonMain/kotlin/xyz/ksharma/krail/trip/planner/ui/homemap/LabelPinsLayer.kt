package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature.Companion.getStringProperty
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.maps.state.GeoJsonFeatureTypes
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.core.maps.state.geoJsonProperties
import xyz.ksharma.krail.taj.theme.KrailTheme
import org.maplibre.spatialk.geojson.Feature as GeoJsonFeature

private const val STOP_ID_PROPERTY = "stopId"
private const val GLYPH_PROPERTY = "glyph"
private const val LABEL_PIN_TYPE = "label_pin"
private val LabelPinRadius = 14.dp
private val HitTargetRadius = 24.dp
private val StrokeWidth = 2.dp
private const val GLYPH_TEXT_SIZE_EM = 1.1f
private const val GLYPH_VERTICAL_OFFSET_EM = 0.03f

/**
 * The rider's Home and Work on the map: a disc in the app's own ink with the label's first letter in
 * the surface colour, so it reads against the `P` discs (the theme colour) and the stops (the mode
 * colours) without a fourth hue. A tap on it is a tap on its stop.
 *
 * Positions come from the local stops table, so this layer needs no network and has no bearing on the
 * polling lifecycle (`docs/POLLING_LIFECYCLE.md`). The glyph is a letter and not the label's emoji
 * because the map's fonts cannot be trusted to draw one.
 */
@Composable
internal fun LabelPinsLayer(
    labels: List<LabelPin>,
    onLabelClick: (LabelPin) -> Unit,
) {
    val source = rememberGeoJsonSource(data = GeoJsonData.Features(labels.toFeatureCollection()))

    CircleLayer(
        id = "home-label-pins-circle",
        source = source,
        radius = const(LabelPinRadius),
        color = const(KrailTheme.colors.onSurface),
        strokeColor = const(Color.White),
        strokeWidth = const(StrokeWidth),
    )

    SymbolLayer(
        id = "home-label-pins-glyph",
        source = source,
        textField = format(span(Feature[GLYPH_PROPERTY].asString())),
        textFont = const(listOf("Noto Sans Bold")),
        textSize = const(GLYPH_TEXT_SIZE_EM.em),
        textColor = const(KrailTheme.colors.surface),
        textOffset = offset(0f.em, GLYPH_VERTICAL_OFFSET_EM.em),
        textAllowOverlap = const(true),
    )

    // The larger invisible hit target goes last so it sits on top and takes the tap first, the way the
    // stop and station layers do. A 14 dp disc is an awkward target.
    CircleLayer(
        id = "home-label-pins-hit",
        source = source,
        radius = const(HitTargetRadius),
        color = const(Color.Transparent),
        onClick = { features ->
            val stopId = features.firstOrNull()?.getStringProperty(STOP_ID_PROPERTY)
            labels.firstOrNull { it.stopId == stopId }?.let(onLabelClick)
            ClickResult.Consume
        },
    )
}

private fun List<LabelPin>.toFeatureCollection(): FeatureCollection<*, *> {
    // MapLibre cannot serialise an empty collection, so an off-screen placeholder stands in, the same
    // workaround the stop and station layers use.
    if (isEmpty()) {
        return FeatureCollection(
            features = listOf(
                GeoJsonFeature(
                    geometry = Point(coordinates = Position(latitude = 0.0, longitude = 0.0)),
                    properties = geoJsonProperties {
                        property(GeoJsonPropertyKeys.TYPE, GeoJsonFeatureTypes.EMPTY)
                    },
                ),
            ),
        )
    }
    return FeatureCollection(
        features = map { pin ->
            GeoJsonFeature(
                geometry = Point(
                    coordinates = Position(latitude = pin.position.latitude, longitude = pin.position.longitude),
                ),
                properties = geoJsonProperties {
                    property(GeoJsonPropertyKeys.TYPE, LABEL_PIN_TYPE)
                    property(STOP_ID_PROPERTY, pin.stopId)
                    property(GLYPH_PROPERTY, labelGlyph(pin.labelKey))
                },
            )
        },
    )
}
