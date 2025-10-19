package xyz.ksharma.krail.discover.network.real.db

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.discover.network.api.db.DiscoverCardOrderingEngine
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.sandook.DiscoverCardSeenPreferences
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RealDiscoverCardOrderingEngine(
    private val discoverCardPreferences: DiscoverCardSeenPreferences,
) : DiscoverCardOrderingEngine {

    @OptIn(ExperimentalTime::class)
    private val todayDate by lazy {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getSortedCards(cards: List<DiscoverModel>): List<DiscoverModel> {
        log("Sorting ${cards.size} discover cards")
        val seenCardIds = discoverCardPreferences.selectAllCardSeen().toSet()

        val activeCards = filterActiveCards(cards, todayDate)
        val sortedCards = sortCardsByPriority(activeCards, seenCardIds, todayDate)

        logSortingResults(cards.size, activeCards.size, sortedCards, seenCardIds)
        return sortedCards
    }

    override suspend fun markCardAsSeen(cardId: String) {
        log("$cardId card marked as seen")
        discoverCardPreferences.insertCardSeen(cardId)
    }

    override suspend fun resetAllSeenCards() {
        log("Resetting all seen cards")
        discoverCardPreferences.deleteAllCardSeen()
    }

    /**
     * Filters out cards that are past events based on their start and end dates.
     * An active card is defined as one that has not yet ended
     * (i.e., its end date is today or in the future).
     * If both start and end dates are null, then also card is considered active.
     */
    private fun filterActiveCards(cards: List<DiscoverModel>, today: LocalDate): List<DiscoverModel> {
        return cards.filterNot { it.isPastEvent(today) }
    }

    /**
     * Sorts the cards based on the following priority:
     * 1. Unseen today events (where startDate == endDate == today) at the top.
     * 2. All other unseen cards.
     * 3. Seen today events.
     * 4. All other seen cards.
     */
    private fun sortCardsByPriority(
        cards: List<DiscoverModel>,
        seenCardIds: Set<String>,
        today: LocalDate,
    ): List<DiscoverModel> {
        val (todayEvents, otherCards) = cards.partition { it.isTodayEvent(today) }

        val unseenTodayEvents = todayEvents.filterNot { it.cardId in seenCardIds }
        val seenTodayEvents = todayEvents.filter { it.cardId in seenCardIds }
        val (seenOthers, unseenOthers) = otherCards.partition { it.cardId in seenCardIds }

        return unseenTodayEvents + unseenOthers + seenTodayEvents + seenOthers
    }

    private fun logSortingResults(
        originalSize: Int,
        activeSize: Int,
        sortedCards: List<DiscoverModel>,
        seenCardIds: Set<String>,
    ) {
        log("Filtered out ${originalSize - activeSize} past events")

        sortedCards.forEach { card ->
            val isSeen = card.cardId in seenCardIds
            val isToday = card.isTodayEvent(todayDate)
            log("\tCard: ${card.cardId.take(6)}, Seen: $isSeen, Title: ${card.title.take(10)}, Today: $isToday,")
        }
    }

    private fun DiscoverModel.toLocalStartDate(): LocalDate? {
        return startDate?.parseToLocalDateOrNull()
    }

    private fun DiscoverModel.toLocalEndDate(): LocalDate? {
        return endDate?.parseToLocalDateOrNull()
    }

    /**
     * Parses a date string in ISO 8601 format to a LocalDate.
     * Returns null if the parsing fails.
     */
    private fun String.parseToLocalDateOrNull(): LocalDate? {
        return try {
            LocalDate.parse(this)
        } catch (e: Exception) {
            logError("Failed to parse date: $this", e)
            null
        }
    }

    private fun DiscoverModel.isTodayEvent(today: LocalDate): Boolean {
        val start = toLocalStartDate()
        val end = toLocalEndDate()
        return start == today && (end == null || end == start)
    }

    private fun DiscoverModel.isPastEvent(today: LocalDate): Boolean {
        val end = toLocalEndDate()
        val start = toLocalStartDate()
        return when {
            end != null -> end < today
            start != null -> start < today
            else -> false
        }
    }
}
