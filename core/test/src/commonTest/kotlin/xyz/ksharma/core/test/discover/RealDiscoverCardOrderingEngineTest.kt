package xyz.ksharma.core.test.discover

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.core.test.fakes.FakeDiscoverCardSeenPreferences
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.discover.network.real.db.RealDiscoverCardOrderingEngine
import xyz.ksharma.krail.discover.state.DiscoverCardType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)

class RealDiscoverCardOrderingEngineTest {

    private lateinit var fakePreferences: FakeDiscoverCardSeenPreferences
    private lateinit var orderingEngine: RealDiscoverCardOrderingEngine

    private val today: LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val yesterday = today.minus(1, DateTimeUnit.DAY)
    private val tomorrow = today.plus(1, DateTimeUnit.DAY)
    private val nextWeek = today.plus(7, DateTimeUnit.DAY)
    private val farFuture = today.plus(30, DateTimeUnit.DAY)

    @BeforeTest
    fun setup() {
        fakePreferences = FakeDiscoverCardSeenPreferences()
        orderingEngine = RealDiscoverCardOrderingEngine(fakePreferences)
    }

    @Test
    fun `getSortedCards - today single day events appear first`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Today Event", today.toString(), today.toString()),
            createCard("card3", "Another Regular", null)
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card2", result[0].cardId) // Today event first
        assertEquals("card1", result[1].cardId) // Regular unseen second
        assertEquals("card3", result[2].cardId) // Regular unseen third
    }

    @Test
    fun `getSortedCards - today event with no end date gets priority`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Today Event No End", today.toString(), null),
            createCard("card3", "Tomorrow Event", tomorrow.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card2", result[0].cardId) // Today event first
        assertEquals("card1", result[1].cardId) // Regular unseen second
        assertEquals("card3", result[2].cardId) // Future unseen third
    }

    @Test
    fun `getSortedCards - today multi-day event does not get priority`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Today Multi-day", today.toString(), tomorrow.toString()),
            createCard("card3", "Another Regular", null)
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card1", result[0].cardId) // Regular unseen first
        assertEquals("card2", result[1].cardId) // Multi-day unseen second
        assertEquals("card3", result[2].cardId) // Regular unseen third
    }

    @Test
    fun `getSortedCards - seen today events appear at top of seen cards`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Today Seen Event", today.toString(), today.toString()),
            createCard("card3", "Today Unseen Event", today.toString(), today.toString()),
            createCard("card4", "Another Regular", null)
        )

        fakePreferences.markAsSeen("card2") // Mark today event as seen

        val result = orderingEngine.getSortedCards(cards)

        assertEquals(4, result.size) // Seen today event now included
        assertEquals("card3", result[0].cardId) // Unseen today event first
        assertEquals("card1", result[1].cardId) // Regular unseen second
        assertEquals("card4", result[2].cardId) // Regular unseen third
        assertEquals("card2", result[3].cardId) // Seen today event at top of seen cards
    }

    @Test
    fun `getSortedCards - past events are filtered out`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Yesterday Event", yesterday.toString()),
            createCard("card3", "Past Event with End", yesterday.toString(), yesterday.toString()),
            createCard("card4", "Future Event", tomorrow.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals(2, result.size) // Past events filtered out
        assertEquals("card1", result[0].cardId) // Regular unseen first
        assertEquals("card4", result[1].cardId) // Future unseen second
    }

    @Test
    fun `getSortedCards - event ending yesterday is filtered out`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard(
                "card2",
                "Event Ended Yesterday",
                yesterday.minus(2, DateTimeUnit.DAY).toString(),
                yesterday.toString()
            ),
            createCard("card3", "Current Event", yesterday.toString(), tomorrow.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals(2, result.size) // Event ending yesterday filtered out
        assertEquals("card1", result[0].cardId) // Regular unseen first
        assertEquals("card3", result[1].cardId) // Current event second
    }

    @Test
    fun `getSortedCards - maintains unseen then seen order for non-today events`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Unseen", null),
            createCard("card2", "Future Seen", tomorrow.toString()),
            createCard("card3", "Regular Seen", null),
            createCard("card4", "Future Unseen", nextWeek.toString())
        )

        fakePreferences.markAsSeen("card2", "card3")

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card1", result[0].cardId) // Regular unseen first
        assertEquals("card4", result[1].cardId) // Future unseen second
        assertEquals("card2", result[2].cardId) // Future seen third
        assertEquals("card3", result[3].cardId) // Regular seen fourth
    }

    @Test
    fun `getSortedCards - today seen event appears at top of seen cards`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Today Seen", today.toString(), today.toString()),
            createCard("card3", "Another Regular", null)
        )

        fakePreferences.markAsSeen("card2")

        val result = orderingEngine.getSortedCards(cards)

        assertEquals(3, result.size) // Seen today event now included
        assertEquals("card1", result[0].cardId) // Regular unseen first
        assertEquals("card3", result[1].cardId) // Regular unseen second
        assertEquals("card2", result[2].cardId) // Seen today event at top of seen cards
    }

    @Test
    fun `getSortedCards - complex scenario with today events and filtering`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Unseen", null),
            createCard("card2", "Today Single Day", today.toString(), today.toString()),
            createCard("card3", "Past Event", yesterday.toString()),
            createCard("card4", "Today Multi Day", today.toString(), tomorrow.toString()),
            createCard("card5", "Regular Seen", null),
            createCard("card6", "Today Single Seen", today.toString(), today.toString())
        )

        fakePreferences.markAsSeen("card5", "card6")

        val result = orderingEngine.getSortedCards(cards)

        assertEquals(5, result.size) // Only past event filtered out, seen today event now included
        assertEquals("card2", result[0].cardId) // Today single unseen first
        assertEquals("card1", result[1].cardId) // Regular unseen second
        assertEquals("card4", result[2].cardId) // Today multi-day unseen third
        assertEquals("card6", result[3].cardId) // Seen today event at top of seen cards
        assertEquals("card5", result[4].cardId) // Regular seen fifth
        // Only card3 (past event) should not appear
    }

    @Test
    fun `getSortedCards - invalid dates handled gracefully`() = runTest {
        val cards = listOf(
            createCard("card1", "Valid Today", today.toString()),
            createCard("card2", "Invalid Date", "invalid-date"),
            createCard("card3", "Another Valid", tomorrow.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals(3, result.size) // No crashes, all cards included
        assertEquals("card1", result[0].cardId) // Valid unseen first
        assertEquals("card2", result[1].cardId) // Invalid treated as regular
        assertEquals("card3", result[2].cardId) // Future unseen third
    }

    @Test
    fun `getSortedCards - null dates are handled`() = runTest {
        val cards = listOf(
            createCard("card1", "No Date", null),
            createCard("card2", "Today Event", today.toString(), today.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card2", result[0].cardId) // Today first
        assertEquals("card1", result[1].cardId) // No date second
    }

    @Test
    fun `getSortedCards - empty list returns empty`() = runTest {
        val result = orderingEngine.getSortedCards(emptyList())
        assertEquals(0, result.size)
    }

    @Test
    fun `markCardAsSeen - adds card to seen list`() = runTest {
        orderingEngine.markCardAsSeen("test-card")

        val seenCards = fakePreferences.selectAllCardSeen()
        assertEquals(listOf("test-card"), seenCards)
    }

    @Test
    fun `resetAllSeenCards - clears all seen cards`() = runTest {
        fakePreferences.markAsSeen("card1", "card2", "card3")

        orderingEngine.resetAllSeenCards()

        val seenCards = fakePreferences.selectAllCardSeen()
        assertEquals(emptyList(), seenCards)
    }

    private fun createCard(
        cardId: String,
        title: String,
        startDate: String?,
        endDate: String? = null
    ): DiscoverModel {
        return DiscoverModel(
            title = title,
            description = "Test description",
            startDate = startDate,
            endDate = endDate,
            disclaimer = null,
            imageList = listOf("https://example.com/image.jpg"),
            buttons = null,
            type = DiscoverCardType.Events,
            cardId = cardId
        )
    }
}
