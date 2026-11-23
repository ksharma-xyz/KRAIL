package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.sandook.NswStopsSandook
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.db.SelectStopsNearby
import xyz.ksharma.krail.sandook.utils.GeoUtils
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * How far from the rider the Intro looks for stations to lead the origin list with. A guess, kept as one constant
 * so it is one line to change: far enough for a suburban station that is a short bus ride away, near enough that
 * "near you" is still true.
 */
internal const val INTRO_NEARBY_RADIUS_KM = 3.0

/**
 * How many stops the nearby query may return. Several stops of one station come back as separate rows and are
 * collapsed afterwards, so it is more than the three stations the list leads with.
 */
internal const val INTRO_NEARBY_QUERY_LIMIT = 10L

/** The most results the typed search shows. The search ranks best first, so the rest is below any rider's scroll. */
internal const val INTRO_SEARCH_RESULT_LIMIT = 20

/** A stop as a list draws it: its id and name, and the modes that serve it for the row's badges. */
internal data class ListedStop(val stop: IntroStop, val modes: List<TransportMode>)

/**
 * What the destination and origin screens read, kept apart from the view model the way [IntroCompletion] and
 * [IntroParkRideGateway] are: the suggested lists, the stations near the rider, the modes of the listed stops, and
 * the typed search. All of it is local. Nothing here makes a network call, and every call runs on the IO
 * dispatcher.
 *
 * The rider's position is used for one query and is not kept: the stops that come back are reduced to ids, names
 * and modes before they leave, so no coordinate of a stop or of the rider reaches the state, a log or analytics.
 */
internal class IntroStopsGateway(
    flag: Flag,
    private val nswStops: NswStopsSandook,
    private val sandook: Sandook,
    private val stopResults: StopResultsManager,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /** Read once, so the lists never change under the rider. */
    val suggestedStops: SuggestedStops = flag.introSuggestedStops()

    /**
     * The train, metro and light rail stops within [INTRO_NEARBY_RADIUS_KM] of [location], nearest first, from the
     * stops table's own nearby query. [location] is used for this one call. An unusable position is no stops.
     */
    suspend fun nearbyStations(location: LatLng): List<ListedStop> = withContext(ioDispatcher) {
        val box = GeoUtils.calculateBoundingBox(location.latitude, location.longitude, INTRO_NEARBY_RADIUS_KM)
            ?: return@withContext emptyList()
        nswStops.selectStopsNearby(
            centerLat = location.latitude,
            centerLon = location.longitude,
            minLat = box.southwest.latitude,
            maxLat = box.northeast.latitude,
            minLon = box.southwest.longitude,
            maxLon = box.northeast.longitude,
            radiusKm = INTRO_NEARBY_RADIUS_KM,
            filterByModes = 1L,
            productClasses = STATION_PRODUCT_CLASSES,
            maxResults = INTRO_NEARBY_QUERY_LIMIT,
        ).map { it.toListedStop() }
    }

    /** The modes of the stops with these ids, for the row badges. A stop the table does not have is left out. */
    suspend fun modesFor(stopIds: List<String>): Map<String, List<TransportMode>> = withContext(ioDispatcher) {
        sandook.selectStopsByIds(stopIds).associate { it.stopId to it.productClasses.toModes() }
    }

    /**
     * The stops that match [query], from the same local stop search the Search Stop screen runs, best first. Routes
     * and addresses are not searched: only a stop can be a trip's end here.
     */
    suspend fun search(query: String): List<ListedStop> = withContext(ioDispatcher) {
        stopResults.fetchStopResults(query, searchRoutesEnabled = false)
            .filterIsInstance<SearchStopState.SearchResult.Stop>()
            .take(INTRO_SEARCH_RESULT_LIMIT)
            .map { ListedStop(IntroStop(stopId = it.stopId, name = it.stopName), it.transportModeType) }
    }
}

// Train, metro and light rail, read from the transport modes and never typed as numbers.
private val STATION_PRODUCT_CLASSES: List<Long> =
    listOf(TransportMode.Train, TransportMode.Metro, TransportMode.LightRail).map { it.productClass.toLong() }

// The id, the name and the modes of the row, and nothing else: the row's own position is dropped here.
private fun SelectStopsNearby.toListedStop() =
    ListedStop(IntroStop(stopId = stopId, name = stopName), productClasses.toModes())

private fun String.toModes(): List<TransportMode> = split(",")
    .mapNotNull { it.trim().toIntOrNull() }
    .mapNotNull { NswTransportConfig.modeFromProductClass(it) }
