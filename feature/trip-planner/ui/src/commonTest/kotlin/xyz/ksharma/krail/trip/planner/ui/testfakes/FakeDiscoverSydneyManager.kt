package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.discover.network.api.DiscoverSydneyManager
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel

/**
 * Canned cards plus a call record. All seen/reset bookkeeping in production lives behind this
 * interface, so the fake holds no logic of its own — `DiscoverViewModel` is the real class under
 * test.
 */
internal class FakeDiscoverSydneyManager : DiscoverSydneyManager {

    var cards: List<DiscoverModel> = emptyList()

    val seenCardIds = mutableListOf<String>()

    var resetCallCount = 0
        private set

    override suspend fun fetchDiscoverData(): List<DiscoverModel> = cards

    override suspend fun markCardAsSeen(cardId: String) {
        seenCardIds += cardId
    }

    override suspend fun resetAllDiscoverCardsDebugOnly() {
        resetCallCount++
    }
}
