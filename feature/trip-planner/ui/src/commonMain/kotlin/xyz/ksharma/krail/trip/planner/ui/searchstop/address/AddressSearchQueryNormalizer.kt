package xyz.ksharma.krail.trip.planner.ui.searchstop.address

/**
 * Trims leading/trailing whitespace only. Used for eligibility and request construction —
 * the NSW API receives this exact value, case preserved.
 */
fun normalizeAddressQuery(rawQuery: String): String = rawQuery.trim()
