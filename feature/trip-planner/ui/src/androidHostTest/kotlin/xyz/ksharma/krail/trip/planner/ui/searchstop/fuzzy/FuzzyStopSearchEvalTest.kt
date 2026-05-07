package xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Evaluation framework for the fuzzy stop ranker against all 37,208 real NSW stops.
 *
 * Lives in [androidHostTest] (not [commonTest]) because it depends on the JVM
 * classloader and [java.io.File] to load the eval CSV — both unavailable on iOS.
 * The scoring functions under test are platform-neutral and live in commonMain.
 *
 * ┌─ Quick start ───────────────────────────────────────────────────────────┐
 * │ 1. Generate the stop data (once, from project root):                    │
 * │      python3 scripts/extract_nsw_stops.py                               │
 * │                                                                         │
 * │ 2. Run the eval:                                                        │
 * │      ./gradlew :feature:trip-planner:ui:testAndroidHostTest \           │
 * │        --tests "*.FuzzyStopSearchEvalTest"                              │
 * │                                                                         │
 * │ 3. Add a new query: append to [cases] (known good) or [discoveryQueries]│
 * │    (explore first without asserting, then promote to [cases]).           │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Cases are derived from 60-day zero-result analytics (real user queries) plus
 * false-positive guards from manual QA.
 *
 * Scoring:
 *   Each case check (include / exclude) is one point.
 *   Test fails if overall score drops below 80%.
 */
class FuzzyStopSearchEvalTest {

    /**
     * A single quality assertion.
     *
     * @param query   The raw search string exactly as typed by the user.
     * @param include Stop name fragments that MUST appear in the top [topK] results.
     *                Checked as case-insensitive contains — "Blacktown" matches
     *                "Blacktown Station", "Blacktown Station, Stand A", etc.
     * @param exclude Stop name fragments that must NOT appear anywhere in the results.
     * @param topK    Result window for [include] checks (default 5).
     */
    data class EvalCase(
        val query: String,
        val include: List<String> = emptyList(),
        val exclude: List<String> = emptyList(),
        val topK: Int = 5,
    )

    // ═════════════════════════════════════════════════════════════════════════
    // EVAL CASES
    // To add a case: append here. Run the eval to see pass/fail.
    // Use [discoveryQueries] below to explore results for new queries first.
    // ═════════════════════════════════════════════════════════════════════════

