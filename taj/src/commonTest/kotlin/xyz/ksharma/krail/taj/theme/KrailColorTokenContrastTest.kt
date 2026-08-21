package xyz.ksharma.krail.taj.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer.Companion.TEXT_CONTRAST_AA
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer.Companion.UI_COMPONENT_CONTRAST_AA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Measures every foreground/background pairing the app actually renders, in both colour schemes.
 *
 * `ThemeContrastTest` covers the six user-selectable theme colours. This covers the other half:
 * the fixed [KrailColors] tokens, where a regression is invisible in review because the change is
 * one hex digit in `Color.kt` and the surface it lands on is chosen somewhere else entirely.
 *
 * ## Alpha is composited before measuring
 *
 * `Color.contrastRatio` (and therefore `ContrastAnalyzer`) reads `luminance()`, which ignores the
 * alpha channel. For a translucent token that is not a small inaccuracy, it is the wrong answer:
 * `outlineSubtle` is a partially transparent near-black, so an alpha-blind ratio against white
 * reports ~21:1 when what the eye sees is a mid grey hairline. Every pairing here is composited
 * over its background first, which is a no-op for the opaque tokens and the only honest reading
 * for the translucent ones.
 *
 * ## Failing pairings are recorded, not silenced
 *
 * [KNOWN_FAILURES] holds the pairings that do not clear their threshold today, each with the ratio
 * measured when it was added. Thresholds are never lowered to accommodate one. A pairing that
 * starts passing also fails this test, so a fix cannot quietly leave a stale entry behind. The set
 * is empty — every pairing measured here clears its own threshold, and it stays that way.
 */
class KrailColorTokenContrastTest {

    @Test
    fun `every pairing clears its threshold or is a recorded known failure`() {
        val regressions = mutableListOf<String>()
        val fixed = mutableListOf<String>()

        SCHEMES.forEach { (schemeName, colors) ->
            PAIRINGS.forEach { pairing ->
                val id = "$schemeName|${pairing.foregroundName}|${pairing.backgroundName}"
                val ratio = pairing.ratio(colors)
                val passes = ratio >= pairing.minRatio
                val known = id in KNOWN_FAILURES

                if (!passes && !known) {
                    regressions += "$id measured ${ratio.round()}, needs ${pairing.minRatio}"
                }
                if (passes && known) {
                    fixed += "$id now measures ${ratio.round()} and passes"
                }
            }
        }

        if (regressions.isNotEmpty() || fixed.isNotEmpty()) {
            fail(
                buildString {
                    if (regressions.isNotEmpty()) {
                        appendLine("Contrast regressions (fix the colour, never the threshold):")
                        regressions.forEach { appendLine("  - $it") }
                    }
                    if (fixed.isNotEmpty()) {
                        appendLine("These are in KNOWN_FAILURES but now pass — delete their entry:")
                        fixed.forEach { appendLine("  - $it") }
                    }
                },
            )
        }
    }

    /**
     * Self-heal: adding a colour to [KrailColors] fails here until someone decides whether it has
     * a contrast obligation. Property names come from the data class `toString()` rather than
     * reflection, which is not available in common code — every property appears there as `name=`,
     * and `Color`'s own `toString()` is positional so it contributes no false matches.
     */
    @Test
    fun `every KrailColors property is either paired or explicitly excluded`() {
        val declared = PROPERTY_NAME.findAll(KrailLightColors.toString())
            .map { it.groupValues[1] }
            .toSet()
        val classified = PAIRINGS
            .flatMap { listOf(it.foregroundName, it.backgroundName) }
            .toSet() + NOT_A_CONTRAST_PAIR.keys

        val unclassified = declared - classified
        assertTrue(
            unclassified.isEmpty(),
            "New KrailColors ${unclassified.joinToString()} has no contrast obligation " +
                "recorded. Add it to PAIRINGS with the surface it is drawn on and the right " +
                "threshold (TEXT_CONTRAST_AA for anything a rider reads, " +
                "UI_COMPONENT_CONTRAST_AA for borders, tracks and icons), or to " +
                "NOT_A_CONTRAST_PAIR with the reason it can never be one.",
        )

        val stale = classified - declared
        assertTrue(
            stale.isEmpty(),
            "${stale.joinToString()} is listed here but no longer exists on KrailColors — " +
                "delete the entry.",
        )
        assertEquals(declared.size, classified.size)
    }

    private class Pairing(
        val foregroundName: String,
        val backgroundName: String,
        val minRatio: Float,
        val foreground: (KrailColors) -> Color,
        val background: (KrailColors) -> Color,
    ) {
        fun ratio(colors: KrailColors): Float {
            val bg = background(colors)
            return foreground(colors).compositeOver(bg).contrastRatio(bg)
        }
    }

