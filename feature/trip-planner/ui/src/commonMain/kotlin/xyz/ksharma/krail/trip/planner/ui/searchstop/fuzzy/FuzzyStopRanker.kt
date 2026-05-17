// This file is a single cohesive scoring module: a set of small, tightly-coupled pure
// functions (normalize, levenshtein, per-token / concat / sequence scoring) that only make
// sense together. Splitting them across files to satisfy the per-file function count would
// scatter the algorithm and hurt readability, so TooManyFunctions is suppressed here.
@file:Suppress("TooManyFunctions")

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
                val score = scoreCandidateName(normalizedQuery, normalize(stop.stopName, expandAbbreviations = false))
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

// Raised from 0.50 → 0.60: at 0.50, near-homophones such as "Cooper Park" (~0.55) and
// "Cowpasture Rd" (~0.55) cleared the gate for "cowper street" and polluted the results.
// Genuine matches for long queries score well above this (real "Cowper St …" stops ≈ 1.04),
// so 0.60 removes the noise without dropping legitimate fuzzy hits. Guarded by the 37k eval.
private const val LONG_QUERY_THRESHOLD = 0.60
private const val CONCAT_SCORE_WEIGHT = 0.9
private const val PREFIX_BONUS = 0.1
private const val MIN_TOKEN_QUALITY = 0.6

// Small bonus that prefers shorter/more-specific candidates when base scores tie.
// E.g. "Wollongong Station" (2 tokens) over "Wollongong Rd opp Earle St" (5 tokens).
private const val SPECIFICITY_WEIGHT = 0.1

// Word-order bonus for multi-token queries. Bag-of-words scoring ties every stop
// containing the query words regardless of position — "Cowper St at Prince St" (the road
// itself) scores the same as "Connolly Park, Cowper St" (a landmark with Cowper St as a
// cross-street) and even "Page St at Cowper Ave" (wrong road, "st" borrowed from "Page St").
// These tiers reward query tokens that appear in order in the candidate:
//  - LEADING: in order, contiguous, and the run starts at candidate token 0 → the name *is*
//    that road ("Cowper St at …"). Largest bonus.
//  - CONTIGUOUS: in order and adjacent but starting later ("Connolly Park, Cowper St").
//  - IN_ORDER: in order but with gaps ("Cowper before Parkes St").
//  - out of order ("Page St at Cowper Ave") → no bonus, sinks below all genuine matches.
private const val SEQUENCE_LEADING_BONUS = 0.30
private const val SEQUENCE_CONTIGUOUS_BONUS = 0.12
private const val SEQUENCE_IN_ORDER_BONUS = 0.04

// LCS is unreliable for very short query tokens (e.g. "ro" scoring 1.0 inside "barangaroo"
// because the 2-char LCS fills the entire query token length). Only apply LCS when the
// query token is long enough to be discriminative.
private const val LCS_MIN_QUERY_TOKEN_LEN = 3

// ── Abbreviation dictionary ───────────────────────────────────────────────────
// Only expanded when the token is standalone (handled by normalize's token loop).

internal val ABBREVIATIONS: Map<String, String> = mapOf(
    "ally" to "alley",
    "arc" to "arcade",
    "ave" to "avenue",
    "blvd" to "boulevard",
    "cct" to "circuit",
    "cl" to "close",
    "crn" to "corner",
    "ct" to "court",
    "dr" to "drive",
    "esp" to "esplanade",
    "fwy" to "freeway",
    "gdns" to "gardens",
    "gr" to "grove",
    "hwy" to "highway",
    "int" to "interchange",
    "ln" to "lane",
    "pde" to "parade",
    "pkway" to "parkway",
    "pl" to "place",
    "prom" to "promenade",
    "pwy" to "parkway",
    "rd" to "road",
    "res" to "reserve",
    "rt" to "retreat",
    "sq" to "square",
    "st" to "street",
    "stn" to "station",
    "tce" to "terrace",
    "tk" to "track",
    "tway" to "transitway", // NSW T-Way / Transitway bus stops
    "wlk" to "walk",
)

// ── Pure functions (internal so commonTest can access them) ───────────────────

/**
 * Lowercases, strips punctuation, collapses whitespace, and expands common
 * transport abbreviations on a per-token basis.
 *
 * When [expandAbbreviations] is true, "st" at position 0 is left unexpanded
 * because leading "St" is a saint prefix ("St James", "St Leonards"), not a
 * street abbreviation — expanding it causes false positives for unrelated queries.
 */
