package xyz.ksharma.krail.discover.network.real.db

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.discover.network.api.db.DiscoverCardOrderingEngine
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.sandook.DiscoverCardQueries
import xyz.ksharma.krail.sandook.DiscoverCardSeenPreferences

internal class RealDiscoverCardOrderingEngine(
    private val discoverCardPreferences: DiscoverCardSeenPreferences,
) : DiscoverCardOrderingEngine {

    override suspend fun getSortedCards(cards: List<DiscoverModel>): List<DiscoverModel> {
        log("Sorting ${cards.size} discover cards")
        val seenCardIdList = discoverCardPreferences.selectAllCardSeen()
        val (seen, unseen) = cards.partition { it.cardId in seenCardIdList }
        log("Seen discover cards: ${seen.size}, Unseen cards: ${unseen.size}")
        return unseen + seen
    }

    override suspend fun markCardAsSeen(cardId: String) {
        log("$cardId card marked as seen")
        discoverCardPreferences.insertCardSeen(cardId)
    }

    override suspend fun resetSeenCards() {
        log("Resetting all seen cards")
        discoverCardPreferences.deleteAllCardSeen()
    }
}
