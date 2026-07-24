package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * `stop_finder` always uses `type_sf=any` here — that's what unlocks address/POI hits
 * on top of transit stops. Any `stop`-typed location in the response is filtered out:
 * transit stops are local-DB only, always (see [RealStopResultsManager]).
 *
 * Deliberately does **not** catch here: a failed call must propagate as an exception so
 * the caller's `AddressSearchCache` can tell "the request failed" apart from "the
 * request succeeded with zero results" and skip caching the former. The caller
 * (`SearchStopViewModel`) is the single place that turns a failure into an empty list
 * for the UI - see feature/trip-planner/ui/ADDRESS_SEARCH_ELIGIBILITY.md.
 */
internal class RealRemoteAddressResultsManager(
    private val tripPlanningService: TripPlanningService,
    private val ioDispatcher: CoroutineDispatcher,
) : RemoteAddressResultsManager {

    override suspend fun fetchAddressResults(
        query: String,
    ): List<SearchStopState.SearchResult.Address> = withContext(ioDispatcher) {
        val safeQuery = query.take(MAX_QUERY_LENGTH).trim()
        if (safeQuery.length < MIN_QUERY_LENGTH) return@withContext emptyList()

        tripPlanningService.stopFinder(stopSearchQuery = safeQuery, stopType = StopType.ANY)
            .locations
            ?.filter { location -> location.type != STOP_TYPE && location.id != null }
            ?.map { location ->
                SearchStopState.SearchResult.Address(
                    addressId = requireNotNull(location.id),
                    displayName = location.name ?: location.disassembledName ?: safeQuery,
                    addressType = location.type ?: "unknown",
                )
            }
            ?: emptyList()
    }

    private companion object {
        const val STOP_TYPE = "stop"
        const val MAX_QUERY_LENGTH = 64
        const val MIN_QUERY_LENGTH = 2
    }
}
