package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import xyz.ksharma.krail.trip.planner.ui.state.parkride.ParkRideState

/**
 * Represents the overall UI state for the list of Park & Ride stops.
 */
@Serializable
data class ParkRideStopsUiState(
    val items: ImmutableList<ParkRideStopItem> = persistentListOf(),
)

/**
 * Represents a single item in the Park & Ride list.
 * This can be a stop that is either collapsed or expanded.
 */
@Serializable
data class ParkRideStopItem(
    val stopId: String,
    val stopName: String,
    val expansionState: ExpansionState = ExpansionState.Collapsed,
) {
    /**
     * Represents the state of an expandable stop.
     */
    @Serializable
    sealed interface ExpansionState {
        /**
         * The stop is collapsed, only showing its name.
         */
        @Serializable
        data object Collapsed : ExpansionState

        /**
         * The stop is expanded and its facilities are being loaded.
         */
        @Serializable
        data object Loading : ExpansionState

        /**
         * The stop is expanded and its facilities are successfully loaded.
         *
         * @param facilities List of Park & Ride facilities available for this stop.
         *                   Ensure this list contains unique facilities based on facility ID.
         */
        @Serializable
        data class FacilitiesLoaded(
            val facilities: ImmutableList<ParkRideState>
        ) : ExpansionState

        /**
         * The stop was expanded, but loading its facilities failed.
         *
         * @param message Error message describing the failure.
         */
        @Serializable
        data class ErrorLoadingFacilities(val message: String) : ExpansionState
    }
}
