package xyz.ksharma.krail.sandook

internal class RealDiscoverCardSeenPreferences(
    private val discoverCardQueries: DiscoverCardQueries,
) : DiscoverCardSeenPreferences {

    override fun insertCardSeen(cardId: String) {
        discoverCardQueries.insertCardSeen(cardId)
    }

    override fun deleteCardSeenById(cardId: String) {
        discoverCardQueries.deleteCardSeenById(cardId)
    }

    override fun deleteAllCardSeen() {
        discoverCardQueries.deleteAllCardSeen()
    }

    override fun selectAllCardSeen(): List<String> {
        return discoverCardQueries.selectAllCardSeen().executeAsList()
    }


    // New feedback methods
    override fun insertCardFeedback(cardId: String, isPositive: Boolean) {
        discoverCardQueries.insertCardFeedback(
            cardId = cardId,
            isPositive = if (isPositive) 1L else 0L
        )
    }

    override fun selectCardFeedback(cardId: String): CardFeedback? {
        return discoverCardQueries.selectCardFeedback(cardId).executeAsOneOrNull()?.let {
            CardFeedback(
                cardId = it.cardId,
                isPositive = it.isPositive == 1L,
                timestamp = it.timestamp
            )
        }
    }

    override fun selectAllCardFeedback(): List<CardFeedback> {
        return discoverCardQueries.selectAllCardFeedback().executeAsList().map {
            CardFeedback(
                cardId = it.cardId,
                isPositive = it.isPositive == 1L,
                timestamp = it.timestamp
            )
        }
    }

    override fun deleteCardFeedback(cardId: String) {
        discoverCardQueries.deleteCardFeedback(cardId)
    }

    override fun deleteAllCardFeedback() {
        discoverCardQueries.deleteAllCardFeedback()
    }

    override fun hasCardFeedback(cardId: String): Boolean {
        return selectCardFeedback(cardId) != null
    }
}
