package xyz.ksharma.krail.trip.planner.ui.searchstop.address

/**
 * A query this long is treated as address-shaped regardless of how busy the local stop
 * list is: someone typing a string this long is usually typing an address and should
 * still get one. This is the escape hatch on the stop-count gate below.
 */
const val ADDRESS_SEARCH_LONG_QUERY_LENGTH = 12

/**
 * Pure eligibility gate for the address/POI search API call. Evaluated at the
 * ViewModel boundary before any loading state or network job is created — see
 * feature/trip-planner/ui/ADDRESS_SEARCH_ELIGIBILITY.md.
 *
 * Deliberately does not know about caching or in-flight requests: those are
 * [AddressSearchCache]'s and the ViewModel's concerns, not an eligibility question.
 */
object AddressSearchEligibility {

    /**
     * @param localStopResultCount On-device stop matches already on screen for this
     * query, or `null` when they have not settled yet. Query length is a weak predictor
     * of whether anyone wants an address; how many stops the rider is already looking at
     * is a much stronger one — a rider staring at a long, correct stop list is
     * demonstrably not stuck. `null` skips that check rather than guessing with a
     * previous query's count.
     * @param maxLocalStopsForAddressSearch Above this many local matches the address call
     * is suppressed, unless the query is at least [ADDRESS_SEARCH_LONG_QUERY_LENGTH]
     * characters. Remote-config tunable.
     */
    fun evaluate(
        normalizedQuery: String,
        isAddressSearchEnabled: Boolean,
        minQueryLength: Int,
        localStopResultCount: Int?,
        maxLocalStopsForAddressSearch: Int,
    ): AddressSearchGate = when {
        !isAddressSearchEnabled -> AddressSearchGate.DISABLED
        normalizedQuery.isBlank() -> AddressSearchGate.BLANK
        normalizedQuery.length < minQueryLength -> AddressSearchGate.BELOW_THRESHOLD

        isStopListAlreadySufficient(
            normalizedQuery = normalizedQuery,
            localStopResultCount = localStopResultCount,
            maxLocalStopsForAddressSearch = maxLocalStopsForAddressSearch,
        ) -> AddressSearchGate.STOPS_ALREADY_SUFFICIENT

        else -> AddressSearchGate.ELIGIBLE
    }

    private fun isStopListAlreadySufficient(
        normalizedQuery: String,
        localStopResultCount: Int?,
        maxLocalStopsForAddressSearch: Int,
    ): Boolean = localStopResultCount != null &&
        localStopResultCount > maxLocalStopsForAddressSearch &&
        normalizedQuery.length < ADDRESS_SEARCH_LONG_QUERY_LENGTH
}
