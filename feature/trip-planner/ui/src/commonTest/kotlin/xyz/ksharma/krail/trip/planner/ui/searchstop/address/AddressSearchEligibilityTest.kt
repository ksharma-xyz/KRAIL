package xyz.ksharma.krail.trip.planner.ui.searchstop.address

import kotlin.test.Test
import kotlin.test.assertEquals

class AddressSearchEligibilityTest {

    private fun evaluate(
        normalizedQuery: String,
        isAddressSearchEnabled: Boolean = true,
        minQueryLength: Int = 6,
        localStopResultCount: Int? = null,
        maxLocalStopsForAddressSearch: Int = DEFAULT_ADDRESS_SEARCH_MAX_LOCAL_STOPS,
    ) = AddressSearchEligibility.evaluate(
        normalizedQuery = normalizedQuery,
        isAddressSearchEnabled = isAddressSearchEnabled,
        minQueryLength = minQueryLength,
        localStopResultCount = localStopResultCount,
        maxLocalStopsForAddressSearch = maxLocalStopsForAddressSearch,
    )

    @Test
    fun `GIVEN kill switch disabled WHEN evaluate THEN DISABLED regardless of query`() {
        assertEquals(
            AddressSearchGate.DISABLED,
            evaluate(normalizedQuery = "Sydney Opera House", isAddressSearchEnabled = false),
        )
    }

    @Test
    fun `GIVEN blank normalized query WHEN evaluate THEN BLANK`() {
        assertEquals(AddressSearchGate.BLANK, evaluate(normalizedQuery = ""))
    }

    @Test
    fun `GIVEN query shorter than threshold WHEN evaluate THEN BELOW_THRESHOLD`() {
        assertEquals(AddressSearchGate.BELOW_THRESHOLD, evaluate(normalizedQuery = "Syd"))
    }

    @Test
    fun `GIVEN query exactly at threshold WHEN evaluate THEN ELIGIBLE`() {
        assertEquals(AddressSearchGate.ELIGIBLE, evaluate(normalizedQuery = "Sydney"))
    }

    @Test
    fun `GIVEN query longer than threshold WHEN evaluate THEN ELIGIBLE`() {
        assertEquals(AddressSearchGate.ELIGIBLE, evaluate(normalizedQuery = "Sydney Op"))
    }

    @Test
    fun `GIVEN disabled AND blank AND below threshold WHEN evaluate THEN disabled wins`() {
        assertEquals(
            AddressSearchGate.DISABLED,
            evaluate(normalizedQuery = "", isAddressSearchEnabled = false),
        )
    }

    // region Local stop count gate

    @Test
    fun `GIVEN local count is unknown WHEN evaluate THEN the stop count gate is not applied`() {
        assertEquals(
            AddressSearchGate.ELIGIBLE,
            evaluate(normalizedQuery = "Sydney", localStopResultCount = null),
        )
    }

    @Test
    fun `GIVEN more local stops than the max WHEN evaluate THEN STOPS_ALREADY_SUFFICIENT`() {
        assertEquals(
            AddressSearchGate.STOPS_ALREADY_SUFFICIENT,
            evaluate(
                normalizedQuery = "Sydney",
                localStopResultCount = 11,
                maxLocalStopsForAddressSearch = 10,
            ),
        )
    }

    @Test
    fun `GIVEN exactly the max local stops WHEN evaluate THEN ELIGIBLE`() {
        assertEquals(
            AddressSearchGate.ELIGIBLE,
            evaluate(
                normalizedQuery = "Sydney",
                localStopResultCount = 10,
                maxLocalStopsForAddressSearch = 10,
            ),
        )
    }

    @Test
    fun `GIVEN zero local stops WHEN evaluate THEN ELIGIBLE`() {
        assertEquals(
            AddressSearchGate.ELIGIBLE,
            evaluate(normalizedQuery = "Sydney", localStopResultCount = 0),
        )
    }

    @Test
    fun `GIVEN a busy stop list WHEN the query is long enough THEN the escape hatch keeps it ELIGIBLE`() {
        val longQuery = "1 Fulton Place"
        assertEquals(12, ADDRESS_SEARCH_LONG_QUERY_LENGTH)
        assertEquals(
            AddressSearchGate.ELIGIBLE,
            evaluate(
                normalizedQuery = longQuery,
                localStopResultCount = 50,
                maxLocalStopsForAddressSearch = 10,
            ),
        )
    }

    @Test
    fun `GIVEN a busy stop list WHEN the query is one char short of the hatch THEN suppressed`() {
        assertEquals(
            AddressSearchGate.STOPS_ALREADY_SUFFICIENT,
            evaluate(
                normalizedQuery = "abcdefghijk", // 11 chars
                localStopResultCount = 50,
                maxLocalStopsForAddressSearch = 10,
            ),
        )
    }

    @Test
    fun `GIVEN a busy stop list WHEN the query is exactly the hatch length THEN ELIGIBLE`() {
        assertEquals(
            AddressSearchGate.ELIGIBLE,
            evaluate(
                normalizedQuery = "abcdefghijkl", // 12 chars
                localStopResultCount = 50,
                maxLocalStopsForAddressSearch = 10,
            ),
        )
    }

    @Test
    fun `GIVEN a busy stop list AND a below-threshold query WHEN evaluate THEN BELOW_THRESHOLD wins`() {
        assertEquals(
            AddressSearchGate.BELOW_THRESHOLD,
            evaluate(
                normalizedQuery = "Syd",
                localStopResultCount = 50,
                maxLocalStopsForAddressSearch = 10,
            ),
        )
    }

    @Test
    fun `GIVEN a busy stop list AND the kill switch off WHEN evaluate THEN DISABLED wins`() {
        assertEquals(
            AddressSearchGate.DISABLED,
            evaluate(
                normalizedQuery = "Sydney",
                isAddressSearchEnabled = false,
                localStopResultCount = 50,
                maxLocalStopsForAddressSearch = 10,
            ),
        )
    }

    @Test
    fun `GIVEN max local stops of zero WHEN any stop matched THEN STOPS_ALREADY_SUFFICIENT`() {
        assertEquals(
            AddressSearchGate.STOPS_ALREADY_SUFFICIENT,
            evaluate(
                normalizedQuery = "Sydney",
                localStopResultCount = 1,
                maxLocalStopsForAddressSearch = 0,
            ),
        )
    }

    // endregion Local stop count gate
}
