package xyz.ksharma.krail.taj.theme

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer.Companion.TEXT_CONTRAST_AA
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer.Companion.UI_COMPONENT_CONTRAST_AA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [ensureContrastWith] must be **total**: for any colour, on any ground the app draws on, it
 * returns something that clears the threshold. Not "usually", not "for the colours we ship" —
 * the whole point of deriving a colour instead of hand-picking one is that a theme nobody has
 * invented yet is already handled.
 *
 * That is a property, so this measures a property rather than a list of colours: every hue at
 * [HUE_STEP_DEGREES] degrees, across four saturations and four values, against every
 * opaque ground in both schemes, at both thresholds.
 *
 * The same sweep run against [ensureMinimumContrast] is what motivated this function existing at
 * all — it returns a still-failing colour for roughly a quarter of them on the sheet ground, and
 * returns it silently. `the old helper still cannot reach the target` pins that so the justification
 * cannot rot: if it ever starts passing, [ensureMinimumContrast] was fixed and this function's
 * KDoc needs rewriting.
 */
class ContrastAdaptationTest {

    @Test
    fun `every colour on every ground reaches its target`() {
        val unreachable = mutableListOf<String>()
        var adapted = 0
        var measured = 0

        pairings().forEach { (ground, threshold, colour) ->
            measured++
            val result = colour.ensureContrastWith(
                background = ground.background,
                toward = ground.onBackground,
                minContrast = threshold,
            )
            if (result != colour) adapted++
            if (result.contrastRatio(ground.background) < threshold) {
                unreachable += "${colour.hex()} on ${ground.name} at $threshold " +
                    "-> ${result.hex()} measures ${result.contrastRatio(ground.background)}"
            }
        }

        // Scan-broke detector: a sweep that measures nothing, or that never has to adapt
        // anything, passes vacuously and covers nothing.
        assertTrue(
            measured >= MIN_EXPECTED_MEASUREMENTS,
            "Sweep only measured $measured pairings — the colour generator is broken.",
        )
        assertTrue(
            adapted > 0,
            "Nothing in the sweep needed adapting, so nothing was actually exercised.",
        )

        if (unreachable.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("${unreachable.size} of $measured pairings did not reach their target:")
                    unreachable.take(MAX_REPORTED_FAILURES).forEach { appendLine("  - $it") }
                    if (unreachable.size > MAX_REPORTED_FAILURES) {
                        appendLine("  ... and ${unreachable.size - MAX_REPORTED_FAILURES} more")
                    }
                    appendLine(
                        "ensureContrastWith is supposed to be total. Either the ladder no longer " +
                            "ends at `toward`, or a ground was added whose onBackground does not " +
                            "clear the threshold against it.",
                    )
                },
            )
        }
    }

    @Test
    fun `a colour that already passes is returned untouched`() {
        // Bus cyan on the dark surface measures 7.25 — nothing to do, and doing nothing matters:
        // a theme that already reads well must not drift because the helper was called.
        val bus = bus_theme
        assertEquals(
            bus,
            bus.ensureContrastWith(
                background = md_theme_dark_surface,
                toward = md_theme_dark_onSurface,
            ),
            "A colour already clearing the threshold must be returned identically.",
        )
    }

    @Test
    fun `the result is the least adapted rung that clears`() {
        GROUNDS.forEach { ground ->
            SHIPPED_THEME_COLOURS.forEach { colour ->
                val result = colour.ensureContrastWith(
                    background = ground.background,
                    toward = ground.onBackground,
                    minContrast = TEXT_CONTRAST_AA,
                )
                if (result == colour) return@forEach

                val rung = ladderRungOf(result, from = colour, toward = ground.onBackground)
                assertTrue(
                    rung > 0,
                    "${colour.hex()} on ${ground.name}: result ${result.hex()} is not on the ladder.",
                )
                if (rung == 1) return@forEach

                val previous = androidx.compose.ui.graphics.lerp(
                    colour,
                    ground.onBackground,
                    (rung - 1).toFloat() / BLEND_LADDER_STEPS,
                )
                assertTrue(
                    previous.contrastRatio(ground.background) <
                        TEXT_CONTRAST_AA + CONTRAST_SAFETY_MARGIN,
                    "${colour.hex()} on ${ground.name}: rung ${rung - 1} already clears, so rung " +
                        "$rung over-adapts. The scan must stop at the first passing rung.",
                )
            }
        }
    }

    @Test
    fun `the old helper still cannot reach the target`() {
        // Purple Drip on the bottom-sheet ground is the case that started this: raising HSV value
        // tops out below 4.5 because the colour is already fully saturated with little value left.
        val stillFailing = purple_drip_theme.ensureMinimumContrast(
            background = md_theme_dark_modal_sheet_background,
            minContrast = TEXT_CONTRAST_AA,
        )

        assertTrue(
            stillFailing.contrastRatio(md_theme_dark_modal_sheet_background) < TEXT_CONTRAST_AA,
            "ensureMinimumContrast now reaches AA for Purple Drip on the sheet. It was fixed, or " +
                "a token moved. Rewrite ensureContrastWith's KDoc, which cites this as the reason " +
                "it exists, and delete this test.",
        )
    }

    // region helpers

    private data class Ground(val name: String, val background: Color, val onBackground: Color)

    private data class Pairing(val ground: Ground, val threshold: Float, val colour: Color)

    /** Flattened so the assertion loop stays one level deep and reads as one measurement. */
    private fun pairings(): List<Pairing> {
        val colours = sweepColours()
        return GROUNDS.flatMap { ground ->
            THRESHOLDS.flatMap { threshold ->
                colours.map { colour -> Pairing(ground, threshold, colour) }
            }
        }
    }

    private fun sweepColours(): List<Color> = buildList {
        var hue = 0f
        while (hue < FULL_TURN_DEGREES) {
            SATURATIONS.forEach { saturation ->
                VALUES.forEach { value ->
                    add(Color.hsv(hue = hue, saturation = saturation, value = value))
                }
            }
            hue += HUE_STEP_DEGREES
        }
    }

    /** Which rung of the blend ladder [result] sits on, or -1 if it is not on the ladder. */
    private fun ladderRungOf(result: Color, from: Color, toward: Color): Int {
        for (rung in 0..BLEND_LADDER_STEPS) {
            val candidate = androidx.compose.ui.graphics.lerp(
                from,
                toward,
                rung.toFloat() / BLEND_LADDER_STEPS,
            )
            if (candidate == result) return rung
        }
        return -1
    }

    private fun Color.hex(): String {
        fun channel(value: Float) =
            (value * CHANNEL_MAX).toInt().toString(HEX_RADIX).padStart(2, '0').uppercase()
        return "#${channel(red)}${channel(green)}${channel(blue)}"
    }

    // endregion

    private companion object {
        const val HUE_STEP_DEGREES = 3f
        const val FULL_TURN_DEGREES = 360f
        val SATURATIONS = listOf(0.4f, 0.6f, 0.8f, 1.0f)
        val VALUES = listOf(0.4f, 0.6f, 0.8f, 1.0f)

        const val MIN_EXPECTED_MEASUREMENTS = 10_000
        const val MAX_REPORTED_FAILURES = 12
        const val CHANNEL_MAX = 255
        const val HEX_RADIX = 16

        val THRESHOLDS = listOf(TEXT_CONTRAST_AA, UI_COMPONENT_CONTRAST_AA)

        /**
         * Every opaque ground an adapted colour can land on, paired with the content colour it
         * would be blended towards. Adding a ground token here is how a new surface gets covered.
         */
        val GROUNDS = listOf(
            Ground("dark surface", md_theme_dark_surface, md_theme_dark_onSurface),
            Ground("dark sheet", md_theme_dark_modal_sheet_background, md_theme_dark_onSurface),
            Ground(
                "dark past-row",
                md_theme_dark_past_departure_row_surface,
                md_theme_dark_onSurface,
            ),
            Ground("light surface", md_theme_light_surface, md_theme_light_onSurface),
            Ground("light sheet", md_theme_light_modal_sheet_background, md_theme_light_onSurface),
            Ground(
                "light past-row",
                md_theme_light_past_departure_row_surface,
                md_theme_light_onSurface,
            ),
        )

        val SHIPPED_THEME_COLOURS = KrailThemeStyle.entries.map {
            Color(
                red = it.hexColorCode.channel(HEX_OFFSET_RED),
                green = it.hexColorCode.channel(HEX_OFFSET_GREEN),
                blue = it.hexColorCode.channel(HEX_OFFSET_BLUE),
            )
        }

        const val HEX_OFFSET_RED = 3
        const val HEX_OFFSET_GREEN = 5
        const val HEX_OFFSET_BLUE = 7

        /** `#AARRGGBB` — reads two hex digits at [offset] as a 0..1 channel. */
        fun String.channel(offset: Int): Float =
            substring(offset, offset + 2).toInt(HEX_RADIX) / CHANNEL_MAX.toFloat()
    }
}
