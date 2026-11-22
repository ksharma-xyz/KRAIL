package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult

/** What the Intro screens tell the view model. */
internal sealed interface IntroUiEvent {
    /**
     * A badge on the theme screen was tapped. It selects the theme and does not move on. An id the Intro does not
     * offer, or the theme already selected, changes nothing.
     */
    data class PickTheme(val themeId: Int) : IntroUiEvent

    /**
     * `Yes, this one`, the only button on the theme screen: the selected theme is the rider's choice, and the Intro moves
     * on. Ignored with none selected.
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

    /** `Yes, save it` on the confirm screen: save the trip the rider chose, and end the Intro. */
    data object FinishPressed : IntroUiEvent

    /** `Not now` on the confirm screen: end the Intro and write no trip. The only way out without saving. */
    data object FinishWithoutTrip : IntroUiEvent
}

/** One shot instructions for the screen. Each is delivered once and never replayed. */
internal sealed interface IntroEffect {
    /** The Intro is finished and everything it writes is written. */
    data object NavigateToSavedTrips : IntroEffect
}
