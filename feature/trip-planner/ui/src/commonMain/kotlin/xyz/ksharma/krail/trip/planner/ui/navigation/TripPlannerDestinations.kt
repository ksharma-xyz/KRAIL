package xyz.ksharma.krail.trip.planner.ui.navigation

import    androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import xyz.ksharma.krail.trip.planner.ui.alerts.alertsDestination
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.dateTimeSelectorDestination
import xyz.ksharma.krail.trip.planner.ui.savedtrips.savedTripsDestination
import xyz.ksharma.krail.trip.planner.ui.searchstop.searchStopDestination
import xyz.ksharma.krail.trip.planner.ui.settings.about.aboutUsDestination
import xyz.ksharma.krail.trip.planner.ui.settings.settingsDestination
import xyz.ksharma.krail.trip.planner.ui.timetable.timeTableDestination
import xyz.ksharma.krail.trip.planner.ui.themeselection.themeSelectionDestination

/**
 * Nested navigation graph for the trip planner feature.
 * It contains all the screens in the feature Trip Planner.
 */
fun NavGraphBuilder.tripPlannerDestinations(
    // TODO -  do not wanna add NavController here, but moving all callbacks to android-app module is not scaleable.
    navController: NavHostController,
) {
    navigation<TripPlannerNavRoute>(startDestination = SavedTripsRoute) {
        savedTripsDestination(navController)

        searchStopDestination(navController)

        timeTableDestination(navController)

        themeSelectionDestination(navController)

        alertsDestination(navController)

        settingsDestination(navController)

        dateTimeSelectorDestination(navController)

        aboutUsDestination(navController)
    }
}

@Serializable
internal sealed class SearchStopFieldType(val key: String) {
    data object FROM : SearchStopFieldType("FromSearchStopResult")
    data object TO : SearchStopFieldType("ToSearchStopResult")

    companion object {
        fun fromKey(key: String): SearchStopFieldType {
            return when (key) {
                FROM.key -> FROM
                TO.key -> TO
                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
    }
}

@Serializable
internal data object TripPlannerNavRoute

@Serializable
data object SavedTripsRoute

@Serializable
internal data class TimeTableRoute(
    val fromStopId: String,
    val fromStopName: String,
    val toStopId: String,
    val toStopName: String,
)

@Serializable
internal data class SearchStopRoute(val fieldTypeKey: String) {
    val fieldType: SearchStopFieldType
        get() = SearchStopFieldType.fromKey(fieldTypeKey)
}

@Serializable
data object ThemeSelectionRoute

@Serializable
internal data class ServiceAlertRoute(
    val journeyId: String,
)

@Serializable
data object SettingsRoute

@Serializable
data object AboutUsRoute

@Serializable
data class DateTimeSelectorRoute(
    // Noop, need x coz it's data class, need to put keys in companion obj rather than elsewhere.
    val dateTimeSelectionItemJson: String? = null,
) {
    companion object {
        const val DATE_TIME_TEXT_KEY = "DateTimeSelectionKey"
    }
}
