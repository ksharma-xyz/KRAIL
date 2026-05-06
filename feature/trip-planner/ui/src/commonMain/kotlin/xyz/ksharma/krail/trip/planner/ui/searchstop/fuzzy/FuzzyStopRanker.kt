package xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

interface FuzzyStopRanker {
    /**
     * Ranks [candidates] against [query] using fuzzy matching and returns up to [topK] stops
     * sorted by descending relevance score. Stops that score below the length-calibrated threshold
     * are excluded from the result.
     */
    fun rank(
        query: String,
        candidates: List<SearchStopState.SearchResult.Stop>,
        topK: Int = 10,
    ): List<SearchStopState.SearchResult.Stop>
}

class DefaultFuzzyStopRanker : FuzzyStopRanker {
    override fun rank(
        query: String,
        candidates: List<SearchStopState.SearchResult.Stop>,
        topK: Int,
    ): List<SearchStopState.SearchResult.Stop> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty() || candidates.isEmpty()) return emptyList()
        val minScore = minScoreThreshold(normalizedQuery.length)
        return candidates
            .mapNotNull { stop ->
                val score = scoreCandidateName(normalizedQuery, normalize(stop.stopName))
                if (score >= minScore) stop to score else null
            }
            .sortedByDescending { (_, score) -> score }
            .take(topK)
            .map { (stop, _) -> stop }
    }
}

// ── Scoring constants ─────────────────────────────────────────────────────────

private const val SHORT_QUERY_MAX_LEN = 3
private const val MEDIUM_QUERY_MAX_LEN = 6
private const val SHORT_QUERY_THRESHOLD = 0.85
private const val MEDIUM_QUERY_THRESHOLD = 0.55
private const val LONG_QUERY_THRESHOLD = 0.50
private const val CONCAT_SCORE_WEIGHT = 0.9
private const val PREFIX_BONUS = 0.1

// ── Abbreviation dictionary ───────────────────────────────────────────────────
// Only expanded when the token is standalone (handled by normalize's token loop).

internal val ABBREVIATIONS: Map<String, String> = mapOf(
    "rd" to "road",
    "st" to "street",
    "stn" to "station",
    "crn" to "corner",
    "ave" to "avenue",
    "av" to "avenue",
    "pde" to "parade",
    "hwy" to "highway",
    "fwy" to "freeway",
    "dr" to "drive",
    "ct" to "court",
    "pl" to "place",
    "blvd" to "boulevard",
    "sq" to "square",
    "ln" to "lane",
)

// ── Pure functions (internal so commonTest can access them) ───────────────────

/**
 * Lowercases, strips punctuation, collapses whitespace, and expands common
 * transport abbreviations on a per-token basis.
 */
