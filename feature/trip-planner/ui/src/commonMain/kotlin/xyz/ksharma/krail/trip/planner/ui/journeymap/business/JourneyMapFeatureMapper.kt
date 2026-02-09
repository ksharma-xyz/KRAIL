package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.state.GeoJsonFeatureTypes
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.core.maps.state.geoJsonProperties
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyLegFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.RouteSegment

/**
 * Mapper to convert JourneyMapUiState to MapLibre GeoJSON FeatureCollection.
 * Uses reusable GeoJSON infrastructure from core:maps.
 */
object JourneyMapFeatureMapper {

    /**
     * Converts JourneyMapUiState.Ready to a FeatureCollection for MapLibre.
     */
    fun JourneyMapUiState.Ready.toFeatureCollection(): FeatureCollection<*, *> {
        log(
            "JourneyMapFeatureMapper.toFeatureCollection: " +
                "legs=${mapDisplay.legs.size}, stops=${mapDisplay.stops.size}",
        )

        val legFeatures = mapDisplay.legs.mapNotNull { it.toGeoJsonFeature() }
        val stopFeatures = mapDisplay.stops
            .filter { it.position != null }
            .map { it.toGeoJsonFeature() }

        val allFeatures = legFeatures + stopFeatures

        log("JourneyMapFeatureMapper.toFeatureCollection: converted features count=${allFeatures.size}")

        if (allFeatures.isEmpty()) {
            // Return a harmless dummy feature so the GeoJson polymorphic serializer
            // can determine concrete types and not throw on empty collections.
            val emptyFeature = Feature(
                geometry = Point(Position(longitude = 151.2057, latitude = -33.8727)),
                properties = geoJsonProperties {
                    property(GeoJsonPropertyKeys.TYPE, GeoJsonFeatureTypes.EMPTY)
                },
            )
            return FeatureCollection(features = listOf(emptyFeature))
        }

        return FeatureCollection(features = allFeatures)
    }

    /**
     * Convert JourneyLegFeature to GeoJSON Feature.
     */
    private fun JourneyLegFeature.toGeoJsonFeature(): Feature<*, *>? {
        return when (val segment = routeSegment) {
            is RouteSegment.PathSegment -> createPathSegmentFeature(segment)
            is RouteSegment.StopConnectorSegment -> createStopConnectorFeature(segment)
        }
    }

    /**
     * Create GeoJSON feature for walking/path segments.
     */
    private fun JourneyLegFeature.createPathSegmentFeature(
        segment: RouteSegment.PathSegment,
    ): Feature<*, *>? {
        if (segment.points.isEmpty()) return null

        // IMPORTANT: Position expects (longitude, latitude) - REVERSED from API!
        val positions = segment.points.map { latLng ->
            Position(longitude = latLng.longitude, latitude = latLng.latitude)
        }

        val isWalking = transportMode == null

        return Feature(
            geometry = LineString(positions),
            properties = geoJsonProperties {
                property(GeoJsonPropertyKeys.TYPE, GeoJsonFeatureTypes.JOURNEY_LEG)
                property(GeoJsonPropertyKeys.LEG_ID, legId)
                property(GeoJsonPropertyKeys.COLOR, lineColor) // Use actual line color from leg
                property(GeoJsonPropertyKeys.IS_WALKING, isWalking)
                propertyIfNotNull(GeoJsonPropertyKeys.LINE_NAME, lineName ?: if (isWalking) "Walking" else null)
                transportMode?.let { mode ->
                    property(GeoJsonPropertyKeys.MODE_TYPE, mode.productClass)
                }
            },
        )
    }

    /**
     * Create GeoJSON feature for transit stop connector segments.
     */
    private fun JourneyLegFeature.createStopConnectorFeature(
        segment: RouteSegment.StopConnectorSegment,
    ): Feature<*, *>? {
        val validStops = segment.stops.filter { it.position != null }
        if (validStops.size < 2) return null

        // IMPORTANT: Position expects (longitude, latitude) - REVERSED from API!
        val positions = validStops.mapNotNull { stop ->
            stop.position?.let { pos ->
                Position(longitude = pos.longitude, latitude = pos.latitude)
            }
        }

        return Feature(
            geometry = LineString(positions),
            properties = geoJsonProperties {
                property(GeoJsonPropertyKeys.TYPE, GeoJsonFeatureTypes.JOURNEY_LEG)
                property(GeoJsonPropertyKeys.LEG_ID, legId)
                property(GeoJsonPropertyKeys.COLOR, lineColor) // Use actual line color from leg
                property(GeoJsonPropertyKeys.IS_WALKING, false)
                propertyIfNotNull(GeoJsonPropertyKeys.LINE_NAME, lineName)
                transportMode?.let { mode ->
                    property(GeoJsonPropertyKeys.MODE_TYPE, mode.productClass)
                }
            },
        )
    }

    /**
     * Convert JourneyStopFeature to GeoJSON Feature.
     */
    private fun JourneyStopFeature.toGeoJsonFeature(): Feature<*, *> {
        val pos = position!! // Already filtered in toFeatureCollection

        // IMPORTANT: Position expects (longitude, latitude) - REVERSED from API!
        return Feature(
            geometry = Point(Position(longitude = pos.longitude, latitude = pos.latitude)),
            properties = geoJsonProperties {
                property(GeoJsonPropertyKeys.TYPE, GeoJsonFeatureTypes.JOURNEY_STOP)
                property(GeoJsonPropertyKeys.STOP_ID, stopId)
                property(GeoJsonPropertyKeys.STOP_NAME, stopName)
                property(GeoJsonPropertyKeys.STOP_TYPE, stopType.name)
                propertyIfNotNull(GeoJsonPropertyKeys.TIME, time)
                propertyIfNotNull(GeoJsonPropertyKeys.PLATFORM, platform)
            },
        )
    }
}
