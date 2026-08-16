package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Stops a rider's word for a place from being matched against stop names.
 *
 * Reported, with no Work label set: "I wanna go to work tomorrow at 9am" filled the destination
 * with **Blacktown Workers Club**. The label resolver found no Work label, the chain moved on,
 * and the stop search happily matched the letters in "Workers".
 *
 * A rider who says "work" has never meant a club whose name contains those letters. These words
 * are their vocabulary for places they name, not fragments to search stop names with, so a label
 * word resolves to a label or to nothing at all.
 *
 * ## Why a wrapper rather than a rule inside the search resolver
 *
 * The stop search is a general capability with other callers, and "work" IS a legitimate thing
 * to type into an ordinary stop search — somebody looking for Blacktown Workers Club has every
 * right to find it by typing part of its name. What is wrong is reaching it through a *label
 * word* in a sentence. That distinction belongs to this chain, not to the search.
 *
 * ## What the rider sees instead
 *
 * Nothing resolves, so the failure quotes their word back. That is the honest outcome, and it is
 * also the hook for offering to set the label they clearly want — see §8 of `ASK_KRAIL_UX.md`.
 */
internal class LabelWordGuard(
    private val delegate: StopTextResolver,
) : StopTextResolver {

    override val name: String = "label-word-guard(${delegate.name})"

    override suspend fun resolve(query: String): StopItem? =
        if (LabelSynonyms.isLabelWord(query)) null else delegate.resolve(query)
}
