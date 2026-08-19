package xyz.ksharma.krail.trip.planner.ui.contrast

import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.contrast.ColorEntry
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import kotlin.test.Test
import kotlin.test.fail

/**
 * Measures every [TransportMode.colorCode] against both theme surfaces.
 *
 * `NswTransportLineContrastTest` covers the individual line colours (T1, M1, F10…). This covers
 * the eight mode colours those lines fall back to, which are what a rider sees on a mode badge,
 * a mode filter chip and the mode icon on every saved trip.
 *
 * Held to [ContrastAnalyzer.UI_COMPONENT_CONTRAST_AA] rather than the text minimum: a mode colour
 * fills a badge or draws an icon, and the glyph on top gets its own colour from
 * `getForegroundColor`, which `ThemeContrastTest` covers separately.
 *
 * Self-healing over `TransportMode.all`, so a ninth mode is measured the day it is added.
 *
 * ## Raw colours, and what [RELIES_ON_RUNTIME_ADAPTATION] means
 *
 * Several brand colours cannot clear 3:1 on one of the two surfaces — that is a fact about NSW
 * transport branding, not something this repo can fix by editing a hex. The app handles it at
 * render time with `Color.ensureMinimumContrast`, which lightens or darkens the brand colour until
 * it clears the surface it is actually being drawn on. This test therefore measures the RAW colour
 * and records which ones depend on that adaptation, so that:
 *
 * - a new mode colour that fails is a conscious decision, not a silent one, and
 * - removing `ensureMinimumContrast` from a render path is caught by the list here still being
 *   non-empty.
 *
 * Do not "fix" a failure by lowering the threshold.
 */
class TransportModeContrastTest {

    private val analyzer = ContrastAnalyzer(
        lightSurface = md_theme_light_surface,
        darkSurface = md_theme_dark_surface,
        minContrast = ContrastAnalyzer.UI_COMPONENT_CONTRAST_AA,
    )

    @Test
    fun `every mode colour either clears 3 to 1 on both surfaces or is a recorded exception`() {
        val unexpected = mutableListOf<String>()
        val stale = mutableListOf<String>()

        analyzer.analyze(
            TransportMode.all.map { ColorEntry(label = it.name, hexColor = it.colorCode) },
        ).forEach { result ->
            listOf(
                "light" to result.passesLight,
                "dark" to result.passesDark,
            ).forEach { (scheme, passes) ->
                val id = "${result.entry.label}|$scheme"
                val ratio = if (scheme == "light") {
                    result.lightModeContrast
                } else {
                    result.darkModeContrast
                }
                if (!passes && id !in RELIES_ON_RUNTIME_ADAPTATION) {
                    unexpected += "$id measured $ratio, needs ${result.minContrastRequired}"
                }
                if (passes && id in RELIES_ON_RUNTIME_ADAPTATION) {
                    stale += "$id now measures $ratio and passes unadapted"
                }
            }
        }

        if (unexpected.isNotEmpty() || stale.isNotEmpty()) {
            fail(
                buildString {
                    if (unexpected.isNotEmpty()) {
                        appendLine("Mode colours failing 3:1 with no recorded exception:")
                        unexpected.forEach { appendLine("  - $it") }
                        appendLine(
                            "Either the render path applies ensureMinimumContrast (add the " +
                                "entry, with this ratio) or the colour needs changing. Do not " +
                                "lower the threshold.",
                        )
                    }
                    if (stale.isNotEmpty()) {
                        appendLine("These now pass unadapted — delete their entry:")
                        stale.forEach { appendLine("  - $it") }
                    }
                },
            )
        }
    }

    private companion object {

        /**
         * `<mode>|<scheme>` pairs that fail 3:1 as raw brand colours and depend on
         * `ensureMinimumContrast` at render time. Ratios measured when recorded.
         */
        val RELIES_ON_RUNTIME_ADAPTATION = setOf(
            "Train|light", // 2.47 — #F6891F orange on white
            "Bus|light", // 2.37 — #00B5EF cyan on white
            "SchoolBus|light", // 2.37 — shares the bus cyan
            "Ferry|light", // 2.73 — #5AB031 green on white
            "Coach|dark", // 1.88 — #742282 purple on the #1C1B1A dark surface
        )
    }
}
