package xyz.ksharma.krail.core.maps.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.sandook.NswStopsSandook
import xyz.ksharma.krail.sandook.SelectStopsNearby
import xyz.ksharma.krail.sandook.utils.GeoUtils
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

internal class RealNearbyStopsRepository(
    private val nswStopsSandook: NswStopsSandook,
    private val ioDispatcher: CoroutineDispatcher,
) : NearbyStopsRepository {

    override suspend fun getStopsNearby(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        productClasses: Set<Int>,
        maxResults: Int,
    ): List<NearbyStop> = withContext(ioDispatcher) {
        logQueryParameters(centerLat, centerLon, radiusKm, productClasses, maxResults)

        val bounds = calculateAndLogBoundingBox(centerLat, centerLon, radiusKm)
            ?: return@withContext emptyList()

        val results = executeNearbyStopsQuery(
            centerLat = centerLat,
            centerLon = centerLon,
            radiusKm = radiusKm,
            productClasses = productClasses,
            maxResults = maxResults,
            bounds = bounds,
        ) ?: return@withContext emptyList()

        log("[NEARBY_STOPS_REPO] SQL query returned ${results.size} raw results")

        if (results.isEmpty()) {
            logEmptyResults()
        }

        val mappedResults = mapResultsToDomain(results)
        log("[NEARBY_STOPS_REPO] Returning ${mappedResults.size} mapped results")
        mappedResults
    }

    private fun logQueryParameters(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        productClasses: Set<Int>,
        maxResults: Int,
    ) {
        log(
            "[NEARBY_STOPS_REPO] getStopsNearby called with: centerLat=$centerLat, " +
                "centerLon=$centerLon, radiusKm=$radiusKm, productClasses=$productClasses," +
                " maxResults=$maxResults",
        )
    }

    private fun calculateAndLogBoundingBox(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
    ) = GeoUtils.calculateBoundingBox(centerLat, centerLon, radiusKm)?.also { bounds ->
        log(
            "[NEARBY_STOPS_REPO] Bounding box: SW(${bounds.southwest.latitude}, " +
                "${bounds.southwest.longitude}) - NE(${bounds.northeast.latitude}, " +
                "${bounds.northeast.longitude})",
        )
    } ?: run {
        log("[NEARBY_STOPS_REPO] ERROR: Failed to calculate bounding box. Returning empty list.")
        null
    }

    @Suppress("LongParameterList")
    private fun executeNearbyStopsQuery(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        productClasses: Set<Int>,
        maxResults: Int,
        bounds: xyz.ksharma.krail.core.maps.state.BoundingBox,
    ): List<SelectStopsNearby>? = runCatching {
        nswStopsSandook.selectStopsNearby(
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
        )
    }.onFailure { error ->
        log("[NEARBY_STOPS_REPO] ERROR: Database query failed - ${error.message}")
    }.getOrNull() ?: run {
        log("[NEARBY_STOPS_REPO] Returning empty list due to query error")
        null
    }

    private fun logEmptyResults() {
        log("[NEARBY_STOPS_REPO] WARNING: No results from database!")
        val allStopsCount = runCatching {
            nswStopsSandook.selectStopsCount()
        }.onFailure { error ->
            log("[NEARBY_STOPS_REPO] ERROR: Failed to count all stops - ${error.message}")
        }.getOrNull() ?: -1L

        log("[NEARBY_STOPS_REPO] Total stops in database: $allStopsCount")
    }

    private fun mapResultsToDomain(results: List<SelectStopsNearby>): List<NearbyStop> =
        results.mapNotNull { row ->
            runCatching {
                val distanceKm = kotlin.math.sqrt(row.distanceSquared)

                if (!distanceKm.isFinite() || distanceKm < 0) {
                    log("[NEARBY_STOPS_REPO] Invalid distance for stop ${row.stopId}: $distanceKm")
                    return@mapNotNull null
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
                    transportModes = modes,
                )
            }.onFailure { error ->
                log("[NEARBY_STOPS_REPO] Error mapping stop ${row.stopId}: ${error.message}")
            }.getOrNull()
        }
}
