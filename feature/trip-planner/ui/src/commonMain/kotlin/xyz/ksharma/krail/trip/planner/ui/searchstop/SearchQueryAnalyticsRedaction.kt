package xyz.ksharma.krail.trip.planner.ui.searchstop

/**
 * Decides whether a raw search query may be attached to analytics.
 *
 * Default is never: typed text can be a street address, which identifies a home or
 * workplace, and the privacy policy promises analytics contain no personally
 * identifiable data. The one carve-out exists for fuzzy-matcher diagnostics: a query
 * that matched nothing anywhere, contains no digits (house and unit numbers are the
 * identifying part of an address), and is short. "townhall" returning zero results
 * stays diagnosable; "4 fulton place" is never sent.
 *
 * Call-site contract: when the address pipeline is eligible for the query, the
 * carve-out decision belongs to the address pipeline's completion site (which knows
 * the real address result count) - the local pipeline must not call this with
 * [addressResultsCount] = null in that case.
 */
object SearchQueryAnalyticsRedaction {

    const val MAX_ZERO_RESULT_QUERY_LENGTH = 25

    /**
     * Returns the trimmed query when every carve-out condition holds, else null.
     *
     * @param localResultsCount   Local stop-search result count for the query.
     * @param addressResultsCount Address-pipeline result count for the same query, or
     *                            null when the address pipeline did not run (flag off
     *                            or below the minimum query length).
     */
    fun zeroResultQueryOrNull(
        query: String,
        localResultsCount: Int,
        addressResultsCount: Int?,
    ): String? {
        val trimmed = query.trim()
        val addressRecognisedNothing = addressResultsCount == null || addressResultsCount == 0
        return trimmed.takeIf {
            localResultsCount == 0 &&
                addressRecognisedNothing &&
                it.isNotEmpty() &&
                it.length <= MAX_ZERO_RESULT_QUERY_LENGTH &&
                it.none(Char::isDigit)
        }
    }
}
