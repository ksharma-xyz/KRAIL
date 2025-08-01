package xyz.ksharma.krail.discover.network.api

import xyz.ksharma.krail.discover.network.api.model.DiscoverModel

interface DiscoverSydneyManager {

    /**
     * Returns a list of cards to be displayed in the Discover screen.
     */
    suspend fun fetchDiscoverData(): List<DiscoverModel>

    fun feedbackThumbButtonClicked(feedbackId: String, isPositive: Boolean)

    suspend fun markCardAsSeen(cardId: String)

    suspend fun resetAllSeenCards()
}
