package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The colours of the bar in the home sheet are taj's own default field colours from one place
 * (`homeSearchPillColors`), the page surface with the page's text colour, and not the inverted set. The placeholder
 * and the microphone (drawn in the field's content colour) clear 4.5 to 1 against the bar's fill in light and dark
 * mode, in each of the four transport themes. The text is 4.54 to 1 in light mode, a bare pass, so the floor is
 * asserted at exactly 4.5 and the colour is not touched.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class HomeSearchPillColorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private data class Read(
        val pill: TextFieldColors,
        val defaults: TextFieldColors,
        val inverted: TextFieldColors,
        val onSurface: Color,
    )

    private val reads = mutableMapOf<KrailThemeStyle, Read>()

    private fun readAll(mode: ThemeMode) {
        composeRule.setContent {
            KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
                THEMES.forEach { style ->
                    val themeColor = remember(style) { mutableStateOf(style.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColor) {
                        reads[style] = Read(
                            pill = homeSearchPillColors(),
                            defaults = TextFieldDefaults.colors(),
                            inverted = TextFieldDefaults.invertedColors(),
                            onSurface = KrailTheme.colors.onSurface,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertContrast(style: KrailThemeStyle, mode: ThemeMode) {
        val colors = requireNotNull(reads[style]).pill
        val text = colors.contentColor.contrastRatio(colors.containerColor)
        val mic = colors.contentColor.contrastRatio(colors.containerColor)
        val placeholder = colors.placeholderColor.contrastRatio(colors.containerColor)
        assertTrue(text >= MIN_TEXT_CONTRAST, "$style $mode: the text is $text to 1 on the bar")
        assertTrue(mic >= MIN_TEXT_CONTRAST, "$style $mode: the microphone is $mic to 1 on the bar")
        assertTrue(placeholder >= MIN_TEXT_CONTRAST, "$style $mode: the placeholder is $placeholder to 1 on the bar")
    }

    @Test
    fun `in light mode the bar is taj's default, a white fill with dark text, in every theme`() {
        readAll(ThemeMode.LIGHT)

        THEMES.forEach { style ->
            val read = requireNotNull(reads[style])
            assertEquals(md_theme_light_surface, read.pill.containerColor, "$style fill")
            assertEquals(md_theme_light_onSurface, read.pill.contentColor, "$style content")
            assertEquals(md_theme_light_softLabel, read.pill.placeholderColor, "$style placeholder")
            assertEquals(read.defaults, read.pill, "$style is taj's default set")
            assertNotEquals(read.inverted.containerColor, read.pill.containerColor, "$style is not the inverted set")
            assertNotEquals(read.onSurface, read.pill.containerColor, "$style is not the black bar")
            assertContrast(style, ThemeMode.LIGHT)
        }
    }

    @Test
    fun `in dark mode the bar is taj's default, a dark fill with light text, in every theme`() {
        readAll(ThemeMode.DARK)

        THEMES.forEach { style ->
            val read = requireNotNull(reads[style])
            assertEquals(md_theme_dark_surface, read.pill.containerColor, "$style fill")
            assertEquals(md_theme_dark_onSurface, read.pill.contentColor, "$style content")
            assertEquals(md_theme_dark_softLabel, read.pill.placeholderColor, "$style placeholder")
            assertEquals(read.defaults, read.pill, "$style is taj's default set")
            assertNotEquals(read.inverted.containerColor, read.pill.containerColor, "$style is not the inverted set")
            assertNotEquals(read.onSurface, read.pill.containerColor, "$style is not the cream bar")
            assertContrast(style, ThemeMode.DARK)
        }
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
        val THEMES = listOf(KrailThemeStyle.Train, KrailThemeStyle.Metro, KrailThemeStyle.Bus, KrailThemeStyle.Ferry)
    }
}
