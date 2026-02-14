package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

sealed interface SearchStopUiEvent {
    data class SearchTextChanged(val query: String) : SearchStopUiEvent
    data class TrackStopSelected(
        val stopItem: StopItem,
        val isRecentSearch: Boolean = false,
        val searchQuery: String? = null,
    ) : SearchStopUiEvent

    data class ClearRecentSearchStops(val recentSearchCount: Int) : SearchStopUiEvent

    data object RefreshRecentStopsList : SearchStopUiEvent

    data class StopSelectionTypeClicked(val stopSelectionType: StopSelectionType) :
        SearchStopUiEvent

    // Map-related events
    data class MapCenterChanged(val center: LatLng) : SearchStopUiEvent

    data class TransportModeFilterToggled(val mode: TransportMode) : SearchStopUiEvent

    data class NearbyStopClicked(val stop: NearbyStopFeature) : SearchStopUiEvent

    data object MapOptionsClicked : SearchStopUiEvent

    data class SearchRadiusChanged(val radiusKm: Double) : SearchStopUiEvent

    data class ShowDistanceScaleToggled(val enabled: Boolean) : SearchStopUiEvent

    data class ShowCompassToggled(val enabled: Boolean) : SearchStopUiEvent
}
