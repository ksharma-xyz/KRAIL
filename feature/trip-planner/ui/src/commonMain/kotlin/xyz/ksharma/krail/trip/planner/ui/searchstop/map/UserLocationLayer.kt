package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.animation.core.EaseOut
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
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import org.maplibre.spatialk.geojson.Feature as GeoJsonFeature

/**
 * Renders user's current location as an animated pulsing circle on the map.
 *
 * Features:
 * - Inner solid circle with theme color (10dp radius)
 * - Outer pulsing circle that animates from 10dp → 20dp with fading alpha
 * - White stroke for visibility
 * - Continuous pulse animation (2 seconds per cycle)
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

    // Get theme color
    val themeColor by LocalThemeColor.current
    val themeComposeColor = themeColor.hexToComposeColor()

    // Pulsing animation - infinite repeating
    val infiniteTransition = rememberInfiniteTransition(label = "user-location-pulse")

    // Outer circle radius: 10dp → 20dp (expands outward)
    val outerRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "outer-radius",
    )

    // Outer circle opacity: 0.3 → 0.0 (fades out as it expands)
    val outerOpacity by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "outer-opacity",
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

    // Outer pulsing circle (animates outward and fades)
    CircleLayer(
        id = "user-location-pulse",
        source = geoJsonSource,
        radius = const(outerRadius.dp),
        color = const(themeComposeColor.copy(alpha = outerOpacity)),
        strokeWidth = const(0.dp), // No stroke for pulse
    )

    // Inner solid circle (main location indicator)
    CircleLayer(
        id = "user-location-circle",
        source = geoJsonSource,
        radius = const(10.dp), // Fixed size, 25% larger than stop circles (8dp)
        color = const(themeComposeColor),
        strokeColor = const(Color.White),
        strokeWidth = const(3.dp), // Thick white stroke for visibility
    )
}
