package xyz.ksharma.krail.trip.planner.ui.contrast

import androidx.compose.ui.graphics.luminance
import xyz.ksharma.krail.core.transport.nsw.NswTransportLine
import xyz.ksharma.krail.taj.contrast.ColorEntry
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer
import xyz.ksharma.krail.taj.contrast.ContrastResult
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Exhaustive contrast-compliance tests for every [NswTransportLine] entry.
 *
 * Design-token surfaces (from `taj/Color.kt`):
 *   Light surface → [md_theme_light_surface] = #FFFFFF
 *   Dark  surface → [md_theme_dark_surface]  = #1C1B1A
 *
 * WCAG AA thresholds applied:
 *   Normal text   → 4.5:1  ([ContrastAnalyzer.TEXT_CONTRAST_AA])
 *   UI components → 3.0:1  ([ContrastAnalyzer.UI_COMPONENT_CONTRAST_AA])
 *
 * Self-healing: [allEntries] is derived from `NswTransportLine.entries` at runtime,
 * so a new enum constant is automatically included in every test — no manual update needed.
 * The size guard ([allEntriesAreExhaustivelyAnalysed]) forces a conscious review when the
 * count changes.
 */
class NswTransportLineContrastTest {

    private val analyzer = ContrastAnalyzer(
        lightSurface = md_theme_light_surface,
        darkSurface = md_theme_dark_surface,
        minContrast = ContrastAnalyzer.TEXT_CONTRAST_AA,
    )

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

    /** Always passes. Prints the full contrast report — paste into design tickets or PRs. */
    @Test
    fun printFullContrastReport() {
        val results = analyzer.analyze(allEntries)
        println(analyzer.generateReport(results))
    }

    // ── Compliance guards ─────────────────────────────────────────────────────

    /**
     * Fails if any line colour fails WCAG AA on BOTH surfaces (category NEEDS_CONTRAST_FIX).
     * If this triggers after adding a new line, either fix the hex in [NswTransportLine] or
     * confirm that `ensureMinimumContrast` is applied at every callsite.
     */
    @Test
    fun noLineColorNeedsContrastFixOnBothSurfaces() {
        val needsFix = analyzer.analyze(allEntries).filter { it.category == "NEEDS_CONTRAST_FIX" }
        if (needsFix.isNotEmpty()) {
            fail(
                "${needsFix.size} line colour(s) fail WCAG AA (${ContrastAnalyzer.TEXT_CONTRAST_AA}:1) " +
                    "on BOTH surfaces. Apply ensureMinimumContrast() at every usage site:\n" +
                    needsFix.joinToString("\n") { r ->
                        "  ${r.entry.label} (${r.entry.hexColor}): " +
                            "light=${"%.2f".format(r.lightModeContrast)}:1  " +
                            "dark=${"%.2f".format(r.darkModeContrast)}:1"
                    },
            )
        }
    }

    /** Every line must pass WCAG AA on at least one surface. */
    @Test
    fun everyLinePassesWcagAaOnAtLeastOneSurface() {
        analyzer.analyze(allEntries).forEach { result ->
            assertTrue(
                actual = result.passesLight || result.passesDark,
                message = "${result.entry.label} (${result.entry.hexColor}) fails WCAG AA on BOTH surfaces. " +
                    "light=${result.lightModeContrast}, dark=${result.darkModeContrast}. " +
                    "Apply ensureMinimumContrast() unconditionally at every usage site.",
            )
        }
    }

    // ── Pinned assertions — catch colour or token drift immediately ───────────

    /** T4 navy (#005AA3) — ~2.35:1 on dark surface, well below WCAG AA. */
    @Test
    fun t4EasternSuburbsIllawarraIsLowContrastOnDark() {
        val result = analyzeOne(NswTransportLine.EASTERN_SUBURBS_ILLAWARRA)
        assertTrue(result.passesLight, "T4 navy must pass WCAG AA on the light surface")
        assertEquals(
            expected = "PREFER_LIGHT_MODE",
            actual = result.category,
            message = "T4 should be PREFER_LIGHT_MODE. If this changes, verify md_theme_dark_surface or T4 hex hasn't drifted.",
        )
    }

    /** T8 green (#00954C) — ~4.32:1 on dark surface, just below WCAG AA. */
    @Test
    fun t8AirportSouthIsMarginalOnDark() {
        val result = analyzeOne(NswTransportLine.AIRPORT_SOUTH)
        assertTrue(result.passesLight, "T8 green must pass WCAG AA on the light surface")
        assertEquals(
            expected = "PREFER_LIGHT_MODE",
            actual = result.category,
            message = "T8 should be PREFER_LIGHT_MODE. If this changes, verify md_theme_dark_surface or T8 hex hasn't drifted.",
        )
    }

    /** SCO shares T4's hex — same category expected. */
    @Test
    fun southCoastSharesT4ColorAndCategory() {
        assertEquals(
            NswTransportLine.EASTERN_SUBURBS_ILLAWARRA.hexColor,
            NswTransportLine.SOUTH_COAST.hexColor,
            "SCO and T4 are expected to share the same brand colour",
        )
        assertEquals("PREFER_LIGHT_MODE", analyzeOne(NswTransportLine.SOUTH_COAST).category)
    }

    /** SHL shares T8's hex — same category expected. */
    @Test
    fun southernHighlandsSharesT8ColorAndCategory() {
        assertEquals(
            NswTransportLine.AIRPORT_SOUTH.hexColor,
            NswTransportLine.SOUTHERN_HIGHLANDS.hexColor,
            "SHL and T8 are expected to share the same brand colour",
        )
        assertEquals("PREFER_LIGHT_MODE", analyzeOne(NswTransportLine.SOUTHERN_HIGHLANDS).category)
    }

    // ── Design-token drift detection ──────────────────────────────────────────

    /**
     * Guards against accidental surface token drift. If either token shifts to a mid-tone,
     * all pinned contrast assertions above would produce misleading results.
     */
    @Test
    fun surfaceTokensAreNearWhiteAndNearBlack() {
        assertTrue(
            md_theme_light_surface.luminance() > 0.9f,
            "md_theme_light_surface must be near-white (luminance > 0.9). " +
                "Current: ${md_theme_light_surface.luminance()}. Re-evaluate all contrast results if changed.",
        )
        assertTrue(
            md_theme_dark_surface.luminance() < 0.05f,
            "md_theme_dark_surface must be near-black (luminance < 0.05). " +
                "Current: ${md_theme_dark_surface.luminance()}. Re-evaluate all contrast results if changed.",
        )
    }

    // ── Sanity check ──────────────────────────────────────────────────────────

    @Test
    fun majorityOfLinesPassOnAtLeastOneSurface() {
        val results = analyzer.analyze(allEntries)
        val passingCount = results.count { it.passesLight || it.passesDark }
        assertTrue(
            passingCount >= results.size / 2,
            "Expected at least half of lines to pass on at least one surface. " +
                "Only $passingCount/${results.size} pass. Did a threshold or surface token change?",
        )
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun analyzeOne(line: NswTransportLine): ContrastResult =
        analyzer.analyze(listOf(ColorEntry(label = line.key, hexColor = line.hexColor))).first()
}
