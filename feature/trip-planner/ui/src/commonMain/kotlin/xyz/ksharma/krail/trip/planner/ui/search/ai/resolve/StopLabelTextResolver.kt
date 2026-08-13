package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import kotlinx.coroutines.flow.first
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.StopLabels
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Resolves a rider's own name for a place: "work", "home", "uni", or any label they made
 * themselves.
 *
 * A label already carries the stop it points at, so a hit needs no search at all. This sits
 * first in the chain because a label is the rider deliberately naming a stop, which is
 * stronger evidence than a text match against every stop in the state. Someone who labels a
 * stop "Central" and then says "to central" means their stop.
 *
 * Matching is deliberately exact (case- and space-insensitive) rather than fuzzy: a label is
 * a short word the rider chose, and loose matching here would let "home" capture "homebush".
 * Anything not an exact label falls through to the next capability.
 */
internal class StopLabelTextResolver(
    private val sandook: Sandook,
) : StopTextResolver {

    override val name: String = "label"

    override suspend fun resolve(query: String): StopItem? {
        val normalised = query.normaliseLabel()
        if (normalised.isEmpty()) return null

        // A label the rider made but never pointed at a stop resolves to null, same as no
        // match: there is nothing to fill the field with.
        return sandook.observeStopLabels().first()
            .firstOrNull { it.label.normaliseLabel() == normalised }
            ?.toStopItemOrNull()
    }
}

/**
 * Labels are entered by hand, so "Work", "work " and "WORK" are the same label to a rider
 * even though they are three different strings.
 */
private fun String.normaliseLabel(): String = trim().lowercase()

private fun StopLabels.toStopItemOrNull(): StopItem? {
    val id = stop_id
    val name = stop_name
    return if (id != null && name != null) StopItem(stopId = id, stopName = name) else null
}
