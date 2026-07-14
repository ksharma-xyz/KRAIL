package xyz.ksharma.krail.trip.planner.ui.searchstop.address

import kotlin.test.Test
import kotlin.test.assertEquals

class AddressSearchEligibilityTest {

    @Test
    fun `GIVEN kill switch disabled WHEN evaluate THEN DISABLED regardless of query`() {
        assertEquals(
            AddressSearchGate.DISABLED,
            AddressSearchEligibility.evaluate(
                normalizedQuery = "Sydney Opera House",
                isAddressSearchEnabled = false,
                minQueryLength = 6,
            ),
        )
    }

    @Test
    fun `GIVEN blank normalized query WHEN evaluate THEN BLANK`() {
        assertEquals(
            AddressSearchGate.BLANK,
            AddressSearchEligibility.evaluate(
                normalizedQuery = "",
                isAddressSearchEnabled = true,
                minQueryLength = 6,
            ),
        )
    }

    @Test
    fun `GIVEN query shorter than threshold WHEN evaluate THEN BELOW_THRESHOLD`() {
        assertEquals(
            AddressSearchGate.BELOW_THRESHOLD,
            AddressSearchEligibility.evaluate(
                normalizedQuery = "Syd",
                isAddressSearchEnabled = true,
                minQueryLength = 6,
            ),
        )
    }

    @Test
    fun `GIVEN query exactly at threshold WHEN evaluate THEN ELIGIBLE`() {
        assertEquals(
            AddressSearchGate.ELIGIBLE,
            AddressSearchEligibility.evaluate(
                normalizedQuery = "Sydney",
                isAddressSearchEnabled = true,
                minQueryLength = 6,
            ),
        )
    }

    @Test
    fun `GIVEN query longer than threshold WHEN evaluate THEN ELIGIBLE`() {
        assertEquals(
            AddressSearchGate.ELIGIBLE,
            AddressSearchEligibility.evaluate(
                normalizedQuery = "Sydney Opera House",
                isAddressSearchEnabled = true,
                minQueryLength = 6,
            ),
        )
    }

    @Test
    fun `GIVEN disabled AND blank AND below threshold WHEN evaluate THEN disabled wins`() {
        assertEquals(
            AddressSearchGate.DISABLED,
            AddressSearchEligibility.evaluate(
                normalizedQuery = "",
                isAddressSearchEnabled = false,
                minQueryLength = 6,
            ),
        )
    }
}
