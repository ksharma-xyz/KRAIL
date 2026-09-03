package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.trip.planner.ui.searchstop.address.AddressSearchGate
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * One firing per resolved (non-stale) address fetch, which is also one firing per network
 * call - there is no result cache, so `resultSource = address` counts requests directly.
 * [addressResults] is null when the fetch failed.
 *
 * Carries no query text. The local firing happens for every settled query and already
 * carries it, so attaching it here too would send the same text twice for one keystroke
 * and double-count it in the eval corpus. Join on [searchSessionId] instead.
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
        ),
    )
}

/**
 * One firing per settled local stop search. Unlike the address firing this happens for
 * every query, so it is the only place a *suppressed* address call leaves a trace — hence
 * [addressSearchGate] riding along here, and the only firing that carries the query text.
 */
internal fun Analytics.trackLocalSearchResolved(
    query: String,
    searchSessionId: String,
    localResultsCount: Int,
    addressSearchGate: AddressSearchGate,
) {
    track(
        AnalyticsEvent.SearchStopQuery(
            queryLength = query.length,
            searchSessionId = searchSessionId,
            resultsCount = localResultsCount,
            maskedQuery = SearchQueryAnalyticsRedaction.maskedQueryOrNull(query),
            addressSearchGate = addressSearchGateOutcome(addressSearchGate),
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
            maskedQuery = SearchQueryAnalyticsRedaction.maskedQueryOrNull(query),
            queryHasDigit = queryHasDigit(query),
        ),
    )
}

/**
 * Maps the address pipeline's decision onto the analytics enum for the local firing,
 * which is the only firing a suppressed query produces at all.
 */
internal fun addressSearchGateOutcome(
    gate: AddressSearchGate,
): AnalyticsEvent.SearchStopQuery.AddressGate = when (gate) {
    AddressSearchGate.DISABLED -> AnalyticsEvent.SearchStopQuery.AddressGate.DISABLED
    AddressSearchGate.BLANK -> AnalyticsEvent.SearchStopQuery.AddressGate.BLANK
    AddressSearchGate.BELOW_THRESHOLD ->
        AnalyticsEvent.SearchStopQuery.AddressGate.BELOW_THRESHOLD

    AddressSearchGate.ELIGIBLE -> AnalyticsEvent.SearchStopQuery.AddressGate.ELIGIBLE
}

/** A house number is the cheapest address signal there is - a bool, never the text. */
internal fun queryHasDigit(query: String): Boolean = query.any { it.isDigit() }
