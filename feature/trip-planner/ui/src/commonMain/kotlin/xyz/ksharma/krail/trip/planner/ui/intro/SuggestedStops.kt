package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.serialization.Serializable
import xyz.ksharma.krail.core.remoteconfig.JsonConfig

/** A stop the Intro offers as a start or an end. [name] is the stop's own name in the stops table. */
@Serializable
internal data class SuggestedStop(val stopId: String, val name: String)

@Serializable
internal data class SuggestedStops(val start: List<SuggestedStop>, val end: List<SuggestedStop>)

/**
 * Parses the Remote Config value, `{"start":[{"stopId":"..","name":".."}],"end":[..]}`. Fields the app
 * does not know are ignored.
 *
 * Returns [fallback] as a whole, never a repaired half, when the text is not that shape, either list
 * is empty, a stop id repeats within a list, a stop id or name is blank, or picking one stop would
 * leave the other list with nothing different to pick. The same stop on both lists is normal.
 */
internal fun parseSuggestedStops(json: String, fallback: SuggestedStops): SuggestedStops {
    val parsed = runCatching {
        JsonConfig.lenient.decodeFromString(SuggestedStops.serializer(), json)
    }.getOrNull()
    return parsed?.takeIf { it.isUsable() } ?: fallback
}

private fun SuggestedStops.isUsable(): Boolean =
    start.isNotEmpty() && end.isNotEmpty() &&
        start.isWellFormed() && end.isWellFormed() &&
        start.none { first -> end.all { it.stopId == first.stopId } } &&
        end.none { last -> start.all { it.stopId == last.stopId } }

private fun List<SuggestedStop>.isWellFormed(): Boolean =
    all { it.stopId.isNotBlank() && it.name.isNotBlank() } &&
        map { it.stopId }.distinct().size == size

/**
 * What the Intro offers until a Remote Config value replaces it. Every id is the station level
 * (parent) stop in the bundled stops data, and every name is that stop's own name there.
 * `DefaultSuggestedStopsDataTest` reads the bundled file, so a data refresh that changes one of
 * these fails CI and names this constant.
 */
internal val DEFAULT_SUGGESTED_STOPS = SuggestedStops(
    start = listOf(
        SuggestedStop(stopId = "2155384", name = "Tallawong Station"),
        SuggestedStop(stopId = "200060", name = "Central Station"),
        SuggestedStop(stopId = "276220", name = "Schofields Station"),
        SuggestedStop(stopId = "215020", name = "Parramatta Station"),
        SuggestedStop(stopId = "200070", name = "Town Hall Station"),
    ),
    end = listOf(
        SuggestedStop(stopId = "200060", name = "Central Station"),
        SuggestedStop(stopId = "200080", name = "Wynyard Station"),
        SuggestedStop(stopId = "215020", name = "Parramatta Station"),
        SuggestedStop(stopId = "200070", name = "Town Hall Station"),
        SuggestedStop(stopId = "200030", name = "Martin Place Station"),
    ),
)
