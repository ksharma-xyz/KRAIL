package xyz.ksharma.krail.trip_planner.ui.state.searchstop

sealed interface SearchStopUiEvent {
    data class SearchTextChanged(val query: String) : SearchStopUiEvent
}
