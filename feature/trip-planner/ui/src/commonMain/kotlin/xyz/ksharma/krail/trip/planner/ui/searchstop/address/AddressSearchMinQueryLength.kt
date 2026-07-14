package xyz.ksharma.krail.trip.planner.ui.searchstop.address

import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asNumber

const val DEFAULT_ADDRESS_SEARCH_MIN_QUERY_LENGTH = 6
private val VALID_ADDRESS_SEARCH_MIN_QUERY_LENGTH_RANGE = 2L..12L

/**
 * Resolves `search_stop_address_min_query_length` with the bounds contract from
 * feature/trip-planner/ui/ADDRESS_SEARCH_ELIGIBILITY.md: a missing, malformed, or
 * out-of-range remote value falls back to [DEFAULT_ADDRESS_SEARCH_MIN_QUERY_LENGTH]
 * rather than being silently clamped into range. The range check happens in `Long`
 * space before narrowing to `Int`, so an oversized remote value can't wrap around
 * into a false in-range result.
 */
fun resolveAddressSearchMinQueryLength(flag: Flag): Int {
    val raw = flag.getFlagValue(FlagKeys.SEARCH_STOP_ADDRESS_MIN_QUERY_LENGTH.key)
        .asNumber(DEFAULT_ADDRESS_SEARCH_MIN_QUERY_LENGTH.toLong())
    return if (raw in VALID_ADDRESS_SEARCH_MIN_QUERY_LENGTH_RANGE) {
        raw.toInt()
    } else {
        DEFAULT_ADDRESS_SEARCH_MIN_QUERY_LENGTH
    }
}
