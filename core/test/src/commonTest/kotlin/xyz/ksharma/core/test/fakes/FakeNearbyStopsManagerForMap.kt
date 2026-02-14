package xyz.ksharma.core.test.fakes

import kotlinx.coroutines.CoroutineScope
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.NearbyStopsManager
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState

/**
 * Fake implementation of [NearbyStopsManager] for testing.
 * Allows control over behavior and verification of interactions.
 */
class FakeNearbyStopsManagerForMap : NearbyStopsManager {

    // State tracking
    var loadNearbyStopsCallCount = 0
        private set
    var invalidateCacheCallCount = 0
        private set
    var cancelOngoingQueryCallCount = 0
        private set

    // Last call parameters
    var lastMapState: MapUiState.Ready? = null
        private set
    var lastCenter: LatLng? = null
        private set
    var lastScope: CoroutineScope? = null
        private set

    // Configurable behavior
    var stopsToReturn: List<NearbyStop> = emptyList()
    var shouldUseCachedResults: Boolean = false
    var errorToThrow: Throwable? = null
    var delayBeforeResult: Long = 0

    // Captured callbacks
    private var onLoadingStateChanged: ((Boolean) -> Unit)? = null
    private var onStopsLoaded: ((List<NearbyStop>) -> Unit)? = null
    private var onError: ((Throwable) -> Unit)? = null

    override fun loadNearbyStops(
        mapState: MapUiState.Ready,
        center: LatLng,
        scope: CoroutineScope,
        onLoadingStateChanged: (Boolean) -> Unit,
        onStopsLoaded: (List<NearbyStop>) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        loadNearbyStopsCallCount++
        lastMapState = mapState
        lastCenter = center
        lastScope = scope
        this.onLoadingStateChanged = onLoadingStateChanged
        this.onStopsLoaded = onStopsLoaded
        this.onError = onError

        if (shouldUseCachedResults) {
            // Do nothing - simulate cache hit
            return
        }

        // Simulate loading state
        onLoadingStateChanged(true)

        // Simulate async behavior if needed
        if (delayBeforeResult > 0) {
            // In real tests, you'd use TestCoroutineScope and advanceTimeBy
            // For now, just invoke callbacks immediately
        }

        errorToThrow?.let {
            onLoadingStateChanged(false)
            onError(it)
        } ?: run {
            onStopsLoaded(stopsToReturn)
            onLoadingStateChanged(false)
        }
    }

    override fun invalidateCache() {
        invalidateCacheCallCount++
        shouldUseCachedResults = false
    }

    override fun cancelOngoingQuery() {
        cancelOngoingQueryCallCount++
    }

    /**
     * Manually trigger loading state change (for testing)
     */
    fun triggerLoadingStateChange(isLoading: Boolean) {
        onLoadingStateChanged?.invoke(isLoading)
    }

    /**
     * Manually trigger stops loaded (for testing)
     */
    fun triggerStopsLoaded(stops: List<NearbyStop>) {
        onStopsLoaded?.invoke(stops)
    }

    /**
     * Manually trigger error (for testing)
     */
    fun triggerError(error: Throwable) {
        onError?.invoke(error)
    }

    /**
     * Reset all state for next test
     */
    fun reset() {
        loadNearbyStopsCallCount = 0
        invalidateCacheCallCount = 0
        cancelOngoingQueryCallCount = 0
        lastMapState = null
        lastCenter = null
        lastScope = null
        stopsToReturn = emptyList()
        shouldUseCachedResults = false
        errorToThrow = null
        delayBeforeResult = 0
        onLoadingStateChanged = null
        onStopsLoaded = null
        onError = null
    }
}

