package xyz.ksharma.krail.trip.planner.ui.state.timetable

import androidx.compose.ui.graphics.ImageBitmap
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem

sealed interface TimeTableUiEvent {

    data object SaveTripButtonClicked : TimeTableUiEvent

    data class LoadTimeTable(val trip: Trip) : TimeTableUiEvent

    data class JourneyCardClicked(val journeyId: String) : TimeTableUiEvent

    data class DateTimeSelectionChanged(val dateTimeSelectionItem: DateTimeSelectionItem?) :
        TimeTableUiEvent

    data object ReverseTripButtonClicked : TimeTableUiEvent

    data object RetryButtonClicked : TimeTableUiEvent

    data object AnalyticsDateTimeSelectorClicked : TimeTableUiEvent

    data object BackClick : TimeTableUiEvent

    data class AnalyticsJourneyLegClicked(
        val expanded: Boolean,
        val transportMode: String,
        val lineName: String,
    ) : TimeTableUiEvent

    data class ModeSelectionChanged(val unselectedModes: Set<Int>) : TimeTableUiEvent

    /**
     * when tru, the selection row is displayed else it is hidden.
     */
    data class ModeClicked(val displayModeSelectionRow: Boolean) : TimeTableUiEvent

    data class ShareJourneyClicked(
        val bitmap: ImageBitmap,
        val shareText: String,
        val journeyId: String,
        val isPastDeparture: Boolean,
    ) : TimeTableUiEvent

    /** Load additional future trips after the last currently shown departure. */
    data object LoadMoreTrips : TimeTableUiEvent

    /** Load past trips before the earliest currently shown departure. */
    data object LoadPreviousTrips : TimeTableUiEvent

    /**
     * Fired when the user taps the origin or destination label in the timetable
     * sticky header (the gesture that opens stop search scoped to that leg).
     *
     * Analytics-only — navigation to search is wired at the entry level.
     * Routing this through the VM keeps analytics calls in one place and makes
     * the path testable with `FakeAnalytics`.
     */
    data class OriginDestinationStopHeaderClicked(
        val stopId: String,
        val stopName: String,
        val isOrigin: Boolean,
    ) : TimeTableUiEvent

    /**
     * Fired when the user taps the departures icon next to a stop name in the
     * timetable header (the gesture that opens the stop-details bottom sheet).
     *
     * Analytics-only — the sheet itself is opened by local Compose state in
     * `TimeTableScreen`.
     */
    data class DeparturesIconClicked(
        val stopId: String,
        val stopName: String,
        val isOrigin: Boolean,
    ) : TimeTableUiEvent

    /**
     * Fired when the user picked a replacement stop from the leg-scoped search
     * opened via [OriginDestinationStopHeaderClicked]. Reloads the timetable in
     * place with the changed leg (no back-stack rebuild), reusing the
     * reverse-trip reload mechanics.
     */
    data class TripStopChanged(
        val stopId: String,
        val stopName: String,
        val isOrigin: Boolean,
    ) : TimeTableUiEvent
}
