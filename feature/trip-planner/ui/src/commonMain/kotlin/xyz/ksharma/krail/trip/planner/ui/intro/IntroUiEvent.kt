package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult

/** What the Intro screens tell the view model. */
internal sealed interface IntroUiEvent {
    data object Back : IntroUiEvent

    /**
     * A badge on the theme screen was tapped. It selects the theme and does not move on. An id the Intro does not
     * offer, or the theme already selected, changes nothing.
     */
    data class PickTheme(val themeId: Int) : IntroUiEvent

    /** `No, show me another`: selects the next theme in the cycle, the first when none is selected yet. */
    data object ShowAnotherTheme : IntroUiEvent

    /** `Yes, this one`: the selected theme is the rider's choice, and the Intro moves on. Ignored with none selected. */
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
     * A row of the destination or origin list was tapped, or the right button took the highlighted one: the stop is
     * the end on the destination screen and the start on the origin screen, and the Intro moves on. There is no Next.
     */
    data class ChooseStop(val stop: IntroStop) : IntroUiEvent

    /** `Yes, this one`: takes the highlighted row, which is the first one listed, and does what tapping it does. */
    data object UseHighlightedStop : IntroUiEvent

    /**
     * `No, search`: the search field takes the focus and the keyboard opens. The screen answers it, because only the
     * screen has the field, and the view model ignores it. It never leaves a screen and never chooses anything.
     */
    data object FocusSearch : IntroUiEvent

    /**
     * The text in the search field changed. The view model runs the local stop search for it after a short pause in
     * the typing and the list shows what it finds. The text goes nowhere else: not to analytics, not to a log and
     * not to the saved state.
     */
    data class QueryChanged(val text: String) : IntroUiEvent

    data class PickStart(val stop: SuggestedStop) : IntroUiEvent

    data class PickEnd(val stop: SuggestedStop) : IntroUiEvent

    /** A stop chosen in the app's own stop search, for the start or the end. */
    data class StopPickedFromSearch(
        val isStart: Boolean,
        val stopId: String,
        val stopName: String,
    ) : IntroUiEvent

    /** The positive answer on the last screens: save the trip if there is a complete pair, and end the Intro. */
    data object FinishPressed : IntroUiEvent

    /** The negative answer on the last screens: end the Intro and write no trip. */
    data object FinishWithoutTrip : IntroUiEvent
}

/** One shot instructions for the screen. Each is delivered once and never replayed. */
internal sealed interface IntroEffect {
    /** The Intro is finished and everything it writes is written. */
    data object NavigateToSavedTrips : IntroEffect
}
