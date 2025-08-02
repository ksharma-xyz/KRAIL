package xyz.ksharma.krail.sandook

interface DiscoverCardSeenPreferences {

    // region Card Seen
    fun insertCardSeen(cardId: String)

    fun deleteCardSeenById(cardId: String)

    fun deleteAllCardSeen()

    fun selectAllCardSeen(): List<String>

    // endregion

    // region Card Feedback

    fun insertCardFeedback(cardId: String, isPositive: Boolean)

    fun selectCardFeedback(cardId: String): CardFeedback?

    fun selectAllCardFeedback(): List<CardFeedback>

    fun deleteCardFeedback(cardId: String)

    fun deleteAllCardFeedback()

    fun hasCardFeedback(cardId: String): Boolean

    // endregion
}

data class CardFeedback(
    val cardId: String,
    val isPositive: Boolean,
    val timestamp: Long
)
