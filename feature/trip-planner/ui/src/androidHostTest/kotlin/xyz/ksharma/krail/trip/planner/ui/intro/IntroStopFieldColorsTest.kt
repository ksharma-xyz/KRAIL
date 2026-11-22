package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
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
 * The colours of the stop field are taj's own default field colours, from one place (`introStopFieldColors`): a white field
 * with dark text in light mode and a dark field with light text in dark mode, and not the inverted set. The text clears 4.5
 * to 1 against the field in both modes, and so does the placeholder the default gives.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroStopFieldColorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var colors: TextFieldColors? = null
    private var defaults: TextFieldColors? = null
    private var inverted: TextFieldColors? = null
    private var onSurface: Color = Color.Unspecified

    private fun read() {
        composeRule.setContent {
            PreviewTheme {
                colors = introStopFieldColors()
                defaults = TextFieldDefaults.colors()
                inverted = TextFieldDefaults.invertedColors()
                onSurface = KrailTheme.colors.onSurface
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertContrast(colors: TextFieldColors) {
        val text = colors.contentColor.contrastRatio(colors.containerColor)
        val placeholder = colors.placeholderColor.contrastRatio(colors.containerColor)
        assertTrue(text >= MIN_TEXT_CONTRAST, "the text is $text to 1 on the field")
        assertTrue(placeholder >= MIN_TEXT_CONTRAST, "the placeholder is $placeholder to 1 on the field")
    }

    @Test
    fun `in light mode the field is white with dark text, taj's default and not the inverted set`() {
        read()

        val field = requireNotNull(colors)
        assertEquals(md_theme_light_surface, field.containerColor)
        assertEquals(md_theme_light_onSurface, field.contentColor)
        assertEquals(md_theme_light_softLabel, field.placeholderColor)
        assertEquals(defaults, field)
        assertNotEquals(requireNotNull(inverted).containerColor, field.containerColor)
        assertNotEquals(onSurface, field.containerColor, "the field is the dark one of the inverted set")
        assertContrast(field)
    }

    @Test
    @Config(qualifiers = "+night")
    fun `in dark mode the field is dark with light text, taj's default and not the inverted set`() {
        read()

        val field = requireNotNull(colors)
        assertEquals(md_theme_dark_surface, field.containerColor)
        assertEquals(md_theme_dark_onSurface, field.contentColor)
        assertEquals(md_theme_dark_softLabel, field.placeholderColor)
        assertEquals(defaults, field)
        assertNotEquals(requireNotNull(inverted).containerColor, field.containerColor)
        assertNotEquals(onSurface, field.containerColor, "the field is the light one of the inverted set")
        assertContrast(field)
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
    }
}
