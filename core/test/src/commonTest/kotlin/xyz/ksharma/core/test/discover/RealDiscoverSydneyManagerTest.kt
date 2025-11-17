@file:Suppress("VisibleForTests")
package xyz.ksharma.core.test.discover

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import xyz.ksharma.core.test.fakes.FakeDiscoverCardSeenPreferences
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.discover.network.real.RealDiscoverSydneyManager
import xyz.ksharma.krail.discover.network.real.db.RealDiscoverCardOrderingEngine
import xyz.ksharma.krail.discover.state.DiscoverCardType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for RealDiscoverSydneyManager.
 *
 * These tests use the real RealDiscoverCardOrderingEngine with FakeDiscoverCardSeenPreferences
 * to provide more realistic testing without needing fake implementations.
 *
 * **Important**: ButtonListSerializer only supports deserialization, not serialization.
 * For tests involving buttons, create JSON strings manually instead of using Json.encodeToString().
 */
class RealDiscoverSydneyManagerTest {

    private lateinit var fakeFlag: FakeFlag
    private lateinit var fakePreferences: FakeDiscoverCardSeenPreferences
    private lateinit var orderingEngine: RealDiscoverCardOrderingEngine
    private lateinit var manager: RealDiscoverSydneyManager
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        fakeFlag = FakeFlag()
        fakePreferences = FakeDiscoverCardSeenPreferences()
        orderingEngine = RealDiscoverCardOrderingEngine(fakePreferences)
        manager = RealDiscoverSydneyManager(
            flag = fakeFlag,
            defaultDispatcher = testDispatcher,
            discoverCardOrderingEngine = orderingEngine
        )
    }

    @Test
    fun testFetchDiscoverData_withValidJson_returnsCards() = runTest(testDispatcher) {
        val cards = listOf(
            createCard("card1", "Test Card 1"),
            createCard("card2", "Test Card 2")
        )
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        val result = manager.fetchDiscoverData()

        assertEquals(2, result.size)
        assertEquals("card1", result[0].cardId)
        assertEquals("card2", result[1].cardId)
    }

    @Test
    fun testFetchDiscoverData_withEmptyJson_returnsEmptyList() = runTest(testDispatcher) {
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue("[]"))

        val result = manager.fetchDiscoverData()

        assertEquals(0, result.size)
    }

    @Test
    fun testFetchDiscoverData_withInvalidJson_returnsEmptyList() = runTest(testDispatcher) {
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue("invalid json"))

        val result = manager.fetchDiscoverData()

        assertEquals(0, result.size)
    }

    @Test
    fun testFetchDiscoverData_withNonJsonFlagValue_usesDefault() = runTest(testDispatcher) {
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.BooleanValue(true))

        val result = manager.fetchDiscoverData()

        // Should return default (empty array)
        assertEquals(0, result.size)
    }

    @Test
    fun testFetchDiscoverData_appliesOrdering_unseenCardsFirst() = runTest(testDispatcher) {
        val cards = listOf(
            createCard("card1", "Card 1"),
            createCard("card2", "Card 2"),
            createCard("card3", "Card 3")
        )
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        // Mark card2 as seen
        fakePreferences.markAsSeen("card2")

        val result = manager.fetchDiscoverData()

        assertEquals(3, result.size)
        // Unseen cards (card1, card3) should come before seen card (card2)
        assertEquals("card1", result[0].cardId)
        assertEquals("card3", result[1].cardId)
        assertEquals("card2", result[2].cardId)
    }

    @Test
    fun testFetchDiscoverData_filtersPastEvents() = runTest(testDispatcher) {
        val cards = listOf(
            createCard("card1", "Current Card", startDate = null),
            createCard("card2", "Past Event", startDate = "2020-01-01", endDate = "2020-01-05"),
            createCard("card3", "Future Event", startDate = "2030-01-01", endDate = "2030-01-05")
        )
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        val result = manager.fetchDiscoverData()

        // Past event should be filtered out
        assertEquals(2, result.size)
        assertTrue(result.none { it.cardId == "card2" })
        assertTrue(result.any { it.cardId == "card1" })
        assertTrue(result.any { it.cardId == "card3" })
    }

    @Test
    fun testFetchDiscoverData_cachesParsedCards() = runTest(testDispatcher) {
        val cards = listOf(createCard("card1", "Test"))
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        // First call
        val result1 = manager.fetchDiscoverData()
        assertEquals(1, result1.size)

        // Mark card as seen
        fakePreferences.markAsSeen("card1")

        // Second call with same flag value - should use cache but apply new ordering
        val result2 = manager.fetchDiscoverData()
        assertEquals(1, result2.size)
        // Card should still be returned (ordering engine handles seen cards)
        assertEquals("card1", result2[0].cardId)
    }

    @Test
    fun testFetchDiscoverData_reparsesWhenFlagChanges() = runTest(testDispatcher) {
        // First flag value
        val cards1 = listOf(createCard("card1", "Test 1"))
        val json1 = Json.encodeToString(cards1)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json1))

        val result1 = manager.fetchDiscoverData()
        assertEquals(1, result1.size)
        assertEquals("card1", result1[0].cardId)

        // Change flag value
        val cards2 = listOf(
            createCard("card2", "Test 2"),
            createCard("card3", "Test 3")
        )
        val json2 = Json.encodeToString(cards2)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json2))

        val result2 = manager.fetchDiscoverData()
        assertEquals(2, result2.size)
        assertEquals("card2", result2[0].cardId)
        assertEquals("card3", result2[1].cardId)
    }

    @Test
    fun testFetchDiscoverData_withComplexCard_parsesCorrectly() = runTest(testDispatcher) {
        // Create JSON manually because ButtonListSerializer doesn't support serialization
        // This is intentional because the serializer is designed to parse remote config JSON
        // (one-way: server → app), not to create JSON.
        val json = """
        [
            {
                "title": "Complex Card",
                "description": "Test description with special chars: @#$%",
                "startDate": "2026-05-22",
                "endDate": "2026-06-13",
                "disclaimer": "Test disclaimer",
                "imageList": ["https://example.com/img1.jpg", "https://example.com/img2.jpg"],
                "buttons": [
                    {
                        "buttonType": "cta",
                        "label": "Learn More",
                        "url": "https://example.com"
                    },
                    {
                        "buttonType": "share"
                    }
                ],
                "type": "Events",
                "cardId": "complex_card"
            }
        ]
        """.trimIndent()
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        val result = manager.fetchDiscoverData()

        assertEquals(1, result.size)
        val returnedCard = result[0]
        assertEquals("Complex Card", returnedCard.title)
        assertEquals("2026-05-22", returnedCard.startDate)
        assertEquals("2026-06-13", returnedCard.endDate)
        assertEquals("Test disclaimer", returnedCard.disclaimer)
        assertEquals(2, returnedCard.imageList.size)
        assertNotNull(returnedCard.buttons)
        assertEquals(2, returnedCard.buttons!!.size)
    }

    @Test
    fun testFetchDiscoverData_withDifferentCardTypes_preservesTypes() = runTest(testDispatcher) {
        val cards = listOf(
            createCard("card1", "Travel Card", type = DiscoverCardType.Travel),
            createCard("card2", "Food Card", type = DiscoverCardType.Food),
            createCard("card3", "Sports Card", type = DiscoverCardType.Sports),
            createCard("card4", "Events Card", type = DiscoverCardType.Events)
        )
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        val result = manager.fetchDiscoverData()

        assertEquals(4, result.size)
        assertEquals(DiscoverCardType.Travel, result[0].type)
        assertEquals(DiscoverCardType.Food, result[1].type)
        assertEquals(DiscoverCardType.Sports, result[2].type)
        assertEquals(DiscoverCardType.Events, result[3].type)
    }

    @Test
    fun testMarkCardAsSeen_updatesOrderingInNextFetch() = runTest(testDispatcher) {
        val cards = listOf(
            createCard("card1", "Card 1"),
            createCard("card2", "Card 2")
        )
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        // First fetch - both unseen
        val result1 = manager.fetchDiscoverData()
        assertEquals(2, result1.size)
        assertEquals("card1", result1[0].cardId)
        assertEquals("card2", result1[1].cardId)

        // Mark card1 as seen
        manager.markCardAsSeen("card1")

        // Second fetch - card1 should now be last
        val result2 = manager.fetchDiscoverData()
        assertEquals(2, result2.size)
        assertEquals("card2", result2[0].cardId) // Unseen first
        assertEquals("card1", result2[1].cardId) // Seen last
    }

    @Test
    fun testMarkCardAsSeen_withMultipleCards() = runTest(testDispatcher) {
        val cards = listOf(
            createCard("card1", "Card 1"),
            createCard("card2", "Card 2"),
            createCard("card3", "Card 3")
        )
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        manager.markCardAsSeen("card1")
        manager.markCardAsSeen("card2")

        val result = manager.fetchDiscoverData()

        assertEquals(3, result.size)
        assertEquals("card3", result[0].cardId) // Only unseen card first
        // Seen cards (card1, card2) come after
        assertTrue(result[1].cardId in setOf("card1", "card2"))
        assertTrue(result[2].cardId in setOf("card1", "card2"))
    }

    @Test
    fun testResetAllDiscoverCards_clearsSeenStatus() = runTest(testDispatcher) {
        val cards = listOf(
            createCard("card1", "Card 1"),
            createCard("card2", "Card 2")
        )
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        // Mark cards as seen
        manager.markCardAsSeen("card1")
        manager.markCardAsSeen("card2")

        val result1 = manager.fetchDiscoverData()
        assertEquals(2, result1.size)

        // Reset
        manager.resetAllDiscoverCardsDebugOnly()

        // Verify preferences are cleared
        val seenCards = fakePreferences.selectAllCardSeen()
        assertEquals(0, seenCards.size)
    }

    @Test
    fun testFetchDiscoverData_withMalformedDates_handlesGracefully() = runTest(testDispatcher) {
        // Create JSON manually to include malformed dates
        val json = """
        [
            {
                "title": "Valid Card",
                "description": "Test",
                "startDate": "2026-13-45",
                "endDate": "invalid-date",
                "imageList": ["https://example.com/img.jpg"],
                "type": "Events",
                "cardId": "malformed_dates"
            }
        ]
        """.trimIndent()
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        // Should not crash, will be handled by ordering engine or parsed as-is
        val result = manager.fetchDiscoverData()

        // The card is parsed (dates are stored as strings), validation happens elsewhere
        assertEquals(1, result.size)
    }

    @Test
    fun testFetchDiscoverData_withNullDates_parsesCorrectly() = runTest(testDispatcher) {
        val card = createCard("card1", "No Dates", startDate = null, endDate = null)
        val json = Json.encodeToString(listOf(card))
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        val result = manager.fetchDiscoverData()

        assertEquals(1, result.size)
        assertEquals(null, result[0].startDate)
        assertEquals(null, result[0].endDate)
    }

    @Test
    fun testFetchDiscoverData_withLargeNumberOfCards() = runTest(testDispatcher) {
        val cards = (1..100).map { createCard("card$it", "Card $it") }
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        val result = manager.fetchDiscoverData()

        assertEquals(100, result.size)
        assertEquals("card1", result[0].cardId)
        assertEquals("card100", result[99].cardId)
    }

    @Test
    fun testFetchDiscoverData_multipleCallsSameFlag_usesCache() = runTest(testDispatcher) {
        val cards = listOf(createCard("card1", "Test"))
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        // Call multiple times
        val result1 = manager.fetchDiscoverData()
        val result2 = manager.fetchDiscoverData()
        val result3 = manager.fetchDiscoverData()

        // All should return same result
        assertEquals(1, result1.size)
        assertEquals(1, result2.size)
        assertEquals(1, result3.size)
        assertEquals("card1", result1[0].cardId)
        assertEquals("card1", result2[0].cardId)
        assertEquals("card1", result3[0].cardId)
    }

    @Test
    fun testFetchDiscoverData_integrationWithOrdering_todayEvents() = runTest(testDispatcher) {
        // This test demonstrates the integration with RealDiscoverCardOrderingEngine
        // Testing today events requires dynamic date, but we can test the flow
        val cards = listOf(
            createCard("regular", "Regular Card", startDate = null),
            createCard("future", "Future Event", startDate = "2030-01-01", endDate = "2030-01-05")
        )
        val json = Json.encodeToString(cards)
        fakeFlag.setFlagValue(FlagKeys.DISCOVER_SYDNEY.key, FlagValue.JsonValue(json))

        val result = manager.fetchDiscoverData()

        // Both cards should be returned in order determined by ordering engine
        assertEquals(2, result.size)
    }

    // ========== Helper Methods ==========

    /**
     * Creates a DiscoverModel for testing.
     * Note: buttons is always null to allow safe use with Json.encodeToString().
     * For tests involving buttons, create JSON strings manually.
     */
    private fun createCard(
        cardId: String,
        title: String,
        startDate: String? = null,
        endDate: String? = null,
        type: DiscoverCardType = DiscoverCardType.Events
    ): DiscoverModel {
        return DiscoverModel(
            title = title,
            description = "Test description for $cardId",
            startDate = startDate,
            endDate = endDate,
            disclaimer = null,
            imageList = listOf("https://example.com/$cardId.jpg"),
            buttons = null,
            type = type,
            cardId = cardId
        )
    }

    // ========== Fake Flag ==========

    private class FakeFlag : Flag {
        private val flagValues = mutableMapOf<String, FlagValue>()

        fun setFlagValue(key: String, value: FlagValue) {
            flagValues[key] = value
        }

        override fun getFlagValue(key: String): FlagValue {
            return flagValues[key] ?: FlagValue.BooleanValue(false)
        }
    }
}
