package xyz.ksharma.krail.trip.planner.ui.state.intro.v2

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
    /** What the question step shows below the question. [ParkRideCardPhase.Card] with [addedParkRide] is Added. */
    val parkRideCard: ParkRideCardPhase = ParkRideCardPhase.None,
    val startStopId: String? = null,
    val startName: String? = null,
    val endStopId: String? = null,
    val endName: String? = null,
    /**
     * The start was filled in by Add to home and the rider has not picked a start since, so taking
     * the station off home takes the start away again. A start the rider picked is never this.
     */
    val startFromParkRide: Boolean = false,
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

/**
 * What the Park & Ride step shows below the question. The rider's position is never part of it: it
 * is used in memory to pick the station and dropped, so no phase and nothing saved carries one.
 */
@Stable
sealed interface ParkRideCardPhase {
    /** No card: the question is unanswered, or the answer produced no usable location. */
    data object None : ParkRideCardPhase

    /**
     * Waiting for the permission and the one fix, then for the stops, shown as a skeleton in the
     * card's space. [stationId] is set only while a card the rider already had is being worked out
     * again after the process was killed.
     */
    data class Locating(val stationId: String? = null) : ParkRideCardPhase

    /** No station within the radius, or the stops were not ready in time. One plain line and Next. */
    data object NoStationNear : ParkRideCardPhase

    /** The nearest station. It is Added when [IntroState.addedParkRide] is true. */
    data class Card(val station: ParkRideCardStation) : ParkRideCardPhase
}

/**
 * The station on the card. [availability] is [ParkRideAvailabilityState.Loading] from the moment the
 * card shows, and settles to Content or Unavailable once, after the one availability call.
 */
@Stable
data class ParkRideCardStation(
    /** The smallest stop id of the station group, the key the card is worked out again from. */
    val stationId: String,
    val stationName: String,
    val carParks: ImmutableList<ParkRideCarPark> = persistentListOf(),
    val availability: ParkRideAvailabilityState = ParkRideAvailabilityState.Loading,
)

/** One car park. The numbers are null until a reading exists, so a name alone is a valid row. */
@Stable
data class ParkRideCarPark(
    val facilityId: String,
    val name: String,
    val spotsAvailable: Int? = null,
    val percentageFull: Int? = null,
    /** The time of the reading as the car park feed gave it, such as 8:05 AM. */
    val timeText: String? = null,
) {
    val hasReading: Boolean get() = spotsAvailable != null
}
