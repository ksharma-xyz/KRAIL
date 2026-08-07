package xyz.ksharma.krail.trip.planner.ui.searchstop.address

import kotlin.test.Test
import kotlin.test.assertEquals

class AddressSearchEligibilityTest {

    private fun evaluate(
        normalizedQuery: String,
        isAddressSearchEnabled: Boolean = true,
        minQueryLength: Int = 6,
    ) = AddressSearchEligibility.evaluate(
        normalizedQuery = normalizedQuery,
        isAddressSearchEnabled = isAddressSearchEnabled,
        minQueryLength = minQueryLength,
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

    /**
     * Regression guard for the reverted stop-count gate: "13 hassall" matches dozens of
     * Hassall St bus stops, and that used to suppress the address call outright. A short
     * address query must stay ELIGIBLE no matter how busy the local stop list is — nothing
     * about the local pipeline reaches this function any more.
     */
    @Test
    fun `GIVEN a short address-shaped query WHEN evaluate THEN ELIGIBLE`() {
        assertEquals(AddressSearchGate.ELIGIBLE, evaluate(normalizedQuery = "13 hassall"))
    }

    @Test
    fun `GIVEN a query one char under the threshold WHEN evaluate THEN BELOW_THRESHOLD`() {
        assertEquals(
            AddressSearchGate.BELOW_THRESHOLD,
            evaluate(normalizedQuery = "13 ha", minQueryLength = 6),
        )
    }
}
