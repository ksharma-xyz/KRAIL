package xyz.ksharma.krail.discover.network.api.db

import xyz.ksharma.krail.discover.network.api.model.DiscoverModel

interface DiscoverCardOrderingEngine {
    suspend fun getSortedCards(cards: List<DiscoverModel>): List<DiscoverModel>

    suspend fun markCardAsSeen(cardId: String)

    suspend fun resetSeenCards()
}
