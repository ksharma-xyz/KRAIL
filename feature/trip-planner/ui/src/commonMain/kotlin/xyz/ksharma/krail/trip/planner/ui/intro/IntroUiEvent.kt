package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult

/** What the Intro screens tell the view model. */
internal sealed interface IntroUiEvent {
    /**
     * A badge on the theme screen was tapped. It selects the theme and does not move on. An id the Intro does not
     * offer, or the theme already selected, changes nothing.
     */
    data class PickTheme(val themeId: Int) : IntroUiEvent

    /**
     * `Yes, this one`, the only button on the theme screen, which is the last: the selected theme is the rider's
     * choice, and the Intro finishes, writing the trip if the rider said yes to saving it. Ignored with none selected.
     */
    data object ConfirmTheme : IntroUiEvent

    /**
     * The answer to the Park & Ride question. Yes starts the location step, and the screen then asks for the
     * permission and sends [LocationResultReceived]. No moves on to the next screen.
     */
    data class AnswerParkRide(val yes: Boolean) : IntroUiEvent

    /**
     * The permission result and the one fix, sent once after a yes and ignored at any other time.
     * [location] is the fix behind a granted result, and null for any other result. It is used to
     * find the station and kept nowhere, so a screen must not hold on to it either.
     */
    data class LocationResultReceived(val result: LocationResult, val location: LatLng?) : IntroUiEvent

    /**
     * Add to home. Adds the station and moves on to the next screen in one step. A station already added is
     * left as it is and the rider moves on.
     */
    data object AddParkRideAndContinue : IntroUiEvent

    /**
     * Not now, on the card. Moves on to the next screen with nothing added, and takes a station the rider had
     * already added off home, because this answer is their latest.
     */
    data object DeclineParkRide : IntroUiEvent

    /**
     * A row of the destination or origin list was tapped, or a typed result: the stop is selected, highlighted and
     * announced as selected, and the single button appears. A tap on another row moves the selection. Nothing is chosen
     * and the Intro does not move on.
     */
    data class SelectStop(val stop: IntroStop) : IntroUiEvent

    /**
     * `Yes, this one`, the only button on the destination and origin screens: the selected stop is the end on the
     * destination screen and the start on the origin screen, and the Intro moves on. Ignored with no stop selected.
     */
    data object ConfirmStop : IntroUiEvent

    /**
     * The text in the search field changed. The view model runs the local stop search for it after a short pause in
     * the typing and the list shows what it finds. The text goes nowhere else: not to analytics, not to a log and
     * not to the saved state.
     */
    data class QueryChanged(val text: String) : IntroUiEvent

    /**
     * The answer on the confirm screen, `Yes, save it` ([yes] true) or `Not now` ([yes] false). It only records the
     * answer and moves on to the theme screen, which finishes the Intro: nothing is written here. Not now is the
     * only way to leave without saving.
     */
    data class AnswerSaveTrip(val yes: Boolean) : IntroUiEvent
}

/**
 * The only steps an event is valid on, and it is dropped on every other, whatever the state says. This is what keeps a tap that
 * arrives late, from the screen that is sliding out or from a double tap, from doing anything on the screen that came after.
 * One table over the sealed type, so a new event is a compile error here and not an event that is accepted anywhere.
 *
 * The stop screens share their three events, so those also carry the step they were pressed on (see
 * [IntroFlowViewModel.onEvent]), because the destination and the origin screen cannot tell them apart by type.
 */
internal fun IntroUiEvent.validSteps(): Set<IntroStep> = when (this) {
    is IntroUiEvent.AnswerParkRide,
    is IntroUiEvent.LocationResultReceived,
    IntroUiEvent.AddParkRideAndContinue,
    IntroUiEvent.DeclineParkRide,
    -> setOf(IntroStep.PARK_RIDE)
    is IntroUiEvent.SelectStop,
    IntroUiEvent.ConfirmStop,
    is IntroUiEvent.QueryChanged,
    -> setOf(IntroStep.DESTINATION, IntroStep.ORIGIN)
    is IntroUiEvent.AnswerSaveTrip -> setOf(IntroStep.CONFIRM)
    is IntroUiEvent.PickTheme,
    IntroUiEvent.ConfirmTheme,
    -> setOf(IntroStep.THEME)
}

/** One shot instructions for the screen. Each is delivered once and never replayed. */
internal sealed interface IntroEffect {
    /**
     * The press that finishes the Intro was accepted. Sent at once, in the same step as the press and before anything is
     * written or waited for, so the finale starts and the rider sees an answer to the tap straight away, never a button
     * that does nothing while the stops data loads. A second press after it does nothing.
     */
    data object StartFinale : IntroEffect

    /**
     * The Intro is finished and everything it writes is written: the trip, both flags and the outcome, and the wait for the
     * stops, which is bounded, is over. It follows [StartFinale], and it lets the app under the finale's colour be shown.
     */
    data object NavigateToSavedTrips : IntroEffect
}