internal fun normalize(s: String): String {
    val stripped = s.lowercase().trim()
        .replace(Regex("[^a-z0-9 ]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    return stripped.split(" ").filter { it.isNotEmpty() }.joinToString(" ") { token ->
        ABBREVIATIONS[token] ?: token
    }
}

/**
 * Standard Levenshtein edit distance with an O(min(|a|,|b|)) rolling-row DP.
 * Exits early when the running row minimum exceeds [maxDistance], returning
 * [maxDistance] + 1 to signal "too far apart" without completing the matrix.
 */
internal fun levenshtein(a: String, b: String, maxDistance: Int = Int.MAX_VALUE): Int {
    if (a == b) return 0
    if (a.isEmpty()) return if (b.length <= maxDistance) b.length else maxDistance + 1
    if (b.isEmpty()) return if (a.length <= maxDistance) a.length else maxDistance + 1
    val (shorter, longer) = if (a.length <= b.length) a to b else b to a
    var prev = IntArray(shorter.length + 1) { it }
    var curr = IntArray(shorter.length + 1)
    for (i in longer.indices) {
        curr[0] = i + 1
        var rowMin = curr[0]
        for (j in shorter.indices) {
            val cost = if (longer[i] == shorter[j]) 0 else 1
            curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            rowMin = minOf(rowMin, curr[j + 1])
        }
        if (rowMin > maxDistance) return maxDistance + 1
        val temp = prev
        prev = curr
        curr = temp
    }
    return prev[shorter.length]
}

/**
 * For each query token, finds the best-matching candidate token using three
 * complementary signals, then averages across all query tokens:
 *
 * - **Prefix** — "schofil" is a prefix of "schofields" → 1.0
 * - **LCS** — "arraw" is the longest common substring of "earraw"/"warrawong" → 5/6
 * - **Edit distance** — 1 − levenshtein / max(|q|, |c|) handles fat-finger swaps
 *
 * A score of 1.0 is a perfect match.
 */
internal fun tokenOverlapScore(query: String, candidate: String): Double {
    val queryTokens = query.split(" ").filter { it.isNotEmpty() }
    val candidateTokens = candidate.split(" ").filter { it.isNotEmpty() }
    if (queryTokens.isEmpty() || candidateTokens.isEmpty()) return 0.0
    val scores = queryTokens.map { qTok ->
        candidateTokens.maxOf { cTok ->
            val prefixScore = when {
                cTok.startsWith(qTok) -> 1.0
                qTok.startsWith(cTok) -> cTok.length.toDouble() / qTok.length
                else -> commonPrefixLength(qTok, cTok).toDouble() / qTok.length
            }
            val maxLen = maxOf(qTok.length, cTok.length)
            val editScore = 1.0 - levenshtein(qTok, cTok, maxLen).toDouble() / maxLen
            val lcsScore = longestCommonSubstringLength(qTok, cTok).toDouble() / qTok.length
            maxOf(prefixScore, editScore, lcsScore).coerceIn(0.0, 1.0)
        }
    }
    return scores.average()
}

/**
 * Detects queries where the user omitted spaces between stop-name words
 * (e.g. "townhall" → "Town Hall"). Each candidate token is searched as a
 * consecutive substring of the query; matched character count divided by the
 * larger of total-candidate-length and query-length gives a [0, 1] score.
 */
internal fun concatSplitScore(query: String, candidate: String): Double {
    val candidateTokens = candidate.split(" ").filter { it.isNotEmpty() }
    val totalLen = candidateTokens.sumOf { it.length }
    if (candidateTokens.isEmpty() || query.isEmpty() || totalLen == 0) return 0.0
    var remaining = query
    var matched = 0
    for (token in candidateTokens) {
        val pos = remaining.indexOf(token)
        if (pos >= 0) {
            matched += token.length
            remaining = remaining.removeRange(pos, pos + token.length)
        }
    }
    return matched.toDouble() / maxOf(totalLen, query.length)
}

/**
 * Combines [tokenOverlapScore] and [concatSplitScore] into a single relevance
 * value in [0, 1.1]. The concat path is weighted slightly below the token path;
 * a small prefix bonus rewards stops whose name literally starts with the query.
 */
internal fun scoreCandidateName(normalizedQuery: String, normalizedCandidate: String): Double {
    val tokenScore = tokenOverlapScore(normalizedQuery, normalizedCandidate)
    val concatScore = concatSplitScore(normalizedQuery, normalizedCandidate)
    val prefixBonus = if (normalizedCandidate.startsWith(normalizedQuery)) PREFIX_BONUS else 0.0
    return maxOf(tokenScore, concatScore * CONCAT_SCORE_WEIGHT) + prefixBonus
}

/**
 * Minimum score required to surface a candidate. Shorter queries are noisier
 * so they get a tighter gate; longer queries contain more signal and can tolerate
 * a looser threshold without surfacing irrelevant stops.
 */
internal fun minScoreThreshold(queryLength: Int): Double = when {
    queryLength <= SHORT_QUERY_MAX_LEN -> SHORT_QUERY_THRESHOLD
    queryLength <= MEDIUM_QUERY_MAX_LEN -> MEDIUM_QUERY_THRESHOLD
    else -> LONG_QUERY_THRESHOLD
}

private fun commonPrefixLength(a: String, b: String): Int {
    var i = 0
    while (i < a.length && i < b.length && a[i] == b[i]) i++
    return i
}

/**
 * Returns the length of the longest contiguous substring common to [a] and [b].
 * O(|a|×|b|) time and space — safe for the short stop-name tokens used here.
 */
private fun longestCommonSubstringLength(a: String, b: String): Int {
    if (a.isEmpty() || b.isEmpty()) return 0
    var maxLen = 0
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            if (a[i - 1] == b[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1] + 1
                maxLen = maxOf(maxLen, dp[i][j])
            }
        }
    }
    return maxLen
}
