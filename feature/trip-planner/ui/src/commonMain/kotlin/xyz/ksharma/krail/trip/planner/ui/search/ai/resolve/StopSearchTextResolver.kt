package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

private const val MIN_QUERY_TOKEN_LENGTH_TO_MATCH_AS_PREFIX = 3
private const val MAX_WORDS_JOINED = 4

/**
 * Resolves a place name against the same stop search the search-stop screen uses, taking the
 * top stop result — but only when that result is a good enough match to fill a field with
 * unattended.
 *
 * This is the general fallback and belongs last in the chain: it can match almost anything, so
 * putting it earlier would let a coincidental stop-name match beat a rider's own label.
 *
 * Route search is off — a place name is never a route — and non-stop results (addresses, POIs,
 * trips) are ignored, so a query with no stop match resolves to `null` rather than to something
 * of the wrong kind.
 */
internal class StopSearchTextResolver(
    private val stopResultsManager: StopResultsManager,
) : StopTextResolver {

    override val name: String = "stopSearch"

    override suspend fun resolve(query: String): StopItem? {
        val results = stopResultsManager.fetchStopResults(query, searchRoutesEnabled = false)
        val topStop = results.filterIsInstance<SearchStopState.SearchResult.Stop>()
            .firstOrNull { it.stopName.matchesOnTokenBoundaries(query) }
            ?: return null
        return StopItem(stopName = topStop.stopName, stopId = topStop.stopId)
    }
}

/**
 * Whether every word of [query] starts a word of this stop name.
 *
 * The stop search is tuned for a rider reading a list of ten results and picking one, where a
 * loose match is a helpful suggestion. This path picks for them, silently, which is a different
 * bet: "work" matching *Powder**work**s Rd* put a stop on the northern beaches into a rider's
 * destination with nothing to show anything had gone wrong.
 *
 * Matching on word starts rather than anywhere in the string is what separates the two:
 * "central" still finds *Central Station*, "st" still finds *Central Street*, and "work" no
 * longer finds *Powderworks*.
 *
 * The cost is deliberate. A misheard or misspelt word ("sentral") no longer resolves, so the
 * field is left empty for the rider to fill. An empty field is a problem they can see; a
 * confident wrong one is a problem they find out about on a platform.
 */
private fun String.matchesOnTokenBoundaries(query: String): Boolean {
    val candidateTokens = tokenise()
    val queryTokens = query.tokenise()
    if (queryTokens.isEmpty()) return false

    val runs = candidateTokens.consecutiveRuns()
    return queryTokens.all { queryToken ->
        runs.any { run ->
            if (queryToken.length >= MIN_QUERY_TOKEN_LENGTH_TO_MATCH_AS_PREFIX) {
                run.startsWith(queryToken)
            } else {
                // One and two letter tokens prefix far too much to be trusted as a prefix.
                run == queryToken
            }
        }
    }
}

/**
 * Each word of the stop name, plus each run of consecutive words joined up.
 *
 * Riders write stop names closed that the timetable writes open: "townhall", "kingscross",
 * "circularquay". Matching word by word rejected all of those, which put this path in the
 * absurd position of refusing a stop the app's own search screen finds instantly, and telling
 * the rider no such stop exists.
 *
 * Joining runs rather than allowing a match anywhere inside the name is what keeps the
 * original guarantee: "townhall" reaches *Town Hall Station* because those words are adjacent
 * and in order, while "work" still cannot reach *70 Powderworks Rd*, because no run of that
 * name begins with it.
 */
private fun List<String>.consecutiveRuns(): List<String> = buildList {
    for (start in this@consecutiveRuns.indices) {
        val last = minOf(start + MAX_WORDS_JOINED, this@consecutiveRuns.size)
        for (end in start + 1..last) {
            add(this@consecutiveRuns.subList(start, end).joinToString(separator = ""))
        }
    }
}

private fun String.tokenise(): List<String> =
    lowercase().split(' ', ',', '-', '/', '(', ')', '.', '\'').filter { it.isNotBlank() }
