package xyz.ksharma.krail.trip.planner.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Marker interface for all Trip Planner routes.
 */
sealed interface TripPlannerRoute : NavKey

/**
 * Top-level route: Saved Trips (List in List-Detail pattern)
 */
@Serializable
data object SavedTripsRoute : TripPlannerRoute

/**
 * Detail route: Search for a stop
 *
 * @param editTripLeg When true, the search was opened from the timetable header
 * to replace one leg of the current trip ("Change origin" / "Change destination").
 * The selected stop is delivered as a `TimetableStopChangedResult` so the
 * timetable reloads in place instead of updating the SavedTrips fields.
 */
@Serializable
data class SearchStopRoute(
    val fieldType: SearchStopFieldType,
    val labelKey: String? = null,
    val editTripLeg: Boolean = false,
) : TripPlannerRoute

@Serializable
enum class SearchStopFieldType {
    FROM,
    TO,
    LABEL,
}

/**
 * Detail route: Time table for a trip
 */
@Serializable
data class TimeTableRoute(
    val fromStopId: String,
    val fromStopName: String,
    val toStopId: String,
    val toStopName: String,
    /**
     * A [xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem] as
     * JSON, when the rider chose a departure or arrival time before the timetable was opened.
     * Null means "now", which is what every existing caller passes and what the timetable has
     * always defaulted to.
     *
     * On the route rather than handed over after navigation, because the back stack is
     * serialised: a rider who chose "arrive 6pm" and then had the app killed in the background
     * comes back to 6pm rather than to now.
     */
    val dateTimeSelectionJson: String? = null,
) : TripPlannerRoute

/**
 * Detail route: Journey Map visualization
 */
@Serializable
data class JourneyMapRoute(
    val journeyId: String,
) : TripPlannerRoute

/**
 * Regular routes (not part of list-detail)
 */
@Serializable
data object ThemeSelectionRoute : TripPlannerRoute

@Serializable
data object SettingsRoute : TripPlannerRoute

@Serializable
data object OurStoryRoute : TripPlannerRoute

@Serializable
data object IntroRoute : TripPlannerRoute

/*
 * Service alerts and the date/time selector are deliberately not routes. Both render as a
 * `ModalBottomSheet` from inside `TimeTableEntry`, over the timetable that owns their state.
 * Route declarations for them existed with no `entry<…>` to render them, so navigating to
 * either would have failed at runtime; they were deleted rather than wired up (#1916).
 */

@Serializable
data object DiscoverRoute : TripPlannerRoute

/**
 * Detail route: Manage stop labels (rename / remove-assignment / delete / reorder).
 * A real screen, not a sheet — Google Maps "Your Places" shape.
 */
@Serializable
data object ManageStopLabelsRoute : TripPlannerRoute

/**
 * Detail route: pick a Park & Ride facility to follow on the home screen.
 */
@Serializable
data object AddParkRideRoute : TripPlannerRoute
