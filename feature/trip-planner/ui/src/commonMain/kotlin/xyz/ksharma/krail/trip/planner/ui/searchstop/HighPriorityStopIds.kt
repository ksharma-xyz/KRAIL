package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remoteconfig.JsonConfig
import xyz.ksharma.krail.core.remoteconfig.RemoteConfigDefaults
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue

private const val STOP_IDS_KEY = "stop_ids"

/**
 * Reads the high priority stop ids out of the `high_priority_stop_ids` flag.
 *
 * The flag arrives as [FlagValue.JsonValue] in two shapes, and both are valid:
 *  - an object, `{"stop_ids": ["200060", ...]}`, which is what the server publishes, and
 *  - a bare array, `["200060", ...]`, which is the in-app default that Remote Config serves
 *    until its first successful fetch (offline first launch, failed fetch).
 *
 * Anything else falls back to the in-app default list: a value that is not JSON, a JSON
 * shape that is neither of the two above, or a flag that is not a [FlagValue.JsonValue] at all.
 * Reading this flag must never throw, because typed stop search reads it on every query.
 */
internal fun FlagValue.toHighPriorityStopIds(): List<String> {
    val parsed = (this as? FlagValue.JsonValue)?.let { parseHighPriorityStopIds(it.value) }
    return parsed ?: defaultHighPriorityStopIds()
}

/**
 * Parses either accepted shape of the flag (see [toHighPriorityStopIds]).
 *
 * Returns `null` when [json] is malformed or has an unexpected shape, so the caller can choose
 * the fallback. An object without a `stop_ids` key is a valid, empty list.
 */
internal fun parseHighPriorityStopIds(json: String): List<String>? {
    // SerializationException is an IllegalArgumentException, and the parser documents both.
    val element = try {
        JsonConfig.lenient.parseToJsonElement(json)
    } catch (e: IllegalArgumentException) {
        log("high_priority_stop_ids is not valid JSON: ${e.message}")
        null
    }
    val ids: JsonArray? = when (element) {
        is JsonArray -> element
        is JsonObject -> element.stopIdsOrNull()
        else -> null
    }
    return ids?.mapNotNull { it.asStopIdOrNull() }
}

private fun JsonObject.stopIdsOrNull(): JsonArray? = when (val ids = this[STOP_IDS_KEY]) {
    null -> JsonArray(emptyList())
    is JsonArray -> ids
    else -> null
}

private fun JsonElement.asStopIdOrNull(): String? =
    (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

private fun defaultHighPriorityStopIds(): List<String> {
    val defaultJson = RemoteConfigDefaults.getDefaults()
        .firstOrNull { it.first == FlagKeys.HIGH_PRIORITY_STOP_IDS.key }
        ?.second as? String
    return defaultJson?.let(::parseHighPriorityStopIds).orEmpty()
}