    private val cases = listOf(

        // ── Progressive typing: Town Hall ─────────────────────────────────────
        // Analytics: townh / townha / townhal / townhall all returned zero results
        EvalCase("town",     include = listOf("Town Hall")),
        EvalCase("townh",    include = listOf("Town Hall")),
        EvalCase("townha",   include = listOf("Town Hall")),
        EvalCase("townhal",  include = listOf("Town Hall")),
        EvalCase("townhall", include = listOf("Town Hall")),
        EvalCase("town hall", include = listOf("Town Hall Station")),

        // ── Progressive typing: Schofields ────────────────────────────────────
        EvalCase("scho",     include = listOf("Schofields")),
        EvalCase("schoi",    include = listOf("Schofields")),
        EvalCase("schofi",   include = listOf("Schofields")),
        EvalCase("schofil",  include = listOf("Schofields")),
        EvalCase("schofild", include = listOf("Schofields")),
        EvalCase("schofld",  include = listOf("Schofields")),    // vowel drop
        EvalCase("schofields", include = listOf("Schofields Station")),

        // ── Progressive typing: Wollongong ────────────────────────────────────
        EvalCase("wollo",      include = listOf("Wollongong")),
        EvalCase("wollongo",   include = listOf("Wollongong")),
        EvalCase("wollongong", include = listOf("Wollongong Station")),

        // ── Progressive typing: Bella Vista ──────────────────────────────────
        // Analytics: bellav returned zero results
        EvalCase("bellav",        include = listOf("Bella Vista")),
        EvalCase("bella vista",   include = listOf("Bella Vista Station")),
        EvalCase("bella vista m", include = listOf("Bella Vista Station")),

        // ── Progressive typing: Warrawong via wrong first syllable ────────────
        // Analytics: earraw / earra / earrawo / earrawon / earrawong all zero
        // "earra"/"earraw" LCS-match both "warrawong" and "warrawee"/"narraweena" equally;
        // wider window is needed until a longer prefix resolves the ambiguity.
        EvalCase("earra",    include = listOf("Warrawong"), topK = 10),
        EvalCase("earraw",   include = listOf("Warrawong"), topK = 10),
        EvalCase("earrawo",  include = listOf("Warrawong")),
        EvalCase("earrawon", include = listOf("Warrawong")),

        // ── Progressive typing: Warrawong via alternate spelling ──────────────
        // Analytics: wareawo / wareawon / wareawong / warrawonngg all zero
        EvalCase("wareawo",    include = listOf("Warrawong")),
        EvalCase("wareawon",   include = listOf("Warrawong")),
        EvalCase("wareawong",  include = listOf("Warrawong")),
        EvalCase("warrawonngg", include = listOf("Warrawong")),

        // ── Progressive typing: Cowper Street ────────────────────────────────
        // Analytics: cowper street / cowpe st / cowper str all returned zero results
        EvalCase("cowpe st",      include = listOf("Cowper St")),
        EvalCase("cowper str",    include = listOf("Cowper St")),
        EvalCase("cowper stree",  include = listOf("Cowper St")),
        EvalCase("cowper street", include = listOf("Cowper St"), exclude = listOf("St James Station")),

        // ── Progressive typing: Port Kembla ───────────────────────────────────
        EvalCase("port kembla",  include = listOf("Port Kembla")),
        EvalCase("port kem",     include = listOf("Port Kembla")),
        EvalCase("portkembla",   include = listOf("Port Kembla")),   // concatenated

        // ── Typos / fat finger / transpositions ───────────────────────────────
        // All from zero-result analytics
        EvalCase("blackyown",   include = listOf("Blacktown")),
        EvalCase("cebtral",     include = listOf("Central Station")),
        EvalCase("wollong9ng",  include = listOf("Wollongong")),
        // "woll9n": "9" edit-matches "i" → "wollin" (Wollin Pl) ranks above "wollongong".
        // This is an inherent keyboard-proximity ambiguity; explore via discoveryQueries.
        EvalCase("paramatta",   include = listOf("Parramatta")),
        EvalCase("kingsfprd",   include = listOf("Kingsford")),
        EvalCase("bella visa",  include = listOf("Bella Vista")),
        EvalCase("wewollongong", include = listOf("Wollongong")),
        EvalCase("macquire",    include = listOf("Macquarie")),
        EvalCase("hustvill",    include = listOf("Hurstville")),
        EvalCase("lidcome",     include = listOf("Lidcombe")),
        EvalCase("strarhfi",    include = listOf("Strathfield")),
        EvalCase("sevenhi",     include = listOf("Seven Hills")),
        EvalCase("crowsnest",   include = listOf("Crows Nest")),

        // ── Abbreviations ─────────────────────────────────────────────────────
        // Analytics: third avenue / highfield roa / highfield ro all zero results
        EvalCase("third avenue",  include = listOf("Third Ave")),
        EvalCase("third aven",    include = listOf("Third Ave")),
        EvalCase("third avenu",   include = listOf("Third Ave")),
        EvalCase("highfield road", include = listOf("Highfield Rd")),
        EvalCase("highfield roa",  include = listOf("Highfield Rd")),
        EvalCase("highfield ro",   include = listOf("Highfield Rd")),

        // ── Existing working queries (regression guard) ────────────────────────
        EvalCase("central",     include = listOf("Central Station")),
        EvalCase("wollongong",  include = listOf("Wollongong Station")),
        EvalCase("blacktown",   include = listOf("Blacktown Station")),
        EvalCase("parramatta",  include = listOf("Parramatta Station")),
        EvalCase("schofields",  include = listOf("Schofields Station")),

        // ── Multi-token + typo (zero-result analytics) ────────────────────────
        // Real queries that returned nothing in 60-day analytics; promoted from
        // the discovery list once the ranker handled them.
        EvalCase("wollngong hospital", include = listOf("Wollongong Hospital")),
        EvalCase("bella vista metr",   include = listOf("Bella Vista")),
        EvalCase("livwrpoo",           include = listOf("Liverpool")),
        EvalCase("eouse hill",         include = listOf("Rouse Hill")),
        EvalCase("sudney",             include = listOf("Sydney")),
        EvalCase("sudne",              include = listOf("Sydney")),

        // ── False-positive guards ──────────────────────────────────────────────
        EvalCase(
            query   = "blacktwon ro",
            include = listOf("Blacktown"),
            exclude = listOf("Barangaroo"),
        ),
        EvalCase(
            query   = "blacktown road",
            include = listOf("Blacktown"),
            exclude = listOf("Alison Park Blackwall"),
        ),
        EvalCase(
            query   = "pitt st",
            include = listOf("Pitt St"),
            exclude = listOf("St James Station"),
        ),
    )

