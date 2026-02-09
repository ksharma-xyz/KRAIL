package xyz.ksharma.krail.core.maps.state

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Type-safe builder for GeoJSON feature properties.
 * Provides reusable way to create GeoJSON property objects.
 */
class GeoJsonPropertiesBuilder {
    private val properties = mutableMapOf<String, JsonPrimitive>()

    fun property(key: String, value: String) {
        properties[key] = JsonPrimitive(value)
    }

    fun property(key: String, value: Int) {
        properties[key] = JsonPrimitive(value)
    }

    fun property(key: String, value: Long) {
        properties[key] = JsonPrimitive(value)
    }

    fun property(key: String, value: Boolean) {
        properties[key] = JsonPrimitive(value)
    }

    fun property(key: String, value: Double) {
        properties[key] = JsonPrimitive(value)
    }

    fun propertyIfNotNull(key: String, value: String?) {
        value?.let { properties[key] = JsonPrimitive(it) }
    }

    fun propertyIfNotNull(key: String, value: Int?) {
        value?.let { properties[key] = JsonPrimitive(it) }
    }

    fun propertyIfNotNull(key: String, value: Long?) {
        value?.let { properties[key] = JsonPrimitive(it) }
    }

    fun propertyIfNotNull(key: String, value: Boolean?) {
        value?.let { properties[key] = JsonPrimitive(it) }
    }

    fun build(): JsonObject = buildJsonObject {
        properties.forEach { (key, value) ->
            put(key, value)
        }
    }
}

/**
 * DSL function to build GeoJSON properties.
 */
inline fun geoJsonProperties(block: GeoJsonPropertiesBuilder.() -> Unit): JsonObject {
    return GeoJsonPropertiesBuilder().apply(block).build()
}

/**
 * Common GeoJSON property keys as constants to avoid hardcoding strings.
 */
object GeoJsonPropertyKeys {
    const val TYPE = "type"
    const val ID = "id"
    const val NAME = "name"
    const val COLOR = "color"
    const val LINE_ID = "lineId"
    const val STOP_ID = "stopId"
    const val STOP_NAME = "stopName"
    const val STOP_TYPE = "stopType"
    const val LEG_ID = "legId"
    const val MODE_TYPE = "modeType"
    const val IS_WALKING = "isWalking"
    const val LINE_NAME = "lineName"
    const val LINE_NUMBER = "lineNumber"
    const val TIME = "time"
    const val PLATFORM = "platform"
}

/**
 * Common GeoJSON feature type values.
 */
object GeoJsonFeatureTypes {
    const val ROUTE = "route"
    const val STOP = "stop"
    const val JOURNEY_LEG = "journey_leg"
    const val JOURNEY_STOP = "journey_stop"
    const val ROUTE_LABEL = "route_label"
    const val EMPTY = "empty"
}
