package xyz.ksharma.core.test.discover

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.discover.network.real.db.RealDiscoverCardOrderingEngine
import xyz.ksharma.krail.discover.state.DiscoverCardType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import xyz.ksharma.core.test.fakes.FakeDiscoverCardSeenPreferences
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class RealDiscoverCardOrderingEngineTest {

    private lateinit var fakePreferences: FakeDiscoverCardSeenPreferences
    private lateinit var orderingEngine: RealDiscoverCardOrderingEngine

    private val today: LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val yesterday = today.minus(1, DateTimeUnit.DAY)
    private val tomorrow = today.plus(1, DateTimeUnit.DAY)
    private val nextWeekDay3 = today.plus(3, DateTimeUnit.DAY)
    private val nextWeekDay7 = today.plus(7, DateTimeUnit.DAY)
    private val nextWeekDay8 = today.plus(8, DateTimeUnit.DAY)
    private val farFuture = today.plus(30, DateTimeUnit.DAY)

    @BeforeTest
    fun setup() {
        fakePreferences = FakeDiscoverCardSeenPreferences()
        orderingEngine = RealDiscoverCardOrderingEngine(fakePreferences)
    }

    @Test
    fun `getSortedCards - unseen next week events appear first`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Next Week Event", nextWeekDay3.toString()),
            createCard("card3", "Another Regular", null)
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card2", result[0].cardId) // Next week unseen first
        assertEquals("card1", result[1].cardId) // Other unseen cards follow
        assertEquals("card3", result[2].cardId)
    }

    @Test
    fun `getSortedCards - today events appear after next week unseen`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Today Event", today.toString()),
            createCard("card3", "Next Week Event", nextWeekDay7.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card3", result[0].cardId) // Next week unseen first
        assertEquals("card2", result[1].cardId) // Today event second
        assertEquals("card1", result[2].cardId) // Regular unseen last
    }

    @Test
    fun `getSortedCards - today seen events still appear in priority position`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Today Event Seen", today.toString()),
            createCard("card3", "Next Week Event", nextWeekDay3.toString())
        )

        fakePreferences.markAsSeen("card2")

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card3", result[0].cardId) // Next week unseen first
        assertEquals("card2", result[1].cardId) // Today seen second (rule 2)
        assertEquals("card1", result[2].cardId) // Regular unseen third
    }

    @Test
    fun `getSortedCards - seen next week events do not get priority`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Unseen", null),
            createCard("card2", "Next Week Seen", nextWeekDay3.toString()),
            createCard("card3", "Regular Unseen 2", null)
        )

        fakePreferences.markAsSeen("card2")

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card1", result[0].cardId) // Regular unseen first
        assertEquals("card3", result[1].cardId) // Regular unseen second
        assertEquals("card2", result[2].cardId) // Seen next week last
    }

    @Test
    fun `getSortedCards - events beyond 7 days do not get priority`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Day 8 Event", nextWeekDay8.toString()),
            createCard("card3", "Far Future Event", farFuture.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card1", result[0].cardId) // All unseen, maintain order
        assertEquals("card2", result[1].cardId)
        assertEquals("card3", result[2].cardId)
    }

    @Test
    fun `getSortedCards - day 7 events get priority if unseen`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Day 7 Event", nextWeekDay7.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card2", result[0].cardId) // Day 7 is within next week
        assertEquals("card1", result[1].cardId)
    }

    @Test
    fun `getSortedCards - yesterday events do not get today priority`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Card", null),
            createCard("card2", "Yesterday Event", yesterday.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card1", result[0].cardId) // Both unseen, maintain order
        assertEquals("card2", result[1].cardId)
    }

    @Test
    fun `getSortedCards - complex scenario with all priorities`() = runTest {
        val cards = listOf(
            createCard("card1", "Regular Unseen", null),
            createCard("card2", "Today Seen", today.toString()),
            createCard("card3", "Next Week Unseen", nextWeekDay3.toString()),
            createCard("card4", "Next Week Seen", nextWeekDay8.toString()),
            createCard("card5", "Today Unseen", today.toString()),
            createCard("card6", "Regular Seen", null),
            createCard("card7", "Far Future", farFuture.toString())
        )

        fakePreferences.markAsSeen("card2", "card4", "card6")

        val result = orderingEngine.getSortedCards(cards)

        // Expected order:
        // 1. Next week unseen (priority 0)
        assertEquals("card3", result[0].cardId)

        // 2. Today events regardless of seen (priority 1)
        assertEquals("card2", result[1].cardId) // Today seen
        assertEquals("card5", result[2].cardId) // Today unseen

        // 3. Other unseen (priority 2)
        assertEquals("card1", result[3].cardId) // Regular unseen
        assertEquals("card7", result[4].cardId) // Far future unseen

        // 4. Other seen (priority 3)
        assertEquals("card4", result[5].cardId) // Next week seen
        assertEquals("card6", result[6].cardId) // Regular seen
    }

    @Test
    fun `getSortedCards - invalid date formats are handled gracefully`() = runTest {
        val cards = listOf(
            createCard("card1", "Valid Date", today.toString()),
            createCard("card2", "Invalid Date", "invalid-date"),
            createCard("card3", "Another Valid", nextWeekDay3.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        // Should not crash and treat invalid date as no date
        assertEquals(3, result.size)
        assertEquals("card3", result[0].cardId) // Next week first
        assertEquals("card1", result[1].cardId) // Today second
        assertEquals("card2", result[2].cardId) // Invalid date treated as regular
    }

    @Test
    fun `getSortedCards - null dates are handled`() = runTest {
        val cards = listOf(
            createCard("card1", "No Date", null),
            createCard("card2", "Today Event", today.toString()),
            createCard("card3", "Next Week", nextWeekDay3.toString())
        )

        val result = orderingEngine.getSortedCards(cards)

        assertEquals("card3", result[0].cardId) // Next week first
        assertEquals("card2", result[1].cardId) // Today second
        assertEquals("card1", result[2].cardId) // No date last
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
        startDate: String?
    ): DiscoverModel {
        return DiscoverModel(
            title = title,
            description = "Test description",
            startDate = startDate,
            endDate = null,
            disclaimer = null,
            imageList = listOf("https://example.com/image.jpg"),
            buttons = null,
            type = DiscoverCardType.Events,
            cardId = cardId
        )
    }
}
