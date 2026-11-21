package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep

/**
 * What the bottom button says and does. [GettingReady] and [PickBothStops] are disabled, and
 * [PickBothStops] reads Save and start because that is what it becomes once both stops are picked.
 */
internal enum class IntroFinishAction { Next, GettingReady, PickBothStops, SaveAndStart, SkipForNow, LetsKrail }

/**
 * The bottom button for [state] while the stops are in [readiness]. The `when` over the step and
 * the one over the readiness are exhaustive with no `else`, so a new step or a new readiness is a
 * compile error here and not a button that quietly says the wrong thing.
 *
 * Only stops that are still loading hold the button back. A failed load is no different from a
 * ready one here, because the picks carry their own id and name, so a trip never needs the stops
 * table.
 */
internal fun introFinishAction(state: IntroState, readiness: StopsReadiness): IntroFinishAction =
    when (state.step) {
        IntroStep.PARK_RIDE -> IntroFinishAction.Next
        IntroStep.FIRST_TRIP -> firstTripAction(state, readiness)
    }

/**
 * The button once the stops no longer matter, which is where a finish that has waited for them as
 * long as it will ends up: the picks alone decide.
 */
internal fun introFinishActionIgnoringStops(state: IntroState): IntroFinishAction =
    when (state.step) {
        IntroStep.PARK_RIDE -> IntroFinishAction.Next
        IntroStep.FIRST_TRIP -> if (state.saved) IntroFinishAction.LetsKrail else picksAction(state)
    }

// Once the trip is written there is nothing left to wait for, so readiness no longer matters.
private fun firstTripAction(state: IntroState, readiness: StopsReadiness): IntroFinishAction =
    if (state.saved) IntroFinishAction.LetsKrail else stopsGatedAction(state, readiness)

private fun stopsGatedAction(state: IntroState, readiness: StopsReadiness): IntroFinishAction =
    when (readiness) {
        StopsReadiness.Unknown, StopsReadiness.Loading -> IntroFinishAction.GettingReady
        is StopsReadiness.Failed, is StopsReadiness.Ready -> picksAction(state)
    }

// Only a complete pair is a trip. Exactly one pick disables the button until both or neither are picked.
private fun picksAction(state: IntroState): IntroFinishAction = when {
    state.hasTrip -> IntroFinishAction.SaveAndStart
    state.startStopId != null || state.endStopId != null -> IntroFinishAction.PickBothStops
    else -> IntroFinishAction.SkipForNow
}
