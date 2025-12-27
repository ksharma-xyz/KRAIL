package xyz.ksharma.krail.trip.planner.ui.api

/**
 * Navigation events for Trip Planner feature.
 * This interface decouples the UI from the Navigator implementation.
 *
 * Note: Results are passed using simple data classes to avoid circular dependencies.
 */
interface TripPlannerNavigator {
    // Navigation results as a reactive stream of result objects
    val navigationResults: kotlinx.coroutines.flow.SharedFlow<NavResult>

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

    // For emitting results from screens
    suspend fun emitStopSelected(fieldType: SearchStopFieldType, stopId: String, stopName: String)
    suspend fun emitDateTimeSelected(dateTimeJson: String)
}

/**
 * Base interface for navigation results
 */
sealed interface NavResult

/**
 * Result when a stop is selected
 */
data class StopSelectedResult(
    val fieldType: SearchStopFieldType,
    val stopId: String,
    val stopName: String
) : NavResult

/**
 * Result when date/time is selected
 */
data class DateTimeSelectedResult(
    val dateTimeJson: String
) : NavResult

