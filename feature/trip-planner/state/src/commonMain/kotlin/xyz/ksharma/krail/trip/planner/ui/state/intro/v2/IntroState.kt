package xyz.ksharma.krail.trip.planner.ui.state.intro.v2

import androidx.compose.runtime.Stable
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem

/**
 * State of the two step Intro, the Park & Ride question and then the first trip.
 *
 * Lives in `intro.v2` because the carousel [xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState]
 * still owns the plain `intro` package. The branch that removes the carousel moves this file up and
 * drops the `v2`.
 */
@Stable
data class IntroState(
    val step: IntroStep = IntroStep.PARK_RIDE,
    val parkRideAnswer: ParkRideAnswer = ParkRideAnswer.ASK,
    val locationResult: LocationResult = LocationResult.NOT_ASKED,
    val nearestStation: ParkRideStationPickerItem? = null,
    val availability: ParkRideAvailabilityState = ParkRideAvailabilityState.Loading,
    val startStopId: String? = null,
    val startName: String? = null,
    val endStopId: String? = null,
    val endName: String? = null,
    val addedParkRide: Boolean = false,
    val usedSearch: Boolean = false,
    /** The trip has been written, so the last button now reads Let's KRAIL. */
    val saved: Boolean = false,
) {
    /**
     * A trip needs both ends. A start with no end cannot be saved, so it counts as nothing picked.
     */
    val hasTrip: Boolean get() = startStopId != null && endStopId != null
}

enum class IntroStep { PARK_RIDE, FIRST_TRIP }

/** [ASK] is the unanswered question. */
enum class ParkRideAnswer { ASK, YES, NO }

enum class LocationResult { NOT_ASKED, GRANTED, DENIED, UNAVAILABLE }

/** Whether the live spots on the Park & Ride card are still loading, shown, or could not be read. */
enum class ParkRideAvailabilityState { Loading, Content, Unavailable }
