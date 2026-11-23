package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue

/**
 * How far from Home, or from the rider's position before Home is set, the suggestion looks for a
 * Park & Ride station when Remote Config has no usable value. The default of
 * `park_ride_suggestion_radius_km`, which says the same number, and the radius the Intro asks within.
 */
internal const val PARK_RIDE_SUGGESTION_DEFAULT_RADIUS_KM = 5.0

/** The smallest and largest radius Remote Config may ask for. Outside these it is ignored. */
internal const val PARK_RIDE_SUGGESTION_MIN_RADIUS_KM = 0.5
internal const val PARK_RIDE_SUGGESTION_MAX_RADIUS_KM = 10.0

private val VALID_RADIUS_RANGE_KM = PARK_RIDE_SUGGESTION_MIN_RADIUS_KM..PARK_RIDE_SUGGESTION_MAX_RADIUS_KM

/**
 * Resolves `park_ride_suggestion_radius_km`, the one place the suggestion's radius is read.
 *
 * Remote Config can hand a number back three ways, as for `nearby_stops_radius_km`: a whole number
 * (`5`), a Double default or console value that arrives as text (`"5.0"`), or something that is no
 * number at all. The first two are read, and anything else, or a number outside 0.5 to 10, gives
 * [PARK_RIDE_SUGGESTION_DEFAULT_RADIUS_KM] as a whole. It is not pulled into range, so a typo of
 * `500` offers the usual station and not one across the city. NaN and infinity fall outside the range.
 */
internal fun resolveParkRideSuggestionRadiusKm(flag: Flag): Double {
    val raw = when (val value = flag.getFlagValue(FlagKeys.PARK_RIDE_SUGGESTION_RADIUS_KM.key)) {
        is FlagValue.NumberValue -> value.value.toDouble()
        is FlagValue.StringValue -> value.value.trim().toDoubleOrNull()
        else -> null
    }
    return raw?.takeIf { it in VALID_RADIUS_RANGE_KM } ?: PARK_RIDE_SUGGESTION_DEFAULT_RADIUS_KM
}
