package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep

/** What the bottom button says and does. */
internal enum class IntroFinishAction { Next, GettingReady, TryAgain, SaveAndStart, SkipForNow, LetsKrail }

/**
 * The bottom button for [state] while the stops are in [readiness]. Every `when` below is exhaustive
 * with no `else`, so a new step or a new readiness is a compile error here and not a button that
 * quietly says the wrong thing.
 */
internal fun introFinishAction(state: IntroState, readiness: StopsReadiness): IntroFinishAction =
    when (state.step) {
        IntroStep.PARK_RIDE -> IntroFinishAction.Next
        IntroStep.FIRST_TRIP -> firstTripAction(state, readiness)
    }

// Once the trip is written there is nothing left to wait for, so readiness no longer matters.
private fun firstTripAction(state: IntroState, readiness: StopsReadiness): IntroFinishAction =
    if (state.saved) IntroFinishAction.LetsKrail else stopsGatedAction(state, readiness)

private fun stopsGatedAction(state: IntroState, readiness: StopsReadiness): IntroFinishAction =
    when (readiness) {
        StopsReadiness.Unknown, StopsReadiness.Loading -> IntroFinishAction.GettingReady
        is StopsReadiness.Failed -> IntroFinishAction.TryAgain
        is StopsReadiness.Ready ->
            if (state.hasTrip) IntroFinishAction.SaveAndStart else IntroFinishAction.SkipForNow
    }
