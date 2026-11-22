package xyz.ksharma.krail.trip.planner.ui.navigation

import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Navigation events for Trip Planner feature.
 * This interface decouples the UI from the Navigator implementation.
 *
 * Results are passed using ResultEventBus for event-based communication.
 */
@Suppress("ComplexInterface", "TooManyFunctions")
interface TripPlannerNavigator {

    /**
     * Opens Search Stop. [presetFrom] and [presetTo] open the To and From form already holding a stop, so
     * a caller that knows one end (a pin card, a label chip, an Ask KRAIL answer) hands it over and the
     * rider only chooses the other and goes.
     */
    fun navigateToSearchStop(
        fieldType: SearchStopFieldType,
        labelKey: String? = null,
        editTripLeg: Boolean = false,
        presetFrom: StopItem? = null,
        presetTo: StopItem? = null,
    )

    /**
     * @param dateTimeSelectionJson A serialised
     * [xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem] when a
     * departure or arrival time was chosen before the timetable opened. Null means now.
     */
    fun navigateToTimeTable(
        fromStopId: String,
        fromStopName: String,
        toStopId: String,
        toStopName: String,
        dateTimeSelectionJson: String? = null,
    )
    fun navigateToJourneyMap(journeyId: String)
    fun navigateToSettings()
    fun navigateToDiscover()
    fun navigateToManageStopLabels()

    fun navigateToAddParkRide()
    fun navigateToThemeSelection()
    fun navigateToOurStory()
    fun navigateToIntro()
    fun navigateToTrackTrip(encodedData: String? = null)
    fun navigateToDebugConfig()
    fun navigateToDebugConfigNetwork()
    fun goBack()

    // Special operations
    fun updateTheme(hexColorCode: String)
    fun clearBackStackAndNavigate(route: NavKey)
}

/**
 * Result when a stop is selected for one field. Search Stop sends it for a label. The To and From form
 * sends no result at all: its go circle opens the trip results itself, so a pick on it never reaches the
 * caller on its own.
 */
data class StopSelectedResult(
    val fieldType: SearchStopFieldType,
    val stopId: String,
    val stopName: String,
    val labelKey: String? = null,
    val locationKind: LocationKind = LocationKind.TRANSIT_STOP,
    val addressType: String? = null,
)

/**
 * Result when the user picked a replacement stop from the leg-scoped search
 * opened via the timetable header (`SearchStopRoute.editTripLeg`). Delivered
 * on its own ResultEventBus channel so it never races SavedTrips' listener
 * for [StopSelectedResult].
 */
data class TimetableStopChangedResult(
    val isOrigin: Boolean,
    val stopId: String,
    val stopName: String,
)
