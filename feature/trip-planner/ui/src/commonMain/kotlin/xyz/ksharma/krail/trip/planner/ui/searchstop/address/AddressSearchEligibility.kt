package xyz.ksharma.krail.trip.planner.ui.searchstop.address

/**
 * Pure eligibility gate for the address/POI search API call. Evaluated at the
 * ViewModel boundary before any loading state or network job is created — see
 * feature/trip-planner/ui/ADDRESS_SEARCH_ELIGIBILITY.md.
 *
 * Deliberately minimal: a kill switch, a blank check, and a query-length threshold.
 * Nothing here may depend on what the local stop pipeline found. A stop-count gate was
 * tried and reverted — "13 hassall" matches dozens of Hassall St bus stops, which is
 * exactly when a rider wants the address, so a busy local list suppressed the calls that
 * mattered most. Query length is a weaker signal, but it never hides a result the rider
 * was asking for.
 */
object AddressSearchEligibility {

    fun evaluate(
        normalizedQuery: String,
        isAddressSearchEnabled: Boolean,
        minQueryLength: Int,
    ): AddressSearchGate = when {
        !isAddressSearchEnabled -> AddressSearchGate.DISABLED
        normalizedQuery.isBlank() -> AddressSearchGate.BLANK
        normalizedQuery.length < minQueryLength -> AddressSearchGate.BELOW_THRESHOLD
        else -> AddressSearchGate.ELIGIBLE
    }
}
