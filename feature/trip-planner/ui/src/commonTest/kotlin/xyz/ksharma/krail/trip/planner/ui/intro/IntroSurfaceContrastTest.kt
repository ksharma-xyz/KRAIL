package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_secondary_label
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_secondary_label
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The text on every tinted surface of an Intro screen, which is the screen's accent made translucent over the page by taj (0.45 in
 * dark mode, 0.15 in light), in both modes and for all five accents. The text of a card, a row and the trip card is the page's ink, and
 * the line `Last updated` is the soft label, so both are held to 4.5 to 1 on the tint, and the white letter of the `P` tile to 4.5 on
 * the button's deep fill. The numbers are printed and the ones of the mock (v13, P1) are the reference.
 */
class IntroSurfaceContrastTest {

    private val accents = IntroStep.entries.map { it to introAccentStyle(it) }

    private fun surface(dark: Boolean) = if (dark) md_theme_dark_surface else md_theme_light_surface

    private fun ink(dark: Boolean) = if (dark) md_theme_dark_onSurface else md_theme_light_onSurface

    private fun softLabel(dark: Boolean) = if (dark) md_theme_dark_secondary_label else md_theme_light_secondary_label

    private fun tint(style: KrailThemeStyle, dark: Boolean): Color =
        introGroundColour(style.hexColorCode.hexToComposeColor(), surface(dark), dark)

    @Test
    fun `text on the tint of each of the five accents is at least 4_5 to 1 in dark and light, and the numbers are printed`() {
        listOf(true, false).forEach { dark ->
            accents.forEach { (step, style) ->
                val fill = tint(style, dark)
                val name = ink(dark).contrastRatio(fill)
                val last = softLabel(dark).contrastRatio(fill)
                println(
                    "P1 contrast, ${if (dark) "dark" else "light"}, ${style.name} ($step): name and figures $name, last updated $last",
                )

                assertTrue(name >= MIN_TEXT, "${style.name} dark $dark: name and figures $name")
                assertTrue(last >= MIN_TEXT, "${style.name} dark $dark: last updated $last")
            }
        }
    }

    @Test
    fun `the numbers are the ones of the approved mock, for the three screens that have a card or a row`() {
        // v13 index.md, P1, name and numbers on the tint, then last updated.
        val mock = mapOf(
            Triple(KrailThemeStyle.Ferry, true, "name") to 7.01f,
            Triple(KrailThemeStyle.Ferry, false, "name") to 18.15f,
            Triple(KrailThemeStyle.Bus, true, "name") to 6.59f,
            Triple(KrailThemeStyle.Bus, false, "name") to 18.20f,
            Triple(KrailThemeStyle.BarbiePink, true, "name") to 9.64f,
            Triple(KrailThemeStyle.BarbiePink, false, "name") to 16.64f,
            Triple(KrailThemeStyle.Metro, true, "name") to 8.15f,
            Triple(KrailThemeStyle.Metro, false, "name") to 17.51f,
            Triple(KrailThemeStyle.Ferry, true, "last") to 5.80f,
            Triple(KrailThemeStyle.Bus, true, "last") to 5.45f,
            Triple(KrailThemeStyle.BarbiePink, true, "last") to 7.97f,
            Triple(KrailThemeStyle.Ferry, false, "last") to 11.81f,
            Triple(KrailThemeStyle.Bus, false, "last") to 11.84f,
            Triple(KrailThemeStyle.BarbiePink, false, "last") to 10.82f,
        )

        mock.forEach { (key, expected) ->
            val (style, dark, which) = key
            val actual = (if (which == "name") ink(dark) else softLabel(dark)).contrastRatio(tint(style, dark))

            assertTrue(
                abs(actual - expected) < MOCK_TOLERANCE,
                "${style.name} dark $dark $which: $actual against the mock's $expected",
            )
        }
    }

    @Test
    fun `no screen colour reads worse than the Train tint it replaces, by more than a hair`() {
        val train = tint(KrailThemeStyle.Train, dark = true)

        // 6.61 in the mock. The blue is a hair under it and the pink and teal are far better.
        assertEquals(6.61f, ink(true).contrastRatio(train), MOCK_TOLERANCE)
        accents.forEach { (_, style) ->
            assertTrue(
                ink(true).contrastRatio(tint(style, true)) >= ink(true).contrastRatio(train) - MOCK_TOLERANCE,
                style.name,
            )
        }
    }

    @Test
    fun `the white letter of the P tile is at least 4_5 to 1 on the green button fill in both modes, where the raw green was 2_73`() {
        val raw = KrailThemeStyle.Ferry.hexColorCode.hexToComposeColor()
        val fill = introButtonFill(raw)

        println(
            "P tile, white on $fill: ${Color.White.contrastRatio(fill)} (raw green ${Color.White.contrastRatio(raw)})",
        )
        assertTrue(Color.White.contrastRatio(fill) >= MIN_TEXT)
        assertTrue(
            Color.White.contrastRatio(raw) < 3f,
            "the raw green is under 3 to 1, which is why the tile uses the fill",
        )
    }

    private companion object {
        const val MIN_TEXT = 4.5f
        const val MOCK_TOLERANCE = 0.06f
    }
}
