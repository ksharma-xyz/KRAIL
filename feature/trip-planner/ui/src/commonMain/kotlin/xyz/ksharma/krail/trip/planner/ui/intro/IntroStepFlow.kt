package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroFirstStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase

/**
 * The one place that says which screen comes next and which one Back returns to. The screens are
 * [IntroStep.entries] in order, minus the ones the rider has nothing to decide on.
 *
 * The step the rider is on is always part of the order, so a screen that has just become skippable
 * (the Park & Ride screen once its answer was no station near) still has a neighbour on each side.
 */
internal fun IntroState.stepsInFlow(first: IntroStep = IntroFirstStep): List<IntroStep> =
    IntroStep.entries.filter { it == step || !isSkipped(it, first) }

/** The screen after this one, or null on the last. */
internal fun IntroState.stepAfter(first: IntroStep = IntroFirstStep): IntroStep? {
    val order = stepsInFlow(first)
    return order.getOrNull(order.indexOf(step) + 1)
}

/** The screen Back returns to, or null on the first. */
internal fun IntroState.stepBefore(first: IntroStep = IntroFirstStep): IntroStep? {
    val order = stepsInFlow(first)
    return order.getOrNull(order.indexOf(step) - 1)
}

/** The state moved back one screen, or as it is on the first. */
internal fun IntroState.movedBack(first: IntroStep = IntroFirstStep): IntroState =
    stepBefore(first)?.let { copy(step = it) } ?: this

/** The state moved to the next screen, or as it is on the last. */
internal fun IntroState.movedOn(first: IntroStep = IntroFirstStep): IntroState =
    stepAfter(first)?.let { copy(step = it) } ?: this

/**
 * The state after the rider is done with the Park & Ride screen, whatever the way: an answer, a
 * decline, an add, or an answer that left nothing to decide. It moves on only while the rider is
 * still on that screen, so a look that finishes after Back has taken them elsewhere never pulls
 * them forward.
 */
internal fun IntroState.leavingParkRide(first: IntroStep = IntroFirstStep): IntroState =
    if (step == IntroStep.PARK_RIDE) movedOn(first) else this

/**
 * The Park & Ride screen asked and there is nothing for the rider to decide: no car park near, or no
 * location to look from. Such a screen is passed over, going forward and going back.
 */
internal fun IntroState.parkRideHasNothingToDecide(): Boolean =
    parkRideCard == ParkRideCardPhase.NoStationNear ||
        (parkRideAnswer == ParkRideAnswer.YES && parkRideCard == ParkRideCardPhase.None)

/**
 * Screens before [first] have no content yet. The Park & Ride screen is skipped when it has nothing to
 * decide. The origin screen is skipped when Add to home already filled in the start, because that
 * station is the start, and Back from the confirm screen then returns to the destination screen.
 */
private fun IntroState.isSkipped(candidate: IntroStep, first: IntroStep): Boolean = when (candidate) {
    IntroStep.THEME -> candidate.ordinal < first.ordinal
    IntroStep.PARK_RIDE -> candidate.ordinal < first.ordinal || parkRideHasNothingToDecide()
    IntroStep.DESTINATION, IntroStep.CONFIRM -> false
    IntroStep.ORIGIN -> startFromParkRide
}

/**
 * The screens that end in a trip decision. Until the destination, origin and confirm screens have their own bars
 * this is the one place that says which screens may finish the Intro.
 */
internal fun IntroStep.isTripStep(): Boolean = when (this) {
    IntroStep.THEME, IntroStep.PARK_RIDE -> false
    IntroStep.DESTINATION, IntroStep.ORIGIN, IntroStep.CONFIRM -> true
}
