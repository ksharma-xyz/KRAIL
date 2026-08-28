package xyz.ksharma.krail.trip.planner.ui.state.alerts

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ServiceAlert(
    val heading: String,
    val message: String,
) {

    /**
     * Stable identity for one alert, unique within any collection of alerts.
     *
     * The feed gives an alert no id, and the heading alone is not unique: NSW sends distinct
     * alerts under one heading, which is how a `LazyColumn` keyed on heading crashed with
     * "Key was already used". Since alerts reach the UI as a `Set` of this data class, the
     * (heading, message) pair is distinct by construction, so it is the identity.
     *
     * Computed rather than stored, so it stays out of the serialized form.
     */
    val alertId: String get() = "$heading|$message"

    fun toJsonString() = Json.encodeToString(serializer(), this)

    @Suppress("ConstPropertyName")
    companion object {
        private const val serialVersionUID: Long = 1L

        fun fromJsonString(json: String) =
            kotlin.runCatching { Json.decodeFromString(serializer(), json) }.getOrNull()
    }
}

@Serializable
enum class ServiceAlertPriority {
    HIGH, MEDIUM, LOW
}
