package xyz.ksharma.krail.discover.network.api.db

import xyz.ksharma.krail.discover.network.api.model.DiscoverModel

interface DiscoverCardOrderingEngine {

    /**
     * Sorts the Discover cards based on the following logic:
     * 1. Filter out past events (where end date or start date < today)
     * 2. Filter out seen today events (today single-day events that have been marked as seen)
     * 3. Priority order:
     *    - Unseen today events (startDate == endDate == today) at the top
     *    - All other unseen cards
     *    - All other seen cards
     */
    suspend fun getSortedCards(cards: List<DiscoverModel>): List<DiscoverModel>

    suspend fun markCardAsSeen(cardId: String)

    suspend fun resetAllSeenCards()
}
