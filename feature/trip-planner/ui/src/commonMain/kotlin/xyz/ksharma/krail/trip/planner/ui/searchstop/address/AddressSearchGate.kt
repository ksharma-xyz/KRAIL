package xyz.ksharma.krail.trip.planner.ui.searchstop.address

/**
 * Outcome of [AddressSearchEligibility.evaluate]. Only [ELIGIBLE] may schedule a
 * debounced address/POI request; every other value clears the address section
 * without a loading state or a network call.
 *
 * [STOPS_ALREADY_SUFFICIENT] is deliberately distinct from [BELOW_THRESHOLD]: they are
 * two different reasons for not calling, and `search_stop_query`'s `addressSearchGate`
 * param has to be able to tell them apart - that is the whole point of reporting it.
 */
enum class AddressSearchGate {
    DISABLED,
    BLANK,
    BELOW_THRESHOLD,
    STOPS_ALREADY_SUFFICIENT,
    ELIGIBLE,
}
