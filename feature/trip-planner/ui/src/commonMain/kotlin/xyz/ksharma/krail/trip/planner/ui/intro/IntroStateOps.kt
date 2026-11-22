package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCarPark
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * Sets the start or the end. The same stop is not a trip, so when the pick equals the stop already
 * chosen for the other end, the new pick stays and the other end is cleared. Two different stops
 * that happen to be on both suggestion lists are fine.
 */
internal fun IntroState.withPick(isStart: Boolean, stopId: String, stopName: String): IntroState =
    if (isStart) {
        // A start the rider picks is theirs, even when it is the stop Add to home had filled in.
        copy(startStopId = stopId, startName = stopName, startFromParkRide = false).let {
            if (it.endStopId == stopId) it.copy(endStopId = null, endName = null) else it
        }
    } else {
        copy(endStopId = stopId, endName = stopName).let {
            if (it.startStopId == stopId) {
                it.copy(startStopId = null, startName = null, startFromParkRide = false)
            } else {
                it
            }
        }
    }

/**
 * The station is on home. The start is filled in with ([stopId], [stopName]) only when the rider has
 * not picked a start and the stop is not already the end, so a pick of theirs is never overridden
 * and the trip is never from a stop to itself. [stopId] is null when no stop of the station
 * resolves in the stops table, and then there is no pre-fill.
 */
internal fun IntroState.withAddedParkRide(stopId: String?, stopName: String?): IntroState {
    val added = copy(addedParkRide = true)
    val startIsFree = startStopId == null && endStopId != stopId
    return if (startIsFree && stopId != null && stopName != null) {
        added.copy(startStopId = stopId, startName = stopName, startFromParkRide = true)
    } else {
        added
    }
}

/** The station is off home. A start Add to home filled in and the rider has not touched goes with it. */
internal fun IntroState.withoutParkRide(): IntroState =
    if (startFromParkRide) {
        copy(addedParkRide = false, startStopId = null, startName = null, startFromParkRide = false)
    } else {
        copy(addedParkRide = false)
    }

/** The picked pair as a [Trip], or null until both ends are set. */
internal fun IntroState.toTripOrNull(): Trip? {
    val fromId = startStopId ?: return null
    val fromName = startName ?: return null
    val toId = endStopId ?: return null
    val toName = endName ?: return null
    return Trip(fromStopId = fromId, fromStopName = fromName, toStopId = toId, toStopName = toName)
}

/** True while a location result is awaited or a card shows, when a second yes must start nothing. */
internal fun ParkRideCardPhase.isInProgressOrShown(): Boolean =
    this is ParkRideCardPhase.Locating || this is ParkRideCardPhase.Card

/**
 * The card for [stationId] once its one availability call is back. [carParks] is null when the call
 * could not be made. A card with no reading at all is Unavailable and keeps its car park names, so
 * the screen shows the name and Add with no numbers. A card that is no longer for [stationId], or
 * no longer a card, is left as it is.
 */
internal fun IntroState.withReadings(stationId: String, carParks: List<ParkRideCarPark>?): IntroState {
    val card = parkRideCard as? ParkRideCardPhase.Card ?: return this
    if (card.station.stationId != stationId) return this
    val readings = carParks.orEmpty()
    val settled = if (readings.any { it.hasReading }) {
        card.station.copy(
            carParks = readings.toImmutableList(),
            availability = ParkRideAvailabilityState.Content,
        )
    } else {
        card.station.copy(availability = ParkRideAvailabilityState.Unavailable)
    }
    return copy(parkRideCard = ParkRideCardPhase.Card(settled))
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
