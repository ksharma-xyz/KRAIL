package xyz.ksharma.krail.trip.planner.ui.searchstop.address

/**
 * Trims leading/trailing whitespace only. Used for eligibility, request construction,
 * and as the basis for [addressSearchCacheKey] — the NSW API receives this exact
 * value, case preserved.
 */
fun normalizeAddressQuery(rawQuery: String): String = rawQuery.trim()

/**
 * Case-folded cache/coalescing key derived from an already-[normalizeAddressQuery]d
 * query. Kept separate from the request value itself: the doc calls for a
 * case-insensitive cache key "unless NSW API testing proves case-insensitivity" of the
 * request, so the two must not be conflated.
 */
fun addressSearchCacheKey(normalizedQuery: String): String = normalizedQuery.lowercase()
