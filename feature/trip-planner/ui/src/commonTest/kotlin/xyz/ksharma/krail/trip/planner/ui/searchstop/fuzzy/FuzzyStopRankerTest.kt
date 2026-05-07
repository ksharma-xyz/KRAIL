package xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime

class FuzzyStopRankerTest {

    private val ranker = DefaultFuzzyStopRanker()

    // region normalize

    @Test
    fun `normalize lowercases and trims`() {
        assertEquals("central station", normalize("  Central Station  "))
    }

    @Test
    fun `normalize strips punctuation`() {
        assertEquals("kings cross", normalize("King's Cross"))
    }

    @Test
    fun `normalize collapses whitespace`() {
        assertEquals("town hall", normalize("Town  Hall"))
    }

    @Test
    fun `normalize expands rd abbreviation as standalone token`() {
        assertEquals("york road", normalize("York Rd"))
    }

    @Test
    fun `normalize expands st abbreviation as standalone token`() {
        assertEquals("pitt street", normalize("Pitt St"))
    }

    @Test
    fun `normalize does not expand st inside a word`() {
        assertEquals("station", normalize("Station"))
    }

    @Test
    fun `normalize expands stn to station`() {
        assertEquals("central station", normalize("Central Stn"))
    }

    @Test
    fun `normalize expands ave to avenue`() {
        assertEquals("third avenue", normalize("Third Ave"))
    }

    @Test
    fun `normalize handles empty string`() {
        assertEquals("", normalize(""))
    }

    @Test
    fun `normalize handles digits in input`() {
        assertEquals("wollong9ng", normalize("wollong9ng"))
    }

    @Test
    fun `normalize strips non ascii punctuation`() {
        assertEquals("oconnell street", normalize("O'Connell Street"))
    }

    @Test
    fun `normalize does not expand leading st as saint prefix`() {
        assertEquals("st james", normalize("St James"))
    }

    // endregion

    // region levenshtein

    @Test
    fun `levenshtein identical strings returns 0`() {
        assertEquals(0, levenshtein("wollongong", "wollongong"))
    }

    @Test
    fun `levenshtein single substitution`() {
        assertEquals(1, levenshtein("blackyown", "blacktown"))
    }

    @Test
    fun `levenshtein single substitution digit for letter`() {
        assertEquals(1, levenshtein("wollong9ng", "wollongong"))
    }

    @Test
    fun `levenshtein single substitution cebtral to central`() {
        assertEquals(1, levenshtein("cebtral", "central"))
    }

    @Test
    fun `levenshtein empty first string returns length of second`() {
        assertEquals(5, levenshtein("", "hello", maxDistance = 10))
    }

    @Test
    fun `levenshtein empty second string returns length of first`() {
        assertEquals(5, levenshtein("hello", "", maxDistance = 10))
    }

    @Test
    fun `levenshtein single char mismatch`() {
        assertEquals(1, levenshtein("a", "b"))
    }

    @Test
    fun `levenshtein maxDistance early exit`() {
        val result = levenshtein("completely", "different", maxDistance = 2)
        assertTrue(result > 2, "Expected early-exit value > maxDistance but got $result")
    }

    @Test
    fun `levenshtein prefix truncation earraw vs warrawong`() {
        assertEquals(1, levenshtein("earrawong", "warrawong"))
    }

    // endregion

    // region tokenOverlapScore

    @Test
    fun `tokenOverlapScore perfect match returns 1`() {
        assertEquals(1.0, tokenOverlapScore("central", "central"), absoluteTolerance = 0.01)
    }

    @Test
    fun `tokenOverlapScore prefix typing returns high score`() {
        // "scho" is a 4-char common prefix → LCS 4/5 = 0.8 minimum
        val score = tokenOverlapScore("schoi", "schofields")
        assertTrue(score >= 0.75, "Expected score >= 0.75 for prefix typing but got $score")
    }

    @Test
    fun `tokenOverlapScore single typo returns high score`() {
        val score = tokenOverlapScore("blackyown", "blacktown")
        assertTrue(score >= 0.8, "Expected score >= 0.8 for one-char typo but got $score")
    }

    @Test
    fun `tokenOverlapScore empty query returns 0`() {
        assertEquals(0.0, tokenOverlapScore("", "central"))
    }

    @Test
    fun `tokenOverlapScore empty candidate returns 0`() {
        assertEquals(0.0, tokenOverlapScore("central", ""))
    }

