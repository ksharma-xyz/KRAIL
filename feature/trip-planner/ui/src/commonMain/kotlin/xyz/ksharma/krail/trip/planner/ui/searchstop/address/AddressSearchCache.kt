package xyz.ksharma.krail.trip.planner.ui.searchstop.address

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * In-memory, per-`SearchStopViewModel` LRU cache for address/POI results, keyed by
 * [addressSearchCacheKey]. Never persisted to disk — see
 * feature/trip-planner/ui/ADDRESS_SEARCH_ELIGIBILITY.md. A successful (non-empty)
 * result and an empty result get different TTLs so a broad no-match prefix isn't
 * treated as durable truth for as long as a real result would be.
 *
 * Not thread-safe: callers on this codebase's single-threaded ViewModel dispatch
 * pattern only, same assumption as the rest of `SearchStopViewModel`'s mutable state.
 */
@OptIn(ExperimentalTime::class)
class AddressSearchCache(
    private val maxEntries: Int = MAX_ENTRIES,
    private val successTtlMillis: Long = SUCCESS_TTL_MILLIS,
    private val emptyResultTtlMillis: Long = EMPTY_RESULT_TTL_MILLIS,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    // Iteration order tracks recency: put()/get() both re-insert on access, so the
    // first key is always the least-recently-used one.
    private val entries = LinkedHashMap<String, Entry>()

    fun get(key: String): List<SearchStopState.SearchResult.Address>? {
        val entry = entries.remove(key) ?: return null
        if (nowMillis() >= entry.expiresAtMillis) {
            return null
        }
        entries[key] = entry
        return entry.results
    }

    fun put(key: String, results: List<SearchStopState.SearchResult.Address>) {
        entries.remove(key)
        val ttlMillis = if (results.isEmpty()) emptyResultTtlMillis else successTtlMillis
        entries[key] = Entry(results, nowMillis() + ttlMillis)
        if (entries.size > maxEntries) {
            entries.remove(entries.keys.first())
        }
    }

    private data class Entry(
        val results: List<SearchStopState.SearchResult.Address>,
        val expiresAtMillis: Long,
    )

    private companion object {
        const val MAX_ENTRIES = 20
        const val SUCCESS_TTL_MILLIS = 120_000L
        const val EMPTY_RESULT_TTL_MILLIS = 30_000L
    }
}
