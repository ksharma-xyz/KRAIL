package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.maps.state.GeoJsonFeatureTypes
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.geoJsonProperties
import xyz.ksharma.krail.taj.theme.KrailTheme
import org.maplibre.spatialk.geojson.Feature as GeoJsonFeature

/**
 * Renders user's current location on the map.
 *
 * - Outer ring: static 2dp stroke, transparent fill — always visible boundary.
 * - Inner dot: solid fill, breathes between 80% and 20% of the outer radius, slow 2s cycle.
 *   All sizing is derived from [outerRadius] — change that one value to resize the whole dot.
 *
 * @param userLocation User's current location, or null if unknown
 */
@Composable
fun UserLocationLayer(
    userLocation: LatLng?,
) {
    if (userLocation == null) {
        // No location to render
        return
    }

    val locationColor = KrailTheme.colors.userLocationDot

    // Single source of truth — all sizing derived from outerRadius.
    val outerRadius = 10f
    val innerRadiusMax = outerRadius * 0.8f // 8dp — largest inner size, 2dp gap from outer
    val innerRadiusMin = outerRadius * 0.6f // 2dp — smallest inner size

    // Inner breathes between innerRadiusMax and innerRadiusMin; 2s per half-cycle = 4s full breath.
    val infiniteTransition = rememberInfiniteTransition(label = "user-location-pulse")
    val innerRadius by infiniteTransition.animateFloat(
        initialValue = innerRadiusMax,
        targetValue = innerRadiusMin,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "inner-radius",
    )

    // Create GeoJSON feature for user location
    val feature = GeoJsonFeature(
        geometry = Point(
            coordinates = Position(
                latitude = userLocation.latitude,
                longitude = userLocation.longitude,
            ),
        ),
        properties = geoJsonProperties {
            property(GeoJsonPropertyKeys.TYPE, GeoJsonFeatureTypes.USER_LOCATION)
        },
    )

    val featureCollection = FeatureCollection(features = listOf(feature))
    val geoJsonSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(featureCollection),
    )

    // Outer ring — static boundary, transparent fill so only the 2dp stroke is visible.
    CircleLayer(
        id = "user-location-outer",
        source = geoJsonSource,
        radius = const(outerRadius.dp),
        color = const(Color.Transparent),
        strokeColor = const(locationColor),
        strokeWidth = const(2.dp),
    )

    // Inner dot — breathes slowly between 90% and 50% of the outer radius.
    CircleLayer(
        id = "user-location-circle",
        source = geoJsonSource,
        radius = const(innerRadius.dp),
        color = const(locationColor),
    )
}
