package xyz.ksharma.krail.taj.contrast

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.contrastRatio

/**
 * A transport-agnostic, scalable color entry for contrast analysis.
 *
 * Use this to feed any set of brand colors (transport lines, themes, etc.) into
 * [ContrastAnalyzer] without coupling the analyzer to a specific enum or module.
 *
 * @param label   Human-readable identifier (e.g. "T4", "EASTERN_SUBURBS_ILLAWARRA", "Bus theme")
 * @param hexColor Brand color in `#RRGGBB` or `#AARRGGBB` format
 */
data class ColorEntry(val label: String, val hexColor: String)

/**
 * Per-entry result produced by [ContrastAnalyzer].
 */
data class ContrastResult(
    val entry: ColorEntry,
    val lightModeContrast: Float,
    val darkModeContrast: Float,
    val minContrastRequired: Float,
) {
    val passesLight: Boolean get() = lightModeContrast >= minContrastRequired
    val passesDark: Boolean get() = darkModeContrast >= minContrastRequired

    /**
     * High-level category for this entry.
     *
     * - **PASS_BOTH** — works on both light and dark surfaces as-is.
     * - **PREFER_LIGHT_MODE** — acceptable on light surfaces only; apply
     *   `ensureMinimumContrast` when rendering on dark surfaces.
     * - **PREFER_DARK_MODE** — acceptable on dark surfaces only; apply
     *   `ensureMinimumContrast` when rendering on light surfaces.
     * - **NEEDS_CONTRAST_FIX** — fails on both surfaces; requires contrast
     *   adaptation in ALL display modes.
     */
    val category: String get() = when {
        passesLight && passesDark -> "PASS_BOTH"
        passesLight && !passesDark -> "PREFER_LIGHT_MODE"
        !passesLight && passesDark -> "PREFER_DARK_MODE"
        else -> "NEEDS_CONTRAST_FIX"
    }
}

/**
 * Generic, reusable contrast analyzer for any list of brand colors.
 *
 * Surface references (light / dark) are supplied by the caller — typically from
 * design-system tokens like `md_theme_light_surface` / `md_theme_dark_surface` —
 * so nothing is hard-coded inside this class.
 *
 * Usage example (transport lines):
 * ```
 * val analyzer = ContrastAnalyzer(
 *     lightSurface = md_theme_light_surface,
 *     darkSurface  = md_theme_dark_surface,
 * )
 * val results = analyzer.analyze(
 *     NswTransportLine.entries.map { ColorEntry(label = it.name, hexColor = it.hexColor) }
 * )
 * println(analyzer.generateReport(results))
 * ```
 *
 * @param lightSurface Surface color for light mode (use design-system token, not raw hex).
 * @param darkSurface  Surface color for dark mode.
 * @param minContrast  Minimum acceptable contrast ratio; defaults to
 *                     [UI_COMPONENT_CONTRAST_AA] (3.0 — WCAG AA for non-text UI components
 *                     such as badges and icons).
 */
class ContrastAnalyzer(
    private val lightSurface: Color,
    private val darkSurface: Color,
    private val minContrast: Float = UI_COMPONENT_CONTRAST_AA,
) {

    /**
     * Analyzes each [ColorEntry] against both surfaces and returns a [ContrastResult] for each.
     * New entries in the input list are automatically included — no manual list to update.
     */
    fun analyze(entries: List<ColorEntry>): List<ContrastResult> = entries.map { entry ->
        val color = entry.hexColor.hexToComposeColor()
        ContrastResult(
            entry = entry,
            lightModeContrast = color.contrastRatio(lightSurface),
            darkModeContrast = color.contrastRatio(darkSurface),
            minContrastRequired = minContrast,
        )
    }

    /**
     * Builds a human-readable contrast report from [analyze] results.
     *
     * Designed for `println()` in tests — paste the output to quickly understand
     * which colors need `ensureMinimumContrast` treatment and in which modes.
     */
    fun generateReport(results: List<ContrastResult>): String = buildString {
        val pass = "OK"
        val fail = "!!"

        appendLine("=".repeat(82))
        appendLine("Color Contrast Report  (min: ${minContrast}:1 — WCAG AA UI-component threshold)")
        appendLine(
            "Light surface: ${lightSurface.toDisplayHex()}  |  " +
                "Dark surface: ${darkSurface.toDisplayHex()}",
        )
        appendLine("=".repeat(82))
        appendLine(
            "%-42s %-10s %-14s %-14s %s".format(
                "Label", "Hex", "Light", "Dark", "Category",
            ),
        )
        appendLine("-".repeat(82))

        results.sortedBy { it.entry.label }.forEach { r ->
            appendLine(
                "%-42s %-10s %-14s %-14s %s".format(
                    r.entry.label.take(42),
                    r.entry.hexColor,
                    "${"%.2f".format(r.lightModeContrast)}:1 [${if (r.passesLight) pass else fail}]",
                    "${"%.2f".format(r.darkModeContrast)}:1 [${if (r.passesDark) pass else fail}]",
                    r.category,
                ),
            )
        }

        appendLine()
        appendLine("=== Summary by Category ===")
        results.groupBy { it.category }.toSortedMap().forEach { (category, items) ->
            appendLine("\n$category (${items.size}):")
            items.sortedBy { it.entry.label }
                .forEach { r -> appendLine("  ${r.entry.label}  ${r.entry.hexColor}") }
        }

        val needsFix = results.filter { !it.passesLight && !it.passesDark }
        if (needsFix.isNotEmpty()) {
            appendLine()
            appendLine("⚠ ${needsFix.size} entry(ies) fail on BOTH surfaces — require ensureMinimumContrast everywhere:")
            needsFix.forEach { r ->
                appendLine(
                    "  ${r.entry.label}: " +
                        "light=${"%.2f".format(r.lightModeContrast)}:1  " +
                        "dark=${"%.2f".format(r.darkModeContrast)}:1",
                )
            }
        } else {
            appendLine("\n✓ All entries pass on at least one surface — ensureMinimumContrast covers the other.")
        }

        appendLine()
        appendLine("=== Suggestive Treatment ===")
        appendLine("PREFER_LIGHT_MODE entries → apply ensureMinimumContrast() on dark backgrounds")
        appendLine("PREFER_DARK_MODE entries  → apply ensureMinimumContrast() on light backgrounds")
        appendLine("NEEDS_CONTRAST_FIX entries → apply ensureMinimumContrast() on ALL backgrounds")
    }

    companion object {
        /** WCAG AA minimum contrast for non-text UI components (badges, icons, etc.). */
        const val UI_COMPONENT_CONTRAST_AA = 3.0f

        /** WCAG AA minimum contrast for normal-sized body text. */
        const val TEXT_CONTRAST_AA = 4.5f
    }
}

private fun Color.toDisplayHex(): String {
    val r = (red * 255).toInt().toString(16).padStart(2, '0').uppercase()
    val g = (green * 255).toInt().toString(16).padStart(2, '0').uppercase()
    val b = (blue * 255).toInt().toString(16).padStart(2, '0').uppercase()
    return "#$r$g$b"
}
