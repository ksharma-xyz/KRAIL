package xyz.ksharma.krail.trip.planner.ui.state.timetable

import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem

sealed interface TimeTableUiEvent {
    data object SaveTripButtonClicked : TimeTableUiEvent
    data class LoadTimeTable(val trip: Trip) : TimeTableUiEvent
    data class JourneyCardClicked(val journeyId: String) : TimeTableUiEvent
    data class DateTimeSelectionChanged(val dateTimeSelectionItem: DateTimeSelectionItem?) :
        TimeTableUiEvent

    data object ReverseTripButtonClicked : TimeTableUiEvent
    data object RetryButtonClicked : TimeTableUiEvent
}
