package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.sandook.DiscoverCardSeenPreferences

class FakeDiscoverCardSeenPreferences : DiscoverCardSeenPreferences {
    private val seenCards = mutableSetOf<String>()

    override fun insertCardSeen(cardId: String) {
        seenCards.add(cardId)
    }

    override fun deleteCardSeenById(cardId: String) {
        seenCards.remove(cardId)
    }

    override fun deleteAllCardSeen() {
        seenCards.clear()
    }

    override fun selectAllCardSeen(): List<String> {
        return seenCards.toList()
    }

    // Test helper methods
    fun markAsSeen(vararg cardIds: String) {
        cardIds.forEach { seenCards.add(it) }
    }

    fun clear() {
        seenCards.clear()
    }
}
