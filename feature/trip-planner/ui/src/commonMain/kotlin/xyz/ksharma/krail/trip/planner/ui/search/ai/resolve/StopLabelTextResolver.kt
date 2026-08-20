package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import kotlinx.coroutines.flow.first
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.db.StopLabels
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
 *
 * Exact does not mean the app's vocabulary only. A rider whose label is `Work` says "office by
 * Monday morning" and means it, so [LabelSynonyms] widens which WORDS count while keeping every
 * comparison a whole-word exact one. The Homebush case is unaffected.
 *
 * Two passes, and the order matters: a rider with both a `Work` and an `Office` label has named
 * two different stops, and "office" has to reach the one they called Office. Only when nothing
 * matches literally does the synonym pass run.
 */
internal class StopLabelTextResolver(
    private val sandook: Sandook,
) : StopTextResolver {

    override val name: String = "label"

    override suspend fun resolve(query: String): StopItem? {
        val normalised = query.normaliseLabel()
        if (normalised.isEmpty()) return null

        val labels = sandook.observeStopLabels().first()

        // A label the rider made but never pointed at a stop resolves to null, same as no
        // match: there is nothing to fill the field with.
        val literal = labels.firstOrNull { it.label.normaliseLabel() == normalised }
        val synonym = literal ?: labels.firstOrNull { LabelSynonyms.sameMeaning(it.label, normalised) }

        return synonym?.toStopItemOrNull()
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
