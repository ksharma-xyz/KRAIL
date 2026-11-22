package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

sealed interface SavedTripUiEvent {

    data class DeleteSavedTrip(val trip: Trip) : SavedTripUiEvent

    data class AnalyticsSavedTripCardClick(val fromStopId: String, val toStopId: String) :
        SavedTripUiEvent

    data class AnalyticsLoadTimeTableClick(val fromStopId: String, val toStopId: String) :
        SavedTripUiEvent

    data object ReverseStopClick : SavedTripUiEvent

    data object AnalyticsSettingsButtonClick : SavedTripUiEvent

    data object AnalyticsFromButtonClick : SavedTripUiEvent

    data object AnalyticsToButtonClick : SavedTripUiEvent

    data object AnalyticsDiscoverButtonClick : SavedTripUiEvent

    data class ParkRideCardClick(
        val parkRideState: ParkRideUiState,
        val isExpanded: Boolean,
    ) : SavedTripUiEvent

    data class DismissInfoTile(val infoTile: InfoTileData) : SavedTripUiEvent

    data class InfoTileCtaClick(val infoTile: InfoTileData) : SavedTripUiEvent

    data class InfoTileExpand(val key: String) : SavedTripUiEvent

    // JSON-based events for navigation compatibility
    data class FromStopChanged(val fromJson: String) : SavedTripUiEvent
    data class ToStopChanged(val toJson: String) : SavedTripUiEvent

    /**
     * A departure or arrival time understood before a timetable was opened. `null` clears it
     * back to now, which is what starting a fresh search means.
     */
    data class DateTimeSelectionChanged(val dateTimeJson: String?) : SavedTripUiEvent

    data object StopTracking : SavedTripUiEvent

    data class MoveSavedTripToIndex(val tripId: String, val targetIndex: Int) : SavedTripUiEvent

    data object MarkReorderTipSeen : SavedTripUiEvent

    /** A Home or Work chip was tapped. */
    data class LabelChipClick(val labelKey: String) : SavedTripUiEvent

    /** The one location fix a set chip's tap asked for. Null coordinates mean there was no fix. */
    data class LabelChipLocated(
        val labelKey: String,
        val latitude: Double?,
        val longitude: Double?,
    ) : SavedTripUiEvent

    /** The screen has acted on the decision and must not act on it again. */
    data object LabelChipTapHandled : SavedTripUiEvent

    /** Stop search returned the stop the rider chose for a label they are setting from home. */
    data class LabelStopAssigned(
        val labelKey: String,
        val stopId: String,
        val stopName: String,
    ) : SavedTripUiEvent
}
