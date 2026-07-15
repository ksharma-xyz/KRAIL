package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.trip.planner.ui.searchstop.address.AddressSearchGate
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * One firing per resolved (non-stale) address fetch; cache hits are excluded so
 * `resultSource = address` counts network calls, the number the API cost model cares
 * about. This is also the carve-out site for address-eligible queries - only here are
 * both pipelines' result counts known. [addressResults] is null when the fetch failed.
 */
internal fun Analytics.trackAddressSearchResolved(
    normalizedQuery: String,
    searchSessionId: String,
    localResultsCount: Int,
    addressResults: List<SearchStopState.SearchResult.Address>?,
) {
    track(
        AnalyticsEvent.SearchStopQuery(
            queryLength = normalizedQuery.length,
            searchSessionId = searchSessionId,
            resultSource = AnalyticsEvent.SearchStopQuery.ResultSource.ADDRESS,
            resultsCount = addressResults?.size,
            isError = addressResults == null,
            zeroResultQuery = addressResults?.let {
                SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
                    query = normalizedQuery,
                    localResultsCount = localResultsCount,
                    addressResultsCount = it.size,
                )
            },
        ),
    )
}

/**
 * Local-pipeline carve-out site. When the address pipeline is eligible for the query,
 * it resolves later and owns the carve-out decision (the query may be a real address
 * the NSW API recognises), so this returns null.
 */
internal fun resolveLocalZeroResultQuery(
    query: String,
    localResultsCount: Int,
    addressSearchGate: AddressSearchGate,
): String? {
    if (addressSearchGate == AddressSearchGate.ELIGIBLE) return null
    return SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
        query = query,
        localResultsCount = localResultsCount,
        addressResultsCount = null,
    )
}
