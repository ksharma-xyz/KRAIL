package xyz.ksharma.krail.trip.planner.ui.state.timetable

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.trip.planner.ui.state.parkride.ParkRideState

@Serializable
@Stable
data class Trip(
    val fromStopId: String,
    val fromStopName: String,
    val toStopId: String,
    val toStopName: String,
    val parkRideUiState: ParkRideUiState = ParkRideUiState.NotAvailable,
) {
    val tripId: String
        get() = "$fromStopId$toStopId"

    fun toJsonString() = Json.encodeToString(serializer(), this)

    companion object {
        fun fromJsonString(json: String) =
            kotlin.runCatching { Json.decodeFromString(serializer(), json) }.getOrNull()
    }
}

@Serializable
sealed class ParkRideUiState {

    /**
     * Park&Ride is not available for the current trip.
     */
    @Serializable
    data object NotAvailable : ParkRideUiState()

    /**
     * Park&Ride is available, but not loaded from api yet.
     */
    @Serializable
    data object Available : ParkRideUiState() // Park&Ride available, but not loaded

    /**
     * Park&Ride data is being loaded from the api.
     */
    @Serializable
    data object Loading : ParkRideUiState() // Loading Park&Ride data

    /**
     * Park&Ride data is loaded successfully.
     *
     * @param parkRideList List of Park&Ride facilities available for the trip.
     */
    @Serializable
    data class Loaded(
        val parkRideList: ImmutableList<ParkRideState>
    ) : ParkRideUiState()

    /**
     * Park&Ride data failed to load.
     *
     * @param message Error message describing the failure.
     */
    @Serializable
    data class Error(val message: String) : ParkRideUiState()
}
