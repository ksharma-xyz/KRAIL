package xyz.ksharma.krail.trip.planner.ui.flow

import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The navigation edge of the home entry. A flow test only ever needs to know *that* the entry
 * asked to leave and for which field, because the harness plays the other screen's part by
 * putting a result back on the real [xyz.ksharma.krail.core.navigation.ResultEventBus]. No
 * second destination is composed.
 */
internal class RecordingTripPlannerNavigator : TripPlannerNavigator {

    /** Every stop-search request the entry made, oldest first. */
    val searchStopRequests = mutableListOf<SearchStopFieldType>()

    /** The label key of each stop-search request, in the same order. Null for a From or To search. */
    val searchStopLabelKeys = mutableListOf<String?>()

    /** How many times the entry opened Manage stop labels. */
    var manageStopLabelsRequests = 0
        private set

    /** Every timetable the entry asked to open, as "fromStopId>toStopId" pairs. */
    val timeTableRequests = mutableListOf<Pair<String, String>>()

    /** The serialised time each timetable request carried, in the same order. Null means now. */
    val timeTableDateTimes = mutableListOf<String?>()

    /** How many times the entry asked to go back. */
    var goBackRequests = 0
        private set

    /** The start and destination each stop-search request opened holding, in the same order. */
    val searchStopPresets = mutableListOf<Pair<StopItem?, StopItem?>>()

    override fun navigateToSearchStop(
        fieldType: SearchStopFieldType,
        labelKey: String?,
        editTripLeg: Boolean,
        presetFrom: StopItem?,
        presetTo: StopItem?,
    ) {
        searchStopRequests += fieldType
        searchStopLabelKeys += labelKey
        searchStopPresets += presetFrom to presetTo
    }

    override fun navigateToTimeTable(
        fromStopId: String,
        fromStopName: String,
        toStopId: String,
        toStopName: String,
        dateTimeSelectionJson: String?,
    ) {
        timeTableRequests += fromStopId to toStopId
        timeTableDateTimes += dateTimeSelectionJson
    }

    override fun navigateToJourneyMap(journeyId: String) = Unit
    override fun navigateToSettings() = Unit
    override fun navigateToDiscover() = Unit
    override fun navigateToManageStopLabels() {
        manageStopLabelsRequests++
    }
    override fun navigateToAddParkRide() = Unit
    override fun navigateToThemeSelection() = Unit
    override fun navigateToOurStory() = Unit
    override fun navigateToTrackTrip(encodedData: String?) = Unit
    override fun navigateToDebugConfig() = Unit
    override fun navigateToDebugConfigNetwork() = Unit
    override fun goBack() {
        goBackRequests++
    }
    override fun updateTheme(hexColorCode: String) = Unit
    override fun clearBackStackAndNavigate(route: NavKey) = Unit
}
