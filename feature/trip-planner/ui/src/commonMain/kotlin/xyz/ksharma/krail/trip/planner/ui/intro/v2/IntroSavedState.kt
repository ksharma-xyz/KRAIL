package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer

internal const val INTRO_SAVED_STATE_KEY = "intro_state"

/**
 * What the Intro keeps across the process being killed: the step, the answer, the permission result,
 * the picks and the flags. The station card and its live spots are not kept, the park and ride step
 * works them out again.
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
)
