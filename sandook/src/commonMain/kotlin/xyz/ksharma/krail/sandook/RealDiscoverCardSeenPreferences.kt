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
}
