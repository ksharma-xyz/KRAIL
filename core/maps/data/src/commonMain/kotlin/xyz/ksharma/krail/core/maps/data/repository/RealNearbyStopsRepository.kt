package xyz.ksharma.krail.core.maps.data.repository

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.sandook.KrailSandook
import xyz.ksharma.krail.sandook.utils.GeoUtils
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

class RealNearbyStopsRepository(
    private val database: KrailSandook,
) : NearbyStopsRepository {

    override suspend fun getStopsNearby(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        productClasses: Set<Int>,
        maxResults: Int,
    ): List<NearbyStop> {
        log(
            "[NEARBY_STOPS_REPO] getStopsNearby called with: centerLat=$centerLat, centerLon=$centerLon, radiusKm=$radiusKm, productClasses=$productClasses, maxResults=$maxResults",
        )

        // Calculate bounding box for pre-filtering
        val bounds = GeoUtils.calculateBoundingBox(centerLat, centerLon, radiusKm)
        log(
            "[NEARBY_STOPS_REPO] Bounding box: SW(${bounds.southwest.latitude}, ${bounds.southwest.longitude}) - NE(${bounds.northeast.latitude}, ${bounds.northeast.longitude})",
        )

        // Execute query
        log("[NEARBY_STOPS_REPO] Executing SQL query...")
        val results = database.nswStopsQueries.selectStopsNearby(
            centerLat = centerLat,
            centerLon = centerLon,
            minLat = bounds.southwest.latitude,
            maxLat = bounds.northeast.latitude,
            minLon = bounds.southwest.longitude,
            maxLon = bounds.northeast.longitude,
            radiusKm = radiusKm,
            filterByModes = if (productClasses.isEmpty()) 0 else 1,
            productClasses = productClasses.map { it.toLong() },
            maxResults = maxResults.toLong(),
        ).executeAsList()

        log("[NEARBY_STOPS_REPO] SQL query returned ${results.size} raw results")

        if (results.isEmpty()) {
            log("[NEARBY_STOPS_REPO] WARNING: No results from database!")
            // Let's check if there are any stops at all
            val allStopsCount = try {
                database.nswStopsQueries.selectStopsCount().executeAsOne()
            } catch (e: Exception) {
                log("[NEARBY_STOPS_REPO] ERROR: Failed to count all stops - ${e.message}")
                -1L
            }
            log("[NEARBY_STOPS_REPO] Total stops in database: $allStopsCount")
        }

        // Map to domain model
        val mappedResults = results.mapIndexed { index, row ->
            // Convert squared distance back to actual distance in km
            // distanceSquared is in km² (already scaled by the formula)
            // So we just need to take the square root
            val distanceKm = kotlin.math.sqrt(row.distanceSquared)

            if (index < 3) {
                log(
                    "[NEARBY_STOPS_REPO] Row $index: stopId=${row.stopId}, stopName=${row.stopName}, lat=${row.stopLat}, lon=${row.stopLon}, distanceSquared=${row.distanceSquared}, distance=${distanceKm}km, productClasses='${row.productClasses}'",
                )
            }

            val modes = row.productClasses
                .split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { it.toIntOrNull() }
                .mapNotNull { TransportMode.toTransportModeType(it) }

            NearbyStop(
                stopId = row.stopId,
                stopName = row.stopName,
                latitude = row.stopLat,
                longitude = row.stopLon,
                distanceKm = distanceKm,
                transportModes = modes,
            )
        }

        log("[NEARBY_STOPS_REPO] Returning ${mappedResults.size} mapped results")
        return mappedResults
    }
}
