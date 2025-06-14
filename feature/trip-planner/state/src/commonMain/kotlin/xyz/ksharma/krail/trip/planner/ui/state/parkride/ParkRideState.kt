package xyz.ksharma.krail.trip.planner.ui.state.parkride

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ParkRideState(
    val spotsAvailable: Int,
    val totalSpots: Int,
    val facilityName: String,
    val percentageFull: Int,
    val stopId: String,
    val timeText: String,
    // TODO - add location details.
)
