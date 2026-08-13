package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Runs [resolvers] in order and takes the first answer.
 *
 * Order is the whole design: it is where precedence between capabilities is declared, in one
 * readable list, instead of being buried in conditionals. Callers build the chain most
 * specific first, so a rider's own words for a place beat a generic search over every stop in
 * the state.
 */
class ChainedStopTextResolver(
    private val resolvers: List<StopTextResolver>,
) : StopTextResolver {

    override val name: String = "chain(${resolvers.joinToString(",") { it.name }})"

    override suspend fun resolve(query: String): StopItem? {
        for (resolver in resolvers) {
            val resolved = resolver.resolve(query)
            if (resolved != null) {
                log("StopTextResolver: '${resolver.name}' resolved '$query' -> ${resolved.stopName}")
                return resolved
            }
        }
        log("StopTextResolver: nothing resolved '$query'")
        return null
    }
}
