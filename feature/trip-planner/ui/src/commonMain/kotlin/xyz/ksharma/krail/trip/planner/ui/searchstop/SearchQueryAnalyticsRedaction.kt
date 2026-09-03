package xyz.ksharma.krail.trip.planner.ui.searchstop

/**
 * Turns a raw search query into the only form of it allowed to leave the device.
 *
 * From 1.27 the privacy policy discloses that search text is kept to improve search, so
 * the query is attached to every settled search rather than only to the ones that found
 * nothing. What the policy also promises is that house and unit numbers are masked, and
 * that promise is kept **here**, on the device, rather than downstream: analytics goes
 * straight to Firebase from [xyz.ksharma.krail.core.analytics.RealAnalytics], so anything
 * this function returns is stored as-is by a third party. A masking step further down the
 * pipeline would be masking data that had already been sent.
 *
 * ```
 * "4 fulton place" -> "# fulton place"
 * "T80"            -> "T##"
 * "861"            -> "861"      // all digits, see below
 * "wynyard"        -> "wynyard"
 * ```
 *
 * A house number is the part of an address that identifies a home; the street is what the
 * fuzzy stop matcher needs to be fixed. Masking keeps the second and destroys the first.
 *
 * **An all-digit query is sent as typed.** Riders search bus and train route numbers and
 * stop IDs, and those are digits with nothing else in them. A number on its own is not an
 * address: a house number identifies a home only in combination with the street beside it,
 * and there is no street here to combine with. Masking these would erase a whole class of
 * search - "861" and "200060" would arrive as "###" and "######", indistinguishable from
 * each other and from every other number a rider types.
 *
 * The moment a query carries anything that is not a digit, it can carry a street, so every
 * digit in it is masked.
 *
 * The length cap stays for the same reason it existed before masking: a long query carries
 * street and suburb together even with no digits in it, and that pair is identifying in a
 * way a stop name is not.
 */
object SearchQueryAnalyticsRedaction {

    const val MAX_QUERY_LENGTH = 40

    /** Stands in for every digit, so "12" and "99" are indistinguishable but both visible. */
    const val DIGIT_MASK = '#'

    /**
     * Returns the trimmed query - unchanged when it is all digits, digit-masked otherwise -
     * or null when there is nothing safe to send.
     *
     * Null means blank (nothing typed) or longer than [MAX_QUERY_LENGTH]. Result counts do
     * not appear here: whether a query found something is no longer part of the decision.
     */
    fun maskedQueryOrNull(query: String): String? {
        val trimmed = query.trim()
        return when {
            trimmed.isEmpty() || trimmed.length > MAX_QUERY_LENGTH -> null
            trimmed.all(Char::isDigit) -> trimmed
            else -> trimmed.map { if (it.isDigit()) DIGIT_MASK else it }
                .joinToString(separator = "")
        }
    }
}
