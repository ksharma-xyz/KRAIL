package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * One way of turning a scrap of extracted text ("work", "central", "the airport") into a
 * real [StopItem].
 *
 * Each resolver is a single capability with a single question to answer, which is what makes
 * them testable in isolation and cheap to add: a new kind of smartness (a rider's saved trips,
 * recent searches, an address lookup) is a new implementation rather than another branch in
 * one growing function.
 *
 * Contract:
 * - Return `null` for "not mine to answer", never a guess. The chain relies on `null` to fall
 *   through to the next capability, so a resolver that returns something weak swallows the
 *   turn that a better-informed one would have handled correctly.
 * - Be side-effect free. Resolvers run on whatever the AI extracted, which may be nonsense.
 */
// Exploratory, not scheduled: a weak match is the only failure here with no visible failure,
// and the honest answer is to offer two or three candidates with the reason each was suggested
// rather than commit to one. That needs a scored result rather than a StopItem? — FuzzyStopRanker
// already computes the score and discards it at this boundary. See AI_SEARCH_UX.md.
interface StopTextResolver {

    /** Stable, human-readable id used in logs to show which capability answered. */
    val name: String

    suspend fun resolve(query: String): StopItem?
}
