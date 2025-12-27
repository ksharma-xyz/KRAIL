package xyz.ksharma.krail.trip.planner.ui.api

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
 */
@Serializable
data class SearchStopRoute(
    val fieldType: SearchStopFieldType
) : TripPlannerRoute

@Serializable
enum class SearchStopFieldType {
    FROM,
    TO
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

