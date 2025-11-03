package xyz.ksharma.krail.trip.planner.ui.state.timetable

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
@Stable
data class Trip(
    val fromStopId: String,
    val fromStopName: String,
    val toStopId: String,
    val toStopName: String,
    val isFromStopValid: Boolean = true,
    val isToStopValid: Boolean = true,
) {
    val tripId: String
        get() = "$fromStopId$toStopId"

    val isValidTrip: Boolean
        get() = isFromStopValid && isToStopValid

    fun toJsonString() = Json.encodeToString(serializer(), this)

    companion object {
        fun fromJsonString(json: String) =
            kotlin.runCatching { Json.decodeFromString(serializer(), json) }.getOrNull()
    }
}