internal fun normalize(s: String, expandAbbreviations: Boolean = true): String {
    val stripped = s.lowercase().trim()
        .replace(Regex("[^a-z0-9 ]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (!expandAbbreviations) return stripped
    return stripped.split(" ").filter { it.isNotEmpty() }.mapIndexed { index, token ->
        if (token == "st" && index == 0) token else ABBREVIATIONS[token] ?: token
    }.joinToString(" ")
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
 * Returns the per-query-token best-match score against [candidate].
 * Each element is the max of prefix, edit-distance, and LCS signals for that token.
 *
 * Abbreviation reverse-matching (e.g. candidate "st" → query "street") is intentionally
 * excluded here and applied later, after the multi-token gate, in [scoreCandidateName].
 * Including it here would prevent the gate from catching cases like "blacktown road" vs
 * "Alison Park Blackwall Point Rd" — the gate needs the raw pre-abbrev scores.
 */
private fun perTokenScores(query: String, candidate: String): List<Double> {
    val queryTokens = query.split(" ").filter { it.isNotEmpty() }
    val candidateTokens = candidate.split(" ").filter { it.isNotEmpty() }
    if (queryTokens.isEmpty() || candidateTokens.isEmpty()) return emptyList()
    return queryTokens.map { qTok ->
        candidateTokens.maxOf { cTok -> singleTokenScore(qTok, cTok) }
    }
}

/**
 * Max of prefix, edit-distance, and LCS signals for one query token vs one candidate
 * token. Factored out of [perTokenScores] so [sequenceBonus] can reuse the exact same
 * notion of "does this query token match this candidate token".
 */
private fun singleTokenScore(qTok: String, cTok: String): Double {
    val prefixScore = when {
        cTok.startsWith(qTok) -> 1.0
        qTok.startsWith(cTok) -> cTok.length.toDouble() / qTok.length
        else -> commonPrefixLength(qTok, cTok).toDouble() / qTok.length
    }
    val maxLen = maxOf(qTok.length, cTok.length)
    val editScore = 1.0 - levenshtein(qTok, cTok, maxLen).toDouble() / maxLen
    val lcsScore = if (qTok.length < LCS_MIN_QUERY_TOKEN_LEN) {
        0.0
    } else {
        longestCommonSubstringLength(qTok, cTok).toDouble() / qTok.length
    }
    return maxOf(prefixScore, editScore, lcsScore).coerceIn(0.0, 1.0)
}

/**
 * Word-order bonus. Greedily aligns each query token to the **earliest** later candidate
 * token it matches (per-token score ≥ [MIN_TOKEN_QUALITY], or an abbreviation reverse-match
 * such as candidate "st" ⇒ query "street" at candidate position > 0). Returns 0 for
 * single-token queries (leading is already handled by the prefix bonus) and for any query
 * whose tokens cannot all be matched in increasing order.
 *
 * - all matched, contiguous, run starts at candidate token 0 → [SEQUENCE_LEADING_BONUS]
 * - all matched, contiguous, starts later                    → [SEQUENCE_CONTIGUOUS_BONUS]
 * - all matched, in order but with gaps                       → [SEQUENCE_IN_ORDER_BONUS]
 */
internal fun sequenceBonus(normalizedQuery: String, normalizedCandidate: String): Double {
    val queryTokens = normalizedQuery.split(" ").filter { it.isNotEmpty() }
    val candidateTokens = normalizedCandidate.split(" ").filter { it.isNotEmpty() }
    if (queryTokens.size < 2 || candidateTokens.isEmpty()) return 0.0

    val matchedIndices = ArrayList<Int>(queryTokens.size)
    var searchFrom = 0
    for (qTok in queryTokens) {
        val foundAt = (searchFrom until candidateTokens.size).firstOrNull { cIdx ->
            val cTok = candidateTokens[cIdx]
            (cIdx > 0 && ABBREVIATIONS[cTok] == qTok) ||
                singleTokenScore(qTok, cTok) >= MIN_TOKEN_QUALITY
        }
        if (foundAt == null) {
            matchedIndices.clear() // a query token has no in-order match → not a sequence
            break
        }
        matchedIndices += foundAt
        searchFrom = foundAt + 1
    }

    val aligned = matchedIndices.size == queryTokens.size
    val contiguous = aligned && matchedIndices.zipWithNext().all { (a, b) -> b == a + 1 }
    return when {
        !aligned -> 0.0
        contiguous && matchedIndices.first() == 0 -> SEQUENCE_LEADING_BONUS
        contiguous -> SEQUENCE_CONTIGUOUS_BONUS
        else -> SEQUENCE_IN_ORDER_BONUS
    }
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
    val scores = perTokenScores(query, candidate)
    return if (scores.isEmpty()) 0.0 else scores.average()
}

/**
 * Detects queries where the user omitted spaces between stop-name words
 * (e.g. "townhall" → "Town Hall"). Each candidate token is searched as a
 * consecutive substring of the query; matched character count divided by
 * query length measures how fully the query is explained by candidate tokens.
 *
 * Using query length (not max of query/candidate) avoids penalising candidates
 * with many tokens beyond what the query covers — "townhall" matching "town"+"hall"
 * in "town hall station" should score 8/8 = 1.0, not 8/15.
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
    return matched.toDouble() / query.length
}

/**
 * Combines [tokenOverlapScore] and [concatSplitScore] into a single relevance
 * value. The concat path is weighted slightly below the token path; a small
 * prefix bonus rewards stops whose name literally starts with the query; and a
 * specificity bonus prefers candidates with fewer tokens (more specific names)
 * when other scores are equal.
 *
 * Multi-token quality gate: for queries with ≥ 2 tokens, a candidate is rejected
 * outright (score 0) if 2 or more query tokens each score below [MIN_TOKEN_QUALITY]
 * against every candidate token. This prevents weak partial overlaps from
 * accumulating into a passing average (e.g. "blackwall" ≈ "blacktown" at 0.56
 * combined with "rd" ≈ "road" at 0.50 averaging to 0.53 — above the 0.50 threshold
 * but clearly an irrelevant stop).
 *
 * Token scores are length-weighted so a 9-char token like "blacktwon" dominates a
 * 2-char partial word like "ro". Without this weighting, a transposition typo
 * ("blacktwon ro" → "blacktown") would average to 0.44 and fall below threshold.
 */
internal fun scoreCandidateName(normalizedQuery: String, normalizedCandidate: String): Double {
    val perToken = perTokenScores(normalizedQuery, normalizedCandidate)
    val gateBlocked = perToken.isEmpty() ||
        (perToken.size >= 2 && perToken.count { it < MIN_TOKEN_QUALITY } >= 2)
    if (gateBlocked) return 0.0
    val queryTokens = normalizedQuery.split(" ").filter { it.isNotEmpty() }
    val candidateTokens = normalizedCandidate.split(" ").filter { it.isNotEmpty() }
    // Abbreviation reverse-match boost (applied after gate): if a candidate token at position > 0
    // is an abbreviation that expands to the query token, score 1.0. Position 0 is excluded because
    // leading "St" is a saint prefix, not a street abbreviation ("St James" ≠ "Street James").
    val perTokenBoosted = perToken.zip(queryTokens).map { (score, qTok) ->
        val abbrevMatch = candidateTokens.drop(1).any { cTok -> ABBREVIATIONS[cTok] == qTok }
        if (abbrevMatch) maxOf(score, 1.0) else score
    }
    val totalLen = queryTokens.sumOf { it.length }.toDouble()
    val tokenScore = if (totalLen == 0.0 || queryTokens.size != perTokenBoosted.size) {
        perTokenBoosted.average()
    } else {
        perTokenBoosted.zip(queryTokens).sumOf { (score, tok) -> score * tok.length } / totalLen
    }
    val concatScore = concatSplitScore(normalizedQuery, normalizedCandidate)
    val prefixBonus = if (normalizedCandidate.startsWith(normalizedQuery)) PREFIX_BONUS else 0.0
    // Prefer candidates with fewer tokens — "Wollongong Station" (2) over "Wollongong Rd opp …" (5).
    val specificityBonus = queryTokens.size.toDouble() /
        maxOf(queryTokens.size, candidateTokens.size) * SPECIFICITY_WEIGHT
    // Word-order: lifts "Cowper St at Prince St" (Cowper St is the road, tokens 0-1) above
    // "Connolly Park, Cowper St" (trailing cross-street) and drops "Page St at Cowper Ave"
    // (out of order — "st" came from "Page St", not adjacent to "cowper").
    val sequenceBonus = sequenceBonus(normalizedQuery, normalizedCandidate)
    return maxOf(tokenScore, concatScore * CONCAT_SCORE_WEIGHT) +
        prefixBonus + specificityBonus + sequenceBonus
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
