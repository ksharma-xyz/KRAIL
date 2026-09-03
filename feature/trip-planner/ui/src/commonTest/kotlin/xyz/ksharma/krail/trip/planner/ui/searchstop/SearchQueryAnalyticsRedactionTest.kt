package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SearchQueryAnalyticsRedactionTest {

    @Test
    fun `a query with no digits is sent as typed`() {
        assertEquals("townhall", SearchQueryAnalyticsRedaction.maskedQueryOrNull("townhall"))
    }

    @Test
    fun `a house number is masked, the street it identifies is not`() {
        assertEquals(
            "# fulton place",
            SearchQueryAnalyticsRedaction.maskedQueryOrNull("4 fulton place"),
        )
    }

    @Test
    fun `every digit of a unit number is masked, not just the first`() {
        assertEquals(
            "##/### smith st",
            SearchQueryAnalyticsRedaction.maskedQueryOrNull("12/345 smith st"),
        )
    }

    @Test
    fun `an all-digit query is sent as typed - it is a route number or a stop id`() {
        // A number on its own is not an address: a house number identifies a home only in
        // combination with the street beside it, and there is no street here.
        assertEquals("861", SearchQueryAnalyticsRedaction.maskedQueryOrNull("861"))
        assertEquals("200060", SearchQueryAnalyticsRedaction.maskedQueryOrNull("200060"))
        assertEquals("4", SearchQueryAnalyticsRedaction.maskedQueryOrNull("4"))
    }

    @Test
    fun `an all-digit query is still trimmed before it is recognised as one`() {
        assertEquals("861", SearchQueryAnalyticsRedaction.maskedQueryOrNull("  861  "))
    }

    @Test
    fun `one non-digit character is enough to mask the whole query`() {
        // The moment anything else is in there, the query can carry a street.
        assertEquals("T##", SearchQueryAnalyticsRedaction.maskedQueryOrNull("T80"))
        assertEquals("#/#", SearchQueryAnalyticsRedaction.maskedQueryOrNull("4/2"))
        assertEquals("## #", SearchQueryAnalyticsRedaction.maskedQueryOrNull("12 3"))
    }

    @Test
    fun `results found no longer changes anything - the query is sent either way`() {
        // The 1.26 carve-out only sent queries that found nothing anywhere. From 1.27 the
        // policy discloses the collection, so the result count is not part of the decision.
        assertEquals("wynyard", SearchQueryAnalyticsRedaction.maskedQueryOrNull("wynyard"))
    }

    @Test
    fun `query is trimmed before the length check`() {
        assertEquals("townhall", SearchQueryAnalyticsRedaction.maskedQueryOrNull("  townhall  "))
    }

    @Test
    fun `over max length is dropped - street plus suburb identifies even with no digits`() {
        assertNull(
            SearchQueryAnalyticsRedaction.maskedQueryOrNull(
                "a".repeat(SearchQueryAnalyticsRedaction.MAX_QUERY_LENGTH + 1),
            ),
        )
    }

    @Test
    fun `exactly max length is kept`() {
        val query = "a".repeat(SearchQueryAnalyticsRedaction.MAX_QUERY_LENGTH)
        assertEquals(query, SearchQueryAnalyticsRedaction.maskedQueryOrNull(query))
    }

    @Test
    fun `length is measured on the typed query, not on the masked one`() {
        // Masking is 1:1 per character, so this can only break if that ever stops holding.
        val query = "a".repeat(SearchQueryAnalyticsRedaction.MAX_QUERY_LENGTH - 1) + "1"
        assertEquals(
            SearchQueryAnalyticsRedaction.MAX_QUERY_LENGTH,
            SearchQueryAnalyticsRedaction.maskedQueryOrNull(query)?.length,
        )
    }

    @Test
    fun `a real stop name fits inside the cap`() {
        // The cap has to clear the names riders actually type, or the longest queries -
        // which are the ones most likely to be failing - are the ones never reported.
        assertEquals(
            "north sydney interchange stand c",
            SearchQueryAnalyticsRedaction.maskedQueryOrNull("north sydney interchange stand c"),
        )
    }

    @Test
    fun `blank query is never kept`() {
        assertNull(SearchQueryAnalyticsRedaction.maskedQueryOrNull("   "))
        assertNull(SearchQueryAnalyticsRedaction.maskedQueryOrNull(""))
    }

    @Test
    fun `no digit survives a query that has anything else in it`() {
        // The policy promise, stated as one property rather than as examples: a digit only
        // ever leaves as part of a query that is nothing but digits.
        listOf(
            "4 fulton place",
            "unit 3, 27 park rd",
            "level 10 tower 2",
            "9/9/9",
            "12a smith st",
        ).forEach { query ->
            val masked = SearchQueryAnalyticsRedaction.maskedQueryOrNull(query)
            assertFalse(
                masked.orEmpty().any(Char::isDigit),
                "a digit survived masking of '$query': got '$masked'",
            )
        }
    }
}
