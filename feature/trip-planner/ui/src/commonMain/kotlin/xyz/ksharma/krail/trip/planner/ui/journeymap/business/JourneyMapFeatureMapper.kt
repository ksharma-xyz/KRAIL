package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyLegFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.RouteSegment

/**
 * Mapper to convert JourneyMapUiState to MapLibre GeoJSON FeatureCollection.
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
                properties = buildJsonObject {
                    put("type", JsonPrimitive("empty"))
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
            is RouteSegment.PathSegment -> {
                if (segment.points.isEmpty()) return null

                // IMPORTANT: Position expects (longitude, latitude) - REVERSED from API!
                val positions = segment.points.map { latLng ->
                    Position(longitude = latLng.longitude, latitude = latLng.latitude)
                }

                Feature(
                    geometry = LineString(positions),
                    properties = buildJsonObject {
                        put("type", JsonPrimitive("journey_leg"))
                        put("legId", JsonPrimitive(legId))
                        // Walking paths - use gray color
                        put("color", JsonPrimitive("#757575"))
                        put("isWalking", JsonPrimitive(true))
                        put("lineName", JsonPrimitive("Walking"))
                    },
                )
            }

            is RouteSegment.StopConnectorSegment -> {
                val validStops = segment.stops.filter { it.position != null }
                if (validStops.size < 2) return null

                // IMPORTANT: Position expects (longitude, latitude) - REVERSED from API!
                val positions = validStops.mapNotNull { stop ->
                    stop.position?.let { pos ->
                        Position(
                            longitude = pos.longitude,
                            latitude = pos.latitude,
                        )
                    }
                }

                Feature(
                    geometry = LineString(positions),
                    properties = buildJsonObject {
                        put("type", JsonPrimitive("journey_leg"))
                        put("legId", JsonPrimitive(legId))
                        // Use color from TransportMode if available, otherwise default
                        val color = transportMode?.colorCode ?: "#666666"
                        put("color", JsonPrimitive(color))
                        put("isWalking", JsonPrimitive(false))
                        transportMode?.let { mode ->
                            put("modeType", JsonPrimitive(mode.productClass))
                            put("lineName", JsonPrimitive(mode.name))
                        }
                    },
                )
            }
        }
    }

    /**
     * Convert JourneyStopFeature to GeoJSON Feature.
     */
    private fun JourneyStopFeature.toGeoJsonFeature(): Feature<*, *> {
        val pos = position!! // Already filtered in toFeatureCollection

        // IMPORTANT: Position expects (longitude, latitude) - REVERSED from API!
        return Feature(
            geometry = Point(
                Position(
                    longitude = pos.longitude,
                    latitude = pos.latitude,
                ),
            ),
            properties = buildJsonObject {
                put("type", JsonPrimitive("journey_stop"))
                put("stopId", JsonPrimitive(stopId))
                put("stopName", JsonPrimitive(stopName))
                put("stopType", JsonPrimitive(stopType.name))
                time?.let { put("time", JsonPrimitive(it)) }
                platform?.let { put("platform", JsonPrimitive(it)) }
            },
        )
    }
}
