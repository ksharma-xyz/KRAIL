package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.trip.planner.ui.searchstop.address.AddressSearchGate
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * A typed search query can be a street address, and a street address is a home. The rule in
 * `docs/SEARCH_QUERY_TELEMETRY_SPEC.md` is that raw text leaves the device only under the
 * zero-result carve-out: nothing found anywhere, no digits, short.
 *
 * `SearchQueryAnalyticsRedactionTest` already pins the carve-out predicate. This test guards the
 * step after it — the event that actually goes out. It drives the production track helpers and
 * reads the built property map, because that map is what ships, and a leak could arrive through
 * a parameter nobody thought of rather than through a wrong carve-out decision.
 *
 * The assertion deliberately checks **every** property value rather than just the `query` key,
 * and checks the query's individual words as well as the whole string, because the spec calls
 * out that "a first token, a normalised query or a hash of the text are all the text".
 *
 * What it cannot see: a hash or an encoding of the query, which by construction does not
 * contain the query as a substring. That would have to be caught in review.
 */
class SearchQueryTextEgressTest {

    @Test
    fun `a local query that found results never leaves as text`() {
        val analytics = FakeAnalytics()

        analytics.trackLocalSearchResolved(
            query = QUERY,
            searchSessionId = SESSION_ID,
            localResultsCount = 4,
            addressSearchGate = AddressSearchGate.DISABLED,
        )

        assertNoRawQuery(analytics.searchEvent())
    }

    @Test
    fun `an address query that found results never leaves as text`() {
        val analytics = FakeAnalytics()

        analytics.trackAddressSearchResolved(
            normalizedQuery = QUERY,
            searchSessionId = SESSION_ID,
            localResultsCount = 0,
            addressResults = listOf(
                SearchStopState.SearchResult.Address(
                    addressId = "addr-1",
                    displayName = "Wynyard Station, Sydney NSW",
                    addressType = "poi",
                ),
            ),
        )

        assertNoRawQuery(analytics.searchEvent())
    }

    @Test
    fun `a failed local search never leaves as text`() {
        val analytics = FakeAnalytics()

        analytics.trackLocalSearchFailed(query = QUERY, searchSessionId = SESSION_ID)

        assertNoRawQuery(analytics.searchEvent())
    }

    @Test
    fun `an address-eligible query defers rather than leaking on the local event`() {
        // The local site does not know the address count yet, so it must not decide.
        val analytics = FakeAnalytics()

        analytics.trackLocalSearchResolved(
            query = QUERY,
            searchSessionId = SESSION_ID,
            localResultsCount = 0,
            addressSearchGate = AddressSearchGate.ELIGIBLE,
        )

        assertNoRawQuery(analytics.searchEvent())
    }

    @Test
    fun `the carve-out itself still works`() {
        // Control. Without this, every assertion above would also pass if the event stopped
        // carrying any query at all, and the guard would be measuring nothing.
        val analytics = FakeAnalytics()

        analytics.trackLocalSearchResolved(
            query = QUERY,
            searchSessionId = SESSION_ID,
            localResultsCount = 0,
            addressSearchGate = AddressSearchGate.DISABLED,
        )

        assertEquals(QUERY, analytics.searchEvent().properties?.get("query"))
    }

    private fun FakeAnalytics.searchEvent(): AnalyticsEvent.SearchStopQuery {
        val event = getTrackedEvent(EVENT_NAME)
        assertIs<AnalyticsEvent.SearchStopQuery>(event)
        return event
    }

    private fun assertNoRawQuery(event: AnalyticsEvent.SearchStopQuery) {
        val needles = listOf(QUERY) + QUERY.split(" ").filter { it.length >= MIN_TOKEN_LENGTH }
        val values = event.properties.orEmpty().entries

        needles.forEach { needle ->
            val leaking = values.filter { it.value.toString().contains(needle, ignoreCase = true) }
            if (leaking.isNotEmpty()) {
                fail(
                    "search_stop_query carried the rider's typed text for a query that found " +
                        "results. Raw text may only ship under the zero-result carve-out in " +
                        "SearchQueryAnalyticsRedaction.zeroResultQueryOrNull; see " +
                        "docs/SEARCH_QUERY_TELEMETRY_SPEC.md.\n" +
                        "  leaked '$needle' via " +
                        leaking.joinToString { "${it.key}=${it.value}" },
                )
            }
        }
        assertTrue(event.properties.orEmpty().isNotEmpty(), "event carried no properties at all")
    }

    private companion object {
        const val QUERY = "wynyard quaybridge"
        const val SESSION_ID = "session-egress"
        const val EVENT_NAME = "search_stop_query"
        const val MIN_TOKEN_LENGTH = 4
    }
}
