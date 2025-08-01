package xyz.ksharma.krail.discover.network.real.db

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.discover.network.api.db.DiscoverCardOrderingEngine
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.sandook.DiscoverCardQueries

internal class RealDiscoverCardOrderingEngine(
    private val discoverCardQueries: DiscoverCardQueries,
) : DiscoverCardOrderingEngine {

    override suspend fun getSortedCards(cards: List<DiscoverModel>): List<DiscoverModel> {
        log("Sorting ${cards.size} discover cards")
        val seenCardIdList = discoverCardQueries.selectAllCardSeen().executeAsList()
        val (seen, unseen) = cards.partition { it.cardId in seenCardIdList }
        log("Seen discover cards: ${seen.size}, Unseen cards: ${unseen.size}")
        return unseen + seen
    }

    override suspend fun markCardAsSeen(cardId: String) {
        log("$cardId card marked as seen")
        discoverCardQueries.insertCardSeen(cardId)
    }

    override suspend fun resetSeenCards() {
        log("Resetting all seen cards")
        discoverCardQueries.deleteAllCardSeen()
    }
}
