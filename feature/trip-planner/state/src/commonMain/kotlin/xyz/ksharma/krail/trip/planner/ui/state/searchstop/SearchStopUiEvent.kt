package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

sealed interface SearchStopUiEvent {
    data class SearchTextChanged(val query: String) : SearchStopUiEvent
    data class TrackStopSelected(val stopItem: StopItem, val isRecentSearch: Boolean = false) : SearchStopUiEvent
    data object ClearRecentSearchStops : SearchStopUiEvent
}
