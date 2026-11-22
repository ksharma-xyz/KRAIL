package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue

private val VALID_RADIUS_RANGE_KM = NearbyStopsConfig.MIN_RADIUS_KM..NearbyStopsConfig.MAX_RADIUS_KM

/**
 * Resolves `nearby_stops_radius_km`, the one place the radius Search Stop looks within is read.
 *
 * Remote Config can hand a number back three ways: a whole number (`3`), a Double default or console
 * value that arrives as text (`"3.0"`), or something that is no number at all. The first two are
 * read, and anything else, or a number outside [NearbyStopsConfig.MIN_RADIUS_KM] to
 * [NearbyStopsConfig.MAX_RADIUS_KM], gives [NearbyStopsConfig.DEFAULT_RADIUS_KM] as a whole. It is
 * not pulled into range, the same contract the address search threshold keeps, so a typo of `300`
 * shows the usual map and not the widest one. NaN and infinity fall outside the range.
 */
fun resolveNearbyStopsRadiusKm(flag: Flag): Double {
    val raw = when (val value = flag.getFlagValue(FlagKeys.NEARBY_STOPS_RADIUS_KM.key)) {
        is FlagValue.NumberValue -> value.value.toDouble()
        is FlagValue.StringValue -> value.value.trim().toDoubleOrNull()
        else -> null
    }
    return raw?.takeIf { it in VALID_RADIUS_RANGE_KM } ?: NearbyStopsConfig.DEFAULT_RADIUS_KM
}
