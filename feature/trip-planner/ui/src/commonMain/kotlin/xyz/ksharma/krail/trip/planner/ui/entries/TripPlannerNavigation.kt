package xyz.ksharma.krail.trip.planner.ui.entries

/**
 * Navigation events for Trip Planner feature.
 * This interface decouples the UI from the Navigator implementation.
 *
 * Results are passed using ResultEventBus for event-based communication.
 */
interface TripPlannerNavigator {

    fun navigateToSearchStop(fieldType: SearchStopFieldType)
    fun navigateToTimeTable(fromStopId: String, fromStopName: String, toStopId: String, toStopName: String)
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
    fun clearBackStackAndNavigate(route: Any)
}

/**
 * Result when a stop is selected
 */
data class StopSelectedResult(
    val fieldType: SearchStopFieldType,
    val stopId: String,
    val stopName: String
)

/**
 * Result when date/time is selected
 */
data class DateTimeSelectedResult(
    val dateTimeJson: String
)
