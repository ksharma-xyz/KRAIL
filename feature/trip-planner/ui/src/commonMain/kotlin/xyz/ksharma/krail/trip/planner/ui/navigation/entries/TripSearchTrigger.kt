package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The key Saved Trips and Search Stop both ask Koin for the one Saved Trips ViewModel with. ViewModels are
 * scoped to the Activity here (no nav entry decorator holds a store), so the same key is the same instance
 * on either entry. It is the one place the home's stops and the time a rider set (Ask KRAIL's "by 6pm")
 * live, so Search Stop reads and writes them there instead of holding a second copy.
 */
internal const val SAVED_TRIPS_VIEW_MODEL_KEY = "SavedTripsNav"

/**
 * Opens the trip results for [fromStop] and [toStop], or does nothing when either is missing or they are the
 * same stop, which is not a trip. The load analytics event goes first, then the timetable route, so every
 * caller counts and navigates in the same order.
 */
internal fun triggerTripSearch(
    fromStop: StopItem?,
    toStop: StopItem?,
    onEvent: (SavedTripUiEvent) -> Unit,
    tripPlannerNavigator: TripPlannerNavigator,
    dateTimeSelectionJson: String? = null,
) {
    if (fromStop != null && toStop != null && fromStop.stopId != toStop.stopId) {
        onEvent(
            SavedTripUiEvent.AnalyticsLoadTimeTableClick(
                fromStopId = fromStop.stopId,
                toStopId = toStop.stopId,
            ),
        )
        tripPlannerNavigator.navigateToTimeTable(
            fromStopId = fromStop.stopId,
            fromStopName = fromStop.stopName,
            toStopId = toStop.stopId,
            toStopName = toStop.stopName,
            dateTimeSelectionJson = dateTimeSelectionJson,
        )
    }
}

/**
 * The go circle on Search Stop. The trip results open on top of Search Stop, which stays on the back stack
 * with its From and To still filled, so back from the results lands there and not on the home.
 *
 * Both stops are also written to the Saved Trips ViewModel, which the classic home's bars still read, and
 * [dateTimeSelectionJson] is the time held there, so a time the rider set travels with the trip.
 */
internal fun startTripFromSearchStop(
    from: StopItem,
    to: StopItem,
    dateTimeSelectionJson: String?,
    onEvent: (SavedTripUiEvent) -> Unit,
    tripPlannerNavigator: TripPlannerNavigator,
) {
    onEvent(SavedTripUiEvent.FromStopChanged(from.toJsonString()))
    onEvent(SavedTripUiEvent.ToStopChanged(to.toJsonString()))
    triggerTripSearch(
        fromStop = from,
        toStop = to,
        onEvent = onEvent,
        tripPlannerNavigator = tripPlannerNavigator,
        dateTimeSelectionJson = dateTimeSelectionJson,
    )
}
