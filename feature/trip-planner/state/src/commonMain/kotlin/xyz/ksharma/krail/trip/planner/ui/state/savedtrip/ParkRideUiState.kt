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
    val stopId: String,
    val stopName: String,
    val facilities: ImmutableList<ParkRideState> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
