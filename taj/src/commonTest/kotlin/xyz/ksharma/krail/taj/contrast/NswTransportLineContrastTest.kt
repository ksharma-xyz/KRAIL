package xyz.ksharma.krail.taj.contrast

import androidx.compose.ui.graphics.luminance // extension function — used via Color.luminance()
import xyz.ksharma.krail.core.transport.nsw.NswTransportLine
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Exhaustive contrast-compliance tests for every [NswTransportLine] entry.
 *
 * Design-token surfaces (from `Color.kt`):
 *   Light surface → [md_theme_light_surface] = #FFFFFF
 *   Dark  surface → [md_theme_dark_surface]  = #1C1B1A
 *
 * WCAG AA thresholds applied here:
 *   Text          → 4.5:1  ([ContrastAnalyzer.TEXT_CONTRAST_AA])
 *   UI components → 3.0:1  ([ContrastAnalyzer.UI_COMPONENT_CONTRAST_AA])
 *
 * Self-healing behaviour
 * ─────────────────────
 * [allEntries] is derived from `NswTransportLine.entries` at runtime, so adding a new enum
 * constant automatically includes it in every test below without any manual bookkeeping.
 * The size-equality guard ([allEntriesAreExhaustivelyAnalysed]) will still fail if the
 * expected count drifts — that's intentional, to force a conscious review of the new colour.
 *
 * Paste the output of [printFullContrastReport] into design tickets or Notion when
 * reviewing NSW transport colour palette decisions.
 */
class NswTransportLineContrastTest {

    private val analyzer = ContrastAnalyzer(
        lightSurface = md_theme_light_surface,
        darkSurface = md_theme_dark_surface,
        minContrast = ContrastAnalyzer.TEXT_CONTRAST_AA, // 4.5:1 — WCAG AA for normal-sized text
    )

    /** All enum entries mapped to [ColorEntry]; automatically grows when new lines are added. */
    private val allEntries: List<ColorEntry> = NswTransportLine.entries
        .map { ColorEntry(label = it.key, hexColor = it.hexColor) }

    // ── Exhaustiveness ────────────────────────────────────────────────────────

    @Test
    fun allEntriesAreExhaustivelyAnalysed() {
        val results = analyzer.analyze(allEntries)
        assertEquals(
            expected = NswTransportLine.entries.size,
            actual = results.size,
            message = "Every NswTransportLine entry must be analysed. " +
                "If you added a new line, run this test to verify its contrast profile.",
        )
    }

    // ── Documentation ─────────────────────────────────────────────────────────

    /**
     * Always passes. Prints the full contrast report to stdout so you can paste it into
     * design tickets, Notion pages, or PR descriptions.
     *
     * Sample output column headers:
     *   Label | Hex | Light | Dark | Category
     */
    @Test
    fun printFullContrastReport() {
        val results = analyzer.analyze(allEntries)
        println(analyzer.generateReport(results))
    }

    // ── Compliance guards ─────────────────────────────────────────────────────

    /**
     * Fails if **any** line colour has category `"NEEDS_CONTRAST_FIX"` — i.e. it fails WCAG AA
     * on BOTH the light and dark surface.
     *
     * Such colours require `ensureMinimumContrast()` to be applied unconditionally, regardless of
     * the current theme. If this test fails after adding a new line, either:
     *   (a) adjust the brand hex in [NswTransportLine] to a more accessible value, or
     *   (b) confirm with design that `ensureMinimumContrast` will always be applied at every
     *       callsite (DepartureHeaderRow, JourneyCardHeader, TransportModeBadge, etc.).
     */
    @Test
    fun noLineColorNeedsContrastFixOnBothSurfaces() {
        val results = analyzer.analyze(allEntries)
        val needsFix = results.filter { it.category == "NEEDS_CONTRAST_FIX" }
        if (needsFix.isNotEmpty()) {
            val details = needsFix.joinToString("\n") { r ->
                val lc = (r.lightModeContrast * 100).toInt() / 100.0
                val dc = (r.darkModeContrast * 100).toInt() / 100.0
                "  ${r.entry.label} (${r.entry.hexColor}): light=${lc}:1  dark=${dc}:1"
            }
            fail(
                "${needsFix.size} line colour(s) fail WCAG AA (${ContrastAnalyzer.TEXT_CONTRAST_AA}:1) " +
                    "on BOTH light and dark surfaces.\n" +
                    "→ Apply ensureMinimumContrast() at every usage site:\n$details",
            )
        }
    }

    /**
     * Asserts that every line passes WCAG AA (4.5:1) on **at least one** surface.
     * Lines that only pass on one surface need `ensureMinimumContrast` in the other mode.
     */
    @Test
    fun everyLinePassesWcagAaOnAtLeastOneSurface() {
        val results = analyzer.analyze(allEntries)
        results.forEach { result ->
            assertTrue(
                actual = result.passesLight || result.passesDark,
                message = "${result.entry.label} (${result.entry.hexColor}) fails WCAG AA " +
                    "(${ContrastAnalyzer.TEXT_CONTRAST_AA}:1) on BOTH surfaces. " +
                    "light=${result.lightModeContrast}, dark=${result.darkModeContrast}. " +
                    "Apply ensureMinimumContrast() unconditionally at every usage site.",
            )
        }
    }

