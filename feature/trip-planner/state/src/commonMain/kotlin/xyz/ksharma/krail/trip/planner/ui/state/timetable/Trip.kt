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
) {
    /**
     * Stable storage and Compose identity for this ordered stop pair.
     *
     * Keep this format aligned with the Sandook `SavedTrip` constraints and migrations.
     * A separator is required; concatenating opaque stop IDs can create duplicate list keys.
     */
    val tripId: String
        get() = "$fromStopId->$toStopId"

    fun toJsonString() = Json.encodeToString(serializer(), this)

    companion object {
        fun fromJsonString(json: String) =
            kotlin.runCatching { Json.decodeFromString(serializer(), json) }.getOrNull()
    }
}
