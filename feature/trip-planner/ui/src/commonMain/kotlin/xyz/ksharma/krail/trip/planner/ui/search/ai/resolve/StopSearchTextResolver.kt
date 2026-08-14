package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

private const val MIN_QUERY_TOKEN_LENGTH_TO_MATCH_AS_PREFIX = 3

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

    return queryTokens.all { queryToken ->
        candidateTokens.any { candidateToken ->
            if (queryToken.length >= MIN_QUERY_TOKEN_LENGTH_TO_MATCH_AS_PREFIX) {
                candidateToken.startsWith(queryToken)
            } else {
                // One and two letter tokens prefix far too much to be trusted as a prefix.
                candidateToken == queryToken
            }
        }
    }
}

private fun String.tokenise(): List<String> =
    lowercase().split(' ', ',', '-', '/', '(', ')', '.', '\'').filter { it.isNotBlank() }
