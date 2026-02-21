package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import xyz.ksharma.krail.core.maps.state.LatLng
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

    /**
     * Request map initialization. Only triggers if maps are available.
     * Used when entering dual-pane mode or user toggles to map in single-pane mode.
     */
    data object InitializeMap : SearchStopUiEvent

    // Map-related events
    data class MapCenterChanged(val center: LatLng) : SearchStopUiEvent

    data class UserLocationUpdated(val location: LatLng?) : SearchStopUiEvent

    data class TransportModeFilterToggled(val mode: TransportMode) : SearchStopUiEvent

    data class NearbyStopClicked(val stop: NearbyStopFeature) : SearchStopUiEvent

    data object MapOptionsClicked : SearchStopUiEvent

    data class SearchRadiusChanged(val radiusKm: Double) : SearchStopUiEvent

    data class ShowDistanceScaleToggled(val enabled: Boolean) : SearchStopUiEvent

    data class ShowCompassToggled(val enabled: Boolean) : SearchStopUiEvent

    /** Fired when the user taps the Map toggle button in SearchTopBar. Analytics-only. */
    data class MapToggleClicked(val selected: Boolean) : SearchStopUiEvent

    /** Fired when the user taps the Options button on the SearchStopMap. Analytics-only. */
    data object MapOptionsButtonClicked : SearchStopUiEvent

    /**
     * Fired when the user taps the User Location button on the SearchStopMap. Analytics-only.
     * [hadLocation] is true if location was already active at tap time (re-centering).
     */
    data class LocationButtonClicked(val hadLocation: Boolean) : SearchStopUiEvent

    /**
     * Fired when the user taps "Go to Settings" on the location permission denied banner.
     * Analytics-only.
     */
    data object LocationPermissionSettingsClicked : SearchStopUiEvent

    /**
     * Fired once when the user taps Save in MapOptionsBottomSheet. Analytics-only —
     * the individual state-change events (SearchRadiusChanged etc.) still fire separately.
     */
    data class MapOptionsSaved(
        val radiusKm: Double,
        val transportModes: String,
        val showDistanceScale: Boolean,
        val showCompass: Boolean,
        val radiusChanged: Boolean,
        val modesChanged: Boolean,
    ) : SearchStopUiEvent

    /** Fired when a stop is selected via the map's StopDetailsBottomSheet. Analytics-only. */
    data class TrackStopSelectedFromMap(
        val stopId: String,
        val searchRadiusKm: Double,
        val enabledModesCount: Int,
        val nearbyStopsCount: Int,
        val hadUserLocation: Boolean,
    ) : SearchStopUiEvent
}
