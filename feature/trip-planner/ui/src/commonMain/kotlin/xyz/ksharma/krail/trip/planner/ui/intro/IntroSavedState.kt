package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision

internal const val INTRO_SAVED_STATE_KEY = "intro_state"

/**
 * What the Intro keeps across a turn of the screen and the system saving its state: the step, the theme the rider
 * picked, the answer, the permission result, the picks, the answer on the confirm screen and the flags, and which
 * station the card was for. What survives the app being swiped away is [IntroProgress]. The rider's position
 * is never kept, and neither are the card's car parks or their live spots: the Park & Ride step works the
 * card out again from the station id, which costs no call while the readings are inside the cooldown. A
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
    val startFromParkRide: Boolean = false,
    val cardStationId: String? = null,
    val noStationNear: Boolean = false,
    val selectedThemeId: Int? = null,
    val tripDecision: TripDecision? = null,
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
    startFromParkRide = startFromParkRide,
    selectedThemeId = selectedThemeId,
    tripDecision = tripDecision,
    cardStationId = when (val card = parkRideCard) {
        is ParkRideCardPhase.Card -> card.station.stationId
        is ParkRideCardPhase.Locating -> card.stationId
        ParkRideCardPhase.None, ParkRideCardPhase.NoStationNear -> null
    },
    noStationNear = parkRideCard == ParkRideCardPhase.NoStationNear,
)

private fun IntroSnapshot.toState(): IntroState {
    // A yes whose location was still awaited when the state was saved has nothing to show: the awaited location is not kept,
    // so the question is open again and the screen opens on it, never on an answer with no card and no buttons.
    val lookLost = step == IntroStep.PARK_RIDE && parkRideAnswer == ParkRideAnswer.YES &&
        cardStationId == null && !noStationNear
    return IntroState(
        step = step,
        parkRideAnswer = if (lookLost) ParkRideAnswer.ASK else parkRideAnswer,
        locationResult = if (lookLost) LocationResult.NOT_ASKED else locationResult,
        startStopId = startStopId,
        startName = startName,
        endStopId = endStopId,
        endName = endName,
        addedParkRide = addedParkRide,
        usedSearch = usedSearch,
        saved = saved,
        startFromParkRide = startFromParkRide,
        selectedThemeId = selectedThemeId,
        tripDecision = tripDecision,
        parkRideCard = when {
            // Shown as the skeleton until the Park & Ride step has worked the station out again.
            cardStationId != null -> ParkRideCardPhase.Locating(cardStationId)
            noStationNear -> ParkRideCardPhase.NoStationNear
            else -> ParkRideCardPhase.None
        },
    )
}
