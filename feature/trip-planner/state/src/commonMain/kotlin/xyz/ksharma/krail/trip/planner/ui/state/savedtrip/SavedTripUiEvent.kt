package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

sealed interface SavedTripUiEvent {

    data class DeleteSavedTrip(val trip: Trip) : SavedTripUiEvent

    data class AnalyticsSavedTripCardClick(val fromStopId: String, val toStopId: String) :
        SavedTripUiEvent

    data class AnalyticsLoadTimeTableClick(val fromStopId: String, val toStopId: String) :
        SavedTripUiEvent

    data object AnalyticsReverseSavedTrip : SavedTripUiEvent

    data object AnalyticsSettingsButtonClick : SavedTripUiEvent

    data object AnalyticsFromButtonClick : SavedTripUiEvent

    data object AnalyticsToButtonClick : SavedTripUiEvent

    data object AnalyticsDiscoverButtonClick : SavedTripUiEvent

    data class ParkRideCardClick(
        val parkRideState: ParkRideUiState, val isExpanded: Boolean
    ) : SavedTripUiEvent

    data class DismissInfoTile(val infoTile: InfoTileData) : SavedTripUiEvent

    data class InfoTileCtaClick(val infoTile: InfoTileData) : SavedTripUiEvent
}