    // ═════════════════════════════════════════════════════════════════════════
    // DISCOVERY
    //
    // Use this list to ask "what would the user actually see if they typed X?"
    // Add a string here, run the test below, and the top-10 results print to
    // stdout — no assertions, no commit needed if you're just exploring.
    //
    // Workflow:
    //   1. Add the query string to [discoveryQueries].
    //   2. Run: ./gradlew :feature:trip-planner:ui:testAndroidHostTest \
    //              --tests "*.FuzzyStopSearchEvalTest.discover*"
    //   3. Inspect the stdout. If results look right, promote to [cases]
    //      above with an EvalCase(...) and remove from this list.
    //   4. If results look wrong, leave a comment explaining the ambiguity
    //      so future contributors don't re-investigate.
    //
    // Examples (all uncommented entries print on next test run):
    // ═════════════════════════════════════════════════════════════════════════

    private val discoveryQueries: List<String> = listOf(
        // "woll9n": "9"→"i" matches "Wollin Pl" before "Wollongong"; ambiguous without context.
        "woll9n",
        // Uncomment these to explore — they're real zero-result analytics queries
        // we haven't yet asserted on. If they look right, promote to [cases].
        // "withers road after milford",
        // "unsw hifh",
        // "pist office",
        // "stockland sho",
        // "shellharbour sto",
        // "carrigto",
        // "sevenhi",
        // "spit b",
    )

    // ═════════════════════════════════════════════════════════════════════════
    // FRAMEWORK (do not edit below unless changing the scoring logic)
    // ═════════════════════════════════════════════════════════════════════════

    private val allStops: List<SearchStopState.SearchResult.Stop>? by lazy { loadStops() }

    /**
     * Pre-normalized candidate names so we only pay the normalize cost once
     * across all queries, not once per query × candidate.
     */
    private val preNormalized: List<Pair<SearchStopState.SearchResult.Stop, String>>? by lazy {
        allStops?.map { stop -> stop to normalize(stop.stopName, expandAbbreviations = false) }
    }

