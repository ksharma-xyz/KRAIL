package xyz.ksharma.krail.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import xyz.ksharma.krail.trip.planner.ui.api.*

/**
 * Implementation of TripPlannerNavigator using the Navigator.
 * Maps between app module's NavigationResult and api module's NavResult types.
 */
class TripPlannerNavigatorImpl(
    private val navigator: Navigator
) : TripPlannerNavigator {

    // Scope for the shared flow
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Map Navigator's results to api module's NavResult types
    override val navigationResults: SharedFlow<NavResult> =
        navigator.results.map { result ->
            println("TripPlannerNavigatorImpl: Mapping result: $result")
            val mapped = when (result) {
                is NavigationResult.StopSelected -> StopSelectedResult(
                    fieldType = result.fieldType,
                    stopId = result.stopId,
                    stopName = result.stopName
                )
                is NavigationResult.DateTimeSelected -> DateTimeSelectedResult(
                    dateTimeJson = result.dateTimeJson
                )
            }
            println("TripPlannerNavigatorImpl: Mapped to: $mapped")
            mapped
        }.shareIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            replay = 1  // Cache last result for new collectors
        )

    override fun navigateToSearchStop(fieldType: SearchStopFieldType) {
        navigator.navigate(SearchStopRoute(fieldType))
    }

    override fun navigateToTimeTable(
        fromStopId: String,
        fromStopName: String,
        toStopId: String,
        toStopName: String
    ) {
        navigator.navigate(
            TimeTableRoute(
                fromStopId = fromStopId,
                fromStopName = fromStopName,
                toStopId = toStopId,
                toStopName = toStopName
            )
        )
    }

    override fun navigateToSettings() {
        navigator.navigate(SettingsRoute)
    }

    override fun navigateToDiscover() {
        navigator.navigate(DiscoverRoute)
    }

    override fun navigateToThemeSelection() {
        navigator.navigate(ThemeSelectionRoute)
    }

    override fun navigateToAlerts(journeyId: String) {
        navigator.navigate(ServiceAlertRoute(journeyId))
    }

    override fun navigateToDateTimeSelector(json: String?) {
        navigator.navigate(DateTimeSelectorRoute(json))
    }

    override fun navigateToOurStory() {
        navigator.navigate(OurStoryRoute)
    }

    override fun navigateToIntro() {
        navigator.navigate(IntroRoute)
    }

    override fun goBack() {
        navigator.goBack()
    }

    override fun updateTheme(hexColorCode: String) {
        navigator.updateTheme(hexColorCode)
    }

    override fun clearBackStackAndNavigate(route: Any) {
        if (route is NavKey) {
            navigator.clearBackStackAndNavigate(route)
        }
    }

    // Emit typed results using app module's NavigationResult
    override suspend fun emitStopSelected(fieldType: SearchStopFieldType, stopId: String, stopName: String) {
        navigator.goBackWithResult(
            NavigationResult.StopSelected(
                fieldType = fieldType,
                stopId = stopId,
                stopName = stopName
            )
        )
    }

    override suspend fun emitDateTimeSelected(dateTimeJson: String) {
        navigator.goBackWithResult(
            NavigationResult.DateTimeSelected(dateTimeJson = dateTimeJson)
        )
    }
}

