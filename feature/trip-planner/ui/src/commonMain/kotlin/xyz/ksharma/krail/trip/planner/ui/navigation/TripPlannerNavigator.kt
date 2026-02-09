package xyz.ksharma.krail.trip.planner.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Navigation events for Trip Planner feature.
 * This interface decouples the UI from the Navigator implementation.
 *
 * Results are passed using ResultEventBus for event-based communication.
 */
@Suppress("ComplexInterface", "TooManyFunctions")
interface TripPlannerNavigator {

    fun navigateToSearchStop(fieldType: SearchStopFieldType)
    fun navigateToTimeTable(fromStopId: String, fromStopName: String, toStopId: String, toStopName: String)
    fun navigateToJourneyMap(journeyId: String)
    fun navigateToSettings()
    fun navigateToDiscover()
    fun navigateToThemeSelection()
    fun navigateToAlerts(journeyId: String)
    fun navigateToDateTimeSelector(json: String?)
    fun navigateToOurStory()
    fun navigateToIntro()
    fun goBack()

    // Special operations
    fun updateTheme(hexColorCode: String)
    fun clearBackStackAndNavigate(route: NavKey)
}

/**
 * Result when a stop is selected
 */
data class StopSelectedResult(
    val fieldType: SearchStopFieldType,
    val stopId: String,
    val stopName: String,
)
