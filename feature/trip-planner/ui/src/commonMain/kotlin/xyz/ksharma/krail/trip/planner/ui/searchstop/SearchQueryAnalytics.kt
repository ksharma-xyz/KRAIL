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
            localResultsCount = localResultsCount,
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
 * One firing per settled local stop search. Unlike the address firing this happens for
 * every query, so it is the only place a *suppressed* address call leaves a trace — hence
 * [addressSearchGate] and [addressServedFromCache] riding along here.
 */
internal fun Analytics.trackLocalSearchResolved(
    query: String,
    searchSessionId: String,
    localResultsCount: Int,
    addressSearchGate: AddressSearchGate,
    addressServedFromCache: Boolean,
) {
    track(
        AnalyticsEvent.SearchStopQuery(
            queryLength = query.length,
            searchSessionId = searchSessionId,
            resultsCount = localResultsCount,
            zeroResultQuery = resolveLocalZeroResultQuery(
                query = query,
                localResultsCount = localResultsCount,
                addressSearchGate = addressSearchGate,
            ),
            addressSearchGate = addressSearchGateOutcome(
                gate = addressSearchGate,
                servedFromCache = addressServedFromCache,
            ),
            queryHasDigit = queryHasDigit(query),
        ),
    )
}

/** Local pipeline threw. No gate is reported: the query never reached that decision. */
internal fun Analytics.trackLocalSearchFailed(query: String, searchSessionId: String) {
    track(
        AnalyticsEvent.SearchStopQuery(
            queryLength = query.length,
            searchSessionId = searchSessionId,
            isError = true,
            queryHasDigit = queryHasDigit(query),
        ),
    )
}

/**
 * Maps the address pipeline's decision onto the analytics enum for the local firing,
 * which is the only firing a suppressed query produces at all.
 *
 * [servedFromCache] wins over the gate value: the cache is consulted on the keystroke,
 * before the settled local stop count exists, so a query can be both cache-served and
 * (later) stop-count-suppressed. Reporting `CACHE_HIT` there is the honest reading —
 * results were shown and no network call was made — and it is what makes cache
 * effectiveness visible at all.
 */
internal fun addressSearchGateOutcome(
    gate: AddressSearchGate,
    servedFromCache: Boolean,
): AnalyticsEvent.SearchStopQuery.AddressGate = when {
    servedFromCache -> AnalyticsEvent.SearchStopQuery.AddressGate.CACHE_HIT
    gate == AddressSearchGate.DISABLED -> AnalyticsEvent.SearchStopQuery.AddressGate.DISABLED
    gate == AddressSearchGate.BLANK -> AnalyticsEvent.SearchStopQuery.AddressGate.BLANK
    gate == AddressSearchGate.BELOW_THRESHOLD ->
        AnalyticsEvent.SearchStopQuery.AddressGate.BELOW_THRESHOLD

    gate == AddressSearchGate.STOPS_ALREADY_SUFFICIENT ->
        AnalyticsEvent.SearchStopQuery.AddressGate.STOPS_ALREADY_SUFFICIENT

    else -> AnalyticsEvent.SearchStopQuery.AddressGate.ELIGIBLE
}

/** A house number is the cheapest address signal there is - a bool, never the text. */
internal fun queryHasDigit(query: String): Boolean = query.any { it.isDigit() }

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
