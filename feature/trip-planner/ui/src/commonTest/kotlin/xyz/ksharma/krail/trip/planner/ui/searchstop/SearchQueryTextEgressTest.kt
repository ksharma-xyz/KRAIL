package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.trip.planner.ui.searchstop.address.AddressSearchGate
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * From 1.27 the typed query is sent for every settled search, disclosed by the privacy
 * policy. What the policy also promises is that house and unit numbers are masked, and
 * because analytics goes straight to Firebase there is no later stage that could do it -
 * whatever these helpers build is what a third party stores.
 *
 * A query that is nothing but digits is a route number or a stop id, not an address, and
 * goes out as typed. Everything else has its digits masked.
 *
 * `SearchQueryAnalyticsRedactionTest` pins the masking function. This test guards the step
 * after it - the event that actually goes out. It drives the production track helpers and
 * reads the built property map, because that map is what ships, and a digit could arrive
 * through a parameter nobody thought of rather than through a broken mask.
 *
 * What it cannot see: a hash or an encoding of the query, which by construction contains
 * no digit and no substring of the text. That would have to be caught in review.
 */
class SearchQueryTextEgressTest {

    @Test
    fun `no digit leaves an address-shaped query on a resolved local search`() {
        val analytics = FakeAnalytics()

        analytics.trackLocalSearchResolved(
            query = ADDRESS_QUERY,
            searchSessionId = SESSION_ID,
            localResultsCount = 4,
            addressSearchGate = AddressSearchGate.DISABLED,
        )

        assertNoDigitAnywhere(analytics.searchEvent())
    }

    @Test
    fun `no digit leaves an address-shaped query on a failed local search`() {
        val analytics = FakeAnalytics()

        analytics.trackLocalSearchFailed(query = ADDRESS_QUERY, searchSessionId = SESSION_ID)

        assertNoDigitAnywhere(analytics.searchEvent())
    }

    @Test
    fun `the street survives so the query is still diagnosable`() {
        // Control. Without this, every assertion above would also pass if the event stopped
        // carrying any query at all, and the guard would be measuring nothing.
        val analytics = FakeAnalytics()

        analytics.trackLocalSearchResolved(
            query = ADDRESS_QUERY,
            searchSessionId = SESSION_ID,
            localResultsCount = 0,
            addressSearchGate = AddressSearchGate.BELOW_THRESHOLD,
        )

        assertEquals("# fulton place", analytics.searchEvent().properties?.get("query"))
    }

    @Test
    fun `the address firing carries no query text at all`() {
        // The local firing happens for every settled query and already carries the text.
        // Sending it here too would double the egress and double-count the eval corpus.
        val analytics = FakeAnalytics()

        analytics.trackAddressSearchResolved(
            normalizedQuery = ADDRESS_QUERY,
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

        val event = analytics.searchEvent()
        assertNull(event.properties?.get("query"))
        assertNoDigitAnywhere(event)
    }

    @Test
    fun `a query too long to send carries no text rather than a truncated one`() {
        val analytics = FakeAnalytics()

        analytics.trackLocalSearchResolved(
            query = "unit ${"a".repeat(SearchQueryAnalyticsRedaction.MAX_QUERY_LENGTH)}",
            searchSessionId = SESSION_ID,
            localResultsCount = 0,
            addressSearchGate = AddressSearchGate.ELIGIBLE,
        )

        assertNull(analytics.searchEvent().properties?.get("query"))
    }

    @Test
    fun `an all-digit query goes out as typed - it is a route number, not an address`() {
        // Masking these would collapse "861" and "200060" into "###" and "######", which
        // erases the whole class of search rather than protecting anything: a number with
        // no street beside it identifies no home.
        val analytics = FakeAnalytics()

        analytics.trackLocalSearchResolved(
            query = "861",
            searchSessionId = SESSION_ID,
            localResultsCount = 0,
            addressSearchGate = AddressSearchGate.BELOW_THRESHOLD,
        )

        assertEquals("861", analytics.searchEvent().properties?.get("query"))
    }

    private fun FakeAnalytics.searchEvent(): AnalyticsEvent.SearchStopQuery {
        val event = getTrackedEvent(EVENT_NAME)
        assertIs<AnalyticsEvent.SearchStopQuery>(event)
        return event
    }

    /**
     * Numeric parameters (`queryLength`, counts) are numbers, not text, so only String
     * values are inspected - a digit inside a count is the count.
     */
    private fun assertNoDigitAnywhere(event: AnalyticsEvent.SearchStopQuery) {
        val leaking = event.properties.orEmpty()
            .filterKeys { it != PROP_SEARCH_SESSION_ID }
            .filter { (_, value) -> value is String && value.any(Char::isDigit) }

        if (leaking.isNotEmpty()) {
            fail(
                "search_stop_query carried a digit from a query that was not all digits. " +
                    "A house or unit number is what makes a query identify a home, and the " +
                    "privacy policy promises it is masked before it is stored. Masking " +
                    "happens in SearchQueryAnalyticsRedaction.maskedQueryOrNull; see " +
                    "docs/SEARCH_QUERY_TELEMETRY_SPEC.md.\n" +
                    "  leaked via " + leaking.entries.joinToString { "${it.key}=${it.value}" },
            )
        }
    }

    private companion object {
        const val ADDRESS_QUERY = "4 fulton place"
        const val SESSION_ID = "session-egress"
        const val EVENT_NAME = "search_stop_query"

        /** A random hex id, unrelated to anything the rider typed. */
        const val PROP_SEARCH_SESSION_ID = "searchSessionId"
    }
}
