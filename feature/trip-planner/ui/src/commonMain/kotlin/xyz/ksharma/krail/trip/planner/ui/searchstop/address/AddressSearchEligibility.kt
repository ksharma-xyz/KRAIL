package xyz.ksharma.krail.trip.planner.ui.searchstop.address

/**
 * Pure eligibility gate for the address/POI search API call. Evaluated at the
 * ViewModel boundary before any loading state or network job is created — see
 * "Recommended eligibility policy" in docs/plans/ADDRESS_SEARCH_API_CALL_POLICY.md.
 *
 * Deliberately does not know about caching or in-flight requests: those are
 * [AddressSearchCache]'s and the ViewModel's concerns, not an eligibility question.
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
