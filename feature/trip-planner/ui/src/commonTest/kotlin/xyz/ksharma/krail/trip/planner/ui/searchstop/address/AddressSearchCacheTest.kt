package xyz.ksharma.krail.trip.planner.ui.searchstop.address

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddressSearchCacheTest {

    private fun address(id: String) = SearchStopState.SearchResult.Address(
        addressId = id,
        displayName = "Display $id",
        addressType = "street",
    )

    @Test
    fun `GIVEN empty cache WHEN get THEN null`() {
        val cache = AddressSearchCache(nowMillis = { 0L })
        assertNull(cache.get("sydney"))
    }

    @Test
    fun `GIVEN a put WHEN get before ttl expiry THEN returns cached results`() {
        var now = 0L
        val cache = AddressSearchCache(successTtlMillis = 1_000L, nowMillis = { now })
        val results = listOf(address("1"))
        cache.put("sydney", results)
        now = 999L
        assertEquals(results, cache.get("sydney"))
    }

    @Test
    fun `GIVEN a successful put WHEN get after ttl expiry THEN null`() {
        var now = 0L
        val cache = AddressSearchCache(successTtlMillis = 1_000L, nowMillis = { now })
        cache.put("sydney", listOf(address("1")))
        now = 1_000L
        assertNull(cache.get("sydney"))
    }

    @Test
    fun `GIVEN an empty-result put WHEN get after empty ttl expiry THEN null`() {
        var now = 0L
        val cache = AddressSearchCache(emptyResultTtlMillis = 500L, nowMillis = { now })
        cache.put("nowhere", emptyList())
        now = 500L
        assertNull(cache.get("nowhere"))
    }

    @Test
    fun `GIVEN an empty-result put WHEN get before empty ttl expiry THEN empty list not null`() {
        var now = 0L
        val cache = AddressSearchCache(emptyResultTtlMillis = 500L, nowMillis = { now })
        cache.put("nowhere", emptyList())
        now = 499L
        assertEquals(emptyList(), cache.get("nowhere"))
    }

    @Test
    fun `GIVEN cache at max size WHEN put another entry THEN least recently used is evicted`() {
        val cache = AddressSearchCache(maxEntries = 2, nowMillis = { 0L })
        cache.put("a", listOf(address("a")))
        cache.put("b", listOf(address("b")))
        cache.put("c", listOf(address("c")))

        assertNull(cache.get("a"))
        assertEquals(listOf(address("b")), cache.get("b"))
        assertEquals(listOf(address("c")), cache.get("c"))
    }

    @Test
    fun `GIVEN cache at max size WHEN recently accessed entry is kept fresh THEN it survives eviction`() {
        val cache = AddressSearchCache(maxEntries = 2, nowMillis = { 0L })
        cache.put("a", listOf(address("a")))
        cache.put("b", listOf(address("b")))
        // Touch "a" so "b" becomes the least-recently-used entry instead.
        cache.get("a")
        cache.put("c", listOf(address("c")))

        assertEquals(listOf(address("a")), cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals(listOf(address("c")), cache.get("c"))
    }
}