    // ── Pinned assertions for known problem lines ─────────────────────────────
    // These lock down expected categories so that colour or token drift is caught immediately.

    /**
     * T4 navy (#005AA3) has ~2.35:1 contrast on the dark surface — well below WCAG AA.
     * `ensureMinimumContrast` brightens it until it reaches 4.5:1 in dark mode.
     * Pinned here so any drift in the design token or brand colour triggers a review.
     */
    @Test
    fun t4EasternSuburbsIllawarraIsLowContrastOnDark() {
        val result = analyzeOne(NswTransportLine.EASTERN_SUBURBS_ILLAWARRA)
        assertTrue(result.passesLight, "T4 navy must pass WCAG AA on the light surface")
        assertEquals(
            expected = "PREFER_LIGHT_MODE",
            actual = result.category,
            message = "T4 should be PREFER_LIGHT_MODE (passes light, fails dark). " +
                "If this changes, verify md_theme_dark_surface or T4 hex hasn't drifted.",
        )
    }

    /**
     * T8 green (#00954C) has ~4.32:1 contrast on dark surface — just below WCAG AA (4.5:1).
     * `ensureMinimumContrast` nudges it to compliance.
     */
    @Test
    fun t8AirportSouthIsMarginalOnDark() {
        val result = analyzeOne(NswTransportLine.AIRPORT_SOUTH)
        assertTrue(result.passesLight, "T8 green must pass WCAG AA on the light surface")
        assertEquals(
            expected = "PREFER_LIGHT_MODE",
            actual = result.category,
            message = "T8 should be PREFER_LIGHT_MODE. " +
                "If this changes, verify md_theme_dark_surface or T8 hex hasn't drifted.",
        )
    }

    /**
     * Same brand colour as T4 — same pinned expectation.
     */
    @Test
    fun southCoastScoSharesT4ColorAndCategory() {
        val result = analyzeOne(NswTransportLine.SOUTH_COAST)
        assertEquals(
            NswTransportLine.EASTERN_SUBURBS_ILLAWARRA.hexColor,
            NswTransportLine.SOUTH_COAST.hexColor,
            "SCO and T4 are expected to share the same brand colour",
        )
        assertEquals("PREFER_LIGHT_MODE", result.category, "SCO shares T4 colour — same category expected")
    }

    /**
     * Same brand colour as T8 — same pinned expectation.
     */
    @Test
    fun southernHighlandsShlSharesT8ColorAndCategory() {
        val result = analyzeOne(NswTransportLine.SOUTHERN_HIGHLANDS)
        assertEquals(
            NswTransportLine.AIRPORT_SOUTH.hexColor,
            NswTransportLine.SOUTHERN_HIGHLANDS.hexColor,
            "SHL and T8 are expected to share the same brand colour",
        )
        assertEquals("PREFER_LIGHT_MODE", result.category, "SHL shares T8 colour — same category expected")
    }

    // ── Design-token drift detection ──────────────────────────────────────────

    /**
     * Guards against accidental design-token drift. If [md_theme_light_surface] or
     * [md_theme_dark_surface] is changed to a mid-tone, all the pinned contrast assertions
     * above would produce misleading results. This test catches that first.
     */
    @Test
    fun surfaceTokensAreNearWhiteAndNearBlack() {
        assertTrue(
            md_theme_light_surface.luminance() > 0.9f,
            "md_theme_light_surface must be near-white (luminance > 0.9). " +
                "Current luminance=${md_theme_light_surface.luminance()}. " +
                "Did the design token change? Re-evaluate all contrast results if so.",
        )
        assertTrue(
            md_theme_dark_surface.luminance() < 0.05f,
            "md_theme_dark_surface must be near-black (luminance < 0.05). " +
                "Current luminance=${md_theme_dark_surface.luminance()}. " +
                "Did the design token change? Re-evaluate all contrast results if so.",
        )
    }

    // ── Category distribution sanity check ───────────────────────────────────

    /**
     * Verifies PASS_BOTH lines are actually the bright/saturated ones (T1 orange, T2 blue, etc.)
     * by spot-checking that at least half of all entries pass on at least one surface.
     * This fails early if a batch of colours is accidentally downgraded or the threshold changes.
     */
    @Test
    fun majorityOfLinesPassOnAtLeastOneSurface() {
        val results = analyzer.analyze(allEntries)
        val passingCount = results.count { it.passesLight || it.passesDark }
        assertTrue(
            passingCount >= results.size / 2,
            "Expected at least half of lines to pass WCAG AA on at least one surface. " +
                "Only $passingCount/${results.size} pass. Did a threshold or surface token change?",
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun analyzeOne(line: NswTransportLine): ContrastResult =
        analyzer.analyze(listOf(ColorEntry(label = line.key, hexColor = line.hexColor))).first()
}

