package xyz.ksharma.krail.sandook

interface DiscoverCardSeenPreferences {

    // region Card Seen
    fun insertCardSeen(cardId: String)

    fun deleteCardSeenById(cardId: String)

    fun deleteAllCardSeen()

    fun selectAllCardSeen(): List<String>

    // endregion
}