    private companion object {

        val SCHEMES = listOf("light" to KrailLightColors, "dark" to KrailDarkColors)

        val PROPERTY_NAME = Regex("(\\w+)=")

        val PAIRINGS = listOf(
            Pairing("label", "surface", TEXT_CONTRAST_AA, { it.label }, { it.surface }),
            Pairing("softLabel", "surface", TEXT_CONTRAST_AA, { it.softLabel }, { it.surface }),
            Pairing(
                "secondaryLabel",
                "surface",
                TEXT_CONTRAST_AA,
                { it.secondaryLabel },
                { it.surface },
            ),
            // Placeholder text is deliberately quieter than real content, so it is held to the
            // non-text minimum rather than to full body-text legibility.
            Pairing(
                "labelPlaceholder",
                "surface",
                UI_COMPONENT_CONTRAST_AA,
                { it.labelPlaceholder },
                { it.surface },
            ),
            Pairing("onSurface", "surface", TEXT_CONTRAST_AA, { it.onSurface }, { it.surface }),
            Pairing("onError", "error", TEXT_CONTRAST_AA, { it.onError }, { it.error }),
            Pairing(
                "onErrorContainer",
                "errorContainer",
                TEXT_CONTRAST_AA,
                { it.onErrorContainer },
                { it.errorContainer },
            ),
            Pairing(
                "onStopLabelSurface",
                "stopLabelSurface",
                TEXT_CONTRAST_AA,
                { it.onStopLabelSurface },
                { it.stopLabelSurface },
            ),
            Pairing("alert", "surface", TEXT_CONTRAST_AA, { it.alert }, { it.surface }),
            Pairing(
                "deviationEarly",
                "surface",
                TEXT_CONTRAST_AA,
                { it.deviationEarly },
                { it.surface },
            ),
            Pairing(
                "deviationLate",
                "surface",
                TEXT_CONTRAST_AA,
                { it.deviationLate },
                { it.surface },
            ),
            // The same deviation minutes are re-drawn on the dimmed past-departure row, which is a
            // different background from the card surface and is easy to forget when tuning either.
            Pairing(
                "deviationEarly",
                "pastDepartureRowSurface",
                TEXT_CONTRAST_AA,
                { it.deviationEarly },
                { it.pastDepartureRowSurface },
            ),
            Pairing(
                "deviationLate",
                "pastDepartureRowSurface",
                TEXT_CONTRAST_AA,
                { it.deviationLate },
                { it.pastDepartureRowSurface },
            ),
            Pairing(
                "outlineSubtle",
                "surface",
                UI_COMPONENT_CONTRAST_AA,
                { it.outlineSubtle },
                { it.surface },
            ),
        )

        /** Tokens with no fixed foreground/background partner, and why. */
        val NOT_A_CONTRAST_PAIR = mapOf(
            "scrim" to "A dimming overlay behind a sheet. Its job is to reduce contrast.",
            "badge" to "A dot indicator carrying no glyph; nothing is drawn on top of it.",
            "magicYellow" to "A brand accent used in gradients and decoration, never as a " +
                "text-on-surface pairing.",
            "walkingPath" to "A map polyline drawn over map tiles, not over a theme surface.",
            "userLocationDot" to "A map marker over map tiles, with its own white ring for " +
                "separation.",
            "themeSelectionBackground" to "A swatch container whose content is the user's " +
                "theme colour, covered by ThemeContrastTest.",
            "bottomSheetBackground" to "A container surface; what it must contrast with is the " +
                "scrim behind it, not a foreground token.",
            "bottomSheetDragHandle" to "A decorative grabber; its affordance is position and " +
                "shape, not contrast.",
            "discoverChipBackground" to "A container whose foreground is the theme colour, " +
                "resolved per theme by getForegroundColor.",
            "discoverCardBackground" to "A container filled with imagery and per-card colours " +
                "rather than a fixed token.",
            "pastJourney" to "A dimming treatment applied to a whole row; contrast is " +
                "deliberately reduced to signal the departure has gone.",
            "futureJourney" to "An emphasis tint on a row that carries no text of its own.",
            "walkingConnector" to "A dotted timeline connector, deliberately quieter than a " +
                "scheduled-service line and carrying no text.",
        )

        /**
         * Pairings that do not clear their threshold, with the ratio measured when recorded.
         * Every one is a real accessibility gap for the maintainer to decide on — not a licence
         * to lower a threshold.
         *
         * Empty since #1914, which moved the last six tokens above their bars rather than
         * recording them here. Adding an entry means shipping a known accessibility gap: fix the
         * colour first, and only record one when a maintainer has decided the design cannot give.
         */
        val KNOWN_FAILURES = emptySet<String>()

        fun Float.round(): String {
            val scaled = (this * 100).toInt()
            return "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}"
        }
    }
}
