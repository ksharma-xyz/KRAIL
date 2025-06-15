package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.Serializable

/**
 * Represents the overall UI state for the list of Park & Ride stops.
 */
@Serializable
@Stable
data class ParkRideUiState(
    val stopId: String,
    val stopName: String,
    val facilities: ImmutableSet<ParkRideFacilityDetail> = persistentSetOf(),
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
        val facilityId: String,
        // TODO - add location details.
    )
}
