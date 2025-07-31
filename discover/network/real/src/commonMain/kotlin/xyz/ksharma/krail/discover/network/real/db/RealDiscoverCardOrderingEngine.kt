package xyz.ksharma.krail.discover.network.real.db

import xyz.ksharma.krail.discover.network.api.db.DiscoverCardOrderingEngine
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel

class RealDiscoverCardOrderingEngine(
) : DiscoverCardOrderingEngine {

    override suspend fun getSortedCards(cards: List<DiscoverModel>): List<DiscoverModel> {
        val seenIds = emptyList<String>() //db.getSeenCardIds()
        val (seen, unseen) = cards.partition { it.cardId in seenIds }
        return unseen + seen
    }

    override suspend fun markCardSeen(cardId: String) {
    }

    override suspend fun resetSeenCards() {
    }
}