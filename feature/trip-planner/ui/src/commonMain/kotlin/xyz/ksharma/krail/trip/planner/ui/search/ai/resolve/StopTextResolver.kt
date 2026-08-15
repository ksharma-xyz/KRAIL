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
// Exploratory, not scheduled: offering two or three candidates with the reason each was
// suggested, rather than committing to one, for a match that is not clearly right.
//
// This used to say the groundwork was exposing FuzzyStopRanker's score, which it discards at
// this boundary. That was wrong on three counts and the note is kept so it is not proposed
// again from the same reasoning. See AI_SEARCH_UX.md for the long version.
interface StopTextResolver {

    /** Stable, human-readable id used in logs to show which capability answered. */
    val name: String

    suspend fun resolve(query: String): StopItem?
}
