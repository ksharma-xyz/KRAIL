package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.state.RouteFeature
import xyz.ksharma.krail.core.maps.state.StopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState

object StopResultsMapMapper {
    fun MapUiState.Ready.toFeatureCollection(): FeatureCollection<*, *> {
        // Debug: log incoming route / stop payload summary
        val routeIds = this.mapDisplay.routes.map { it.id }
        val stopIds = this.mapDisplay.stops.map { it.stopId }
        log(
            "StopResultsMapMapper.toFeatureCollection: routes=${routeIds.joinToString()} " +
                "(count=${routeIds.size}), stops=${stopIds.joinToString()} (count=${stopIds.size})",
        )

        val routeFeatures = this.mapDisplay.routes.map(::toRouteFeature)
        val stopFeatures = this.mapDisplay.stops.map(::toStopFeature)
        val features = routeFeatures + stopFeatures

        log("StopResultsMapMapper.toFeatureCollection: converted features count=${features.size}")

        if (features.isEmpty()) {
            // return a harmless dummy feature so the GeoJson polymorphic serializer
            // can determine concrete types and not throw on empty collections.
            val emptyFeature = Feature(
                geometry = Point(Position(longitude = 151.2057, latitude = -33.8727)),
                properties = buildJsonObject {
                    put("type", JsonPrimitive("empty"))
                },
            )
            return FeatureCollection(features = listOf(emptyFeature))
        }

        return FeatureCollection(features = features)
    }

    private fun toRouteFeature(route: RouteFeature): Feature<*, *> {
        // Position expects (longitude, latitude)
        val positions = route.points.map {
            Position(
                longitude = it.longitude,
                latitude = it.latitude,
            )
        }
        return Feature(
            geometry = LineString(positions),
            properties = buildJsonObject {
                put("type", JsonPrimitive("route"))
                put("lineId", JsonPrimitive(route.id))
                put("color", JsonPrimitive(route.colorHex))
            },
        )
    }

    private fun toStopFeature(stop: StopFeature): Feature<*, *> {
        // Position expects (longitude, latitude)
        return Feature(
            geometry = Point(
                Position(
                    longitude = stop.position.longitude,
                    latitude = stop.position.latitude,
                ),
            ),
            properties = buildJsonObject {
                put("type", JsonPrimitive("stop"))
                put("stopId", JsonPrimitive(stop.stopId))
                put("stopName", JsonPrimitive(stop.stopName))
                put("lineId", JsonPrimitive(stop.lineId ?: ""))
            },
        )
    }
}
