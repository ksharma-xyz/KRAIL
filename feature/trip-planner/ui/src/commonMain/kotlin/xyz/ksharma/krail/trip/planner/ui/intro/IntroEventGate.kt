package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

/**
 * Whether the Intro takes [event] now. An event is valid on one step only ([validSteps]) and is dropped anywhere else, and one
 * that says the step it was [pressedOn] is dropped when the Intro has moved on from that step ([step]), such as a tap on a screen
 * that was already sliding out. From the finish press on ([finishStarted]) nothing changes anything. So no event can answer a
 * question the rider is not looking at.
 */
internal fun introEventAccepted(
    finishStarted: Boolean,
    step: IntroStep,
    event: IntroUiEvent,
    pressedOn: IntroStep? = null,
): Boolean = !finishStarted && (pressedOn == null || pressedOn == step) && step in event.validSteps()
