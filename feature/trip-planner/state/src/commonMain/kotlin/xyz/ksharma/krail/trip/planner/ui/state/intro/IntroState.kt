package xyz.ksharma.krail.trip.planner.ui.state.intro

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import xyz.ksharma.krail.core.transport.TransportMode

/** State of the Intro, one screen and one question at a time. */
@Stable
data class IntroState(
    val step: IntroStep = IntroFirstStep,
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
    /**
     * The theme the rider picked on the last screen, as the id of the app's theme, or null before a pick. The
     * right button is not shown until there is one. It is kept as the id so the state module needs nothing from
     * the design system, and the Intro's UI turns it back into a theme.
     */
    val selectedThemeId: Int? = null,
    /**
     * The answer the rider gave on the confirm screen, or null before one. The confirm screen only records it, and
     * the finish on the last screen acts on it: the trip is written when it is [TripDecision.SAVED] and both stops are
     * set. It is saved with the rest of the Intro's progress, so a rider who is back on the theme screen after the app was
     * killed keeps the answer. It carries no stop and no name of its own, and a missing answer writes nothing.
     */
    val tripDecision: TripDecision? = null,
    /**
     * The nearest train, metro and light rail stations to the rider, nearest first, as ids and names only. They
     * are worked out once from the fix the Park & Ride question already took, and the position is dropped as soon
     * as they are. Never saved: after the process is killed this is empty and the curated list shows.
     */
    val nearbyStops: ImmutableList<IntroStop> = persistentListOf(),
    /**
     * The transport modes of the stops the screens list, by stop id, which the rows draw as badges. It carries no
     * position and nothing about the rider. A stop with no entry is drawn with no badge. Never saved.
     */
    val stopModes: ImmutableMap<String, ImmutableList<TransportMode>> = persistentMapOf(),
    /**
     * What the rider has typed into the search field of the destination or origin screen. Transient: it is never
     * saved, never reported and never logged.
     */
    val query: String = "",
    /** What the search found for [query]. Transient, like [query]. */
    val search: IntroSearchPhase = IntroSearchPhase.Idle,
    /**
     * The stop the rider has tapped on the destination or origin screen and not yet confirmed, or null while none is.
     * Nothing is selected when a screen opens, so a list never reads as already chosen. The right button is shown
     * only while this is set, and it confirms this stop. Transient, like [query]: it is dropped when the typed text
     * changes, because the list under it is then a different one, and when the rider moves on.
     */
    val selectedStop: IntroStop? = null,
) {
    /**
     * A trip needs both ends. A start with no end cannot be saved, so it counts as nothing picked.
     */
    val hasTrip: Boolean get() = startStopId != null && endStopId != null
}

/**
 * A stop the Intro offers or the rider chose: its id and its name, and nothing else. It never carries a
 * position, so no list of stops can hold where the rider is.
 */
@Stable
data class IntroStop(val stopId: String, val name: String)

/**
 * What the typed search has found, for the destination and origin screens. The curated suggestions show while
 * it is [Idle], the results replace them when there are some, and the app's own no match line shows when the
 * stops table was asked and found nothing.
 */
@Stable
sealed interface IntroSearchPhase {
    /** No query, or the stops table is not ready to answer one: the suggestions show and typing returns nothing yet. */
    data object Idle : IntroSearchPhase

    /** The stops that match the query, best first. Never empty: no match is [NoMatch]. */
    data class Results(val stops: ImmutableList<IntroStop>) : IntroSearchPhase

    /** The stops table answered and nothing matched the query. */
    data object NoMatch : IntroSearchPhase
}

/**
 * The Intro screens in the order a rider meets them, one question on each. Screens the rider has
 * no decision to make on are skipped, and the order in which the rest are shown is decided in one
 * place, the UI module's `IntroStepFlow`.
 */
enum class IntroStep {
    PARK_RIDE,
    DESTINATION,

    /** Skipped when a Park & Ride station was added, because that station is already the start. */
    ORIGIN,
    CONFIRM,

    /** The theme colour, the last screen. Its button ends the Intro. */
    THEME,
}

/**
 * The screen the Intro opens on, Park & Ride. It is one value so a screen can be put before it or
 * taken off the front without changing anything else: the flow, Back and the default state all read it.
 */
val IntroFirstStep: IntroStep = IntroStep.PARK_RIDE

/**
 * What the rider answered on the confirm screen. It only records the answer and moves on: the trip is written
 * (or not) when the Intro finishes, on the theme screen.
 */
enum class TripDecision { SAVED, DECLINED }

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
