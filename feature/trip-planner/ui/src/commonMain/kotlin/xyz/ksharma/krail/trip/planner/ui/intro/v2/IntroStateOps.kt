package xyz.ksharma.krail.trip.planner.ui.intro.v2

import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.trip.planner.ui.intro.DEFAULT_SUGGESTED_STOPS
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStops
import xyz.ksharma.krail.trip.planner.ui.intro.parseSuggestedStops
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * Sets the start or the end. The same stop is not a trip, so when the pick equals the stop already
 * chosen for the other end, the new pick stays and the other end is cleared. Two different stops
 * that happen to be on both suggestion lists are fine.
 */
internal fun IntroState.withPick(isStart: Boolean, stopId: String, stopName: String): IntroState =
    if (isStart) {
        copy(startStopId = stopId, startName = stopName).let {
            if (it.endStopId == stopId) it.copy(endStopId = null, endName = null) else it
        }
    } else {
        copy(endStopId = stopId, endName = stopName).let {
            if (it.startStopId == stopId) it.copy(startStopId = null, startName = null) else it
        }
    }

/** The picked pair as a [Trip], or null until both ends are set. */
internal fun IntroState.toTripOrNull(): Trip? {
    val fromId = startStopId ?: return null
    val fromName = startName ?: return null
    val toId = endStopId ?: return null
    val toName = endName ?: return null
    return Trip(fromStopId = fromId, fromStopName = fromName, toStopId = toId, toStopName = toName)
}

/**
 * The lists in Remote Config, or the bundled ones when the value is empty, unset, or not a usable
 * list. Remote Config hands a `{...}` value back as JSON and an empty default as a string.
 */
internal fun Flag.introSuggestedStops(): SuggestedStops {
    val json = when (val value = getFlagValue(FlagKeys.INTRO_SUGGESTED_STOPS.key)) {
        is FlagValue.JsonValue -> value.value
        is FlagValue.StringValue -> value.value
        else -> ""
    }
    return parseSuggestedStops(json, DEFAULT_SUGGESTED_STOPS)
}
