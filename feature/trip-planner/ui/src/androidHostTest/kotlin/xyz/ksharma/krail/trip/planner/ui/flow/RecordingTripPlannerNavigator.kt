package xyz.ksharma.krail.trip.planner.ui.flow

import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator

/**
 * The navigation edge of the home entry. A flow test only ever needs to know *that* the entry
 * asked to leave and for which field, because the harness plays the other screen's part by
 * putting a result back on the real [xyz.ksharma.krail.core.navigation.ResultEventBus]. No
 * second destination is composed.
 */
internal class RecordingTripPlannerNavigator : TripPlannerNavigator {

    /** Every stop-search request the entry made, oldest first. */
    val searchStopRequests = mutableListOf<SearchStopFieldType>()

    /** Every timetable the entry asked to open, as "fromStopId>toStopId" pairs. */
    val timeTableRequests = mutableListOf<Pair<String, String>>()

    override fun navigateToSearchStop(
        fieldType: SearchStopFieldType,
        labelKey: String?,
        editTripLeg: Boolean,
    ) {
        searchStopRequests += fieldType
    }

    override fun navigateToTimeTable(
        fromStopId: String,
        fromStopName: String,
        toStopId: String,
        toStopName: String,
        dateTimeSelectionJson: String?,
    ) {
        timeTableRequests += fromStopId to toStopId
    }

    override fun navigateToJourneyMap(journeyId: String) = Unit
    override fun navigateToSettings() = Unit
    override fun navigateToDiscover() = Unit
    override fun navigateToManageStopLabels() = Unit
    override fun navigateToAddParkRide() = Unit
    override fun navigateToThemeSelection() = Unit
    override fun navigateToAlerts(journeyId: String) = Unit
    override fun navigateToDateTimeSelector(json: String?) = Unit
    override fun navigateToOurStory() = Unit
    override fun navigateToIntro() = Unit
    override fun navigateToTrackTrip(encodedData: String?) = Unit
    override fun navigateToDebugConfig() = Unit
    override fun navigateToDebugConfigNetwork() = Unit
    override fun goBack() = Unit
    override fun updateTheme(hexColorCode: String) = Unit
    override fun clearBackStackAndNavigate(route: NavKey) = Unit
}