    @Test
    fun `tokenOverlapScore multi token prefix typing`() {
        // "bella" and "vista" match perfectly; "m" has no good match → average ≥ 0.60
        val score = tokenOverlapScore("bella vista m", "bella vista station")
        assertTrue(score >= 0.60, "Expected >= 0.60 for multi-token prefix but got $score")
    }

    // endregion

    // region concatSplitScore

    @Test
    fun `concatSplitScore concatenated words score high`() {
        val score = concatSplitScore("townhall", "town hall")
        assertTrue(score >= 0.9, "Expected >= 0.9 for townhall/town hall but got $score")
    }

    @Test
    fun `concatSplitScore partial concatenation scores above zero`() {
        val score = concatSplitScore("townha", "town hall")
        assertTrue(score > 0.0, "Expected > 0 for partial concat but got $score")
    }

    @Test
    fun `concatSplitScore prefix in single token candidate`() {
        val score = concatSplitScore("wewollongong", "wollongong")
        assertTrue(score >= 0.7, "Expected >= 0.7 for wewollongong/wollongong but got $score")
    }

    @Test
    fun `concatSplitScore empty query returns 0`() {
        assertEquals(0.0, concatSplitScore("", "town hall"))
    }

    @Test
    fun `concatSplitScore empty candidate returns 0`() {
        assertEquals(0.0, concatSplitScore("townhall", ""))
    }

    // endregion

    // region golden cases (derived from 60-day zero-result analytics snapshot)

    private fun candidatePool(): List<SearchStopState.SearchResult.Stop> = listOf(
        stop("Town Hall", "200070"),
        stop("Wollongong", "WGN"),
        stop("Wollongong Station", "WGN2"),
        stop("Central Station", "200060"),
        stop("Blacktown", "BTN"),
        stop("Blacktown Station", "BTN2"),
        stop("Schofields", "SCH"),
        stop("Schofields Station", "SCH2"),
        stop("Bella Vista Station", "BVS"),
        stop("Warrawong", "WRW"),
        stop("Kingsford", "KGF"),
        stop("Cowper Street", "CWP"),
        stop("Third Avenue", "THD"),
        stop("Highfield Road", "HFR"),
        stop("Parramatta Station", "PAR"),
        stop("Redfern", "RDF"),
        stop("Strathfield", "STF"),
        stop("Liverpool Street", "LVP"),
        stop("York Road", "YRD"),
        stop("Pitt Street", "PTT"),
        stop("St James Station", "SJS"),
        stop("St Leonards Station", "SLS"),
        stop("Alison Park Blackwall Point Rd", "APB"),
    )

    private fun assertTopFive(query: String, expectedStopId: String) {
        val results = ranker.rank(query = query, candidates = candidatePool())
        val ids = results.take(5).map { it.stopId }
        assertTrue(
            expectedStopId in ids,
            "Expected '$expectedStopId' in top 5 for query '$query' but got: $ids",
        )
    }

    // Concatenation — missing whitespace
    @Test fun `golden townhall resolves Town Hall`() = assertTopFive("townhall", "200070")
    @Test fun `golden wewollongong resolves Wollongong`() = assertTopFive("wewollongong", "WGN")

    // Progressive typing / truncation
    @Test fun `golden townh resolves Town Hall`() = assertTopFive("townh", "200070")
    @Test fun `golden townha resolves Town Hall`() = assertTopFive("townha", "200070")
    @Test fun `golden townhal resolves Town Hall`() = assertTopFive("townhal", "200070")
    @Test fun `golden bellav resolves Bella Vista Station`() = assertTopFive("bellav", "BVS")
    @Test fun `golden bella vista m resolves Bella Vista Station`() = assertTopFive("bella vista m", "BVS")
    @Test fun `golden schoi resolves Schofields`() = assertTopFive("schoi", "SCH")
    @Test fun `golden schofil resolves Schofields`() = assertTopFive("schofil", "SCH")
    @Test fun `golden earraw resolves Warrawong`() = assertTopFive("earraw", "WRW")

    // Typos / fat-finger / transpositions
    @Test fun `golden wollong9ng resolves Wollongong`() = assertTopFive("wollong9ng", "WGN")
    @Test fun `golden blackyown resolves Blacktown`() = assertTopFive("blackyown", "BTN")
    @Test fun `golden cebtral resolves Central`() = assertTopFive("cebtral", "200060")
    @Test fun `golden kingsfprd resolves Kingsford`() = assertTopFive("kingsfprd", "KGF")
    @Test fun `golden warrawonngg resolves Warrawong`() = assertTopFive("warrawonngg", "WRW")
    @Test fun `golden wareawong resolves Warrawong`() = assertTopFive("wareawong", "WRW")

