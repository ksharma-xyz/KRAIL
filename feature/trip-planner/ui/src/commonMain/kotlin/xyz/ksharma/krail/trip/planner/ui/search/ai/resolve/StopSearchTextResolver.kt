package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Resolves a place name against the same stop search the search-stop screen uses, taking the
 * top stop result as the answer.
 *
 * This is the general fallback and belongs last: it can match almost anything, so putting it
 * earlier would let a coincidental stop-name match beat a rider's own label.
 *
 * Route search is off — a place name is never a route — and non-stop results (addresses, POIs,
 * trips) are ignored, so a query with no stop match resolves to `null` rather than to
 * something of the wrong kind.
 */
internal class StopSearchTextResolver(
    private val stopResultsManager: StopResultsManager,
) : StopTextResolver {

    override val name: String = "stopSearch"

    override suspend fun resolve(query: String): StopItem? {
        val results = stopResultsManager.fetchStopResults(query, searchRoutesEnabled = false)
        val topStop = results.filterIsInstance<SearchStopState.SearchResult.Stop>().firstOrNull() ?: return null
        return StopItem(stopName = topStop.stopName, stopId = topStop.stopId)
    }
}
