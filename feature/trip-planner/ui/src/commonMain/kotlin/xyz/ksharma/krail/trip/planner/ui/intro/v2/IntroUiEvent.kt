package xyz.ksharma.krail.trip.planner.ui.intro.v2

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult

/** What the Intro screens tell the view model. */
internal sealed interface IntroUiEvent {
    data object Back : IntroUiEvent

    /** The question step only. The trip step ends with [FinishPressed]. */
    data object Next : IntroUiEvent

    /** Yes starts the location step. The screen then asks for the permission and sends [LocationResultReceived]. */
    data class AnswerParkRide(val yes: Boolean) : IntroUiEvent

    /**
     * The permission result and the one fix, sent once after a yes and ignored at any other time.
     * [location] is the fix behind a granted result, and null for any other result. It is used to
     * find the station and kept nowhere, so a screen must not hold on to it either.
     */
    data class LocationResultReceived(val result: LocationResult, val location: LatLng?) : IntroUiEvent

    /** Add to home, and Added. */
    data object ToggleParkRide : IntroUiEvent

    data class PickStart(val stop: SuggestedStop) : IntroUiEvent

    data class PickEnd(val stop: SuggestedStop) : IntroUiEvent

    /** A stop chosen in the app's own stop search, for the start or the end. */
    data class StopPickedFromSearch(
        val isStart: Boolean,
        val stopId: String,
        val stopName: String,
    ) : IntroUiEvent

    /** The bottom button: Next, Try again, Save and start, Skip for now or Let's KRAIL. */
    data object FinishPressed : IntroUiEvent
}

/** One shot instructions for the screen. Each is delivered once and never replayed. */
internal sealed interface IntroEffect {
    /** The Intro is finished and everything it writes is written. */
    data object NavigateToSavedTrips : IntroEffect
}
