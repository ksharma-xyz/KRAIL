package xyz.ksharma.krail.sandook

interface DiscoverCardSeenPreferences {

    // region Card Seen
    fun insertCardSeen(cardId: String)

    fun deleteCardSeenById(cardId: String)

    fun deleteAllCardSeen()

    fun selectAllCardSeen(): List<String>

    // endregion

    // region Card Feedback
    fun insertCardFeedback(cardId: String, isPositive: Boolean, isCompleted: Boolean = false)

    fun selectCardFeedback(cardId: String): CardFeedback?

    fun selectAllCardFeedback(): List<CardFeedback>

    fun deleteCardFeedback(cardId: String)

    fun deleteAllCardFeedback()

    fun hasCardFeedback(cardId: String): Boolean

    fun updateCardFeedbackCompletion(cardId: String, isCompleted: Boolean)

    // endregion
}

data class CardFeedback(
    val cardId: String,
    val isPositive: Boolean,
    val timestamp: Long,
    val isCompleted: Boolean,
)
