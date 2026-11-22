package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import xyz.ksharma.krail.info.tile.state.InfoTileData

sealed interface SavedTripUiEvent {

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

    /**
     * Directions to a car park were tapped. Carries the car park's own position, which the tile only
     * offers when it has one, and its name for the maps app to show.
     */
    data class ParkRideDirectionsClick(
        val latitude: Double,
        val longitude: Double,
        val label: String,
    ) : SavedTripUiEvent

    /**
     * Remove from home was tapped on a saved station's card on the map. Only the car parks the rider added
     * themselves are removed, so a station a saved trip holds stays, as it does on the Park & Ride picker.
     */
    data class RemoveParkRideStation(val parkRideState: ParkRideUiState) : SavedTripUiEvent

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

    /**
     * A long press on a card began reorder mode, with that card lifted: a trip's id, or
     * [PARK_RIDE_ROW_KEY] for the Park & Ride row. Nothing else begins it.
     */
    data class EnterReorder(val liftedKey: String) : SavedTripUiEvent

    /** A card was dragged to where another is. Held in the session and written by nothing yet. */
    data class ReorderMove(val fromKey: String, val toKey: String) : SavedTripUiEvent

    /** A trip's delete circle was tapped in reorder mode. Staged, and only deleted by [ReorderDone]. */
    data class ReorderRemove(val tripId: String) : SavedTripUiEvent

    /** Done: the one write of the new order, the trips taken off and the row's place. */
    data object ReorderDone : SavedTripUiEvent

    /** Back, a swipe back or a tap on the map: the session is dropped and nothing is written. */
    data object ReorderCancel : SavedTripUiEvent

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
