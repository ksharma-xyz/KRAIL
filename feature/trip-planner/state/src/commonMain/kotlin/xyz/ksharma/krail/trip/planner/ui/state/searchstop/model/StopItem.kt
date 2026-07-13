package xyz.ksharma.krail.trip.planner.ui.state.searchstop.model

import kotlinx.serialization.json.Json

/**
 * Represents a Stop item in the search results when searching for stops.
 * Need to be Serializable because it is passed as a parameter during navigation.
 */
@kotlinx.serialization.Serializable
data class StopItem(
    val stopName: String,
    val stopId: String,
    val locationKind: LocationKind = LocationKind.TRANSIT_STOP,
    val addressType: String? = null,
) {
    fun toJsonString() = Json.encodeToString(serializer(), this)

    @Suppress("ConstPropertyName")
    companion object {
        private const val serialVersionUID: Long = 1L

        fun fromJsonString(json: String) =
            kotlin.runCatching { Json.decodeFromString(serializer(), json) }.getOrNull()
    }
}

enum class LocationKind { TRANSIT_STOP, ADDRESS }
