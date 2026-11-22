package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroFirstStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase

/**
 * The one place that says which screen comes next. The flow only moves forward. The screens are
 * [IntroStep.entries] in order, minus the ones the rider has nothing to decide on.
 *
 * The step the rider is on is always part of the order, so a screen that has just become skippable
 * (the Park & Ride screen once its answer was no station near) still has a screen after it.
 */
internal fun IntroState.stepsInFlow(first: IntroStep = IntroFirstStep): List<IntroStep> =
    IntroStep.entries.filter { it == step || !isSkipped(it, first) }

/** The screen after this one, or null on the last. */
internal fun IntroState.stepAfter(first: IntroStep = IntroFirstStep): IntroStep? {
    val order = stepsInFlow(first)
    return order.getOrNull(order.indexOf(step) + 1)
}

/** The state moved to the next screen, or as it is on the last. */
internal fun IntroState.movedOn(first: IntroStep = IntroFirstStep): IntroState =
    stepAfter(first)?.let { copy(step = it) } ?: this

/**
 * The state after the rider is done with the Park & Ride screen, whatever the way: an answer, a
 * decline, an add, or an answer that left nothing to decide. It moves on only while the rider is
 * still on that screen, so a look that finishes after the rider has already moved on never pulls
 * them forward a second time.
 */
internal fun IntroState.leavingParkRide(first: IntroStep = IntroFirstStep): IntroState =
    if (step == IntroStep.PARK_RIDE) movedOn(first) else this

/**
 * The Park & Ride screen asked and there is nothing for the rider to decide: no car park near, or no
 * location to look from. Such a screen is passed over.
 */
internal fun IntroState.parkRideHasNothingToDecide(): Boolean =
    parkRideCard == ParkRideCardPhase.NoStationNear ||
        (parkRideAnswer == ParkRideAnswer.YES && parkRideCard == ParkRideCardPhase.None)

/**
 * Screens before [first] have no content yet. The Park & Ride screen is skipped when it has nothing to
 * decide. The origin screen is skipped when Add to home already filled in the start, because that
 * station is the start.
 */
private fun IntroState.isSkipped(candidate: IntroStep, first: IntroStep): Boolean = when (candidate) {
    IntroStep.THEME -> candidate.ordinal < first.ordinal
    IntroStep.PARK_RIDE -> candidate.ordinal < first.ordinal || parkRideHasNothingToDecide()
    IntroStep.DESTINATION, IntroStep.CONFIRM -> false
    IntroStep.ORIGIN -> startFromParkRide
}

/**
 * Whether the Intro can be finished from this screen: the confirm screen alone. Its two answers are the only way
 * out, so a press that arrives from any other screen, such as one that was already on its way when the rider
 * left it, finishes nothing.
 */
internal fun IntroStep.canFinish(): Boolean = this == IntroStep.CONFIRM
