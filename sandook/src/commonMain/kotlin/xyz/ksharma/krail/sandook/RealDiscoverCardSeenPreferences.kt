package xyz.ksharma.krail.sandook

internal class RealDiscoverCardSeenPreferences(
    private val queries: DiscoverCardQueries,
) : DiscoverCardSeenPreferences {

    override fun insertCardSeen(cardId: String) {
        queries.insertCardSeen(cardId)
    }

    override fun deleteCardSeenById(cardId: String) {
        queries.deleteCardSeenById(cardId)
    }

    override fun deleteAllCardSeen() {
        queries.deleteAllCardSeen()
    }

    override fun selectAllCardSeen(): List<String> {
        return queries.selectAllCardSeen().executeAsList()
    }
}
