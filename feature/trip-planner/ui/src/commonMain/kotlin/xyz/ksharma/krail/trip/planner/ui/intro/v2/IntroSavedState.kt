package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardPhase

internal const val INTRO_SAVED_STATE_KEY = "intro_state"

/**
 * What the Intro keeps across the process being killed: the step, the answer, the permission result,
 * the picks and the flags, and which station the card was for. The rider's position is never kept,
 * and neither are the card's car parks or their live spots: the Park & Ride step works the card out
 * again from the station id, which costs no call while the readings are inside the cooldown. A
 * location still being awaited is not kept either, so that rider is asked again.
 */
@Serializable
private data class IntroSnapshot(
    val step: IntroStep,
    val parkRideAnswer: ParkRideAnswer,
    val locationResult: LocationResult,
    val startStopId: String?,
    val startName: String?,
    val endStopId: String?,
    val endName: String?,
    val addedParkRide: Boolean,
    val usedSearch: Boolean,
    val saved: Boolean,
    val cardStationId: String? = null,
    val noStationNear: Boolean = false,
)

internal fun SavedStateHandle.saveIntroState(state: IntroState) {
    this[INTRO_SAVED_STATE_KEY] = Json.encodeToString(IntroSnapshot.serializer(), state.toSnapshot())
}

/** The saved state, or the first screen when there is none or it cannot be read. */
internal fun SavedStateHandle.restoreIntroState(): IntroState {
    val json = get<String>(INTRO_SAVED_STATE_KEY) ?: return IntroState()
    return runCatching { Json.decodeFromString(IntroSnapshot.serializer(), json).toState() }
        .getOrDefault(IntroState())
}

private fun IntroState.toSnapshot() = IntroSnapshot(
    step, parkRideAnswer, locationResult, startStopId, startName, endStopId, endName,
    addedParkRide, usedSearch, saved,
    cardStationId = when (val card = parkRideCard) {
        is ParkRideCardPhase.Card -> card.station.stationId
        is ParkRideCardPhase.Locating -> card.stationId
        ParkRideCardPhase.None, ParkRideCardPhase.NoStationNear -> null
    },
    noStationNear = parkRideCard == ParkRideCardPhase.NoStationNear,
)

private fun IntroSnapshot.toState() = IntroState(
    step = step,
    parkRideAnswer = parkRideAnswer,
    locationResult = locationResult,
    startStopId = startStopId,
    startName = startName,
    endStopId = endStopId,
    endName = endName,
    addedParkRide = addedParkRide,
    usedSearch = usedSearch,
    saved = saved,
    parkRideCard = when {
        // Shown as the skeleton until the Park & Ride step has worked the station out again.
        cardStationId != null -> ParkRideCardPhase.Locating(cardStationId)
        noStationNear -> ParkRideCardPhase.NoStationNear
        else -> ParkRideCardPhase.None
    },
)
