package xyz.ksharma.krail.trip.planner.ui.searchstop.address

import kotlin.test.Test
import kotlin.test.assertEquals

class AddressSearchQueryNormalizerTest {

    @Test
    fun `GIVEN leading and trailing whitespace WHEN normalizeAddressQuery THEN trimmed`() {
        assertEquals("Sydney", normalizeAddressQuery("  Sydney  "))
    }

    @Test
    fun `GIVEN internal whitespace WHEN normalizeAddressQuery THEN preserved`() {
        assertEquals("George Street", normalizeAddressQuery(" George Street "))
    }

    @Test
    fun `GIVEN normalized query WHEN addressSearchCacheKey THEN lowercased`() {
        assertEquals("sydney opera house", addressSearchCacheKey("Sydney Opera House"))
    }

    @Test
    fun `GIVEN different casing of same query WHEN addressSearchCacheKey THEN same key`() {
        assertEquals(
            addressSearchCacheKey("SYDNEY"),
            addressSearchCacheKey("sydney"),
        )
    }
}
