package xyz.ksharma.krail.trip.planner.ui.navigation

import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.core.navigation.NavigatorBase

/**
 * Implementation of TripPlannerNavigator for the trip planner feature module.
 *
 * This class wraps NavigatorBase to provide feature-specific navigation methods,
 * keeping the feature module decoupled from the app module's Navigator implementation.
 * Results are sent via ResultEventBus which is provided via LocalResultEventBus composition local.
 *
 * @param baseNavigator The base navigator interface from core:navigation module
 */
@Suppress("TooManyFunctions")
internal class TripPlannerNavigatorImpl(
    private val baseNavigator: NavigatorBase,
) : TripPlannerNavigator {

    override fun navigateToSearchStop(fieldType: SearchStopFieldType) {
        baseNavigator.goTo(SearchStopRoute(fieldType))
    }

    override fun navigateToTimeTable(
        fromStopId: String,
        fromStopName: String,
        toStopId: String,
        toStopName: String,
    ) {
        baseNavigator.goToSingleTopOrReplace(
            TimeTableRoute(
                fromStopId = fromStopId,
                fromStopName = fromStopName,
                toStopId = toStopId,
                toStopName = toStopName,
            ),
        )
    }

    override fun navigateToSettings() {
        baseNavigator.goTo(SettingsRoute)
    }

    override fun navigateToDiscover() {
        baseNavigator.goTo(DiscoverRoute)
    }

    override fun navigateToThemeSelection() {
        baseNavigator.goTo(ThemeSelectionRoute)
    }

    override fun navigateToAlerts(journeyId: String) {
        baseNavigator.goTo(ServiceAlertRoute(journeyId))
    }

    override fun navigateToDateTimeSelector(json: String?) {
        baseNavigator.goTo(DateTimeSelectorRoute(json))
    }

    override fun navigateToOurStory() {
        baseNavigator.goTo(OurStoryRoute)
    }

    override fun navigateToIntro() {
        baseNavigator.goTo(IntroRoute)
    }

    override fun goBack() {
        baseNavigator.pop()
    }

    override fun updateTheme(hexColorCode: String) {
        baseNavigator.updateTheme(hexColorCode)
    }

    override fun clearBackStackAndNavigate(route: NavKey) {
        baseNavigator.resetRoot(route)
    }
}
