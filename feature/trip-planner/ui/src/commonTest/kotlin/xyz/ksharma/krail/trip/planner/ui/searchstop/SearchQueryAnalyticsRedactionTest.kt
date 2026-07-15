package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchQueryAnalyticsRedactionTest {

    @Test
    fun `zero results everywhere, no digits, short - query is kept`() {
        assertEquals(
            "townhall",
            SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
                query = "townhall",
                localResultsCount = 0,
                addressResultsCount = null,
            ),
        )
    }

    @Test
    fun `address pipeline recognised nothing - query is kept`() {
        assertEquals(
            "townhall",
            SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
                query = "townhall",
                localResultsCount = 0,
                addressResultsCount = 0,
            ),
        )
    }

    @Test
    fun `query is trimmed before length and emptiness checks`() {
        assertEquals(
            "townhall",
            SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
                query = "  townhall  ",
                localResultsCount = 0,
                addressResultsCount = null,
            ),
        )
    }

    @Test
    fun `any digit blocks the query - house numbers are the PII carrier`() {
        assertNull(
            SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
                query = "4 fulton place",
                localResultsCount = 0,
                addressResultsCount = null,
            ),
        )
    }

    @Test
    fun `local results present blocks the query`() {
        assertNull(
            SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
                query = "townhall",
                localResultsCount = 3,
                addressResultsCount = null,
            ),
        )
    }

    @Test
    fun `address results present blocks the query`() {
        assertNull(
            SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
                query = "fulton place",
                localResultsCount = 0,
                addressResultsCount = 5,
            ),
        )
    }

    @Test
    fun `over max length blocks the query`() {
        assertNull(
            SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
                query = "a".repeat(SearchQueryAnalyticsRedaction.MAX_ZERO_RESULT_QUERY_LENGTH + 1),
                localResultsCount = 0,
                addressResultsCount = null,
            ),
        )
    }

    @Test
    fun `exactly max length is kept`() {
        val query = "a".repeat(SearchQueryAnalyticsRedaction.MAX_ZERO_RESULT_QUERY_LENGTH)
        assertEquals(
            query,
            SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
                query = query,
                localResultsCount = 0,
                addressResultsCount = null,
            ),
        )
    }

    @Test
    fun `blank query is never kept`() {
        assertNull(
            SearchQueryAnalyticsRedaction.zeroResultQueryOrNull(
                query = "   ",
                localResultsCount = 0,
                addressResultsCount = null,
            ),
        )
    }
}
