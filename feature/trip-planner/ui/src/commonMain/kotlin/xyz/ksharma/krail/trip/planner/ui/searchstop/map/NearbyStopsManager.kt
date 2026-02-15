package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.sandook.utils.GeoUtils
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Manages nearby stops queries, caching, and loading state.
 * Extracts map-specific logic from ViewModel to keep it focused on event handling.
 */
interface NearbyStopsManager {
    /**
     * Load nearby stops for the given map state and center.
     * Handles caching, debouncing, and error handling.
     */
    @Suppress("LongParameterList")
    fun loadNearbyStops(
        mapState: MapUiState.Ready,
        center: LatLng,
        scope: CoroutineScope,
        onLoadingStateChanged: (Boolean) -> Unit,
        onStopsLoaded: (List<NearbyStop>) -> Unit,
        onError: (Throwable) -> Unit,
    )

    /**
     * Invalidate the cache to force a reload on the next query.
     */
    fun invalidateCache()

    /**
     * Cancel any ongoing query.
     */
    fun cancelOngoingQuery()
}

/**
 * Factory function to create a [NearbyStopsManager] instance.
 */
fun createNearbyStopsManager(
    repository: NearbyStopsRepository,
    ioDispatcher: CoroutineDispatcher,
): NearbyStopsManager = RealNearbyStopsManager(repository, ioDispatcher)

/**
 * Real implementation of [NearbyStopsManager].
 */
@OptIn(ExperimentalTime::class)
@Suppress("TooManyFunctions")
internal class RealNearbyStopsManager(
    private val repository: NearbyStopsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : NearbyStopsManager {
    private var nearbyStopsJob: Job? = null
    private var lastQueryCenter: LatLng? = null
    private var lastQueryTime: Long = 0

    override fun loadNearbyStops(
        mapState: MapUiState.Ready,
        center: LatLng,
        scope: CoroutineScope,
        onLoadingStateChanged: (Boolean) -> Unit,
        onStopsLoaded: (List<NearbyStop>) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        log(
            "[NEARBY_STOPS] loadNearbyStops() called for center: lat=${center.latitude}, " +
                "lon=${center.longitude}",
        )

        if (shouldUseCachedResults(center)) {
            log("[NEARBY_STOPS] Using cached nearby stops")
            return
        }

        startQuery(mapState, center, scope, onLoadingStateChanged, onStopsLoaded, onError)
    }

    override fun invalidateCache() {
        lastQueryCenter = null
        lastQueryTime = 0
        log("[NEARBY_STOPS] Cache invalidated")
    }

    override fun cancelOngoingQuery() {
        nearbyStopsJob?.cancel()
        nearbyStopsJob = null
    }

    @Suppress("LongParameterList")
    private fun startQuery(
        mapState: MapUiState.Ready,
        center: LatLng,
        scope: CoroutineScope,
        onLoadingStateChanged: (Boolean) -> Unit,
        onStopsLoaded: (List<NearbyStop>) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        cancelOngoingQuery()
        onLoadingStateChanged(true)
        log("[NEARBY_STOPS] Loading state set to true")

        nearbyStopsJob = scope.launch(ioDispatcher) {
            delay(NearbyStopsConfig.QUERY_DEBOUNCE_MS)
            log("[NEARBY_STOPS] Debounce complete, starting query...")

            runCatching {
                val stops = fetchNearbyStops(mapState, center)
                logQueryResults(stops)
                onStopsLoaded(stops)
                updateCache(center)
            }.getOrElse { error ->
                handleError(error, onError, onLoadingStateChanged)
            }
        }
    }

    private suspend fun fetchNearbyStops(
        mapState: MapUiState.Ready,
        center: LatLng,
    ): List<NearbyStop> {
        val selectedModes = mapState.mapDisplay.selectedTransportModes
        val radiusKm = mapState.mapDisplay.searchRadiusKm

        logQueryParameters(center, radiusKm, selectedModes)

        return repository.getStopsNearby(
            centerLat = center.latitude,
            centerLon = center.longitude,
            radiusKm = radiusKm,
            productClasses = selectedModes,
            maxResults = NearbyStopsConfig.MAX_NEARBY_RESULTS,
        )
    }

    private fun shouldUseCachedResults(newCenter: LatLng): Boolean {
        val lastCenter = lastQueryCenter ?: return false

        if (isCacheExpired()) return false

        val hasCenterMovedSignificantly = hasCenterMovedSignificantly(lastCenter, newCenter)
        return !hasCenterMovedSignificantly
    }

    private fun isCacheExpired(): Boolean {
        val cacheAge = Clock.System.now().toEpochMilliseconds() - lastQueryTime
        return cacheAge > NearbyStopsConfig.CACHE_EXPIRY_MS
    }

    private fun hasCenterMovedSignificantly(lastCenter: LatLng, newCenter: LatLng): Boolean {
        val distance = GeoUtils.haversineDistance(
            lastCenter.latitude,
            lastCenter.longitude,
            newCenter.latitude,
            newCenter.longitude,
        ) ?: return true // If distance calculation fails, treat as significant movement

        return distance >= NearbyStopsConfig.MIN_DISTANCE_FOR_RELOAD_KM
    }

    private fun updateCache(center: LatLng) {
        lastQueryCenter = center
        lastQueryTime = Clock.System.now().toEpochMilliseconds()
    }

    private fun handleError(
        error: Throwable,
        onError: (Throwable) -> Unit,
        onLoadingStateChanged: (Boolean) -> Unit,
    ) {
        log("[NEARBY_STOPS] ERROR: Query failed - ${error.message}")
        error.printStackTrace()
        onLoadingStateChanged(false)
        onError(error)
    }

    private fun logQueryParameters(
        center: LatLng,
        radiusKm: Double,
        selectedModes: Set<Int>,
    ) {
        log(
            "[NEARBY_STOPS] Query params: centerLat=${center.latitude}, " +
                "centerLon=${center.longitude}, radiusKm=$radiusKm, " +
                "productClasses=$selectedModes, maxResults=${NearbyStopsConfig.MAX_NEARBY_RESULTS}",
        )
    }

    @Suppress("MagicNumber")
    private fun logQueryResults(stops: List<NearbyStop>) {
        log("[NEARBY_STOPS] Query returned ${stops.size} stops")

        stops.take(5).forEach { stop ->
            log(
                "[NEARBY_STOPS] Stop: ${stop.stopName} (${stop.stopId}) - " +
                    "modes=${stop.transportModes.map { it.name }}",
            )
        }
    }
}
