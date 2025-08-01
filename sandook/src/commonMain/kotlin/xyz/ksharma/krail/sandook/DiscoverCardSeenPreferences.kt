package xyz.ksharma.krail.sandook

interface DiscoverCardSeenPreferences {

    fun insertCardSeen(cardId: String)

    fun deleteCardSeenById(cardId: String)

    fun deleteAllCardSeen()

    fun selectAllCardSeen(): List<String>
}
