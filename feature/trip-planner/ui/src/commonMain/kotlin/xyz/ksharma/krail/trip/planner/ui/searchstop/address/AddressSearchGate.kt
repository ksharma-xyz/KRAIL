package xyz.ksharma.krail.trip.planner.ui.searchstop.address

/**
 * Outcome of [AddressSearchEligibility.evaluate]. Only [ELIGIBLE] may schedule a
 * debounced address/POI request; every other value clears the address section
 * without a loading state or a network call.
 */
enum class AddressSearchGate {
    DISABLED,
    BLANK,
    BELOW_THRESHOLD,
    ELIGIBLE,
}
