package xyz.ksharma.krail.trip.planner.ui.state.timetable

import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem

sealed interface TimeTableUiEvent {

    data object SaveTripButtonClicked : TimeTableUiEvent

    data class LoadTimeTable(val trip: Trip) : TimeTableUiEvent

    /**
     * Updates the trip information with resolved stop names from deep links
     */
    data class UpdateTripFromDeepLink(val trip: Trip) : TimeTableUiEvent

    data class JourneyCardClicked(val journeyId: String) : TimeTableUiEvent

    data class DateTimeSelectionChanged(val dateTimeSelectionItem: DateTimeSelectionItem?) :
        TimeTableUiEvent

    data object ReverseTripButtonClicked : TimeTableUiEvent

    data object RetryButtonClicked : TimeTableUiEvent

    data object AnalyticsDateTimeSelectorClicked : TimeTableUiEvent

    data class BackClick(val isPreviousBackStackEntryNull: Boolean) : TimeTableUiEvent

    data class JourneyLegClicked(val expanded: Boolean) : TimeTableUiEvent

    data class ModeSelectionChanged(val unselectedModes: Set<Int>) : TimeTableUiEvent

    /**
     * when tru, the selection row is displayed else it is hidden.
     */
    data class ModeClicked(val displayModeSelectionRow: Boolean) : TimeTableUiEvent
}
