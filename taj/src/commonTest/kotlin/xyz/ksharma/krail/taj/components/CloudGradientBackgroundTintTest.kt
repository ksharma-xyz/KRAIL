package xyz.ksharma.krail.taj.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The cloud field's palette, decided by arithmetic rather than by looking at a device.
 *
 * The property that matters is simple and was the thing that broke: **a glow has to be lighter
 * than what it sits on.** In dark mode both blob tints were being pulled towards `surface`,
 * which is near black, so a saturated theme colour walked *down* towards black and the field
 * read as navy sludge instead of light. In light mode pulling towards surface is correct,
 * because there surface is white.
 *
 * Every theme style is checked in both modes, so adding a theme colour that breaks in one mode
 * fails here instead of on somebody's phone.
 */
class CloudGradientBackgroundTintTest {

    @Test
    fun darkMode_bothTintsAreLighterThanTheSurfaceTheySitOn() {
        val surfaceLuminance = md_theme_dark_surface.luminance()

        KrailThemeStyle.entries.forEach { style ->
            val (tintA, tintB) = tintsFor(style, darkMode = true)

            assertTrue(
                tintA.luminance() > surfaceLuminance,
                "${style.name}: tintA luminance ${tintA.luminance()} should exceed the dark " +
                    "surface's $surfaceLuminance. A tint darker than its background is a smear, " +
                    "not a glow.",
            )
            assertTrue(
                tintB.luminance() > surfaceLuminance,
                "${style.name}: tintB luminance ${tintB.luminance()} should exceed the dark " +
                    "surface's $surfaceLuminance.",
            )
        }
    }

    @Test
    fun darkMode_theFarTintIsLighterThanTheNearOne() {
        KrailThemeStyle.entries.forEach { style ->
            val (tintA, tintB) = tintsFor(style, darkMode = true)

            // B is pulled further towards the light end than A, so the field brightens outward
            // rather than banding.
            assertTrue(
                tintB.luminance() > tintA.luminance(),
                "${style.name}: tintB (${tintB.luminance()}) should be lighter than tintA " +
                    "(${tintA.luminance()}) in dark mode.",
            )
        }
    }

    @Test
    fun lightMode_bothTintsStayLighterThanTheThemeColourItself() {
        KrailThemeStyle.entries.forEach { style ->
            val themeColor = style.hexColorCode.hexToComposeColor()
            val (tintA, tintB) = tintsFor(style, darkMode = false)

            // Towards white, so both soften rather than intensify. This is what stops a
            // saturated theme colour reading as a hot spot on a white screen.
            assertTrue(
                tintA.luminance() > themeColor.luminance(),
                "${style.name}: tintA should be lighter than the raw theme colour in light mode.",
            )
            assertTrue(
                tintB.luminance() > tintA.luminance(),
                "${style.name}: tintB should be lighter than tintA in light mode.",
            )
        }
    }

    private fun tintsFor(style: KrailThemeStyle, darkMode: Boolean): Pair<Color, Color> {
        return cloudBlobTints(
            themeColor = style.hexColorCode.hexToComposeColor(),
            surface = if (darkMode) md_theme_dark_surface else md_theme_light_surface,
            onSurface = if (darkMode) md_theme_dark_onSurface else md_theme_light_onSurface,
            darkMode = darkMode,
        )
    }
}
