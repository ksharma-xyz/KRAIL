package xyz.ksharma.krail.trip.planner.ui.searchstop.address

import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asNumber

const val DEFAULT_ADDRESS_SEARCH_MAX_LOCAL_STOPS = 10
private val VALID_ADDRESS_SEARCH_MAX_LOCAL_STOPS_RANGE = 0L..50L

/**
 * Resolves `search_stop_address_max_local_stops` with the same bounds contract as
 * [resolveAddressSearchMinQueryLength]: a missing, malformed, or out-of-range remote
 * value falls back to [DEFAULT_ADDRESS_SEARCH_MAX_LOCAL_STOPS] rather than being
 * silently clamped into range, so a typo cannot quietly turn the address search off
 * (`0`) or on for everyone (a huge value). The range check happens in `Long` space
 * before narrowing to `Int`, so an oversized remote value can't wrap around into a
 * false in-range result.
 */
fun resolveAddressSearchMaxLocalStops(flag: Flag): Int {
    val raw = flag.getFlagValue(FlagKeys.SEARCH_STOP_ADDRESS_MAX_LOCAL_STOPS.key)
        .asNumber(DEFAULT_ADDRESS_SEARCH_MAX_LOCAL_STOPS.toLong())
    return if (raw in VALID_ADDRESS_SEARCH_MAX_LOCAL_STOPS_RANGE) {
        raw.toInt()
    } else {
        DEFAULT_ADDRESS_SEARCH_MAX_LOCAL_STOPS
    }
}
