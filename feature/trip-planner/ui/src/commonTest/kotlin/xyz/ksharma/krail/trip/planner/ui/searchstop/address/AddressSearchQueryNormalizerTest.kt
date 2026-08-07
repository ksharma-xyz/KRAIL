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
    fun `GIVEN case WHEN normalizeAddressQuery THEN preserved for the API`() {
        assertEquals("Sydney Opera House", normalizeAddressQuery(" Sydney Opera House "))
    }
}
