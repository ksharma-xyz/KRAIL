package xyz.ksharma.krail.discover.network.real.db

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.discover.network.api.db.DiscoverCardOrderingEngine
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.sandook.DiscoverCardSeenPreferences
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// TODO - ADD UT TESTS FOR THIS ENGINE
class RealDiscoverCardOrderingEngine(
    private val discoverCardPreferences: DiscoverCardSeenPreferences,
) : DiscoverCardOrderingEngine {

    /**
     * Sorts the Discover cards based on the following logic:
     * 1. Unseen cards for events in the next week (show on top)
     * 2. Cards for events today (regardless of seen status)
     * 3. All other unseen cards (keep default order)
     */
    @OptIn(ExperimentalTime::class)
    override suspend fun getSortedCards(cards: List<DiscoverModel>): List<DiscoverModel> {
        log("Sorting ${cards.size} discover cards with date logic")
        val seenCardIdList = discoverCardPreferences.selectAllCardSeen()
        val today: LocalDate =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val nextWeekEnd = today.plus(7, DateTimeUnit.DAY)

        fun DiscoverModel.getEventDate(): LocalDate? {
            return startDate?.let { dateString ->
                try {
                    // Parse ISO 8601 date string to LocalDate
                    LocalDate.parse(dateString)
                } catch (e: Exception) {
                    logError("Failed to parse date: $dateString", e)
                    null
                }
            }
        }

        val sortedCards = cards.sortedWith(compareBy<DiscoverModel> { card ->
            val eventDate = card.getEventDate()
            val isSeen = card.cardId in seenCardIdList
            val isToday = eventDate == today
            val isNextWeek = eventDate != null && eventDate > today && eventDate <= nextWeekEnd

            when {
                // Priority 1: Unseen cards for events in next week (show on top)
                isNextWeek && !isSeen -> 0
                // Priority 2: Cards for events today (regardless of seen status)
                isToday -> 1
                // Priority 3: All other unseen cards
                !isSeen -> 2
                // Priority 4: All other seen cards (keep default order)
                else -> 3
            }
        })

        sortedCards.forEach { card ->
            val eventDate = card.getEventDate()
            val isSeen = card.cardId in seenCardIdList
            log(
                "\tCard: ${card.cardId}, Date: $eventDate, Seen: $isSeen, Title: ${
                    card.title.take(6)
                }"
            )
        }

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
}