    // Abbreviations
    @Test fun `golden york rd resolves York Road`() = assertTopFive("york rd", "YRD")
    @Test fun `golden pitt st resolves Pitt Street`() = assertTopFive("pitt st", "PTT")
    @Test fun `golden third ave resolves Third Avenue`() = assertTopFive("third ave", "THD")

    // Missing middle vowels / character deletions
    @Test fun `golden schofld resolves Schofields`() = assertTopFive("schofld", "SCH")
    @Test fun `golden schfld resolves Schofields`() = assertTopFive("schfld", "SCH")

    // Concatenation with skipped letters
    @Test fun `golden twnhall resolves Town Hall`() = assertTopFive("twnhall", "200070")
    @Test fun `golden blackyown resolves Blacktown Station`() = assertTopFive("blackyown", "BTN2")

    // endregion

    // region false-positive guards — stops that must NOT appear for unrelated queries

    @Test
    fun `cowper street does not surface St James Station`() {
        val results = ranker.rank(query = "cowper street", candidates = candidatePool())
        val ids = results.map { it.stopId }
        assertTrue("SJS" !in ids, "St James Station must not match 'cowper street' but appeared in: $ids")
        assertTrue("SLS" !in ids, "St Leonards must not match 'cowper street' but appeared in: $ids")
    }

    @Test
    fun `cowper street surfaces Cowper Street stop`() {
        val results = ranker.rank(query = "cowper street", candidates = candidatePool())
        val ids = results.take(3).map { it.stopId }
        assertTrue("CWP" in ids, "Expected Cowper Street in top 3 for 'cowper street' but got: $ids")
    }

    @Test
    fun `pitt st does not surface St James Station`() {
        val results = ranker.rank(query = "pitt st", candidates = candidatePool())
        val ids = results.map { it.stopId }
        assertTrue("SJS" !in ids, "St James must not match 'pitt st' but appeared in: $ids")
    }

    @Test
    fun `blacktwon ro does not surface Barangaroo`() {
        val results = ranker.rank(query = "blacktwon ro", candidates = candidatePool())
        val ids = results.map { it.stopId }
        assertTrue("APB" !in ids, "Barangaroo-like stop must not match 'blacktwon ro' but appeared in: $ids")
    }

    @Test
    fun `blacktwon ro resolves Blacktown`() {
        val results = ranker.rank(query = "blacktwon ro", candidates = candidatePool())
        val ids = results.take(5).map { it.stopId }
        assertTrue("BTN" in ids || "BTN2" in ids, "Expected Blacktown in top 5 for 'blacktwon ro' but got: $ids")
    }

    @Test
    fun `blacktown road does not surface Alison Park Blackwall Point Rd`() {
        val results = ranker.rank(query = "blacktown road", candidates = candidatePool())
        val ids = results.map { it.stopId }
        assertTrue("APB" !in ids, "Alison Park Blackwall Point Rd must not match 'blacktown road' but appeared in: $ids")
    }

    @Test
    fun `blacktown road surfaces Blacktown Station`() {
        val results = ranker.rank(query = "blacktown road", candidates = candidatePool())
        val ids = results.take(5).map { it.stopId }
        assertTrue("BTN" in ids || "BTN2" in ids, "Expected Blacktown or Blacktown Station in top 5 for 'blacktown road' but got: $ids")
    }

    // endregion

    // region performance

    @Test
    fun `rank 200 candidates completes within 50ms`() {
        val candidates = (1..200).map { i ->
            stop("Stop Number $i Near Station", "stop_$i")
        }
        val elapsed = measureTime {
            repeat(20) { ranker.rank("station", candidates) }
        }
        assertTrue(
            elapsed.inWholeMilliseconds < 1000L,
            "20 × rank(200 candidates) took ${elapsed.inWholeMilliseconds}ms, expected < 1000ms",
        )
    }

    // endregion

    private fun stop(name: String, id: String) = SearchStopState.SearchResult.Stop(
        stopName = name,
        stopId = id,
        transportModeType = persistentListOf(),
    )
}

private fun assertEquals(expected: Double, actual: Double, absoluteTolerance: Double) {
    assertTrue(
        kotlin.math.abs(expected - actual) <= absoluteTolerance,
        "Expected $expected ± $absoluteTolerance but was $actual",
    )
}
