package xyz.ksharma.krail.sandook

private const val LIKE_ESCAPE_CHAR = '\\'

private val LIKE_METACHARACTERS = Regex("""[\\%_]""")

private val NON_SEARCHABLE_CHARACTERS = Regex("""[^a-z0-9\\%_ ]""")

/**
 * Builds the `LIKE` pattern used to match a rider's query against a stop name.
 *
 * NSW GTFS stop names are systematically punctuated - "Wollongong Central, Burelli St",
 * "Central Station, Eddy Ave, Stand C" - but riders type them without the commas. Matching the
 * raw query as one contiguous substring therefore fails on every multi-word stop:
 * `'%wollongong central burell%'` does not occur in "Wollongong Central, Burelli St", while
 * `'%wollongong central, burell%'` does. Making the rider guess the punctuation is not a search.
 *
 * So whitespace becomes a wildcard: the query is split into tokens and rejoined with `%`, which
 * spans whatever punctuation the feed happens to use.
 *
 *     "wollongong central burell"  -> "%wollongong%central%burell%"
 *     "wollongong central, burell" -> "%wollongong%central%burell%"   (same pattern)
 *     "central"                    -> "%central%"                     (unchanged from before)
 *
 * Matching stays **order-preserving**, so this does not widen the result set the way an
 * unordered AND-of-LIKEs would; a query whose tokens are in the wrong order is the fuzzy
 * ranker's job, not this one's.
 *
 * Two details worth keeping:
 * - [LIKE_METACHARACTERS] mean something to `LIKE`, so they are escaped with [LIKE_ESCAPE_CHAR]
 *   and the result binds against an `ESCAPE` clause in `NswStops.sq`. A rider typing `%` would
 *   otherwise match every stop in the state.
 * - [NON_SEARCHABLE_CHARACTERS] are replaced with a space rather than deleted, so "st,burelli"
 *   splits into two tokens instead of fusing into "stburelli".
 *
 * Deliberately not reusing `FuzzyStopRanker.normalize()`: that expands abbreviations
 * ("st" to "street"), which would turn `%burell%st%` into `%burell%street%` and stop it
 * matching "Burelli St".
 */
fun stopNameLikePattern(query: String): String {
    val escaped = query.lowercase()
        .replace(LIKE_METACHARACTERS) { match -> "$LIKE_ESCAPE_CHAR${match.value}" }
        .replace(NON_SEARCHABLE_CHARACTERS, " ")

    return escaped.split(" ")
        .filter { it.isNotEmpty() }
        .joinToString(separator = "%", prefix = "%", postfix = "%")
}
