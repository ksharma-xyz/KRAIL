package xyz.ksharma.krail.taj.theme

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer.Companion.TEXT_CONTRAST_AA
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer.Companion.UI_COMPONENT_CONTRAST_AA
import xyz.ksharma.krail.taj.hexToComposeColor
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The hole the other three contrast guards leave between them.
 *
 * `ThemeContrastTest` measures content drawn **on** a theme colour. `KrailColorTokenContrastTest`
 * measures the fixed [KrailColors] tokens. `TransportModeContrastTest` measures `TransportMode`
 * colours, which do not include Purple Drip or Barbie Pink because those are theme-only. Nothing
 * measured a theme colour used as **ink** against the surfaces it is drawn on, which is exactly
 * where Purple Drip was failing at 2.95:1 on the dark surface and 2.49:1 on a bottom sheet.
 *
 * Self-healing over `KrailThemeStyle.entries` and over [inkGrounds], so a seventh theme or a new
 * ground token is measured the day it is added rather than the day someone remembers.
 *
 * There is no known-failure set here and there should never be one: [asThemeInk] is total, so a
 * failure means the derivation broke, not that a colour is awkward.
 */
class ThemeInkContrastTest {

    @Test
    fun `every theme's ink clears its threshold on every ground in its scheme`() {
        val failures = mutableListOf<String>()
        var measured = 0

        SCHEMES.forEach { (schemeName, darkMode) ->
            THRESHOLDS.forEach { threshold ->
                failures += measureScheme(schemeName, darkMode, threshold).also { measured++ }
            }
        }

        assertTrue(
            measured == SCHEMES.size * THRESHOLDS.size,
            "Expected ${SCHEMES.size * THRESHOLDS.size} scheme/threshold combinations, ran $measured.",
        )
        assertTrue(
            KrailThemeStyle.entries.isNotEmpty() && inkGrounds(darkMode = true).isNotEmpty(),
            "Nothing to measure — the theme enum or the ground list came back empty.",
        )

        if (failures.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("Theme ink below its threshold:")
                    failures.forEach { appendLine("  - $it") }
                    appendLine(
                        "asThemeInk is supposed to make this impossible. Check that " +
                            "hardestInkGround still picks the extreme of inkGrounds for the " +
                            "scheme, and that ensureContrastWith is still reached. Never lower " +
                            "the threshold.",
                    )
                },
            )
        }
    }

    /**
     * Why the ink is derived once per scheme rather than once per ground.
     *
     * Adapting against whatever ground the ink lands on is the obvious implementation and it is
     * wrong: it produces a different shade of the same theme on the surface, on a sheet and on a
     * card, and a sheet opened over a screen shows two of them at once. This asserts that the
     * cheap approach really would split the colour, so the guarantee is not mistaken for a
     * coincidence, and that the single derived value covers every ground anyway.
     */
    @Test
    fun `deriving per ground would split a theme into several shades`() {
        val split = KrailThemeStyle.entries.flatMap { style ->
            SCHEMES.map { (schemeName, darkMode) ->
                Triple(style, schemeName, perGroundShades(style, darkMode))
            }
        }

        assertTrue(
            split.any { it.third.size > 1 },
            "No theme splits when adapted per ground, so deriving against the hardest ground " +
                "buys nothing. Either the ground tokens converged or the derivation stopped " +
                "adapting. Re-check before simplifying asThemeInk to take a ground.",
        )

        split.forEach { (style, schemeName, shades) ->
            val single = style.colour().asThemeInk(darkMode = schemeName == "dark")
            assertTrue(
                shades.isNotEmpty(),
                "${style.name} $schemeName produced no per-ground shades to compare against.",
            )
            inkGrounds(darkMode = schemeName == "dark").forEach { ground ->
                assertTrue(
                    single.contrastRatio(ground) >= TEXT_CONTRAST_AA,
                    "${style.name} $schemeName: the single ink ${single.hex()} measures " +
                        "${single.contrastRatio(ground)} on ${ground.hex()}, so one value does " +
                        "not in fact cover every ground.",
                )
            }
        }
    }

    /**
     * Self-heal over the ground tokens. A property of [KrailColors] that reads as a ground must be
     * either measured by [inkGrounds] or excused here with the reason it can never host theme ink.
     *
     * The classifier is a naming rule, not reflection, which common code does not have. Its limit
     * is honest: a ground token named without `Surface`, `Background` or `Container` in it would
     * slip past. Name new ground tokens accordingly.
     */
    @Test
    fun `every KrailColors ground is either measured or excused`() {
        val groundsInTokens = PROPERTY_NAME.findAll(KrailLightColors.toString())
            .map { it.groupValues[1] }
            .filter { name -> GROUND_NAME.containsMatchIn(name) && !name.startsWith("on") }
            .toSet()

        val accounted = MEASURED_GROUND_PROPERTIES + NOT_AN_INK_GROUND.keys

        val unclassified = groundsInTokens - accounted
        assertTrue(
            unclassified.isEmpty(),
            "New KrailColors ground ${unclassified.joinToString()} is not accounted for. Add it " +
                "to inkGrounds() in ThemeInk.kt and to MEASURED_GROUND_PROPERTIES here, or to " +
                "NOT_AN_INK_GROUND with the reason theme ink is never drawn on it.",
        )

        val stale = accounted - groundsInTokens
        assertTrue(
            stale.isEmpty(),
            "${stale.joinToString()} is listed here but is no longer a KrailColors ground — " +
                "delete the entry, and its line in inkGrounds() if it has one.",
        )

        assertTrue(
            MEASURED_GROUND_PROPERTIES.size == inkGrounds(darkMode = true).size,
            "MEASURED_GROUND_PROPERTIES lists ${MEASURED_GROUND_PROPERTIES.size} grounds but " +
                "inkGrounds() returns ${inkGrounds(darkMode = true).size}. They describe the same " +
                "set and have drifted apart.",
        )
    }

    // region helpers

    private fun measureScheme(scheme: String, darkMode: Boolean, threshold: Float): List<String> =
        KrailThemeStyle.entries.flatMap { style ->
            val ink = style.colour().asThemeInk(darkMode = darkMode, minContrast = threshold)
            inkGrounds(darkMode)
                .filter { ink.contrastRatio(it) < threshold }
                .map { ground ->
                    "${style.name} $scheme ink ${ink.hex()} on ${ground.hex()} measures " +
                        "${ink.contrastRatio(ground)}, needs $threshold"
                }
        }

    /** The distinct colours this theme would take if each ground adapted it separately. */
    private fun perGroundShades(style: KrailThemeStyle, darkMode: Boolean): Set<Color> =
        inkGrounds(darkMode).map { ground ->
            style.colour().ensureContrastWith(
                background = ground,
                toward = if (darkMode) md_theme_dark_onSurface else md_theme_light_onSurface,
            )
        }.toSet()

    private fun KrailThemeStyle.colour(): Color = hexColorCode.hexToComposeColor()

    private fun Color.hex(): String {
        fun channel(value: Float) =
            (value * CHANNEL_MAX).toInt().toString(HEX_RADIX).padStart(2, '0').uppercase()
        return "#${channel(red)}${channel(green)}${channel(blue)}"
    }

    // endregion

    private companion object {
        val SCHEMES = listOf("light" to false, "dark" to true)
        val THRESHOLDS = listOf(TEXT_CONTRAST_AA, UI_COMPONENT_CONTRAST_AA)

        val PROPERTY_NAME = Regex("(\\w+)=")
        val GROUND_NAME = Regex("[Ss]urface|[Bb]ackground|[Cc]ontainer")

        const val CHANNEL_MAX = 255
        const val HEX_RADIX = 16

        /** Mirrors `inkGrounds()` in ThemeInk.kt, by KrailColors property name. */
        val MEASURED_GROUND_PROPERTIES = setOf(
            "surface",
            "bottomSheetBackground",
            "discoverChipBackground",
            "discoverCardBackground",
            "themeSelectionBackground",
            "pastDepartureRowSurface",
        )

        val NOT_AN_INK_GROUND = mapOf(
            "errorContainer" to
                "error content is drawn in onErrorContainer, never in the rider's theme colour",
            "stopLabelSurface" to
                "stop-label pills pair with onStopLabelSurface and are the same dark value in " +
                "both schemes, so the theme colour is never ink on them",
        )
    }
}
