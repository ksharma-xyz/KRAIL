package xyz.ksharma.krail.navigation

import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.trip.planner.ui.entries.DateTimeSelectorRoute
import xyz.ksharma.krail.trip.planner.ui.entries.DiscoverRoute
import xyz.ksharma.krail.trip.planner.ui.entries.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.entries.OurStoryRoute
import xyz.ksharma.krail.trip.planner.ui.entries.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.entries.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.entries.ServiceAlertRoute
import xyz.ksharma.krail.trip.planner.ui.entries.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.entries.ThemeSelectionRoute
import xyz.ksharma.krail.trip.planner.ui.entries.TimeTableRoute
import xyz.ksharma.krail.trip.planner.ui.entries.TripPlannerNavigator

/**
 * Implementation of TripPlannerNavigator using the Navigator.
 * Results are sent via ResultEventBus which is provided via LocalResultEventBus composition local.
 */
class TripPlannerNavigatorImpl(
    private val navigator: Navigator
) : TripPlannerNavigator {


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
}
