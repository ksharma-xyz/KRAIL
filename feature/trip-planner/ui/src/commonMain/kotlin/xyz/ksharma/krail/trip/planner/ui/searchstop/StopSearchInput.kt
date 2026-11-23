package xyz.ksharma.krail.trip.planner.ui.searchstop

/**
 * What a stop search field accepts: letters, digits, spaces and commas. Search Stop's field and any other field
 * that runs the same local stop search take this one filter, so a query that works in one works in the other.
 */
internal fun stopSearchInputFilter(input: CharSequence): CharSequence =
    input.filter { it.isLetterOrDigit() || it.isWhitespace() || it == ',' }