    private fun loadStops(): List<SearchStopState.SearchResult.Stop>? {
        val lines: List<String>? = readFromClasspath() ?: readFromFilesystem()
        if (lines == null) return null
        return lines.drop(1)  // skip header
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val cols = line.split("|")
                if (cols.size < 2) return@mapNotNull null
                SearchStopState.SearchResult.Stop(
                    stopId = cols[0],
                    stopName = cols[1],
                    transportModeType = persistentListOf(),
                )
            }
    }

    private fun readFromClasspath(): List<String>? =
        this::class.java.classLoader
            ?.getResourceAsStream("nsw_stops_eval.csv")
            ?.bufferedReader()
            ?.readLines()

    private fun readFromFilesystem(): List<String>? {
        val candidates = listOf(
            // relative to module root (typical Gradle test working dir)
            "src/androidHostTest/resources/nsw_stops_eval.csv",
            // relative to project root (when running from root)
            "feature/trip-planner/ui/src/androidHostTest/resources/nsw_stops_eval.csv",
        )
        return candidates
            .map { java.io.File(it) }
            .firstOrNull { it.exists() }
            ?.readLines()
    }

    /**
     * Rank all loaded stops against [query].
     * Uses pre-normalized candidate names to avoid repeated normalize() calls.
     */
    private fun rankAll(query: String): List<SearchStopState.SearchResult.Stop> {
        val pre = preNormalized ?: return emptyList()
        val nq = normalize(query)
        if (nq.isEmpty()) return emptyList()
        val threshold = minScoreThreshold(nq.length)
        return pre
            .mapNotNull { (stop, normalizedName) ->
                val score = scoreCandidateName(nq, normalizedName)
                if (score >= threshold) stop to score else null
            }
            .sortedByDescending { (_, score) -> score }
            .take(50)
            .map { (stop, _) -> stop }
    }

    @Test
    fun `eval fuzzy stop quality against real NSW stops`() {
        val stops = allStops
        if (stops == null) {
            println(
                "\nSKIP: nsw_stops_eval.csv not found.\n" +
                "Generate it by running from the project root:\n" +
                "  python3 scripts/extract_nsw_stops.py\n"
            )
            return
        }

        println("\n=== Fuzzy Stop Search Eval (${stops.size} real NSW stops) ===\n")

        var totalChecks = 0
        var passedChecks = 0
        val failures = mutableListOf<String>()

        cases.forEach { case ->
            val results = rankAll(case.query)
            val topK = results.take(case.topK).map { it.stopName }
            val allNames = results.map { it.stopName }
            val caseFailures = mutableListOf<String>()

            case.include.forEach { fragment ->
                val hit = topK.any { it.contains(fragment, ignoreCase = true) }
                totalChecks++
                if (hit) passedChecks++ else caseFailures += "include \"$fragment\" not in top ${case.topK}"
            }

            case.exclude.forEach { fragment ->
                val appeared = allNames.firstOrNull { it.contains(fragment, ignoreCase = true) }
                totalChecks++
                if (appeared == null) passedChecks++ else caseFailures += "exclude \"$fragment\" appeared: \"$appeared\""
            }

            val icon = if (caseFailures.isEmpty()) "✅" else "❌"
            println("$icon  \"${case.query}\"")
            if (caseFailures.isNotEmpty()) {
                caseFailures.forEach { println("     ↳ $it") }
                println("     ↳ Top ${minOf(3, topK.size)}: ${topK.take(3)}")
                failures += caseFailures.map { "\"${case.query}\" — $it" }
            }
        }

        val score = if (totalChecks > 0) passedChecks * 100.0 / totalChecks else 100.0
        println("\n═══════════════════════════════════════════════")
        println("  Score: $passedChecks / $totalChecks checks  (${score.toInt()}%)")
        println("═══════════════════════════════════════════════\n")

        assertTrue(
            score >= 80.0,
            buildString {
                appendLine("Eval score ${score.toInt()}% is below 80%.")
                appendLine("Failing checks:")
                failures.forEach { appendLine("  • $it") }
            }
        )
    }

    @Test
    fun `discover new queries against real NSW stops`() {
        if (discoveryQueries.isEmpty()) return
        val stops = allStops
        if (stops == null) {
            println("SKIP: nsw_stops_eval.csv not found. Run: python3 scripts/extract_nsw_stops.py")
            return
        }
        println("\n=== Discovery (${stops.size} real NSW stops) ===\n")
        discoveryQueries.forEach { query ->
            val results = rankAll(query)
            println("\"$query\" → ${results.size} results")
            results.take(10).forEachIndexed { i, stop -> println("  ${i + 1}. ${stop.stopName}") }
            println()
        }
    }
}
