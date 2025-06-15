package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

/**
 * Represents the overall UI state for the list of Park & Ride stops.
 */
@Serializable
data class ParkRideUiState(
    val stopId: String,
    val stopName: String,
    val facilities: ImmutableList<ParkRideFacilityDetail> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {

    @Stable
    @Serializable
    data class ParkRideFacilityDetail(
        val spotsAvailable: Int,
        val totalSpots: Int,
        val facilityName: String,
        val percentageFull: Int,
        val stopId: String,
        val timeText: String,
        // TODO - add location details.
    )
}
