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
data class ServiceAlertRoute(
    val journeyId: String,
) : TripPlannerRoute

@Serializable
data object SettingsRoute : TripPlannerRoute

@Serializable
data object OurStoryRoute : TripPlannerRoute

@Serializable
data object IntroRoute : TripPlannerRoute

@Serializable
data class DateTimeSelectorRoute(
    val dateTimeSelectionItemJson: String? = null,
) : TripPlannerRoute {
    companion object {
        const val DATE_TIME_TEXT_KEY = "DateTimeSelectionKey"
    }
}

@Serializable
data object DiscoverRoute : TripPlannerRoute

/**
 * Detail route: Manage stop labels (rename / remove-assignment / delete / reorder).
 * A real screen, not a sheet — Google Maps "Your Places" shape.
 */
@Serializable
data object ManageStopLabelsRoute : TripPlannerRoute
