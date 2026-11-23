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
 * The Park & Ride screen asked and there is nothing for the rider to decide and nothing to tell them: there is no location to
 * look from, or the stops were not ready to look in. Such a screen is passed over. No station near is not that: the rider is
 * told so and answers OK ([IntroUiEvent.AcknowledgeNoStation]), so the screen stays until they do.
 */
internal fun IntroState.parkRideHasNothingToDecide(): Boolean =
    parkRideAnswer == ParkRideAnswer.YES && parkRideCard == ParkRideCardPhase.None

/**
 * Screens before [first] have no content yet. The Park & Ride screen is skipped when it has nothing to
 * decide. The origin screen is skipped when Add to home already filled in the start, because that
 * station is the start. The confirm and theme screens are never skipped.
 */
private fun IntroState.isSkipped(candidate: IntroStep, first: IntroStep): Boolean =
    candidate.ordinal < first.ordinal || when (candidate) {
        IntroStep.PARK_RIDE -> parkRideHasNothingToDecide()
        IntroStep.ORIGIN -> startFromParkRide
        IntroStep.DESTINATION, IntroStep.CONFIRM, IntroStep.THEME -> false
    }

/**
 * Whether the Intro can be finished from here: the theme screen, the last one, with a theme chosen. Its button is the
 * only way out, so a press that arrives from any other screen, such as one that was already on its way when the rider
 * left it, finishes nothing.
 */
internal fun IntroState.canFinish(): Boolean = step == IntroStep.THEME && selectedThemeId != null
